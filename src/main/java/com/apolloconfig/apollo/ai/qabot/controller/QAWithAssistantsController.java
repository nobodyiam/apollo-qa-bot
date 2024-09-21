package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.entity.Answer;
import com.apolloconfig.apollo.ai.qabot.openai.OpenAiAssistantsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.theokanning.openai.assistants.StreamEvent;
import com.theokanning.openai.assistants.message.content.Annotation;
import com.theokanning.openai.assistants.message.content.MessageDelta;
import com.theokanning.openai.assistants.message.content.Text;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/qa")
public class QAWithAssistantsController {

  private static final Logger LOGGER = LoggerFactory.getLogger(QAWithAssistantsController.class);
  private static final String END_SYMBOL = "$END$";

  private final OpenAiAssistantsService aiService;
  private final ObjectMapper objectMapper;

  public QAWithAssistantsController(OpenAiAssistantsService aiService) {
    this.aiService = aiService;
    this.objectMapper = new ObjectMapper();
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Answer> qa(@RequestParam String question,
      @RequestParam(required = false, defaultValue = "") String threadId) {
    question = question.trim();
    if (Strings.isNullOrEmpty(question)) {
      return Flux.just(Answer.EMPTY);
    }

    if (Strings.isNullOrEmpty(threadId)) {
      threadId = getThreadId();
    }

    try {
      return doQA(threadId, question);
    } catch (Throwable exception) {
      LOGGER.error("Error while calling Assistants API", exception);
      return Flux.just(Answer.ERROR);
    }
  }

  private String getThreadId() {
    return aiService.createThread().getId();
  }

  private Flux<Answer> doQA(String threadId, String question) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\nPrompt message: {}", question);
    }

    Flowable<AssistantSSE> result = aiService.getAssistantMessage(threadId, question);

    Flux<Answer> flux = Flux.from(result.filter(
            assistantSSE -> assistantSSE.getEvent() == StreamEvent.THREAD_MESSAGE_DELTA)
        .map(assistantSSE -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("event: {}, data: {}", assistantSSE.getEvent(),
                assistantSSE.getData());
          }
          MessageDelta message = objectMapper.readValue(assistantSSE.getData(),
              MessageDelta.class);
          Text text = message.getDelta().getContent().get(0).getText();
          String value = text.getValue();
          if (!CollectionUtils.isEmpty(text.getAnnotations())) {
            for (Annotation annotation : text.getAnnotations()) {
              value = value.replace(annotation.getText(), "");
            }
          }
          return new Answer(value, "");
        }));

    return flux.concatWith(Flux.just(new Answer(END_SYMBOL, threadId)));
  }
}
