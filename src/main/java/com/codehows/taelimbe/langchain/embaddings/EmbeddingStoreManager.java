package com.codehows.taelimbe.langchain.embaddings;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.*;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static io.milvus.grpc.DataType.*;

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

    // application.properties에서 Milvus 컬렉션 이름을 주입받습니다.
    @Value("${milvus.collection-name}")
    private String milvusCollectionName;

    // application.properties에서 Milvus 임베딩 모델의 차원 수를 주입받습니다.
    @Value("${milvus.embedding.dimension}")
    private Integer embeddingDimension;

    /**
     * Milvus 벡터 저장소를 재설정(reset)합니다.
     * 이 메서드는 다음 단계를 수행합니다:
     * 1. Milvus 클라이언트에 연결합니다.
     * 2. 기존 컬렉션이 있다면 삭제합니다.
     * 3. LangChain4j의 `MilvusEmbeddingStore`가 기대하는 스키마로 새로운 컬렉션을 생성합니다.
     *    - `id`: Primary Key, VarChar 타입 (UUID 저장용)
     *    - `text`: 원본 텍스트, VarChar 타입
     *    - `metadata`: 메타데이터, JSON 타입
     *    - `vector`: 임베딩 벡터, FloatVector 타입 (차원은 `embeddingDimension` 사용)
     * 4. `vector` 필드에 인덱스를 생성하여 효율적인 벡터 검색을 가능하게 합니다.
     * 5. 컬렉션을 로드하여 쿼리 준비를 마칩니다.
     * 6. Milvus 클라이언트 연결을 닫습니다.
     *
     * @throws RuntimeException Milvus 작업 중 예기치 않은 오류가 발생할 경우
     */
    public void reset() {
        log.info("Milvus 컬렉션 재설정 시도: '{}'", milvusCollectionName);

        try {
            // Milvus 서비스 클라이언트에 연결합니다.
            MilvusServiceClient milvusClient = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(milvusHost)
                            .withPort(milvusPort)
                            .build()
            );
            log.debug("Milvus에 연결됨: {}:{}", milvusHost, milvusPort);

            // 기존 컬렉션을 삭제합니다. (컬렉션이 없어도 오류 발생하지 않음)
            DropCollectionParam dropReq = DropCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .build();
            milvusClient.dropCollection(dropReq);
            log.info("기존 Milvus 컬렉션 '{}' 삭제 완료 (존재했다면).", milvusCollectionName);

            // 새로운 컬렉션을 생성합니다.
            CreateCollectionParam collectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    // 'id' 필드: 고유 식별자, VarChar 타입, Primary Key
                    .addFieldType(FieldType.newBuilder()
                            .withName("id")
                            .withDataType(VarChar)
                            .withMaxLength(36) // UUID 문자열 길이에 맞춤
                            .withPrimaryKey(true)
                            .withAutoID(false)
                            .build())
                    // 'text' 필드: 원본 텍스트, VarChar 타입
                    .addFieldType(FieldType.newBuilder()
                            .withName("text")
                            .withDataType(VarChar)
                            .withMaxLength(65535) // 긴 텍스트 저장을 위해 최대 길이 설정
                            .build())
                    // 'metadata' 필드: 추가 메타데이터, JSON 타입
                    .addFieldType(FieldType.newBuilder()
                            .withName("metadata")
                            .withDataType(JSON)
                            .build())
                    // 'vector' 필드: 임베딩 벡터, FloatVector 타입, 차원 설정
                    .addFieldType(FieldType.newBuilder()
                            .withName("vector")
                            .withDataType(FloatVector)
                            .withDimension(embeddingDimension) // 주입받은 값 사용
                            .build())
                    .build();

            milvusClient.createCollection(collectionParam);
            log.info("새로운 Milvus 컬렉션 '{}' 생성 완료.", milvusCollectionName);

            // 'vector' 필드에 인덱스를 생성합니다. (COSINE 유사도 측정, FLAT 인덱스 타입)
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .withFieldName("vector")
                    .withIndexType(IndexType.FLAT) // 간단한 FLAT 인덱스 (대규모 데이터셋에서는 IVF_FLAT 등 고려)
                    .withMetricType(MetricType.COSINE) // 코사인 유사도 측정
                    .build();

            milvusClient.createIndex(indexParam);
            log.info("Milvus 컬렉션 '{}'에 인덱스 생성 완료.", milvusCollectionName);

            // 컬렉션을 로드하여 쿼리 준비를 마칩니다.
            LoadCollectionParam request = LoadCollectionParam.newBuilder()
                    .withCollectionName(milvusCollectionName)
                    .build();
            milvusClient.loadCollection(request);
            log.info("Milvus 컬렉션 '{}' 로드 완료.", milvusCollectionName);

            // Milvus 클라이언트 연결을 닫습니다.
            milvusClient.close();

            log.info("Milvus 컬렉션 '{}' 재설정 프로세스 성공적으로 완료.", milvusCollectionName);

        } catch (Exception e) {
            log.error("Milvus 컬렉션 '{}' 재설정 중 예기치 않은 오류 발생: {}", milvusCollectionName, e.getMessage(), e);
            throw new RuntimeException("Milvus 컬렉션 재설정 실패: " + milvusCollectionName, e);
        }
    }
}
