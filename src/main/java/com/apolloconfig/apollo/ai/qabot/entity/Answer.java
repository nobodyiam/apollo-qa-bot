package com.apolloconfig.apollo.ai.qabot.entity;

public record Answer(String answer, String threadId) {

  public static final Answer EMPTY = new Answer("", "");
  public static final Answer ERROR = new Answer(
      "Sorry, I can't answer your question right now. Please try again later.", "");
}
