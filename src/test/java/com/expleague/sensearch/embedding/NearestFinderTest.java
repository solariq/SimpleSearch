// package com.expleague.sensearch.embedding;
//
// import com.expleague.commons.math.vectors.Vec;
// import com.expleague.commons.math.vectors.VecTools;
// import com.expleague.sensearch.ConfigImpl;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import com.expleague.sensearch.index.plain.EmbeddingImpl;
// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;
//
// public class NearestFinderTest {
//
//  private static final int numberOfNeighbors = 50;
//
//  private static final Map<String, String[]> tests;
//
//  static {
//    Map<String, String[]> map = new HashMap<>();
//    map.put("женщина", new String[]{"девушка", "девочка", "молодая", "красивая", "мать"});
//    map.put("вода", new String[]{"пресная", "лёд", "воздух", "солёная", "питьевая"});
//    map.put("школа",
//        new String[]{"гимназия", "начальная", "общеобразовательная", "музыкальная",
// "спортивная"});
//    tests = map;
//  }
//
//  private EmbeddingImpl embedding;
//
//  @Before
//  public void initEmbedding() throws IOException {
//    byte[] jsonData = Files.readAllBytes(Paths.get("./config.json"));
//    ConfigImpl config = new ObjectMapper().readValue(jsonData, ConfigImpl.class);
//    embedding = new EmbeddingImpl(config);
//    Vec v = VecTools.append(VecTools.subtract(embedding.vec("король"),
// embedding.vec("мужчина")), embedding.vec("женщина"));
//    for (String nearestWord : embedding.getNearestWords(v, 50)) {
//      System.out.println(nearestWord);
//    }
//
//
//  }
//
//  private void nearestFinderTest() {
//    for (Map.Entry<String, String[]> entry : tests.entrySet()) {
//      List<String> nearest = embedding
//          .getNearestWords(embedding.vec(entry.getKey()), numberOfNeighbors);
//      for (String neighbor : entry.getValue()) {
//        Assert.assertTrue(nearest.contains(neighbor));
//      }
//    }
//  }
//
//  @Test
//  public void euclideanTest() {
//    embedding.switchMeasureToEuclidean();
//    nearestFinderTest();
//  }
//
//  @Test
//  public void cosineTest() {
//    embedding.switchMeasureToCosine();
//    nearestFinderTest();
//  }
// }
