package com.codehows.taelimbe.ai.config;

import com.codehows.taelimbe.ai.agent.ReportAgent;
import com.codehows.taelimbe.langchain.Agent;
import com.codehows.taelimbe.langchain.tools.ChatTools;
import com.codehows.taelimbe.langchain.tools.ReportTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

@Configuration
@RequiredArgsConstructor
public class AgentConfig {
    /**
     * 최종적으로 AI 서비스를 생성하여 Bean으로 등록합니다.
     * 이 `Agent`는 위에서 정의된 모든 컴포넌트(채팅 모델, 도구, 대화 메모리, 콘텐츠 검색기)를 통합하여
     * AI 서비스의 핵심 로직을 제공합니다.
     *
     * @param streamingChatLanguageModel 스트리밍 채팅 모델 (이전에 정의된 빈을 주입받음)
     * @param tools 사용자 정의 도구 (이전에 정의된 빈을 주입받음)
     * @param chatMemoryProvider 대화 메모리 제공자 (이전에 정의된 빈을 주입받음)
     * @param contentRetriever RAG 콘텐츠 검색기 (이전에 정의된 빈을 주입받음)
     * @return LangChain4j에 의해 동적으로 생성된 `Agent` 인터페이스의 구현체
     */

    @Bean
    public ReportAgent reportAgent(StreamingChatLanguageModel model, ReportTools reportTools) {
        ChatMemoryProvider dummyChatMemoryProvider = (memoryId) -> MessageWindowChatMemory.builder().maxMessages(10).build();

        return AiServices.builder(ReportAgent.class)
                .streamingChatLanguageModel(model)
                .tools(reportTools)
                .chatMemoryProvider(dummyChatMemoryProvider)
                .build();
    }

    @Bean
    public Agent chatAgent(StreamingChatLanguageModel streamingChatLanguageModel, ChatTools tools, ChatMemoryProvider chatMemoryProvider, ContentRetriever contentRetriever) {
        return AiServices.builder(Agent.class)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .tools(tools) // AI가 사용할 수 있는 도구들을 등록합니다.
                .chatMemoryProvider(chatMemoryProvider) // 대화 메모리 제공자를 등록합니다.
                .contentRetriever(contentRetriever) // RAG를 위한 콘텐츠 검색기를 등록합니다.
                .build();
    }


    //Chat 분류 하기 위해 사용
    @Bean
    public Agent intentAgent(StreamingChatLanguageModel streamingChatLanguageModel) {
        return AiServices.builder(Agent.class)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .build();
    }
}