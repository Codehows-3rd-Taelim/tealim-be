package com.codehows.taelimbe.langchain.embaddings;

import java.util.List;

/**
 * 텍스트를 작은 세그먼트(조각)로 분할하는 전략을 정의하는 인터페이스입니다.
 * 이 인터페이스를 구현하여 다양한 텍스트 분할 방식을 제공할 수 있습니다.
 * 예를 들어, 문장 단위 분할, 토큰 수 기반 분할 등이 있습니다.
 */
public interface TextSplitterStrategy {

    /**
     * 주어진 텍스트를 정의된 전략에 따라 여러 개의 텍스트 세그먼트로 분할합니다.
     *
     * @param text 분할할 원본 텍스트
     * @return 분할된 텍스트 세그먼트들의 리스트
     */
    List<String> split(String text);
}
