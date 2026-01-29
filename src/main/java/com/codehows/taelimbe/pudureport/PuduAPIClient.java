package com.codehows.taelimbe.pudureport;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class PuduAPIClient {

    private static final String MAC_NAME = "HmacSHA1";
    private static final String ENCODING = "UTF-8";


    @Value("${api.app.key}")
    private String apiAppKey;

    @Value("${api.app.secret}")
    private String apiAppSecret;

    @Value("${api.host}")
    private String host;

    @Getter
    @Value("${api.base.url}")
    private String baseUrl;

    public ResponseEntity<String> callPuduAPI(String url, String httpMethod) throws Exception {
        String acceptHeader = "application/json";
        String contentType = "";
        String contentMD5 = "";

        // URL parsing
        URL parsedUrl = new URL(url);
        String pathAndParams = parsedUrl.getPath();
        if (parsedUrl.getQuery() != null) {
            pathAndParams += "?" + sortQueryParams(parsedUrl.getQuery());
        }

        String xDate = getGMTTime();

        // 서버가 기대하는 HMAC 문자열 그대로 사용
        String stringToSign = String.format("x-date: %s\n%s\n%s\n%s\n%s\n%s",
                xDate, httpMethod, acceptHeader, contentType, contentMD5, pathAndParams);

        // HMAC 생성
        byte[] hmacStr = HmacSHA1Encrypt(stringToSign, apiAppSecret);
        String signature = Base64.getEncoder().encodeToString(hmacStr);

        // Authorization header 생성 (공백 제거)
        String authHeader = String.format(
                "hmac id=\"%s\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"%s\"",
                apiAppKey.trim(), signature.trim()
        );

        // HTTP 요청 (RestClient 사용)
        RestClient restClient = RestClient.create();
        String responseBody = restClient.get()
                .uri(url)
                .header("Accept", acceptHeader)
                .header("Host", host)
                .header("x-date", xDate)
                .header("Authorization", authHeader)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    public String getGMTTime() {
        Calendar cd = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(cd.getTime());
    }

    public String sortQueryParams(String queryParam) {
        if (queryParam == null || queryParam.isEmpty()) return "";

        String[] queryParams = queryParam.split("&");
        Map<String, String> queryPairs = new TreeMap<>();
        for (String query : queryParams) {
            String[] kv = query.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            queryPairs.put(key, value);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : queryPairs.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }

    public byte[] HmacSHA1Encrypt(String text, String key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(ENCODING), MAC_NAME);
        Mac mac = Mac.getInstance(MAC_NAME);
        mac.init(secretKey);
        return mac.doFinal(text.getBytes(ENCODING));
    }

}