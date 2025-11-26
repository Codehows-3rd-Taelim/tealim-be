package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.Base64;

@Getter
@Setter
public class UserDTO {

    private Long userId;
    @NotBlank(message = "ID는 필수 입력 값입니다.")
    private String id;

    @NotEmpty(message = "비밀번호는 필수 입력 값입니다.")
    @Length(min=8, max=16, message = "비밀번호는 8자 이상,  16자 이하로 입력해주세요.")
    private String pw;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotEmpty(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    private String email;

    @NotNull(message = "권한은 필수 선택 값입니다.")
    private Role role;

    @NotNull(message = "업체 선택은 필수 선택 값입니다.")
    private Long storeId;

    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStoreId(user.getStore().getStoreId());

        // 비밀번호는 복호화 안 함(보안상 평문 반환 금지)
        dto.setPw(user.getPw());

        return dto;
    }

    private static String decode(String encoded) {
        if (encoded == null) return null;
        return new String(Base64.getDecoder().decode(encoded));
    }

}
