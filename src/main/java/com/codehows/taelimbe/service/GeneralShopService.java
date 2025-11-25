package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class GeneralShopService {

    private final PuduAPIClient puduAPIClient;


    public ResponseEntity<String> getShopList(int limit, int offset) {
        try {
            System.out.println("====== 매장 목록 조회 시작 ======");
            System.out.println("Limit: " + limit);
            System.out.println("Offset: " + offset);

            String url = PuduAPIClient.BASE_URL + "/data-open-platform-service/v1/api/shop?limit=" + limit + "&offset=" + offset;
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

            StringBuilder urlBuilder = new StringBuilder(
                    PuduAPIClient.BASE_URL + "/data-open-platform-service/v1/api/robot?"
            );

            urlBuilder.append("limit=").append(limit);
            urlBuilder.append("&offset=").append(offset);

            if (shop_id != null) {
                urlBuilder.append("&shop_id=").append(shop_id);
            }

            if (product_code != null && product_code.length > 0) {
                for (String code : product_code) {
                    urlBuilder.append("&product_code=").append(code);
                }
            }

            String url = urlBuilder.toString();
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
