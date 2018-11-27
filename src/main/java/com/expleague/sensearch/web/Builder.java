package com.expleague.sensearch.web;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.metrics.Metric;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.metrics.WebCrawler;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.google.inject.Singleton;
import java.io.IOException;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

@Singleton
public class Builder {

  private Index index;
  private SenSeArch searcher;
  private Crawler crawler;
  private BigramsBasedSuggestor bigramsBasedSuggestor;
  private Config config;
  private Lemmer lemmer;
  private Metric metric;

  @Inject
  public Builder(Config config) {
    this.config = config;
  }

  public Config build() throws IOException, XMLStreamException {
    crawler = new CrawlerXML(config);
    index = new PlainIndex(config);
    bigramsBasedSuggestor = new BigramsBasedSuggestor(index);
    lemmer = new Lemmer(config.getMyStem());
    searcher = new SenSeArchImpl(this);
    metric = new Metric(new RequestCrawler(index), config.getPathToMetrics());
    return config;
  }

  public Index getIndex() {
    return index;
  }

  public SenSeArch getSearcher() {
    return searcher;
  }

  Crawler getCrawler() {
    return crawler;
  }

  Suggestor getSuggestor() {
    return bigramsBasedSuggestor;
  }

  public int pageSize() {
    return 10;
  }

  public int windowSize() {
    return 4;
  }

  public Lemmer getLemmer() {
    return lemmer;
  }

  public Metric metric() {
    return metric;
  }
}
