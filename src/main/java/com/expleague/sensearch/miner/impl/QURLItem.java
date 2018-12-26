package com.expleague.sensearch.miner.impl;

import com.expleague.ml.meta.DSItem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;

@JsonPropertyOrder({"query", "uri"})
public class QURLItem extends DSItem.Stub {

  private String query;

  private URI pageURI;

  private transient Page pageCache;

  private transient Query queryCache;

  @JsonCreator
  private QURLItem(@JsonProperty("query") String query, @JsonProperty("uri") String uri) {
    this.query = query;
    this.pageURI = URI.create(uri);
  }

  public QURLItem(Page page, Query query) {
    this.query = query.text();
    this.pageURI = page.uri();
    pageCache = page;
    queryCache = query;
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("uri")
  public URI getPageURI() {
    return pageURI;
  }

  public Page pageCache() {
    return pageCache;
  }

  public Query queryCache() {
    return queryCache;
  }

  @Override
  public String id() {
    return query + "::" + pageURI.toString();
  }
}
