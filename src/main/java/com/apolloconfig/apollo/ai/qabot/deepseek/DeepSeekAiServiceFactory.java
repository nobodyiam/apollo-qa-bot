package com.apolloconfig.apollo.ai.qabot.deepseek;

import com.google.common.collect.Maps;
import com.volcengine.ark.runtime.service.ArkService;
import java.time.Duration;
import java.util.Map;
import okhttp3.Dispatcher;

public class DeepSeekAiServiceFactory {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
  private static final DeepSeekAiServiceFactory INSTANCE = new DeepSeekAiServiceFactory();
  private static final Map<String, ArkService> SERVICES = Maps.newConcurrentMap();

  public static ArkService getService(String apiKey) {
    if (!SERVICES.containsKey(apiKey)) {
      synchronized (INSTANCE) {
        if (!SERVICES.containsKey(apiKey)) {
          SERVICES.put(apiKey, INSTANCE.createService(apiKey));
        }
      }
    }

    return SERVICES.get(apiKey);
  }

  private ArkService createService(String apiKey) {
    return ArkService.builder().timeout(DEFAULT_TIMEOUT).dispatcher(new Dispatcher())
        .baseUrl(System.getenv("ARK_SERVICE_URL")).apiKey(apiKey).build();
  }

}
