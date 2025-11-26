package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RobotGeneralService {

    @Autowired
    private PuduAPIClient puduAPIClient;

    /**
     * 로봇 상태 조회 (V2)
     * 로봇의 SN 또는 MAC을 기반으로 상태 정보 조회
     *
     * @param sn 로봇 SN (선택)
     * @param mac 로봇 MAC (선택)
     * @return 로봇 상태 정보
     */
    public ResponseEntity<String> getRobotStatusV2(String sn, String mac) {
        try {
            System.out.println("====== 로봇 상태 조회 (V2) 시작 ======");
            System.out.println("SN: " + sn);
            System.out.println("MAC: " + mac);

            // sn과 mac 둘 다 없으면 에러
            if ((sn == null || sn.isEmpty()) && (mac == null || mac.isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("{\"error\": \"sn 또는 mac 중 하나는 필수입니다\"}");
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/openapi/open-platform-service/v2/status/get_by_sn");

            // 선택 파라미터
            if (sn != null && !sn.isEmpty()) {
                builder.queryParam("sn", sn);
            }
            if (mac != null && !mac.isEmpty()) {
                builder.queryParam("mac", mac);
            }

            String url = builder.toUriString();
            System.out.println("Target URL: " + url);

            System.out.println("🔥 V2 API에 전달되는 URL: " + url); // <-- 이 줄 추가
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
