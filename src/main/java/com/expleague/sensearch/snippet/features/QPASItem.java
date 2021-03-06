package com.expleague.sensearch.snippet.features;

import com.expleague.ml.meta.DSItem;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.passage.Passage;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"query", "passage"})
public class QPASItem extends DSItem.Stub {

  private final String query;
  private final CharSequence passage;

  private transient Query queryCache;
  private transient Passage passageCache;

  @JsonCreator
  private QPASItem (@JsonProperty("query") String query, @JsonProperty("passage") CharSequence passage) {
    this.query = query;
    this.passage = passage;
  }

  public QPASItem(Query query, Passage passage) {
    this.query = query.text();
    this.passage = passage.sentence();
    queryCache = query;
    passageCache = passage;
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("passage")
  public CharSequence getPassage() {
    return passage;
  }

  public Query queryCache() {
    return queryCache;
  }

  public Passage passageCache() {
    return passageCache;
  }

  @Override
  public String id() {
    return query + "::" + passage;
  }
}
