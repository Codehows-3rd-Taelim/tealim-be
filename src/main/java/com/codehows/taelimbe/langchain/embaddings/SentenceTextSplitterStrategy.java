package com.codehows.taelimbe.langchain.embaddings;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * 텍스트를 문장 단위로 분할하고, 각 세그먼트가 최대 토큰 수를 넘지 않도록 조정하는 전략 구현체입니다.
 * 이 구현체는 간단한 문장 분할 규칙과 `StringTokenizer`를 사용한 대략적인 토큰 계산을 기반으로 합니다.
 */
@Component
@RequiredArgsConstructor
public class SentenceTextSplitterStrategy implements TextSplitterStrategy {

    @Value("${maxTokensPerSegment}")
    private int maxTokensPerSegment=250;

    /**
     * 주어진 텍스트를 문장 단위로 분할하고, 각 세그먼트가 최대 토큰 수를 넘지 않도록 조정합니다.
     *
     * @param text 분할할 원본 텍스트
     * @return 분할된 텍스트 세그먼트들의 리스트
     */
    @Override
    public List<String> split(String text) {
        List<String> segments = new ArrayList<>();

        // 대략 문장 단위로 분할 ('.', '?' 등 기준). 실제 프로덕션 환경에서는 더 정교한 문장 분리기를 사용하는 것이 좋습니다.
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder currentSegment = new StringBuilder();
        int currentTokenCount = 0;

        for (String sentence : sentences) {
            int sentenceTokenCount = countTokens(sentence);

            // 현재 세그먼트에 문장을 추가했을 때 최대 토큰 수를 초과하는 경우
            if (currentTokenCount + sentenceTokenCount > maxTokensPerSegment) {
                if (!currentSegment.isEmpty()) {
                    // 현재까지의 세그먼트를 리스트에 추가하고 초기화합니다.
                    segments.add(currentSegment.toString().trim());
                    currentSegment.setLength(0);
                    currentTokenCount = 0;
                }
            }

            // 문장을 현재 세그먼트에 추가하고 토큰 수를 업데이트합니다.
            currentSegment.append(sentence).append(" ");
            currentTokenCount += sentenceTokenCount;
        }

        // 루프 종료 후 마지막 세그먼트가 남아있다면 리스트에 추가합니다.
        if (!currentSegment.isEmpty()) {
            segments.add(currentSegment.toString().trim());
        }

        return segments;
    }

    /**
     * 주어진 텍스트의 토큰 수를 대략적으로 추정합니다.
     * 여기서는 공백을 기준으로 단어 수를 세는 방식으로 구현되었습니다.
     * 실제 LLM의 토큰화 방식과는 다를 수 있으므로, 정확한 토큰 계산이 필요하다면
     * 해당 LLM의 토큰화 라이브러리를 사용하는 것이 좋습니다.
     *
     * @param text 토큰 수를 추정할 텍스트
     * @return 텍스트의 대략적인 토큰 수
     */
    private int countTokens(String text) {
        StringTokenizer tokenizer = new StringTokenizer(text);
        return tokenizer.countTokens();
    }
}
