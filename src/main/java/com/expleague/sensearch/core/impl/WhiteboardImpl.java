package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class WhiteboardImpl implements Whiteboard {

  private final String input;
  private final int page;
  private final int queriesNumber = 1;

  private Page[] results;
  private ResultItem[] googleResults;

  private TIntObjectMap<Query> queries = new TIntObjectHashMap<>();

  private TIntObjectMap<Page[]> subFilterResults = new TIntObjectHashMap<>();
  private TIntObjectMap<Map<Page, Features>> filterFeatures = new TIntObjectHashMap<>();
  private Map<Page, Double> pageFilterScores = new HashMap<>();

  private TIntObjectMap<Page[]> subResults = new TIntObjectHashMap<>();
  private TIntObjectMap<Map<Page, Features>> textFeatures = new TIntObjectHashMap<>();
  private Map<Page, Double> pageScores = new HashMap<>();

  private Snippet[] snippets;

  public WhiteboardImpl(String input, int page) {
    this.input = input;
    this.page = page;
  }

  /*==============================================================================================*/

  @Override
  public synchronized TIntObjectMap<Map<Page, Features>> filterFeatures() {
    return filterFeatures;
  }

  @Override
  public void putFilterFeatures(Map<Page, Features> filterFeatures, int index) {
    this.filterFeatures.put(index, filterFeatures);
  }

  @Override
  public TIntObjectMap<Page[]> subFilterResults() {
    return subFilterResults;
  }

  @Override
  public void putSubFilterResult(Page[] subResult, int index) {
    subFilterResults.put(index, subResult);
  }

  @Override
  public Map<Page, Double> pageFilterScores() {
    return pageFilterScores;
  }

  @Override
  public void putPageFilterScores(Map<Page, Double> scores) {
    this.pageFilterScores.putAll(scores);
  }

  /*==============================================================================================*/

  @Override
  public synchronized TIntObjectMap<Map<Page, Features>> textFeatures() {
    return textFeatures;
  }

  @Override
  public synchronized void putTextFeatures(Map<Page, Features> textFeatures, int index) {
      this.textFeatures.put(index, textFeatures);
  }

  @Nullable
  @Override
  public synchronized TIntObjectMap<Page[]> subResults() {
    return subResults;
  }

  @Override
  public synchronized void putSubResult(Page[] subResult, int index) {
    this.subResults.put(index, subResult);
  }

  @Nullable
  @Override
  public synchronized Map<Page, Double> pageScores() {
    return pageScores;
  }

  @Override
  public synchronized void putPageScores(Map<Page, Double> scores) {
    this.pageScores.putAll(scores);
  }

  /*==============================================================================================*/

  @Nullable
  @Override
  public synchronized Page[] results() {
    return results;
  }

  @Override
  public synchronized void putResults(Page[] pages) {
    this.results = pages;
  }

  @Override
  public int totalResults() {
    return results.length;
  }

  @Nullable
  @Override
  public ResultItem[] groundTruthResults() {
    return googleResults;
  }

  @Override
  public void putGroundTruthResults(ResultItem[] groundTruthResults) {
    this.googleResults = groundTruthResults;
  }

  /*==============================================================================================*/

  @Override
  public Snippet[] snippets() {
    return this.snippets;
  }

  @Override
  public synchronized void putSnippets(Snippet[] snippets) {
    this.snippets = snippets;
  }

  /*==============================================================================================*/

  @Nullable
  @Override
  public synchronized TIntObjectMap<Query> query() {
    return queries;
  }

  @Override
  public synchronized void putQuery(TIntObjectMap<Query> query) {
    this.queries = query;
  }

  @Override
  public int queriesNumber() {
    return queriesNumber;
  }

  /*==============================================================================================*/

  @Nullable
  @Override
  public synchronized String input() {
    return input;
  }

  @Override
  public synchronized int pageNo() {
    return page;
  }
}
