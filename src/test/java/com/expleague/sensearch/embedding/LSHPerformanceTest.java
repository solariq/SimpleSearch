package com.expleague.sensearch.embedding;

import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.plain.Candidate;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
import com.expleague.sensearch.utils.TestConfigImpl;
import gnu.trove.list.TLongList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

public class LSHPerformanceTest {
    private final int attempts = 10;
    private final int num = 100;

    private void baseTest(EmbeddingImpl embedding) {
        long[] ids = Arrays.stream(embedding.allIds).filter(id -> id > 0).toArray();

        double result = 0;
        double timeLSH = 0;
        double timeNaive = 0;
        LongPredicate p = id -> id > 0;
        Random random = new Random();
        for (int i = 0; i < attempts; i++) {
            long id = ids[random.nextInt(ids.length)];
            embedding.setLSHFlag(true);
            TLongSet lshResult;
            {
                long start = System.currentTimeMillis();
                Stream<Candidate> candidates = embedding.nearest(embedding.vec(id), p, num);
                long end = System.currentTimeMillis();
                timeLSH += (end - start) / 1e3;
                lshResult = new TLongHashSet(candidates.mapToLong(Candidate::getId).toArray());
            }

            embedding.setLSHFlag(false);
            TLongSet trueResult;
            {
                long start = System.currentTimeMillis();
                trueResult = new TLongHashSet(embedding.nearest(embedding.vec(id), p, num).mapToLong(Candidate::getId).toArray());
                long end = System.currentTimeMillis();
                timeNaive += (end - start) / 1e3;
            }
            lshResult.retainAll(trueResult);

            result += lshResult.size() / ((double) trueResult.size());
        }
        result /= attempts;
        timeLSH /= attempts;
        timeNaive /= attempts;
        System.out.println("Average result: " + result + "\nAverage time LSH: " + timeLSH + "\nAverage time naive: " + timeNaive);
    }

    @Test
    public void test() throws IOException {
        Path embeddingPath = Paths.get("/Users/solar/data/search/WikiDocs/IndexTmp/embedding");

        long cacheSize = 16 * (1 << 20);
        Options dbOptions = new Options().cacheSize(cacheSize);
        DB vecDB = JniDBFactory.factory.open(embeddingPath.resolve("vecs").toFile(), dbOptions);

        EmbeddingImpl embedding = new EmbeddingImpl(true, vecDB);
        baseTest(embedding);
    }
}
