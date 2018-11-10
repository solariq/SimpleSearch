package com.expleague.sensearch.ranking;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import java.util.Comparator;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Point-wise ranker
 */
public class RankingPhase implements SearchPhase {
  private PointWiseRanker ranker;
  private final int pageSize;
  public RankingPhase(PointWiseRanker ranker, int pageSize) {
    this.ranker = ranker;
    this.pageSize = pageSize;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    final int pageNo = whiteboard.pageNo();
    whiteboard.putResults(
      whiteboard.textFeatures()
          .map(p -> Pair.of(p.getLeft(), p.getRight().fuzzy()))
          .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getRight).reversed())
          .map(Pair::getLeft)
          .skip(pageNo * pageSize)
          .limit(pageSize)
          .toArray(Page[]::new)
    );
  }
}