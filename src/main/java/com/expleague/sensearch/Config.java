package com.expleague.sensearch;

import java.nio.file.Path;

public interface Config {

  Path getIndexRoot();

  String getWebRoot();

  Path getMyStem();

  Path getPathToZIP();

  Path getEmbeddingVectors();

  Path getPathToMetrics();

  int getPageSize();

  default boolean getBuildIndexFlag() {
    return false;
  }

  default boolean getTrainEmbeddingFlag() {
    return false;
  }

  default boolean getLshNearestFlag() {
    return true;
  }

  int maxFilterItems();

  int filterMinerDocNum();

  int filterRankDocNum();

  Path getModelPath();

  Path getFilterModelPath();

  Path getSnippetModelPath();

  Path getGroundTruthPath();
}
