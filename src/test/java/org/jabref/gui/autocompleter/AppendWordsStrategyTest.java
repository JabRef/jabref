package org.jabref.gui.autocompleter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppendWordsStrategyTest {

  private final AppendWordsStrategy appendWordsStrategy = new AppendWordsStrategy();

  @Test
  void analyzeOneWord() {
    String inputToAnalyze = "foo";
    AutoCompletionInput expected = new AutoCompletionInput("", "foo");

    assertEquals(expected.getPrefix(), appendWordsStrategy.analyze(inputToAnalyze).getPrefix());
    assertEquals(expected.getUnfinishedPart(), appendWordsStrategy.analyze(inputToAnalyze).getUnfinishedPart());
  }

  @Test
  void analyzeTwoWords() {
    String inputToAnalyze = "foo bar";
    AutoCompletionInput expected = new AutoCompletionInput("foo ", "bar");

    assertEquals(expected.getPrefix(), appendWordsStrategy.analyze(inputToAnalyze).getPrefix());
    assertEquals(expected.getUnfinishedPart(), appendWordsStrategy.analyze(inputToAnalyze).getUnfinishedPart());
  }

  @Test
  void analyzeMoreThanTwoWords() {
    String inputToAnalyze = "foo bar baz";
    AutoCompletionInput expected = new AutoCompletionInput("foo bar ", "baz");

    assertEquals(expected.getPrefix(), appendWordsStrategy.analyze(inputToAnalyze).getPrefix());
    assertEquals(expected.getUnfinishedPart(), appendWordsStrategy.analyze(inputToAnalyze).getUnfinishedPart());
  }

  @Test
  void analyzeNoWords() {
    String inputToAnalyze = "";
    AutoCompletionInput expected = new AutoCompletionInput("", "");

    assertEquals(expected.getPrefix(), appendWordsStrategy.analyze(inputToAnalyze).getPrefix());
    assertEquals(expected.getUnfinishedPart(), appendWordsStrategy.analyze(inputToAnalyze).getUnfinishedPart());
  }
}
