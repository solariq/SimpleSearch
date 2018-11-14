package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.core.impl.EmbeddingImpl;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.statistics.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * Created by sandulmv on 17.10.18. Should be replaced with interface or abstract class?
 * Straightforward index builder that saves all documents received from Crawler in separate text
 * documents. Saves only title and content of each document
 */
public class PlainIndexBuilder {

  static final String CONTENT_FILE = "content";
  static final String META_FILE = "meta";

  private static final Random RNG = new Random();

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());
  private final Config config;
  private final Embedding embedding;
  private Path indexRoot;

  public PlainIndexBuilder(Config config) throws RuntimeException, IOException {
    this.config = config;
    this.indexRoot = config.getTemporaryIndex();
    embedding = new EmbeddingImpl(config);

    Files.createDirectories(indexRoot);
    FileUtils.deleteDirectory(indexRoot.toFile());

    if (!isPathSuitableForIndex(indexRoot)) {
      String errMsg = String.format(
          "Index already exists by the given path %s",
          indexRoot.toAbsolutePath().toString()
      );
      LOG.severe(errMsg);
      throw new RuntimeException(errMsg);
    }
    try {
      if (!Files.exists(indexRoot)) {
        Files.createDirectories(indexRoot);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          String.format(
              "Failed to create index by given path %s",
              indexRoot.toAbsolutePath().toString()
          ), e
      );
    }
  }

  private static boolean isPathSuitableForIndex(Path indexRoot) {
    try {
      return !Files.exists(indexRoot) || Files.list(indexRoot).count() == 0;
    } catch (IOException e) {
      throw new RuntimeException(
          String.format(
              "Failed to check whether given path %s is suitable for creating the index",
              indexRoot.toAbsolutePath().toString()
          ), e);
    }
  }

  private static Path createNewIndexEntryRoot(Path indexRoot) {
    while (true) {
      long pageId = RNG.nextLong();
      pageId = pageId == Long.MIN_VALUE ? 0 : Math.abs(pageId);
      Path newEntry = indexRoot.resolve(Long.toString(pageId));
      if (!Files.exists(newEntry)) {
        try {
          Files.createDirectory(newEntry);
          return newEntry;
        } catch (IOException e) {
          LOG.severe(String.format("Failed to create new index entry %s",
              newEntry.toAbsolutePath().toString()));
        }
      }
    }
  }

  private static void flushNewIndexEntry(Path indexRoot, CrawlerDocument parsedDocument) {
    Path newIndexEntryPath = createNewIndexEntryRoot(indexRoot);

    try (
        BufferedWriter contentWriter = new BufferedWriter(
            new OutputStreamWriter(
                Files.newOutputStream(newIndexEntryPath.resolve(CONTENT_FILE))
            )
        );

        BufferedWriter titleWriter = new BufferedWriter(
            new OutputStreamWriter(
                Files.newOutputStream(newIndexEntryPath.resolve(META_FILE))
            )
        )
    ) {
      contentWriter.write(parsedDocument.getContent().toString());
      titleWriter.write(parsedDocument.getTitle());
    } catch (Exception e) {
      LOG.warning
          (String.format("Failed to flush new index entry! Cause: %s",
              e.toString())
          );
      try {
        Files.deleteIfExists(newIndexEntryPath);
      } catch (IOException ioe) {
        LOG.severe(String.format("Failed to remove directory: %s. Cause: %s",
            newIndexEntryPath.toAbsolutePath(),
            ioe.toString())
        );
      }
    }
  }

  private static void incInMap(Map<String, Integer> m, String key) {
    Integer currVal = m.get(key);

    if (currVal == null) {
      m.put(key, 0);
    } else {
      m.put(key, currVal + 1);
    }
  }

  private void flushBigrams(String title, TreeMap<String, Integer> map) {
    String t = title.toLowerCase();
    String[] words = t.split("[^a-zA-Zа-яА-ЯЁё]+");
    for (int i = 0; i < words.length - 1; i++) {
      if (words[i].isEmpty()) {
        continue;
      }
      String bigram = words[i] + " " + words[i + 1];
      incInMap(map, bigram);
    }
  }

  public Index buildIndex(Stream<CrawlerDocument> parsedDocumentsStream) throws IOException {
    Path bigramPath = indexRoot.getParent().resolve(Paths.get(config.getTemporaryBigrams()));
    Files.createDirectories(bigramPath);
    TreeMap<String, Integer> result = new TreeMap<>();

    Stats stats = new Stats();

    Set<String> allTitles = new HashSet<>();

    parsedDocumentsStream.forEach(
        doc -> {
          allTitles.add(doc.getTitle());
          flushBigrams(doc.getTitle(), result);
          flushNewIndexEntry(this.indexRoot, doc);
          //stats.acceptDocument(doc);
        }
    );

    ObjectMapper mapper = new ObjectMapper();
    mapper.writeValue(bigramPath.resolve(config.getBigramsFileName()).toFile(), result);

    stats.writeToFile(config.getStatisticsFileName());

    try {
      return new PlainIndex(indexRoot, embedding, allTitles);
    } catch (IOException e) {
      try {
        FileUtils.deleteDirectory(indexRoot.toFile());
      } catch (IOException ignore) {
        LOG.severe(
            String.format(
                "Index failure: failed to cleanup index root: %s. Cause: %s",
                indexRoot,
                ignore.toString()
            )
        );
      }
      throw new IOException("Failed to create index!", e);
    }
  }
}
