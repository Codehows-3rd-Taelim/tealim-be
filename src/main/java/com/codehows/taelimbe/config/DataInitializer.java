package com.codehows.taelimbe.config;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.constant.IndustryType;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component // 스프링 빈으로 등록
public class DataInitializer implements CommandLineRunner {

    private final IndustryRepository industryRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(IndustryRepository industryRepository,
                           StoreRepository storeRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.industryRepository = industryRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // DB 쓰기 작업이므로 트랜잭션 관리 필요
    public void run(String... args) throws Exception {
        // 1. Industry 데이터 초기화
        List<Industry> industries = initializeIndustryData();

        // 2. Store 데이터 초기화 (Industry 데이터에 의존)
        List<Store> stores = initializeStoreData(industries);

        // 3. Admin User 데이터 초기화 (Store 데이터에 의존)
        initializeAdminUserData(stores);
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

    // --- 3. Admin User 초기화 ---
    private void initializeAdminUserData(List<Store> stores) {
        if (userRepository.count() == 0 && !stores.isEmpty()) {
            System.out.println(">>> Initializing Admin User Data...");

            // 초기 관리자 계정 정보
            Store defaultStore = stores.get(0); // 생성된 첫 번째 Store를 할당

            User admin = User.builder()
                    .id("admin")
                    .pw(passwordEncoder.encode("admin123")) // 💡 실제 비밀번호 인코딩
                    .name("관리자")
                    .phone("010-0000-0000")
                    .email("admin@taelim.com")
                    .role(Role.ADMIN) // Role enum 사용
                    .store(defaultStore) // Store 객체 할당
                    .build();

            User manager = User.builder()
                    .id("manager")
                    .pw(passwordEncoder.encode("test1234"))
                    .name("매니저")
                    .phone("010-2222-3333")
                    .email("manager1@test.com")
                    .role(Role.MANAGER)
                    .store(defaultStore)  // 첫 번째 매장 연결
                    .build();

            userRepository.save(manager);

            userRepository.save(admin);
            System.out.println("Admin User (ID: admin) initialized successfully.");
        }
    }
}