package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.entity.User;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private Long userId;      // PK
    private String id;        // 로그인 아이디
    private String pw;        // 비밀번호
    private String name;
    private String address;
    private String email;
    private Role role;
    private Long storeId;     // Store 엔티티의 PK만 저장

    // Entity → DTO
    public static UserDto EntityToDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .id(user.getId())
                .pw(user.getPw())
                .name(user.getName())
                .address(user.getAddress())
                .email(user.getEmail())
                .role(user.getRole())
                .storeId(user.getStore() != null ? user.getStore().getStoreId() : null)
                .build();
    }


}
