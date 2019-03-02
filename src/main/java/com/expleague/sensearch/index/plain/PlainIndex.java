package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.core.IdUtils;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.filter.FilterFeatures;
import com.expleague.sensearch.filter.Filter;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.metrics.LSHSynonymsMetric;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import com.google.common.collect.Streams;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;

@Singleton
public class PlainIndex implements Index {

  // TODO: !index version!
  public static final int VERSION = 13;

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB

  private static final Options DEFAULT_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .createIfMissing(false)
          .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions().fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int SYNONYMS_COUNT = 50;

  private final Map<CharSeq, Term> wordToTerms = new HashMap<>();
  private final TLongObjectMap<Term> idToTerm = new TLongObjectHashMap<>();

  private final DB termStatisticsBase;
  private final DB pageBase;
  private final DB termBase;

  private final DB suggestUnigramDb;
  private final DB suggestMultigramDb;

  private final DB uriMappingDb;

  private final double averagePageSize;
  private double averageSectionTitleSize;
  private int sectionTitlesCount;
  private double averageLinkTargetTitleWordCount;
  private int linksCount;

  private final int indexSize;
  private final int vocabularySize;

  private final Embedding embedding;
  private final Filter filter;
  private final FilterFeatures filterFeatures = new FilterFeatures();
  private final LSHSynonymsMetric lshSynonymsMetric;
  private final Tokenizer tokenizer;

  private TermStatistics lastTermStatistics;

  private SuggestInformationLoader suggestLoader;

  @Override
  public void close() throws Exception {
    embedding.close();
    pageBase.close();
    termBase.close();
    termStatisticsBase.close();
    suggestUnigramDb.close();
    suggestMultigramDb.close();
  }

  @Inject
  public PlainIndex(@IndexRoot Path indexRoot, Embedding embedding, Filter filter)
      throws IOException {

    this.embedding = embedding;
    this.filter = filter;

    LOG.info("Loading PlainIndex...");
    long startTime = System.nanoTime();

    Path embeddingPath = indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT);
    lshSynonymsMetric =
        new LSHSynonymsMetric(embeddingPath.resolve(PlainIndexBuilder.LSH_METRIC_ROOT));

    termStatisticsBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    pageBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.PAGE_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    termBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    suggestUnigramDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.SUGGEST_UNIGRAM_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    suggestMultigramDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.SUGGEST_MULTIGRAMS_ROOT).toFile(),
            DEFAULT_DB_OPTIONS);

    uriMappingDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.URI_MAPPING_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    tokenizer = new TokenizerImpl();

    IndexUnits.IndexMeta indexMeta =
        IndexUnits.IndexMeta.parseFrom(
            Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_META_FILE)));

    if (indexMeta.getVersion() != VERSION) {
      String errorMessage =
          String.format(
              "Built index has version %d while code version is %d",
              indexMeta.getVersion(), VERSION);
      LOG.fatal(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }

    averagePageSize = indexMeta.getAveragePageSize();
    indexSize = indexMeta.getPagesCount();
    vocabularySize = indexMeta.getVocabularySize();
    linksCount = indexMeta.getLinksCount();
    averageLinkTargetTitleWordCount = indexMeta.getAverageLinkTargetTitleWordCount();
    sectionTitlesCount = indexMeta.getSectionTitlesCount();
    averageSectionTitleSize = indexMeta.getAverageSectionTitleSize();

    DBIterator termIterator = termBase.iterator();
    termIterator.seekToFirst();
    termIterator.forEachRemaining(
        item -> {
          try {
            IndexUnits.Term protoTerm = IndexUnits.Term.parseFrom(item.getValue());
            final CharSeq word = CharSeq.intern(protoTerm.getText());

            if (wordToTerms.containsKey(word)) {
              return;
            }

            PartOfSpeech pos =
                protoTerm.getPartOfSpeech() == IndexUnits.Term.PartOfSpeech.UNKNOWN
                    ? null
                    : PartOfSpeech.valueOf(protoTerm.getPartOfSpeech().name());

            final IndexTerm lemmaTerm;

            final long lemmaId = protoTerm.getLemmaId();
            if (lemmaId == -1) {
              lemmaTerm = null;
            } else {
              if (idToTerm.containsKey(lemmaId)) {
                lemmaTerm = (IndexTerm) idToTerm.get(lemmaId);
              } else {
                CharSeq lemmaText =
                    CharSeq.intern(
                        IndexUnits.Term.parseFrom(termBase.get(Longs.toByteArray(lemmaId)))
                            .getText());

                lemmaTerm = new IndexTerm(this, lemmaText, lemmaId, null, pos);
                idToTerm.put(lemmaId, lemmaTerm);
                wordToTerms.put(lemmaText, lemmaTerm);
              }
            }

            IndexTerm term = new IndexTerm(this, word, protoTerm.getId(), lemmaTerm, pos);
            idToTerm.put(protoTerm.getId(), term);
            wordToTerms.put(word, term);

          } catch (InvalidProtocolBufferException e) {
            LOG.fatal("Invalid protobuf for term with id " + Longs.fromByteArray(item.getKey()));
            throw new RuntimeException(e);
          }
        });

    LOG.info(
        String.format("PlainIndex loaded in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  static boolean isSectionId(long id) {
    return id <= 0;
  }

  static boolean isWordId(long id) {
    return id > 0;
  }

  IndexUnits.Page protoPageLoad(long id) throws InvalidProtocolBufferException {
    byte[] pageBytes = pageBase.get(Longs.toByteArray(id));
    if (pageBytes == null) {
      throw new NoSuchElementException(
          String.format("No page with id [ %d ] was found in the index!", id));
    }

    return IndexUnits.Page.parseFrom(pageBytes);
  }

  @Override
  public Stream<Term> mostFrequentNeighbours(Term term) {
    try {
      return termStatistics(((IndexTerm) term).id())
          .getBigramFrequencyList()
          .stream()
          .mapToLong(TermFrequency::getTermId)
          .mapToObj(idToTerm::get);
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(
          String.format(
              "Encountered invalid protobuf in statistics base for word with id [%d] and content [%s]",
              ((IndexTerm) term).id(), term.text()));
      return Stream.empty();
    }
  }

  @Override
  public Page page(URI uri) {
    byte[] result = uriMappingDb.get(uri.toASCIIString().getBytes());
    if (result == null) {
      return PlainPage.EMPTY_PAGE;
    }

    try {
      return PlainPage.create(IndexUnits.UriPageMapping.parseFrom(result).getPageId(), this);
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(e);
      return PlainPage.EMPTY_PAGE;
    }
  }

  @Override
  public Vec vecByTerms(List<Term> terms) {
    final ArrayVec answerVec = new ArrayVec(embedding.dim());
    long cnt =
        terms
            .stream()
            .mapToLong(t -> ((IndexTerm) t).id())
            .mapToObj(embedding::vec)
            .filter(Objects::nonNull)
            .peek(v -> VecTools.append(answerVec, v))
            .count();
    if (cnt > 0) {
      answerVec.scale(1. / ((double) cnt));
    }
    return answerVec;
  }

  @Override
  public Stream<Page> allDocuments() {
    DBIterator iterator = pageBase.iterator();
    iterator.seekToFirst();
    return Streams.stream(iterator)
        .map(entry -> (Page) PlainPage.create(Longs.fromByteArray(entry.getKey()), this))
        .filter(page -> !page.isRoot());
  }

  @Override
  public Term term(CharSequence seq) {
    final CharSequence normalized = CharSeqTools.toLowerCase(CharSeqTools.trim(seq));
    return wordToTerms.get(CharSeq.create(normalized));
  }

  @Override
  public Stream<CharSequence> sentences(CharSequence sequence) {
    return tokenizer.toSentences(sequence);
  }

  @Override
  public Stream<Term> parse(CharSequence sequence) {
    return tokenizer.parseTextToWords(sequence).map(this::term).filter(Objects::nonNull);
  }

  @Override
  public int size() {
    return indexSize;
  }

  @Override
  public double averagePageSize() {
    return averagePageSize;
  }

  @Override
  public double averageSectionTitleSize() {
    return averageSectionTitleSize;
  }

  @Override
  public double averageLinkTargetTitleWordCount() {
    return averageLinkTargetTitleWordCount;
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }

  Stream<Term> synonyms(Term term) {
    Vec termVec = embedding.vec(((IndexTerm) term).id());
    if (termVec == null) {
      return Stream.empty();
    }

    /*Set<Long> LSHIds =
        filter
            .filtrate(termVec, SYNONYMS_COUNT, PlainIndex::isWordId)
            .boxed()
            .collect(Collectors.toSet());

    double result = lshSynonymsMetric.calc(((IndexTerm) term).id(), LSHIds);

    LOG.info("LSHSynonymsMetric: " + result);*/

    return filter
        .filtrate(termVec, PlainIndex::isWordId, SYNONYMS_COUNT)
        .map(c -> idToTerm.get(c.getId()));
  }

  @Override
  public Map<Page, Features> fetchDocuments(Query query, int num) {
    final Vec qVec = vecByTerms(query.terms());
    TLongObjectMap<List<Candidate>> pageIdToCandidatesMap = new TLongObjectHashMap<>();
    filter
        .filtrate(qVec, PlainIndex::isSectionId, 0.5)
        .limit(num)
        .forEach(
            candidate -> {
              long pageId = candidate.getPageId();
              if (!pageIdToCandidatesMap.containsKey(pageId)) {
                pageIdToCandidatesMap.put(pageId, new ArrayList<>());
              }
              pageIdToCandidatesMap.get(pageId).add(candidate);
            });
    Map<Page, Features> allFilterFeatures = new HashMap<>();
    pageIdToCandidatesMap.forEachEntry(
        (pageId, candidates) -> {
          Page page = PlainPage.create(pageId, this);
          filterFeatures.accept(new QURLItem(page, query));
          candidates.forEach(
              candidate -> {
                long id = candidate.getId();
                if (IdUtils.isSecTitleId(id)) {
                  filterFeatures.withTitle(candidate.getDist());
                } else if (IdUtils.isSecTextId(id)) {
                  filterFeatures.withBody(candidate.getDist());
                } else if (IdUtils.isLinkId(id)) {
                  filterFeatures.withLink(candidate.getDist());
                }
              });
          Vec vec = filterFeatures.advance();
          allFilterFeatures.put(page, new FeaturesImpl(filterFeatures, vec));
          return true;
        });
    return allFilterFeatures;
  }

  int documentFrequency(Term term) {
    try {
      return termStatistics(((IndexTerm) term).id()).getDocumentFrequency();
    } catch (DBException | NoSuchElementException | NullPointerException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  int documentLemmaFrequency(IndexTerm term) {
    try {
      return termStatistics((term).id()).getDocumentLemmaFrequency();
    } catch (DBException | NoSuchElementException | NullPointerException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  int termFrequency(Term term) {
    try {
      return (int) termStatistics(((IndexTerm) term).id()).getTermFrequency();
    } catch (DBException | NoSuchElementException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  TermStatistics termStatistics(long termId) throws InvalidProtocolBufferException {
    if (lastTermStatistics == null || lastTermStatistics.getTermId() != termId) {
      lastTermStatistics =
          TermStatistics.parseFrom(
              termStatisticsBase.get(Longs.toByteArray(termId), DEFAULT_READ_OPTIONS));
    }
    return lastTermStatistics;
  }

  // однопоточно
  @Override
  public SuggestInformationLoader getSuggestInformation() {
    if (suggestLoader == null) {
      suggestLoader = new SuggestInformationLoader(suggestUnigramDb, suggestMultigramDb, idToTerm);
    }
    return suggestLoader;
  }
}
