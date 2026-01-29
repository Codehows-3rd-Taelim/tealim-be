package com.codehows.taelimbe.ai.common.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI의 TokenTextSplitter Bean 설정입니다.
 * 토큰 기반으로 텍스트를 분할하며, 청크 간 오버랩을 지원합니다.
 */
@Configuration
public class TokenTextSplitterConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter(
            @Value("${text.splitter.chunk-size:800}") int chunkSize,
            @Value("${text.splitter.min-chunk-size:350}") int minChunkSize,
            @Value("${text.splitter.min-chunk-length:5}") int minChunkLength,
            @Value("${text.splitter.max-tokens:1000}") int maxTokens,
            @Value("${text.splitter.keep-separator:true}") boolean keepSeparator
    ) {
        return new TokenTextSplitter(
                chunkSize,
                minChunkSize,
                minChunkLength,
                maxTokens,
                keepSeparator
        );
    }
}
