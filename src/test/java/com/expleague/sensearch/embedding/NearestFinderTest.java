package com.expleague.sensearch.embedding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expleague.sensearch.core.impl.EmbeddingImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NearestFinderTest {

  private static final int numberOfNeighbors = 50;

  private static final Map<String, String[]> tests;

  static {
    Map<String, String[]> map = new HashMap<>();
    map.put("женщина", new String[]{"девушка", "девочка", "молодая", "красивая", "мать"});
    map.put("вода", new String[]{"пресная", "лёд", "воздух", "солёная", "питьевая"});
    map.put("школа",
        new String[]{"гимназия", "начальная", "общеобразовательная", "музыкальная", "спортивная"});
    tests = map;
  }

  private EmbeddingImpl embedding;

  @Before
  public void initEmbedding() {
    embedding = EmbeddingImpl.getInstance();
  }

  private void nearestFinderTest() {
    for (Map.Entry<String, String[]> entry : tests.entrySet()) {
      List<String> nearest = embedding.getNearestWords(embedding.getVec(entry.getKey()), numberOfNeighbors);
      for (String neighbor : entry.getValue()) {
        Assert.assertTrue(nearest.contains(neighbor));
      }
    }
  }

  @Test
  public void euclideanTest() {
    embedding.switchMeasureToEuclidean();
    nearestFinderTest();
  }

  @Test
  public void cosineTest() {
    embedding.switchMeasureToCosine();
    nearestFinderTest();
  }
}