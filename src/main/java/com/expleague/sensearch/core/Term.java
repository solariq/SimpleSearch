package com.expleague.sensearch.core;

import com.expleague.commons.math.vectors.Vec;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Term {
  CharSequence text();

  // lemma of lemma == lemma
  Term lemma();

  Vec vec();

  Stream<Term> synonyms();

  Stream<Term> synonyms(double synonymThreshold);

  Stream<TermAndDistance> synonymsWithDistance(double synonymThreshold);

  Stream<TermAndDistance> synonymsWithDistance();

  int documentFreq();

  /**
   * @return number of documents which contain at least one term having the same {@link #lemma()} as
   * this term
   */
  int documentLemmaFreq();

  int freq();

  // Can be null if lemma can not be determined
  @Nullable
  PartOfSpeech partOfSpeech();


  interface TermAndDistance {

    Term term();

    double distance();
  }
}
