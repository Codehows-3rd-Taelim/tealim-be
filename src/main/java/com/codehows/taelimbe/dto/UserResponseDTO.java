package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {

    private Long userId;
    private String id;
    private String name;
    private String phone;
    private String email;
    private Role role;
    private Long storeId;

    public static UserResponseDTO fromEntity(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                // store 객체에서 storeId를 추출하여 DTO에 직접 매핑
                .storeId(user.getStore() != null ? user.getStore().getStoreId() : null)
                .build();
    }

}
