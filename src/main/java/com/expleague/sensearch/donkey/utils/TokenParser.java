package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.lemmer.Lemmer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TokenParser implements AutoCloseable {

  private static final char LEMMA_SUFFIX = '$';

  private Dictionary dictionary;

  private static int PUNCT_ID = 0;
  private static final int PUNCTUATION_SIZE = 5_000;
  private static int ID = PUNCTUATION_SIZE;
  private final static int BITS_FOR_META = 8;
  private final static int FIRST_UPPERCASE = 0x00000008; //0000'0000'0000'0000'0000'0000'0000'1000
  private final static int ALL_UPPERCASE = 0x00000004;   //0000'0000'0000'0000'0000'0000'0000'0100
  private final static int PUNCTUATION = 0x00000002;     //0000'0000'0000'0000'0000'0000'0000'0010
  private final Tokenizer tokenizer;
  private final Lemmer lemmer;

  public TokenParser(Dictionary dictionary, Lemmer lemmer, Tokenizer tokenizer) {
    this.dictionary = dictionary;
    this.lemmer = lemmer;
    this.tokenizer = tokenizer;
  }

  public void check(CharSequence originalText, int[] ids) {
    boolean check = true;
    boolean upper;

    StringBuilder res = new StringBuilder();

    int id = 0;
    CharSequence w = "";
    if (id < ids.length) {
      w = formattedText(ids[id]);
    }
    int j = 0;
    for (int i = 0; i < originalText.length(); i++) {
      upper = allUpperCase(ids[id]);
      if (w.charAt(j) != originalText.charAt(i)) {
        if (upper || j == 0 || Character.toUpperCase(w.charAt(j)) != originalText.charAt(i)) {
          check = false;
          break;
        }
      }
      j++;
      if (j == w.length()) {
        res.append(w);
        j = 0;
        id++;
        if (id == ids.length && i != originalText.length() - 1) {
          check = false;
          break;
        }
        if (id < ids.length) {
          w = formattedText(ids[id]);
        }
      }
    }
    if (j != 0 || id != ids.length) {
      check = false;
    }

    if (!check) {
      throw new RuntimeException("Parsed text::\n" + res + "\nAren't equal original text::\n" + originalText);
    }
  }

  private CharSequence formattedText(int id) {
    CharSequence t = dictionary.get(toId(id));
    if (allUpperCase(id)) {
      t = t.toString().toUpperCase();
    } else if (firstUpperCase(id)) {
      CharSequence cp = t.subSequence(1, t.length());
      t = String.valueOf(Character.toUpperCase(t.charAt(0))) + cp;
    }
    return t;
  }

  public Stream<Token> parse(CharSequence text) {
    List<Token> result = new ArrayList<>();
    tokenizer.toWords(text).forEach(t -> {
      result.add(addToken(t));
    });
    return result.stream();
  }

  public Token addToken(CharSequence token) {
    if (token.length() == 0) {
      throw new IllegalArgumentException("Empty token encountered");
    }
    return addToken(CharSeq.intern(token));
  }

  private Token addToken(CharSeq token) {
    boolean firstUp = false;
    boolean punkt = true;
    final boolean[] allUp = {true};
    int id;

    if (Character.isUpperCase(token.at(0))) {
      firstUp = true;
    }
    if (Character.isLetterOrDigit(token.at(0))) {
      punkt = false;
    }
    token.forEach(c -> {
      if (Character.isLowerCase(c)) {
        allUp[0] = false;
      }
    });

    CharSeq lowToken = CharSeq.intern(token.toString().toLowerCase());
    if (dictionary.contains(lowToken)) {
      id = dictionary.get(lowToken);
    } else {
      if (punkt) {
        id = PUNCT_ID;
        PUNCT_ID++;
      } else {
        id = ID;
        ID++;
      }
      if (ID >= (1 << 29)) {
        throw new RuntimeException("Token limit::" + token.toString());
      }
      if (PUNCT_ID == PUNCTUATION_SIZE) {
        throw new RuntimeException("Punctuation limit::" + token.toString());
      }
    }
    id = id << BITS_FOR_META;
    if (punkt) {
      id |= PUNCTUATION;
    } else {
      if (firstUp) {
        id |= FIRST_UPPERCASE;
      }
      if (allUp[0]) {
        id |= ALL_UPPERCASE;
      }
    }

    Token res = new Token(lowToken, id);
    newTerm(res);
    return res;
  }

  private void newTerm(Token token) {
    CharSeq word = CharSeq.intern(token.text());
    WordInfo parse = lemmer.parse(word);
    LemmaInfo lemma = parse.lemma();

    int wordId = token.id();
    if (lemma == null) {
      dictionary.addTerm(ParsedTerm.create(wordId, word, -1, null, null));
      return;
    }

    int lemmaId;
    if (dictionary.contains(lemma.lemma())) {
      lemmaId = dictionary.get(lemma.lemma());
    } else if (token.text.last() == LEMMA_SUFFIX) {
      lemmaId = token.id();
    } else {
      lemmaId = addToken(lemma.lemma() + String.valueOf(LEMMA_SUFFIX)).id();
    }
    dictionary.addTerm(ParsedTerm.create(wordId, word, lemmaId, lemma.lemma(),
        PartOfSpeech.valueOf(lemma.pos().name())));
  }

  public static int toId(int id) {
    return id >>> BITS_FOR_META;
  }

  public static boolean isWord(int id) {
    return (id & PUNCTUATION) == 0;
  }

  /**
   * @param id real id
   * @return true if id in punctuation
   */
  public static boolean isPunct(int id) {
    return id < PUNCTUATION_SIZE;
  }

  public static boolean allUpperCase(int id) {
    return (id & ALL_UPPERCASE) != 0;
  }

  public static boolean firstUpperCase(int id) {
    return (id & FIRST_UPPERCASE) != 0;
  }

  @Override
  public void close() {
    dictionary.close();
  }

  public static class Token {

    private final CharSeq text;
    private final int id;

    public Token(CharSeq text, int id) {
      this.text = text;
      this.id = id;
    }

    /**
     * @return id without META-data
     */
    public int id() {
      return (id >>> BITS_FOR_META);
    }

    /**
     * @return id with META-data
     */
    public int formId() {
      return id;
    }

    /**
     * @return lowercase text
     */
    public CharSequence text() {
      return text;
    }

    public boolean isWord() {
      return (id & PUNCTUATION) == 0;
    }

    public boolean allUpperCase() {
      return (id & ALL_UPPERCASE) != 0;
    }

    public boolean firstUpperCase() {
      return (id & FIRST_UPPERCASE) != 0;
    }
  }
}
