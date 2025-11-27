package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class GeneralBasicService {

    private final PuduAPIClient puduAPIClient;

    /**
     * 매장 목록 조회
     */
    public ResponseEntity<String> getShopList(int limit, int offset) {
        try {
            System.out.println("====== 매장 목록 조회 시작 ======");
            System.out.println("limit = " + limit + ", offset = " + offset);

            // 외부 API 규격: snake_case 유지
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/shop")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .toUriString();

            System.out.println("Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 로봇 목록 조회
     * Controller는 camelCase → shopId, productCode[]
     * 외부 API 요청은 snake_case로 보냄 → shop_id, product_code
     */

    //구문서에 있는 매장 로봇 조회
    public ResponseEntity<String> getRobotList(int limit, int offset, Long shopId, String[] productCode) {
        try {
            System.out.println("====== 로봇 목록 조회 시작 ======");
            System.out.println("limit = " + limit + ", offset = " + offset);
            System.out.println("shopId = " + shopId);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", Math.min(limit, 100))   // Pudu API limit 최대 100 제한
                    .queryParam("offset", offset);

            // 외부 API 파라미터는 snake_case
            if (shopId != null) {
                builder.queryParam("shop_id", shopId);
            }

            if (productCode != null && productCode.length > 0) {
                for (String code : productCode) {
                    builder.queryParam("product_code", code); // 외부 규격 유지
                }
            }

            String url = builder.toUriString();
            System.out.println("Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }


    /**
     * 로봇 목록 조회
     * Controller는 camelCase → shopId, productCode[]
     * 외부 API 요청은 snake_case로 보냄 → shop_id, product_code
     */

    //신문서에 있는 매장 로봇 조회
    public ResponseEntity<String> getnewRobotList(int limit, int offset, Long shopId, String[] productCode) {
        try {
            System.out.println("====== 로봇 목록 조회 시작 ======");
            System.out.println("limit = " + limit + ", offset = " + offset);
            System.out.println("shopId = " + shopId);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/openapi/data-open-platform-service/v1/api/robot")
                    .queryParam("limit", Math.min(limit, 100))   // Pudu API limit 최대 100 제한
                    .queryParam("offset", offset);

            // 외부 API 파라미터는 snake_case
            if (shopId != null) {
                builder.queryParam("shop_id", shopId);
            }

            if (productCode != null && productCode.length > 0) {
                for (String code : productCode) {
                    builder.queryParam("product_code", code); // 외부 규격 유지
                }
            }

            String url = builder.toUriString();
            System.out.println("Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
