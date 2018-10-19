package components.index;

import components.crawler.Crawler;
import components.crawler.document.CrawlerDocument;
import components.index.plain.PlainIndexBuilder;
import components.query.BaseQuery;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by sandulmv on 19.10.18.
 */
public class MinimalFunctionalityPlainIndexTest {

  private static final String CONTENT_FILE = "content";
  private static final String META_FILE = "meta";

  private static final Logger LOG =
      Logger.getLogger(MinimalFunctionalityPlainIndexTest.class.getName());

  private static final Map<String, String> DOCUMENTS_AND_TITLES = new HashMap<>();

  // init documents for crawler
  static {

    DOCUMENTS_AND_TITLES.put(
        "Радиожираф",
        "-- Как живете караси?"
            + "-- Ничего себе, мерси..."
    );

    DOCUMENTS_AND_TITLES.put(
        "Откровение Иоанна Богослова",
        "Знаю дела твои, и труд твой, и терпение твое, и то, что ты не можешь сносить "
            + "развратных, и испытал тех, которые называют себя Апостолами, а они не таковы, и "
            + "нашел, что они лжецы..."
    );

    DOCUMENTS_AND_TITLES.put(
        "Часть первая. Мусорщик",
        "Баки были ржавые, помятые, с отставшими крышками. "
            + "Из-под крышек торчали обрывки газет, свешивалась картофельная шелуха. "
            + "Это было похоже на пасть неопрятного, неразборчивого в еде пеликана..."
    );
  }

  private static class TextDocument implements CrawlerDocument {

    private final String title;
    private final String content;

    TextDocument(String title, String content) {
      this.title = title;
      this.content = content;
    }

    @Override
    public CharSequence getContent() {
      return content;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public Long getID() {
      throw new UnsupportedOperationException();
    }
  }

  private static class LocalCrawler implements Crawler {

    private List<CrawlerDocument> crawledDocuments;

    LocalCrawler() {
      crawledDocuments = new LinkedList<>();
      for (Map.Entry<String, String> docAndTitle : DOCUMENTS_AND_TITLES.entrySet()) {
        crawledDocuments.add(new TextDocument(docAndTitle.getKey(), docAndTitle.getValue()));
      }
    }

    @Override
    public Stream<CrawlerDocument> makeStream() {
      return crawledDocuments.stream();
    }
  }

  private Index plainIndex;
  private Path indexRoot;

  @Before
  public void initIndex() throws Exception {
    indexRoot = Files.createTempDirectory(Paths.get(System.getProperty("user.dir")), "tmp");
    LOG.info(String.format("Will use the following path as index root: %s",
        indexRoot.toAbsolutePath().toString()));

    Crawler localCrawler = new LocalCrawler();
    plainIndex = new PlainIndexBuilder(indexRoot).buildIndex(localCrawler.makeStream());
  }

  @Test
  public void indexStructureTest() throws Exception {
    int documentsCount = DOCUMENTS_AND_TITLES.size();
    Assert.assertTrue(Files.exists(indexRoot));

    List<Path> indexEntries = Files.list(indexRoot).collect(Collectors.toList());
    Assert.assertEquals(indexEntries.size(), documentsCount);

    for (Path indexEntry : indexEntries) {
      Assert.assertTrue(Files.exists(indexEntry.resolve(CONTENT_FILE)));
      Assert.assertTrue(Files.exists(indexEntry.resolve(META_FILE)));
    }
  }

  @Test
  public void indexFunctionalityTest() throws Exception {
    plainIndex.fetchDocuments(new BaseQuery("empty")).forEach(
      doc -> {
        Assert.assertTrue(DOCUMENTS_AND_TITLES.containsKey(doc.getTitle().toString()));
        Assert.assertEquals(
            DOCUMENTS_AND_TITLES.get(doc.getTitle().toString()),
            doc.getContent().toString()
        );
      }
    );
  }

  @After
  public void cleanup() throws Exception {
    FileUtils.deleteDirectory(indexRoot.toFile());
  }

}