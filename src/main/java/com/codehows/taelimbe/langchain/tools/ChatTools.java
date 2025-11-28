package com.codehows.taelimbe.langchain.tools;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatTools {
    private final Gson gson;
}
