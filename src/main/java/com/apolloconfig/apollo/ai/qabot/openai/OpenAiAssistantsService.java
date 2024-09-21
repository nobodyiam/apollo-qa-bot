package com.apolloconfig.apollo.ai.qabot.openai;

import com.apolloconfig.apollo.ai.qabot.config.OpenAiAssistantsConfig;
import com.google.common.collect.Lists;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.assistant.FileSearchResources;
import com.theokanning.openai.assistants.assistant.FileSearchTool;
import com.theokanning.openai.assistants.assistant.ToolResources;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenAiAssistantsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiAssistantsService.class);

  private final OpenAiService service;
  private final Assistant assistant;

  public OpenAiAssistantsService(OpenAiAssistantsConfig config) {
    service = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"));
    AssistantRequest request = getAssistantRequest(config);
    assistant = service.createAssistant(request);
  }

  public Flowable<AssistantSSE> getAssistantMessage(String threadId, String prompt) {
    RunCreateRequest request = new RunCreateRequest();
    request.setAssistantId(this.assistant.getId());
    MessageRequest messageRequest = new MessageRequest();
    messageRequest.setContent(prompt);
    request.setAdditionalMessages(Lists.newArrayList(messageRequest));

    return this.service.createRunStream(threadId, request);
  }

  public Thread createThread() {
    return this.service.createThread(new ThreadRequest());
  }

  private AssistantRequest getAssistantRequest(OpenAiAssistantsConfig config) {
    AssistantRequest request = new AssistantRequest();
    request.setModel(config.getModel());
    request.setName(config.getName());
    request.setInstructions(config.getInstructions());
    request.setTools(Lists.newArrayList(new FileSearchTool()));
    ToolResources toolResources = new ToolResources();
    FileSearchResources fileSearchResources = new FileSearchResources();
    fileSearchResources.setVectorStoreIds(Lists.newArrayList(config.getVectorStoreId()));
    toolResources.setFileSearch(fileSearchResources);
    request.setToolResources(toolResources);
    return request;
  }
}
