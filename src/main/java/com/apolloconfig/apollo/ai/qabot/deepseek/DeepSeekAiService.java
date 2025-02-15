package com.apolloconfig.apollo.ai.qabot.deepseek;

import com.apolloconfig.apollo.ai.qabot.openai.OpenAiServiceFactory;
import com.google.common.collect.Lists;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionChunk;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;
import io.reactivex.Flowable;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("deepseek")
@Component
public class DeepSeekAiService {

  private static final String DEFAULT_MODEL = "ep-20250215172610-hmcz7";
  private static final Double DEFAULT_TEMPERATURE = 0.6;
  private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";

  private final com.theokanning.openai.service.OpenAiService embeddingService;
  private final ArkService arkService;

  public DeepSeekAiService() {
    embeddingService = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"));
    arkService = DeepSeekAiServiceFactory.getService(System.getenv("ARK_API_KEY"));
  }

  public Flowable<ChatCompletionChunk> getCompletion(String prompt) {
    ChatMessage message = ChatMessage.builder().role(ChatMessageRole.USER).content(prompt).build();
    return getCompletionFromMessages(Lists.newArrayList(message));
  }

  public Flowable<ChatCompletionChunk> getCompletionFromMessages(List<ChatMessage> messages) {
    ChatCompletionRequest streamChatCompletionRequest = ChatCompletionRequest.builder()
        .model(DEFAULT_MODEL)
        .messages(messages)
        .temperature(DEFAULT_TEMPERATURE)
        .build();
    return arkService.streamChatCompletion(streamChatCompletionRequest);
  }

  public List<Embedding> getEmbeddings(List<String> chunks) {
    EmbeddingRequest embeddingRequest = EmbeddingRequest.builder().model(DEFAULT_EMBEDDING_MODEL)
        .input(chunks).build();

    return embeddingService.createEmbeddings(embeddingRequest).getData();
  }
}
