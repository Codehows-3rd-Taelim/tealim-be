package com.codehows.taelimbe;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.constant.IndustryType;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component // 스프링 빈으로 등록
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final IndustryRepository industryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional // DB 쓰기 작업이므로 트랜잭션 관리 필요
    public void run(String... args) throws Exception {
        List<Industry> industries = initializeIndustryData();
        List<Store> stores = initializeStoreData(industries);
    }

    // --- 1. Industry 초기화 ---
    private List<Industry> initializeIndustryData() {
        if (industryRepository.count() == 0) {
            System.out.println(">>> Initializing Industry Data...");

            // IndustryType Enum의 모든 값을 스트림으로 변환하여 업종 이름을 가져옵니다.
            List<Industry> industries = Arrays.stream(IndustryType.values())
                    .map(IndustryType::getIndustryName) // Enum에서 정의된 한글 업종 이름을 가져옴
                    .map(name -> Industry.builder().industryName(name).build())
                    .toList();

            industryRepository.saveAll(industries);
            return industries; // 저장된 리스트 반환
        }
        // 이미 데이터가 있다면 기존 데이터를 조회하여 반환 (Store 초기화에 사용)
        return industryRepository.findAll();
    }

    // --- 2. Store 초기화 ---
    private List<Store> initializeStoreData(List<Industry> industries) {
        if (storeRepository.count() == 0) {
            System.out.println(">>> Initializing Store Data...");

            Industry industry = industries.stream()
                    .filter(i -> "산업 시설/창고/물류".equals(i.getIndustryName()))
                    .findFirst()
                    .orElse(null);

            if (industry != null) {
                Store inuStore = Store.builder()
                        .shopId(518350000L) // 임의의 초기 shopId
                        .shopName("인어스트리")
                        .industry(industry)
                        .delYn(DeleteStatus.N)
                        .build();
                storeRepository.save(inuStore);

                Store taelimStore = Store.builder()
                        .shopId(518250000L) // 임의의 초기 shopId
                        .shopName("태림")
                        .industry(industry)
                        .delYn(DeleteStatus.N)
                        .build();
                storeRepository.save(taelimStore);

                return List.of(inuStore, taelimStore);
            }
        }
        // 이미 데이터가 있거나 초기화에 실패하면 기존 데이터를 조회하여 반환
        return storeRepository.findAll();
    }

}
