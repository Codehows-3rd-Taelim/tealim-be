package com.codehows.taelimbe.langchain.models;

import com.codehows.taelimbe.langchain.converters.MessageConverter;
import com.codehows.taelimbe.langchain.converters.ToolConverter;
import com.google.genai.Client;
import com.google.genai.types.*;
import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.Collections;
import java.util.List;

public class GeminiChatModel implements ChatLanguageModel {

    private final Client geminiClient;
    private final String modelName;
    private final Gson gson = new Gson();

    public GeminiChatModel(Client geminiClient, String modelName) {
        this.geminiClient = geminiClient;
        this.modelName = modelName;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return generate(messages, List.of());
    }

    @Override
    public Response<AiMessage> generate(
            List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications
    ) {
        List<Content> googleMessages =
                MessageConverter.toGoogleAiMessages(messages);

        List<Tool> googleTools =
                ToolConverter.toGoogleAiTools(toolSpecifications);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(googleTools)
                .build();

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        modelName,
                        googleMessages,
                        config
                );

        Candidate candidate = response.candidates()
                .orElse(Collections.emptyList())
                .getFirst();

        // tool call 우선
        for (Part part : candidate.content().get().parts().orElse(List.of())) {
            if (part.functionCall().isPresent()) {
                FunctionCall fc = part.functionCall().get();
                ToolExecutionRequest req =
                        ToolExecutionRequest.builder()
                                .name(fc.name().orElse(""))
                                .arguments(gson.toJson(fc.args().orElse(Collections.emptyMap())))
                                .build();

                return Response.from(AiMessage.from(req));
            }
        }

        // 일반 텍스트
        StringBuilder sb = new StringBuilder();
        candidate.content().get().parts().get()
                .forEach(p -> p.text().ifPresent(sb::append));

        return Response.from(AiMessage.from(sb.toString()));
    }
}
