package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import java.util.Base64;

@Getter
@Builder
public class UserDTO {

    private Long userId;
    @NotBlank(message = "ID는 필수 입력 값입니다.")
    private String id;

    @NotEmpty(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min=8, max=16, message = "비밀번호는 8자 이상,  16자 이하로 입력해주세요.")
    private String pw;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotNull(message = "전화 번호는 필수 입력 값입니다.")
    private String phone;

    @NotEmpty(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;

    @NotNull(message = "권한은 필수 선택 값입니다.")
    private Role role;

    @NotNull(message = "업체 선택은 필수 선택 값입니다.")
    private Long storeId;

    public static UserDTO from(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .id(user.getId())
                .pw(user.getPw())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .storeId(user.getStore().getStoreId())
                .build();
    }

    private static String decode(String encoded) {
        if (encoded == null) return null;
        return new String(Base64.getDecoder().decode(encoded));
    }

}
