package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class BaseQuery implements Query {

  private Map<Term, List<Term>> synonyms;
  private final List<Term> terms;
  private final String text;

  private BaseQuery(String input, List<Term> terms) {
    this.terms = terms;
    this.text = input;
  }

  public static Query create(String input, Index index) {
    return new BaseQuery(input, index.parse(input).collect(Collectors.toList()));
  }

  @Override
  public List<Term> terms() {
    return this.terms;
  }

  @Override
  public Map<Term, List<Term>> synonyms() {
    if (synonyms != null) {
      return synonyms;
    }
    synchronized (this) {
      if (synonyms == null) {
        synonyms =
            terms
                .stream()
                .map(term -> Pair.of(term, term.synonyms().collect(Collectors.toList())))
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
      }
    }
    return synonyms;
  }

  @Override
  public String text() {
    return text;
  }
}
