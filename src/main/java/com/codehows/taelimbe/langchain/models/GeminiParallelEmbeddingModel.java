package com.codehows.taelimbe.langchain.models;

import com.google.genai.Client;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class GeminiParallelEmbeddingModel implements EmbeddingModel {

    private final Client geminiClient;
    private final String modelName;
    private final ExecutorService embeddingExecutor;

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return Response.from(embedAll(List.of(textSegment)).content().getFirst());
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {

        try {
            List<CompletableFuture<Embedding>> futures =
                    textSegments.stream()
                            .map(segment ->
                                    CompletableFuture.supplyAsync(
                                            () -> embedOne(segment.text()),
                                            embeddingExecutor // Spring Bean으로 주입된 executor
                                    )
                            )
                            .toList();

            List<Embedding> embeddings =
                    futures.stream()
                            .map(CompletableFuture::join)
                            .toList();

            return Response.from(embeddings);

        } catch (Exception e) {
            throw new RuntimeException("Parallel embedding failed", e);
        }
    }

    private Embedding embedOne(String text) {
        try {
            EmbedContentResponse response =
                    geminiClient.models.embedContent(
                            modelName,
                            text,
                            EmbedContentConfig.builder().build()
                    );

            List<Float> values = response.embeddings()
                    .orElseThrow()
                    .getFirst()
                    .values()
                    .orElse(Collections.emptyList());

            float[] vector = new float[values.size()];
            IntStream.range(0, values.size()).forEach(i -> vector[i] = values.get(i));

            return Embedding.from(vector);

        } catch (Exception e) {
            throw new RuntimeException("Failed to embed text", e);
        }
    }
}
