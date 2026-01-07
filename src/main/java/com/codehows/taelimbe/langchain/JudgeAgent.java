package com.codehows.taelimbe.langchain;

import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface JudgeAgent {

    @SystemMessage("""
    확실한 답변이 없으면, 불필요하게 정보를 늘어놓지 말고
    질문에 대해 답변이 가능하면 YES,
    불가능하면 NO만 출력하라.
    """)
    TokenStream judge(@UserMessage String message);
}
