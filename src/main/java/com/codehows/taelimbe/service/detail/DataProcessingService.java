package com.codehows.taelimbe.service.detail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class DataProcessingService {

    private final ObjectMapper objectMapper;

    public DataProcessingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parseJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    public String toJsonString(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    // 필요한 추가 처리 로직들을 여기에 추가
}
