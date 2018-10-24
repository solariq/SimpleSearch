package components.embedding;

import components.embedding.impl.EmbeddingImpl;
import components.embedding.impl.NearestFinderImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class NearestFinderTest {

    private static final int numberOfNeighbors = 50;

    private static final HashMap<String, String[]> tests;
    static {
        HashMap<String, String[]> hashMap = new HashMap<>();
        hashMap.put("женщина", new String[]{"девушка", "девочка", "молодая", "красивая", "мать"});
        hashMap.put("вода", new String[]{"пресная", "лёд", "дистиллированная", "солёная", "питьевая"});
        hashMap.put("школа", new String[]{"гимназия", "начальная", "общеобразовательная", "музыкальная", "спортивная"});
        tests = hashMap;
    }

    @Test
    public void nearestFinderTest() {
        Embedding embedding = EmbeddingImpl.getInstance();
        NearestFinder nearestFinder = new NearestFinderImpl();
        for (HashMap.Entry<String, String[]> e : tests.entrySet()) {
            for (String neighbor : e.getValue()) {
                Assert.assertTrue(nearestFinder.getNearestWords(embedding.getVec(e.getKey()), numberOfNeighbors).contains(neighbor));
            }

        }
    }
}
