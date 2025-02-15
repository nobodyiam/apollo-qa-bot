package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.deepseek.DeepSeekAiService;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChunk;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import io.reactivex.Flowable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

  private final DeepSeekAiService aiService;

  public HelloController(DeepSeekAiService aiService) {
    this.aiService = aiService;
  }

  @GetMapping("/{name}")
  public Flowable<String> hello(@PathVariable String name) {
    ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(
        "You are an assistant who responds in the style of Dr Seuss.").build();
    ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(
        "write a brief greeting for " + name).build();

    Flowable<ChatCompletionChunk> result = aiService.getCompletionFromMessages(
        Lists.newArrayList(systemMessage, userMessage));
    return result.filter(chatCompletionChunk -> !chatCompletionChunk.getChoices().isEmpty()).map(
        chatCompletionChunk -> {
          if (!Strings.isNullOrEmpty(
              chatCompletionChunk.getChoices().get(0).getMessage().getReasoningContent())) {
            return chatCompletionChunk.getChoices().get(0).getMessage().getReasoningContent();
          } else {
            return (String) chatCompletionChunk.getChoices().get(0).getMessage().getContent();
          }
        });
  }
}
