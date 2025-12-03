package com.codehows.taelimbe.report.config;


import com.codehows.taelimbe.report.entity.Report;
import com.codehows.taelimbe.report.repository.ReportRepository;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.entity.Industry;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.IndustryRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;


//ai 리포트를 위한 더미임
@Configuration
@RequiredArgsConstructor
public class ReportDummyDataInitializer {

    @Bean
    public CommandLineRunner reportDummyDataInit(
            ReportRepository reportRepository,
            RobotRepository robotRepository,
            StoreRepository storeRepository,
            IndustryRepository industryRepository
    ) {
        return args -> {

            // 1️⃣ Industry 먼저 생성 or 가져오기
            Industry industry = industryRepository.findById(1L).orElseGet(() -> {
                Industry newIndustry = new Industry();
                newIndustry.setIndustryName("Test Industry");
                return industryRepository.save(newIndustry);
            });

            // 2️⃣ Store 생성 or 가져오기
            Store store = storeRepository.findById(1L).orElseGet(() -> {
                Store newStore = Store.builder()
                        .shopId(100L) // 여기 반드시 지정
                        .shopName("Test Store")
                        .industry(industry)
                        .build();
                return storeRepository.save(newStore);
            });

            // 3️⃣ Robot 생성 or 가져오기
            Robot robot = robotRepository.findById(1L).orElseGet(() -> {
                Robot newRobot = Robot.builder()
                        .sn("SN-0001")
                        .mac("00:11:22:33:44:55")
                        .productCode("CC1")   // 문자열로 직접 지정
                        .softVersion("1.0.0")
                        .status(0)            // int 값으로 직접 지정 (예: 0=WAIT)
                        .nickname("테스트 로봇")
                        .battery(100)
                        .online(true)
                        .store(store)
                        .build();

                return robotRepository.save(newRobot);
            });


            // 4️⃣ Report 더미 데이터 30개 생성 (2024-06-01부터 순서대로)
            if (reportRepository.count() == 0) {
                LocalDateTime startDate = LocalDateTime.of(2024, 6, 1, 9, 0);

                for (int i = 0; i < 30; i++) {
                    LocalDateTime startTime = startDate.plusDays(i);
                    LocalDateTime endTime = startTime.plusHours(1);

                    // 랜덤 값 생성 (예: taskArea 80~150, cleanArea 50~120, costBattery 5~20, costWater 20~60)
                    float taskArea = 80 + (float)(Math.random() * 70);
                    float cleanArea = 50 + (float)(Math.random() * 70);
                    long costBattery = 5 + (long)(Math.random() * 15);
                    long costWater = 20 + (long)(Math.random() * 40);

                    Report report = Report.builder()
                            .status(1)
                            .startTime(startTime)
                            .endTime(endTime)
                            .cleanTime(60f)
                            .taskArea(taskArea)
                            .cleanArea(cleanArea)
                            .mode(1)
                            .costBattery(costBattery)
                            .costWater(costWater)
                            .mapName("Office " + (i + 1))
                            .mapUrl("http://map-url.com/map" + (i + 1))
                            .robot(robot)
                            .build();

                    reportRepository.save(report);
                }

            }
        };
    }
}
