package com.codehows.taelimbe.ai.agent;

import com.codehows.taelimbe.ai.service.AiChatService;
import com.codehows.taelimbe.ai.tool.ReportTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class ChatAgent {

    private final ChatClient chatClient;

    public ChatAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ChatClient.ChatClientRequestSpec chat(List<Message> message) {
        return chatClient.prompt(new Prompt(message));
    }



}
