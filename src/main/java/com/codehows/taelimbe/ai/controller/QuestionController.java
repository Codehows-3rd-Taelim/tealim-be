package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.QuestionDTO;
import com.codehows.taelimbe.ai.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionService questionService;

    // 전체
    @GetMapping
    public List<QuestionDTO> listAll() {
        return questionService.findAll();
    }

    // 미해결
    @GetMapping("/unresolved")
    public List<QuestionDTO> listUnresolved() {
        return questionService.findUnresolved();
    }

    // 해결
    @GetMapping("/resolved")
    public List<QuestionDTO> listResolved() {
        return questionService.findResolved();
    }
}
