package com.apolloconfig.apollo.ai.qabot.entity;

import java.util.Collections;
import java.util.Set;

public record Answer(String answer, String threadId, Set<String> relatedFiles) {

  public static final Answer EMPTY = new Answer("", "", Collections.emptySet());
  public static final Answer ERROR = new Answer(
      "Sorry, I can't answer your question right now. Please try again later.", "",
      Collections.emptySet());
}
