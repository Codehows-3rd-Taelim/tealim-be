package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Service
public class GeneralLogService {

    private final PuduAPIClient puduAPIClient;


    //최신순으로 나온다.
    public ResponseEntity<String> getChargingRecordList(long start_time, long end_time,
                                                        int offset, int limit, int timezone_offset,
                                                        Long shop_id) {
        try {
            System.out.println("====== Charging Record 목록 조회 시작 ======");
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Offset: " + offset);
            System.out.println("Limit: " + limit);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/charge/query_list")
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset);

            if (shop_id != null) {
                builder.queryParam("shop_id", shop_id);
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


    //이거 조금 이상함? 쓰지마셈
    public ResponseEntity<String> getBatteryHealthList(long start_time, long end_time,
                                                       int offset, int limit, int timezone_offset,
                                                       Long shop_id, String sn,
                                                       Integer min_cycle, Integer max_cycle,
                                                       Integer min_full_capacity, Integer max_full_capacity) {
        try {
            System.out.println("====== Battery Health 목록 조회 시작 ======");
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Offset: " + offset);
            System.out.println("Limit: " + limit);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/battery/query_list")
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset);

            // 선택 파라미터들
            if (shop_id != null) builder.queryParam("shop_id", shop_id);
            if (sn != null && !sn.isEmpty()) builder.queryParam("sn", sn);
            if (min_cycle != null) builder.queryParam("min_cycle", min_cycle);
            if (max_cycle != null) builder.queryParam("max_cycle", max_cycle);
            if (min_full_capacity != null) builder.queryParam("min_full_capacity", min_full_capacity);
            if (max_full_capacity != null) builder.queryParam("max_full_capacity", max_full_capacity);

            String url = builder.toUriString();

            System.out.println("Battery Health Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ================================================
    // 🟦 Power-on Self-test (부팅 자가진단 로그)
    // ================================================
    public ResponseEntity<String> getBootLogList(long start_time,
                                                 long end_time,
                                                 int offset,
                                                 int limit,
                                                 int timezone_offset,
                                                 Long shop_id,
                                                 String check_step,
                                                 Integer is_success) {

        try {
            System.out.println("====== Boot Log 목록 조회 시작 ======");
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Offset: " + offset);
            System.out.println("Limit: " + limit);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/boot/query_list")
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset);

            // ===== 옵션 파라미터 =====
            if (shop_id != null) {
                builder.queryParam("shop_id", shop_id);
            }

            if (check_step != null && !check_step.isEmpty()) {
                builder.queryParam("check_step", check_step);
            }

            if (is_success != null) {
                builder.queryParam("is_success", is_success);
            }

            // 최종 URL
            String url = builder.toUriString();
            System.out.println("Boot Log Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 예외|이벤트 목록 조회
     *
     * @param start_time 시작 시간 (Unix timestamp)
     * @param end_time 종료 시간 (Unix timestamp)
     * @param offset 오프셋
     * @param limit 조회 개수 (1 ~ 20)
     * @param timezone_offset 타임존 오프셋
     * @param shop_id 매장 ID
     * @param error_levels 오류 수준 (Fatal|Error|Warning|Event, 쉼표로 구분)
     * @param error_types 오류 유형 (쉼표로 구분)
     * @return 예외|이벤트 목록
     */
    public ResponseEntity<String> getErrorEventList(long start_time, long end_time,
                                                    int offset, int limit, int timezone_offset,
                                                    Long shop_id, String error_levels, String error_types) {
        try {
            System.out.println("====== Exception|Event 목록 조회 시작 ======");
            System.out.println("Start Time: " + start_time);
            System.out.println("End Time: " + end_time);
            System.out.println("Offset: " + offset);
            System.out.println("Limit: " + limit);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/error/query_list")
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset);

            if (shop_id != null) {
                builder.queryParam("shop_id", shop_id);
            }

            if (error_levels != null && !error_levels.isEmpty()) {
                builder.queryParam("error_levels", error_levels);
            }

            if (error_types != null && !error_types.isEmpty()) {
                builder.queryParam("error_types", error_types);
            }

            String url = builder.toUriString();
            System.out.println("Error/Event Target URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

}