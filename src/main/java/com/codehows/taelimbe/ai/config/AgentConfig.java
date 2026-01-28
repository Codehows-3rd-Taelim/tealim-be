package com.codehows.taelimbe.ai.config;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.langchain.Agent;
import com.codehows.taelimbe.langchain.tools.ChatTools;
import com.codehows.taelimbe.langchain.tools.ReportTools;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    @Bean
    public ReportAgent reportAgent(StreamingChatModel model, ReportTools reportTools) {
        ChatMemoryProvider memoryProvider = (memoryId) ->
                MessageWindowChatMemory.builder().maxMessages(10).build();

        // @Tool 메서드 추출 → 커스텀 ToolExecutor 래핑 (memoryId로 ToolArgsContextHolder 바인딩)
        Map<ToolSpecification, ToolExecutor> toolMap = new LinkedHashMap<>();
        for (Method method : reportTools.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(Tool.class) == null) continue;
            ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
            DefaultToolExecutor delegate = new DefaultToolExecutor(reportTools, method);
            toolMap.put(spec, (request, memoryId) -> {
                if (memoryId != null) ToolArgsContextHolder.bind(memoryId.toString());
                return delegate.execute(request, memoryId);
            });
        }

        return AiServices.builder(ReportAgent.class)
                .streamingChatModel(model)
                .tools(toolMap)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    @Bean
    public Agent chatAgent(StreamingChatModel streamingChatModel, ChatMemoryProvider chatMemoryProvider, ContentRetriever contentRetriever) {
        return AiServices.builder(Agent.class)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .build();
    }
}
