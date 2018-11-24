package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.ToIntFunction;

public class EmbeddingBuilder {

  public static final String VECS_ROOT = "vecs";
  public static final String LSH_ROOT = "lsh";
  public static final String RAND_VECS = "rand";

  private static final int MAX_BATCH_SIZE = 100;

  public static final int TABLES_NUMBER = 10;
  public static final int TUPLE_SIZE = 12;

  private static final Options DB_OPTIONS = new Options()
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
          .sync(true);
          //.snapshot(false);

  private DB vecDB;
  private WriteBatch batch = null;
  private int batchSize = 0;

  private TLongList[] tables = new TLongArrayList[(1 << TUPLE_SIZE) * TABLES_NUMBER];
  private DB tablesDB;
  private ToIntFunction<Vec>[] hashFuncs;

  EmbeddingBuilder(Path embeddingPath) throws IOException {
    vecDB = JniDBFactory.factory.open(embeddingPath.resolve(VECS_ROOT).toFile(), DB_OPTIONS);
    tablesDB = JniDBFactory.factory.open(embeddingPath.resolve(LSH_ROOT).toFile(), DB_OPTIONS);

    hashFuncs = new ToIntFunction[TABLES_NUMBER];

    try (Writer output =
        new OutputStreamWriter(
            new FileOutputStream(
                embeddingPath.resolve(RAND_VECS).toFile()
            )
        )
    ) {
      Random random = new Random();
      for (int i = 0; i < hashFuncs.length; i++) {

        Vec[] randVecs = new Vec[TUPLE_SIZE];
        for (int j = 0; j < randVecs.length; j++) {
          double[] randCoords = new double[PlainIndexBuilder.VEC_SIZE];
          for (int k = 0; k < randCoords.length; k++) {
            randCoords[k] = random.nextDouble();
            output.write(randCoords[k] + " ");
          }
          randVecs[j] = new ArrayVec(randCoords);
          output.write("\n");
        }

        final int hashNum = i;
        hashFuncs[i] = (vec) -> {

          boolean[] mask = new boolean[TUPLE_SIZE];
          for (int j = 0; j < mask.length; j++) {
            mask[j] = VecTools.multiply(vec, randVecs[j]) >= 0;
          }

          int hash = (1 << TUPLE_SIZE) * hashNum;
          for (int j = 0; j < mask.length; j++) {
            if (mask[j]) {
              hash += 1 << j;
            }
          }

          return hash;
        };
      }
    }
  }

  private void addToTables(long id, Vec vec) {
    for (ToIntFunction<Vec> hashFunc : hashFuncs) {
      int bucketIndex = hashFunc.applyAsInt(vec);
      tables[bucketIndex].add(id);
    }
  }

  private void check(DB db) {
    if (batch == null) {
      batch = db.createWriteBatch();
    }
    if (batchSize > MAX_BATCH_SIZE) {
      db.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = db.createWriteBatch();
    }
  }

  void add(long id, Vec vec) {
    addToTables(id, vec);
    check(vecDB);
    batch.put(Longs.toByteArray(id), ByteTools.toBytes(vec));
  }

  void addAll(TLongObjectMap<Vec> vecs) {
    vecs.forEachEntry((id, vec) -> {
      add(id, vec);
      return true;
    });
  }

  private void addToTablesDB(int bucket, long[] ids) {
    check(tablesDB);
    batch.put(Ints.toByteArray(bucket), ByteTools.toBytes(ids));
  }

  void build() throws IOException {
    if (batchSize > 0) {
      vecDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }
    vecDB.close();

    for (int i = 0; i < tables.length; i++) {
      addToTablesDB(i, tables[i].toArray());
    }
    if (batchSize > 0) {
      tablesDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }
    tablesDB.close();
  }
}