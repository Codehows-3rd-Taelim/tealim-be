package com.codehows.taelimbe.store.dto;

import com.codehows.taelimbe.store.entity.Store;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreDTO {

    private Long storeId;

    private Long shopId;

    private String shopName;

    private Long industryId;
    private String industryName;

    // Entity -> DTO 변환을 위한 팩토리 메서드
    public static StoreDTO fromEntity(Store store) {
        StoreDTO dto = new StoreDTO();
        dto.setStoreId(store.getStoreId());
        dto.setShopId(store.getShopId());
        dto.setShopName(store.getShopName());

        // Industry 엔티티가 null이 아닐 경우 ID를 설정
        if (store.getIndustry() != null) {
            dto.setIndustryId(store.getIndustry().getIndustryId());
            dto.setIndustryName(store.getIndustry().getIndustryName());
        } else {
            dto.setIndustryId(null);
            dto.setIndustryName(null);
        }

        return dto;
    }
}
