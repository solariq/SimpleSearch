package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.index.Index;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BigramsBasedSuggestor implements Suggester {

  private final Index index;

  @Inject
  public BigramsBasedSuggestor(Index index) {
    this.index = index;
  }

  @Override
  public String getName() {
    return "Bigrams based";
  }
  
  public List<String> getSuggestions(String searchString) {
    // TODO: dummy implementation via most frequent neighbours
    // TODO: is it needed to make 'smarter' data structure for such approach?

    String[] tokens = searchString.toLowerCase().split("[^а-яёa-z0-9]");
    if (tokens.length == 0) {
      return Collections.emptyList();
    }

    // avoid empty tokens
    String lastToken = "";
    for (int i = tokens.length - 1; i >= 0; --i) {
      if (!tokens[i].trim().isEmpty()) {
        lastToken = tokens[i].trim();
        break;
      }
    }

    if (lastToken.isEmpty()) {
      return Collections.emptyList();
    }

    // TODO fix this (token can be missing)
    if (index.term(lastToken) == null) {
      return new ArrayList<>();
    }
    return index
        .mostFrequentNeighbours(index.term(lastToken))
        .map(t-> searchString + " " + t.text())
        .collect(Collectors.toList());
  }
}
