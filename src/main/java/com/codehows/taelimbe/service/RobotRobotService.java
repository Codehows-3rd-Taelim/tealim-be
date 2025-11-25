package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.dto.RobotFullInfoDTO;
import com.codehows.taelimbe.service.detail.DataCombiningService;
import com.codehows.taelimbe.service.detail.DataProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;

@Service
public class RobotRobotService {

    private final PuduAPIClient puduAPIClient;
    private final DataCombiningService dataCombiningService;
    private final DataProcessingService dataProcessingService;
    private static final String ENCODING = "UTF-8";

    public RobotRobotService(PuduAPIClient puduAPIClient,
                             DataCombiningService dataCombiningService,
                             DataProcessingService dataProcessingService) {
        this.puduAPIClient = puduAPIClient;
        this.dataCombiningService = dataCombiningService;
        this.dataProcessingService = dataProcessingService;
    }

    public ResponseEntity<String> getRobotDetail(String sn) {
        try {
            System.out.println("====== 로봇 상세 조회 시작 ======");
            System.out.println("SN: " + sn);

            String url = PuduAPIClient.BASE_URL + "/cleanbot-service/v1/api/open/robot/detail?sn=" + URLEncoder.encode(sn, ENCODING);
            System.out.println("Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    public ResponseEntity<?> getRobotFullInfo(String sn, long shop_id) {
        try {
            System.out.println("====== 로봇 전체 정보 조회 시작 ======");
            System.out.println("SN: " + sn);
            System.out.println("Shop ID: " + shop_id);

            // 1. 로봇 상세 정보 API 호출
            ResponseEntity<String> robotResponse = getRobotDetail(sn);
            String robotJson = robotResponse.getBody();

            // 2. 배터리 상태 API 호출 (예시 - 실제 구현 필요)
            long start_time = System.currentTimeMillis() / 1000 - 86400; // 24시간 전
            long end_time = System.currentTimeMillis() / 1000;

            StringBuilder chargingUrlBuilder = new StringBuilder(
                    PuduAPIClient.BASE_URL + "/data-board/v1/log/charge/query_list?"
            );
            chargingUrlBuilder.append("start_time=").append(start_time);
            chargingUrlBuilder.append("&end_time=").append(end_time);
            chargingUrlBuilder.append("&offset=0");
            chargingUrlBuilder.append("&limit=1");
            chargingUrlBuilder.append("&timezone_offset=0");
            chargingUrlBuilder.append("&shop_id=").append(shop_id);

            ResponseEntity<String> chargingResponse = puduAPIClient.callPuduAPI(chargingUrlBuilder.toString(), "GET");
            String chargingJson = chargingResponse.getBody();

            // 3. 데이터 조합
            RobotFullInfoDTO robotFullInfo = dataCombiningService.combineRobotData(robotJson, chargingJson);

            // 4. DB에 저장 (필요시 구현)
            // robotRepository.save(robotFullInfo);

            return ResponseEntity.ok(robotFullInfo);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}