package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Service
public class GeneralBasicService {

    private final PuduAPIClient puduAPIClient;

    public ResponseEntity<String> getShopList(int limit, int offset) {
        try {
            System.out.println("====== 매장 목록 조회 시작 ======");
            System.out.println("Limit: " + limit);
            System.out.println("Offset: " + offset);

            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/shop")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .toUriString();

            System.out.println("Target URL: " + url);
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    public ResponseEntity<String> getRobotList(int limit, int offset, Long shop_id, String[] product_code) {
        try {
            System.out.println("====== 로봇 목록 조회 시작 ======");
            System.out.println("Limit: " + limit);
            System.out.println("Offset: " + offset);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset);

            if (shop_id != null) {
                builder.queryParam("shop_id", shop_id);
            }

            if (product_code != null && product_code.length > 0) {
                for (String code : product_code) {
                    builder.queryParam("product_code", code);
                }
            }

            String url = builder.toUriString();
            System.out.println("Target URL: " + url);
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
