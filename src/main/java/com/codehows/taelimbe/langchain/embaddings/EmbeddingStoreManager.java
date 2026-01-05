package com.codehows.taelimbe.langchain.embaddings;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static io.milvus.grpc.DataType.*;

import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Milvus 임베딩 저장소의 생명주기를 관리하는 컴포넌트입니다.
 * 이 클래스는 Milvus 컬렉션의 생성, 삭제, 인덱싱, 로딩과 같은 작업을 수행하며,
 * LangChain4j의 `EmbeddingStore` 인터페이스에 직접 노출되지 않은 기능을 제공합니다.
 * `@Component` 어노테이션을 통해 Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@Slf4j`는 Lombok 어노테이션으로, 로깅을 위한 `log` 객체를 자동으로 생성합니다.
 */
@Component
@Slf4j
public class EmbeddingStoreManager {

    // application.properties에서 Milvus 호스트를 주입받습니다.
    @Value("${milvus.host}")
    private String milvusHost;

    // application.properties에서 Milvus 포트를 주입받습니다.
    @Value("${milvus.port}")
    private Integer milvusPort;

    @Value("${milvus.collection-name}")
    private String milvusCollectionName;

    // application.properties에서 Milvus 임베딩 모델의 차원 수를 주입받습니다.
    @Value("${milvus.embedding.dimension}")
    private Integer embeddingDimension;



    /**
     * Milvus 벡터 저장소를 재설정(reset)합니다.
     */
    public void reset() {
        log.info("Milvus 컬렉션 재설정 시도: '{}'", milvusCollectionName);

        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        try {
            log.debug("Milvus에 연결됨: {}:{}", milvusHost, milvusPort);

            // 기존 컬렉션 삭제
            DropCollectionParam dropReq = DropCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .build();
            milvusClient.dropCollection(dropReq);
            log.info("기존 Milvus 컬렉션 '{}' 삭제 완료 (존재했다면).", milvusCollectionName);

            // 컬렉션 생성
            CreateCollectionParam collectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .addFieldType(FieldType.newBuilder()
                            .withName("id")
                            .withDataType(VarChar)
                            .withMaxLength(36)
                            .withPrimaryKey(true)
                            .withAutoID(false)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName("text")
                            .withDataType(VarChar)
                            .withMaxLength(65535)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName("metadata")
                            .withDataType(JSON)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName("vector")
                            .withDataType(FloatVector)
                            .withDimension(embeddingDimension)
                            .build())
                    .build();

            milvusClient.createCollection(collectionParam);
            log.info("새로운 Milvus 컬렉션 '{}' 생성 완료.", milvusCollectionName);

            // 인덱스 생성
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.FLAT)
                    .withMetricType(MetricType.COSINE)
                    .build();

            milvusClient.createIndex(indexParam);
            log.info("Milvus 컬렉션 '{}'에 인덱스 생성 완료.", milvusCollectionName);

            // 컬렉션 로드
            LoadCollectionParam request = LoadCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .build();
            milvusClient.loadCollection(request);
            log.info("Milvus 컬렉션 '{}' 로드 완료.", milvusCollectionName);

            log.info("Milvus 컬렉션 '{}' 재설정 프로세스 성공적으로 완료.", milvusCollectionName);

        } catch (Exception e) {
            log.error("Milvus 컬렉션 '{}' 재설정 중 예기치 않은 오류 발생", milvusCollectionName, e);
            throw new RuntimeException("Milvus 컬렉션 재설정 실패", e);
        } finally {
            milvusClient.close();
        }
    }

    /**
     * Milvus에 벡터 데이터를 추가합니다.
     */
    // qna 임베딩용
    public void addDocuments(

            List<String> ids,
            List<String> texts,
            List<JSONObject> metadatas,
            List<List<Float>> vectors
    ) {
        String collectionName = milvusCollectionName;

        log.info("Milvus 벡터 데이터 저장 시작 ({}건, collection={})",
                vectors.size(), collectionName);

        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        try {
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("id", ids));
            fields.add(new InsertParam.Field("text", texts));
            fields.add(new InsertParam.Field("metadata", metadatas));
            fields.add(new InsertParam.Field("vector", vectors));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();

            milvusClient.insert(insertParam);
            milvusClient.flush(
                    FlushParam.newBuilder()
                            .withCollectionNames(Collections.singletonList(collectionName))
                            .build()
            );

            log.info("Milvus 벡터 데이터 저장 완료 (collection={})", collectionName);

        } catch (Exception e) {
            log.error("Milvus 벡터 데이터 저장 중 오류 발생 (collection={})", collectionName, e);
            throw new RuntimeException("Milvus 데이터 저장 실패", e);
        } finally {
            milvusClient.close();
        }
    }

    /**
     * 특정 key에 해당하는 벡터 데이터를 삭제합니다.
     */
    // true = 삭제됨, false = 삭제 불확실
    public boolean deleteDocuments(String key) {

        log.info("Milvus 데이터 삭제 시작 (key={})", key);

        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        try {
            List<String> targetCollections = new ArrayList<>();
            targetCollections.add(milvusCollectionName);

            for (String collection : targetCollections) {

                String queryExpr =
                        "metadata[\"key\"] == \"%s\" || metadata[\"embedKey\"] == \"%s\""
                                .formatted(key, key);

                QueryParam queryParam = QueryParam.newBuilder()
                        .withCollectionName(collection)
                        .withExpr(queryExpr)
                        .withOutFields(Collections.singletonList("id"))
                        .build();

                // 1️. query
                R<QueryResults> queryResponse = milvusClient.query(queryParam);
                if (queryResponse.getStatus() != R.Status.Success.getCode()) {
                    log.warn("Milvus query failed (status={})", queryResponse.getStatus());
                    return false;
                }

                QueryResultsWrapper wrapper =
                        new QueryResultsWrapper(queryResponse.getData());

                List<String> ids =
                        (List<String>) wrapper.getFieldWrapper("id").getFieldData();

                // 2. 이미 없음 = 확실
                if (ids.isEmpty()) {
                    log.info("삭제 대상 없음 (이미 삭제됨) key={}", key);
                    return true;
                }

                // 3️. 삭제
                String deleteExpr = "id in [%s]".formatted(
                        ids.stream()
                                .map(id -> "\"" + id + "\"")
                                .collect(Collectors.joining(", "))
                );

                DeleteParam deleteParam = DeleteParam.newBuilder()
                        .withCollectionName(collection)
                        .withExpr(deleteExpr)
                        .build();

                R<MutationResult> deleteResponse =
                        milvusClient.delete(deleteParam);

                if (deleteResponse.getStatus() != R.Status.Success.getCode()) {
                    log.warn("Milvus delete failed (status={})", deleteResponse.getStatus());
                    return false;
                }

                // 4️. flush
                R<FlushResponse> flushResponse =
                        milvusClient.flush(
                                FlushParam.newBuilder()
                                        .withCollectionNames(
                                                Collections.singletonList(collection))
                                        .build()
                        );

                if (flushResponse.getStatus() != R.Status.Success.getCode()) {
                    log.warn("Milvus flush failed (status={})", flushResponse.getStatus());
                    return false;
                }

                log.info("Milvus 데이터 삭제 완료 ({}건, collection={}, key={})",
                        ids.size(), collection, key);
            }

            return true;

        } catch (Exception e) {
            log.warn("Milvus 데이터 삭제 중 불확실 오류 (key={})", key, e);
            return false;
        } finally {
            milvusClient.close();
        }
    }


}

