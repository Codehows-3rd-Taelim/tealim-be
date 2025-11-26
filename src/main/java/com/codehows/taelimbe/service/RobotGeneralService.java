package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

//이거 key가 권한 없다고 안된다고 함 v2인데
@RequiredArgsConstructor
@Service
public class RobotGeneralService {

    private final PuduAPIClient puduAPIClient;

    @Value("${api.status.base.url}")
    private String statusBaseUrl;

    public ResponseEntity<String> getRobotStatusV2(String sn, String mac) {
        try {
            if ((sn == null || sn.isBlank()) && (mac == null || mac.isBlank())) {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"sn 또는 mac 중 하나는 필수입니다\"}");
            }

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(statusBaseUrl) // ★ 중국 노드로 호출
                    .path("/openapi/open-platform-service/v2/status/get_by_sn");

            if (sn != null && !sn.isBlank()) builder.queryParam("sn", sn);
            if (mac != null && !mac.isBlank()) builder.queryParam("mac", mac);

            String url = builder.toUriString();
            System.out.println("🔥 Final URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET"); // ★ HMAC 필요함

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
