package com.codehows.taelimbe.store.constant;

// 업종 타입 Enum으로 이름 변경을 제안합니다.
public enum IndustryType {
    FOOD_BEVERAGE("식음료"),
    RETAIL("소매"),
    HOSPITALITY("접객"),
    INDUSTRIAL_FACILITY("산업 시설/창고/물류"), // 기존 문자열과 동일하게 유지
    HEALTHCARE("헬스케어"),
    TRANSPORTATION("운송 및 관련 서비스"),
    ENTERTAINMENT_SPORTS("엔터테인먼트 및 스포츠"),
    RESIDENTIAL_OFFICE("주거 및 오피스 빌딩"),
    EDUCATION("교육"),
    PUBLIC_SERVICE("공공 서비스");

    private final String industryName;

    IndustryType(String industryName) {
        this.industryName = industryName;
    }

    public String getIndustryName() {
        return industryName;
    }
}