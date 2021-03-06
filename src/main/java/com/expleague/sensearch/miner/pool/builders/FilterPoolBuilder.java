package com.expleague.sensearch.miner.pool.builders;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.FilterMinerDocNum;
import com.expleague.sensearch.core.Annotations.RankFilterModel;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.TargetFS;
import com.expleague.sensearch.features.sets.TargetSet;
import com.expleague.sensearch.filter.AccumulatorFilterFeatureSet;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.index.plain.PlainPage;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class FilterPoolBuilder extends PoolBuilder<QueryAndResults> {

  /* Контракт на данные:
     никакие два запроса не повторяются!
  */

  private static final Logger LOG = Logger.getLogger(FilterPoolBuilder.class.getName());
  private final int filterDocNum;
  private int SAVE_SIZE = 5;

  private static final int FILTER_SIZE = 10;
  private final Index index;
  private final Trans model;

  @Inject
  public FilterPoolBuilder(
      Index index,
      @RankFilterModel Pair<Function, FeatureMeta[]> rankModel,
      @FilterMinerDocNum int filterDocNum) {
    super();
    this.index = index;
    this.model = (Trans) rankModel.getFirst();
    this.filterDocNum = filterDocNum;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(FilterPoolBuilder.class).build(Paths.get("./PoolData/filter/"), 1);
  }

  public void build(Path dataPath, int iteration) {
    LOG.info("FilterPool build start");
    long startTime = System.nanoTime();
    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
    AccumulatorFilterFeatureSet features = new AccumulatorFilterFeatureSet(index);
    TargetSet targetFeatures = new TargetFS();
    FeatureMeta[] metas = metaData(features, targetFeatures);
    Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

    AtomicInteger status = new AtomicInteger(0);
    AtomicInteger added = new AtomicInteger(0);

    QueryAndResults[] positiveExamples = readData(QueryAndResults.class, iteration, dataPath);
    Map<Query, List<PageAndWeight>> data =
        Arrays.stream(positiveExamples)
            .collect(
                Collectors.toMap(
                    qNr -> BaseQuery.create(qNr.getQuery(), index),
                    qNr -> Arrays.asList(qNr.getAnswers())));
    List<QueryAndResults> newData = Collections.synchronizedList(new ArrayList<>());

    data.entrySet()
        .parallelStream()
        .limit(200)
        .forEach(
            entry -> {
              Query query = entry.getKey();
              List<PageAndWeight> res = entry.getValue();
              if (status.get() % 100 == 0) {
                System.err.println(status.get() + " queries completed");
              }
              status.incrementAndGet();
              Map<Page, Features> allDocs;

              {
                AccumulatorFilterFeatureSet accumulatorFilterFs = new AccumulatorFilterFeatureSet(index);
                allDocs =
                    index.fetchDocuments(query, filterDocNum).entrySet().stream()
                        .collect(Collectors.toMap(Entry::getKey, curEntry -> {
                          Page page = curEntry.getKey();
                          Features pageFeatures = curEntry.getValue();
                          accumulatorFilterFs.accept(new QURLItem(page, query));
                          accumulatorFilterFs.withFilterDistFeatures(pageFeatures);
                          Vec featureVec = accumulatorFilterFs.advance();
                          return new FeaturesImpl(accumulatorFilterFs, featureVec);
                        }));
              }
              res.forEach(
                  pNw -> {
                    Page page = index.page(pNw.getUri());
                    if (page != PlainPage.EMPTY_PAGE) {
                      double target = 0;
                      if (pNw.getWeight() > 0) {
                        target = 1;
                      }

                      Features feat = ((PlainIndex) index).filterFeatures(query, page.uri());
                      synchronized (poolBuilder) {
                        poolBuilder.accept(new QURLItem(page, query));
                        double finalTarget = target;
                        poolBuilder
                            .features()
                            .forEach(
                                fs -> {
                                  if (fs instanceof TargetFS) {
                                    ((TargetFS) fs).acceptTargetValue(finalTarget);
                                  } else if (fs instanceof AccumulatorFilterFeatureSet) {
                                    ((AccumulatorFilterFeatureSet) fs).withFilterDistFeatures(feat);
                                  }
                                });
                        poolBuilder.advance();
                      }
                      allDocs.remove(page);
                    }
                  });

              List<PageAndWeight> newRes = new ArrayList<>(res);
              final int[] tmpAdded = {0};
              int[] cnt = {0};
              allDocs
                  .entrySet()
                  .stream()
                  .sorted(
                      Comparator.comparingDouble(e -> -model.trans(e.getValue().features()).get(0)))
                  .forEach(
                      e -> {
                        Page page = e.getKey();
                        Features feat = e.getValue();
                        if (page == PlainPage.EMPTY_PAGE) {
                          return;
                        }

                        AccumulatorFilterFeatureSet accumulatorFilterFs = new AccumulatorFilterFeatureSet(index);
                        accumulatorFilterFs.accept(new QURLItem(page, query));
                        accumulatorFilterFs.withFilterDistFeatures(feat);
                        final Vec vec = VecTools.concat(accumulatorFilterFs.advance(), new ArrayVec(0.0));

                        if (cnt[0] < FILTER_SIZE) {
                          synchronized (poolBuilder) {
                            poolBuilder.accept(new QURLItem(page, query), vec, metas);
                          }
                          if (tmpAdded[0] < SAVE_SIZE) {
                            tmpAdded[0]++;
                            newRes.add(new PageAndWeight(page.uri().toString(), 0));
                          }
                          cnt[0]++;
                        }
                      });
              added.addAndGet(tmpAdded[0]);
              newData.add(new QueryAndResults(query.text(), newRes));
            });

    saveNewIterationData(dataPath, newData.toArray(new QueryAndResults[0]), iteration + 1);

    LOG.info(String.format("Memorized %d new results\n", added.get()));

    Pool<QURLItem> pool = poolBuilder.create();
    try {
      DataTools.writePoolTo(pool, Files.newBufferedWriter(dataPath.resolve("filter.pool")));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    LOG.info(
        String.format(
            "FilterPool build finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
