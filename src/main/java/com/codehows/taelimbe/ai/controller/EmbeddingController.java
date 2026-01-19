package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmbeddingController {
    private final EmbeddingStoreManager storeManager;


    @DeleteMapping("/collections/all")
    public ResponseEntity<Void> deleteAllCollections() {
        storeManager.dropAllCollections();
        return ResponseEntity.noContent().build();
    }

}
