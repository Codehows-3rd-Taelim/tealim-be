package com.codehows.taelimbe.sync.controller;

import com.codehows.taelimbe.sync.dto.SyncRecordDTO;
import com.codehows.taelimbe.sync.service.SyncRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sync")
public class SyncController {

    private final SyncRecordService syncRecordService;

    // 동기화 실행 (버튼 클릭)
    @PostMapping("/now")
    public ResponseEntity<String> sync() {
        return ResponseEntity.ok(syncRecordService.executeSync());
    }

    // 마지막 동기화 시간 조회 (수동 동기화, 스케줄러)
    @GetMapping("/last")
    public ResponseEntity<SyncRecordDTO> getLastSyncTime() {
        return ResponseEntity.ok(syncRecordService.getLastSyncTime());
    }
}
