package com.codehows.taelimbe.langchain.converters;

public class PdfEmbeddingNormalizer {

    private PdfEmbeddingNormalizer() {}

    public static String normalize(String rawText, String fileName) {
        if (rawText == null || rawText.isBlank()) {
            return """
[문서정보]
- 파일출처: PDF
- 파일명: %s

[원문본문]
(본문 없음)
""".formatted(fileName);
        }

        return """
[문서정보]
- 파일출처: PDF
- 파일명: %s

[원문본문]
%s
""".formatted(fileName, rawText.trim());
    }
}
