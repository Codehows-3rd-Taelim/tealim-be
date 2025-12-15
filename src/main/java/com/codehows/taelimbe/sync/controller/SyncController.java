package com.codehows.taelimbe.sync.controller;

import com.codehows.taelimbe.sync.dto.SyncRecordDTO;
import com.codehows.taelimbe.sync.service.SyncRecordService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sync")
public class SyncController {

    private final SyncRecordService syncRecordService;

    // 동기화 실행 (버튼 클릭)
    @PostMapping("/now")
    public ResponseEntity<String> sync(Authentication authentication) {
        // authentication.getPrincipal()에 UserPrincipal 객체가 들어있음
        var principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        // userId를 SyncRecordService에 전달
        return ResponseEntity.ok(syncRecordService.executeSync(userId));
    }

    // 마지막 동기화 시간 조회 (수동 동기화, 스케줄러)
    @GetMapping("/last")
    public ResponseEntity<SyncRecordDTO> getLastSyncTime(Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        Long userId = principal.userId();

        return ResponseEntity.ok(syncRecordService.getLastSyncTime(userId));
    }
}
