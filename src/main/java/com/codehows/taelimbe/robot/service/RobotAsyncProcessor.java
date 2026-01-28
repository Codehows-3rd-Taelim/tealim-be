package com.codehows.taelimbe.robot.service;

import com.codehows.taelimbe.pudu.PuduAPIClient;
import com.codehows.taelimbe.robot.dto.RobotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class RobotAsyncProcessor {

    private final Executor robotSyncExecutor;
    private final PuduAPIClient puduAPIClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public RobotAsyncProcessor(
            @Qualifier("RobotSyncExecutor") Executor robotSyncExecutor,
            PuduAPIClient puduAPIClient
    ) {
        this.robotSyncExecutor = robotSyncExecutor;
        this.puduAPIClient = puduAPIClient;
    }

    @Async("RobotSyncExecutor")
    public CompletableFuture<RobotDTO> fetchRobotAsync(
            JsonNode baseNode, Long shopId
    ) {
        try {
            String sn = baseNode.path("sn").asText();

            String mac = baseNode.path("mac").asText(null);
            String productCode = baseNode.path("product_code").asText(null);

            CompletableFuture<JsonNode> detailFuture =
                    CompletableFuture.supplyAsync(
                            () -> fetchRobotDetail(sn), robotSyncExecutor
                    );

            CompletableFuture<JsonNode> statusFuture =
                    CompletableFuture.supplyAsync(
                            () -> fetchRobotStatusV2(sn), robotSyncExecutor
                    );

            CompletableFuture.allOf(detailFuture, statusFuture).join();

            JsonNode detail = detailFuture.join();
            JsonNode status = statusFuture.join();

            String nickname = null;
            Boolean online = false;
            int battery = 0;
            int statusCode = 0;
            int isCharging = 0;

            if (detail != null) {
                nickname = detail.path("nickname").asText(null);
                battery = detail.path("battery").asInt();
                online = detail.path("online").asBoolean();
                statusCode = detail.path("cleanbot").path("clean").path("status").asInt();
            }

            if (status != null) {
                isCharging = status.path("is_charging").asInt();
            }

            return CompletableFuture.completedFuture(
                    RobotDTO.builder()
                            .sn(sn)
                            .mac(mac)
                            .nickname(nickname)
                            .online(online)
                            .battery(battery)
                            .status(statusCode)
                            .productCode(productCode)
                            .isCharging(isCharging)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }







    private JsonNode fetchRobotDetail(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/cleanbot-service/v1/api/open/robot/detail")
                    .queryParam("sn", sn)
                    .toUriString();

            return mapper.readTree(
                    puduAPIClient.callPuduAPI(url, "GET").getBody()
            ).path("data");

        } catch (Exception ignored) {}

        return null;
    }

    private JsonNode fetchRobotStatusV2(String sn) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/open-platform-service/v2/status/get_by_sn")
                    .queryParam("sn", sn)
                    .toUriString();

            return mapper.readTree(
                    puduAPIClient.callPuduAPI(url, "GET").getBody()
            ).path("data");

        } catch (Exception ignored) {}

        return null;
    }
}
