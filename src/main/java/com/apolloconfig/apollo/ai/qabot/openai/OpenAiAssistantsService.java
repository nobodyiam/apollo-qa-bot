package com.apolloconfig.apollo.ai.qabot.openai;

import com.apolloconfig.apollo.ai.qabot.config.OpenAiAssistantsConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.theokanning.openai.ListSearchParameters;
import com.theokanning.openai.ListSearchParameters.Order;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.assistant.AssistantRequest;
import com.theokanning.openai.assistants.assistant.FileSearchResources;
import com.theokanning.openai.assistants.assistant.FileSearchTool;
import com.theokanning.openai.assistants.assistant.ToolResources;
import com.theokanning.openai.assistants.assistant.VectorStoreFileRequest;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.assistants.vector_store_file.VectorStoreFile;
import com.theokanning.openai.file.File;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.service.assistant_stream.AssistantSSE;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class OpenAiAssistantsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiAssistantsService.class);

  private final OpenAiService service;
  private final Assistant assistant;
  private final String vectorStoreId;

  public OpenAiAssistantsService(OpenAiAssistantsConfig config) {
    service = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"));
    AssistantRequest request = getAssistantRequest(config);
    this.vectorStoreId = config.getVectorStoreId();
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

  public Map<String, String> getVectorStoreFileIds() {
    int batchSize = 100;
    boolean hasMore = true;
    String lastId = null;
    Map<String, String> fileIdsMap = Maps.newConcurrentMap();
    Set<String> fileIds = Sets.newHashSet();

    while (hasMore) {
      ListSearchParameters searchParameters = new ListSearchParameters();
      searchParameters.setOrder(Order.ASCENDING);
      searchParameters.setLimit(batchSize);
      if (lastId != null) {
        searchParameters.setAfter(lastId);
      }

      List<VectorStoreFile> files = this.service.listVectorStoreFiles(
          this.vectorStoreId, searchParameters).getData();

      if (CollectionUtils.isEmpty(files)) {
        break;
      }

      for (VectorStoreFile file : files) {
        fileIds.add(file.getId());
      }

      int loadedFiles = fileIds.size();
      lastId = files.get(loadedFiles - 1).getId();
      hasMore = files.size() == batchSize;
    }

    List<File> files = this.service.listFiles();

    for (File file : files) {
      if (fileIds.contains(file.getId())) {
        fileIdsMap.put(file.getFilename(), file.getId());
      }
    }

    return fileIdsMap;
  }

  public String createVectorStoreFile(String filename, String content) {
    String purpose = "assistants";

    InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    File file = this.service.uploadFile(purpose, contentStream, filename);

    VectorStoreFileRequest request = new VectorStoreFileRequest();
    request.setFileId(file.getId());

    this.service.createVectorStoreFile(this.vectorStoreId, request);

    return file.getId();
  }

  public void deleteVectorStoreFile(String fileId) {
    // first delete the vector store file
    this.service.deleteVectorStoreFile(this.vectorStoreId, fileId);

    // then delete the file
    this.service.deleteFile(fileId);
  }

  public String getFileName(String fileId) {
    return this.service.retrieveFile(fileId).getFilename();
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
