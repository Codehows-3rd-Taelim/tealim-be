package com.codehows.taelimbe.client;

import lombok.Getter;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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
        System.out.println("x-date: " + xDate);

        // 서버가 기대하는 HMAC 문자열 그대로 사용
        String stringToSign = String.format("x-date: %s\n%s\n%s\n%s\n%s\n%s",
                xDate, httpMethod, acceptHeader, contentType, contentMD5, pathAndParams);

        System.out.println("stringToSign:");
        System.out.println(stringToSign);

        // HMAC 생성
        byte[] hmacStr = HmacSHA1Encrypt(stringToSign, apiAppSecret);
        String signature = Base64.getEncoder().encodeToString(hmacStr);
        System.out.println("Signature: " + signature);

        // Authorization header 생성 (공백 제거)
        String authHeader = String.format(
                "hmac id=\"%s\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\"%s\"",
                apiAppKey.trim(), signature.trim()
        );
        System.out.println("Authorization: " + authHeader);

        // HTTP 요청
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", acceptHeader);
        httpGet.setHeader("Host", host);
        httpGet.setHeader("x-date", xDate);
        httpGet.setHeader("Authorization", authHeader);

        CloseableHttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        System.out.println("Response Status: " + statusCode);
        System.out.println("Response Body: " + responseBody);
        System.out.println("====== 완료 ======\n");

        httpClient.close();

        return ResponseEntity.status(statusCode)
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


    // ============================
// 🔵 OpenPlatform 전용 (HMAC X)
// ============================
    public ResponseEntity<String> callOpenPlatformAPI(String url) throws Exception {

        System.out.println("🔥 [OpenPlatform API 호출] URL = " + url);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);

        // 이 API는 Content-Type만 요구함
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("Accept", "application/json");

        CloseableHttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        String responseBody = EntityUtils.toString(response.getEntity());

        System.out.println("🔵 OpenPlatform Response Status: " + statusCode);
        System.out.println("🔵 OpenPlatform Response Body: " + responseBody);

        httpClient.close();

        return ResponseEntity.status(statusCode).body(responseBody);
    }
}