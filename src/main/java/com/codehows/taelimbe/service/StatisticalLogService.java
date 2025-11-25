// StatisticalLogService.java
package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class StatisticalLogService {

    private final PuduAPIClient puduAPIClient;

    public StatisticalLogService(PuduAPIClient puduAPIClient) {
        this.puduAPIClient = puduAPIClient;
    }

    public ResponseEntity<String> getChargingRecordList(long start_time, long end_time,
                                                        int offset, int limit, int timezone_offset,
                                                        Long shop_id) {
        try {
            System.out.println("====== Charging Record 목록 조회 시작 ======");
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Offset: " + offset);
            System.out.println("Limit: " + limit);

            StringBuilder urlBuilder = new StringBuilder(
                    PuduAPIClient.BASE_URL + "/data-board/v1/log/charge/query_list?"
            );

            urlBuilder.append("start_time=").append(start_time);
            urlBuilder.append("&end_time=").append(end_time);
            urlBuilder.append("&offset=").append(offset);
            urlBuilder.append("&limit=").append(limit);
            urlBuilder.append("&timezone_offset=").append(timezone_offset);

            if (shop_id != null) {
                urlBuilder.append("&shop_id=").append(shop_id);
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
