package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.dto.RobotFullInfoDTO;
import com.codehows.taelimbe.service.detail.DataCombiningService;
import com.codehows.taelimbe.service.detail.DataProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;

@RequiredArgsConstructor
@Service
public class RobotRobotService {

    private final PuduAPIClient puduAPIClient;
    private final DataCombiningService dataCombiningService;
    private final DataProcessingService dataProcessingService;
    private static final String ENCODING = "UTF-8";

    public ResponseEntity<String> getRobotDetail(String sn) {
        try {
            System.out.println("====== 로봇 상세 조회 시작 ======");
            System.out.println("SN: " + sn);

            String url = puduAPIClient.getBaseUrl()
                    + "/cleanbot-service/v1/api/open/robot/detail?sn="
                    + URLEncoder.encode(sn, ENCODING);
            System.out.println("Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 로봇 전체 정보 조회
     *
     * @param sn 로봇 시리얼 번호
     * @param shop_id 매장 ID
     * @param start_time 충전 기록 조회 시작 시간 (unix timestamp, 초 단위)
     * @param end_time 충전 기록 조회 종료 시간 (unix timestamp, 초 단위)
     * @param timezone_offset 타임존 오프셋
     * @param limit 충전 기록 조회 개수 (기본값: 10)
     * @return 로봇 정보 + 충전 기록 조합 DTO
     */
    public ResponseEntity<?> getRobotFullInfo(
            String sn,
            long shop_id,
            long start_time,
            long end_time,
            int timezone_offset,
            int limit) {
        try {
            System.out.println("====== 로봇 전체 정보 조회 시작 ======");
            System.out.println("SN: " + sn);
            System.out.println("Shop ID: " + shop_id);
            System.out.println("Time Range: " + start_time + " ~ " + end_time);
            System.out.println("Timezone Offset: " + timezone_offset);
            System.out.println("Limit: " + limit);

            // 1. 로봇 상세 정보 API 호출
            ResponseEntity<String> robotResponse = getRobotDetail(sn);
            String robotJson = robotResponse.getBody();

            // 2. 충전 기록 API 호출 (입력받은 파라미터 사용)
            StringBuilder UrlBuilder = new StringBuilder(
                    puduAPIClient.getBaseUrl() + "/data-board/v1/log/charge/query_list?"
            );
            UrlBuilder.append("start_time=").append(start_time);
            UrlBuilder.append("&end_time=").append(end_time);
            UrlBuilder.append("&offset=0");
            UrlBuilder.append("&limit=").append(limit);
            UrlBuilder.append("&timezone_offset=").append(timezone_offset);
            UrlBuilder.append("&shop_id=").append(shop_id);

            String chargingUrl = UrlBuilder.toString();
            System.out.println("Charging URL: " + chargingUrl);

            ResponseEntity<String> chargingResponse = puduAPIClient.callPuduAPI(chargingUrl, "GET");
            String chargingJson = chargingResponse.getBody();

            // 3. 데이터 조합
            RobotFullInfoDTO robotFullInfo = dataCombiningService.combineRobotData(robotJson, chargingJson);

            return ResponseEntity.ok(robotFullInfo);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 로봇 전체 정보 조회 (기본값 오버로드)
     * - 시간: 지금으로부터 24시간 전
     * - timezone_offset: 0 (GMT)
     * - limit: 10
     */
    public ResponseEntity<?> getRobotFullInfo(String sn, long shop_id) {
        long endTime = System.currentTimeMillis() / 1000;
        long startTime = endTime - 86400; // 24시간 전

        return getRobotFullInfo(sn, shop_id, startTime, endTime, 0, 10);
    }
}