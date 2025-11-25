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
import java.util.stream.Collectors;

/**
 * LangChain4j의 `EmbeddingModel` 인터페이스를 `com.google.genai` 클라이언트를 사용하여 구현한 커스텀 클래스입니다.
 * 이 클래스는 LangChain4j 프레임워크가 Google의 제네릭 Gemini 클라이언트를 통해 임베딩을 생성할 수 있도록 하는 어댑터 역할을 합니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 */
@RequiredArgsConstructor
public class GeminiEmbeddingModel implements EmbeddingModel {

    // Google Gemini API 클라이언트 주입
    private final Client geminiClient;
    // 사용할 Gemini 임베딩 모델의 이름
    private final String modelName;

    /**
     * 단일 텍스트 세그먼트를 임베딩합니다.
     * 이 메서드는 내부적으로 `embedAll` 메서드를 호출하여 단일 항목 리스트를 처리합니다.
     *
     * @param textSegment 임베딩할 텍스트 세그먼트
     * @return 임베딩 결과를 포함하는 `Response` 객체
     */
    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        // `embedAll`을 사용하여 단일 세그먼트를 임베딩하고, 결과 리스트의 첫 번째 임베딩을 반환합니다.
        return Response.from(embedAll(Collections.singletonList(textSegment)).content().getFirst());
    }

    /**
     * 여러 텍스트 세그먼트를 한 번에 임베딩합니다.
     * `com.google.genai` 라이브러리는 현재 배치 임베딩을 직접 지원하지 않으므로,
     * 각 텍스트를 순차적으로 임베딩 요청을 보냅니다.
     *
     * @param textSegments 임베딩할 텍스트 세그먼트 리스트
     * @return 임베딩 결과 리스트를 포함하는 `Response` 객체
     * @throws RuntimeException 임베딩 생성 중 오류 발생 시
     */
    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        // 텍스트 세그먼트 리스트에서 텍스트만 추출합니다.
        List<String> texts = textSegments.stream()
                .map(TextSegment::text)
                .toList();

        // 각 텍스트에 대해 Gemini API를 호출하여 임베딩을 생성합니다.
        List<Embedding> embeddings = texts.stream()
                .map(text -> {
                    try {
                        // `geminiClient.models.embedContent`를 호출하여 임베딩을 요청합니다.
                        EmbedContentResponse response = geminiClient.models.embedContent(modelName, text, EmbedContentConfig.builder().build());
                        // Gemini 응답을 LangChain4j의 `Embedding` 객체로 변환합니다.
                        return toLangChainEmbedding(response);
                    } catch (Exception e) {
                        // 임베딩 실패 시 런타임 예외를 발생시킵니다.
                        throw new RuntimeException("Failed to embed text: " + text, e);
                    }
                })
                .collect(Collectors.toList());

        return Response.from(embeddings);
    }

    /**
     * `com.google.genai`의 `EmbedContentResponse`를 LangChain4j의 `Embedding` 객체로 변환합니다.
     *
     * @param response `com.google.genai` 클라이언트로부터 받은 임베딩 응답
     * @return LangChain4j의 `Embedding` 객체
     * @throws RuntimeException 응답에 임베딩이 없거나 유효하지 않은 경우 발생
     */
    private Embedding toLangChainEmbedding(EmbedContentResponse response) {
        // 응답에서 임베딩 벡터 값을 추출합니다.
        List<Float> embeddingValues = response.embeddings().orElse(Collections.emptyList()).stream()
                .findFirst() // 첫 번째 임베딩을 가져옵니다.
                .orElseThrow(() -> new RuntimeException("No embedding found in response")) // 임베딩이 없으면 예외 발생
                .values().orElse(Collections.emptyList()); // 벡터 값 리스트를 가져옵니다.
        
        // List<Float>를 float[] 배열로 변환합니다.
        float[] floatArray = new float[embeddingValues.size()];
        for (int i = 0; i < embeddingValues.size(); i++) {
            floatArray[i] = embeddingValues.get(i);
        }
        // float[] 배열로 LangChain4j의 `Embedding` 객체를 생성하여 반환합니다.
        return Embedding.from(floatArray);
    }
}
