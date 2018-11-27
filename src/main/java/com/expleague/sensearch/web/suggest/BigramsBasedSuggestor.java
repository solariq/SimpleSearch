package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class BigramsBasedSuggestor implements Suggestor {

  private final Index index;

  public BigramsBasedSuggestor(Index index) throws IOException {
    this.index = index;
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

    List<String> suggestions = new ArrayList<>();
    for (String neighbour : index.mostFrequentNeighbours(lastToken)) {
      suggestions.add(searchString + " " + neighbour);
    }

    return suggestions;
  }
}
