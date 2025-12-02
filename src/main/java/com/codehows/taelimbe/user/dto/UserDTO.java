package com.codehows.taelimbe.user.dto;

import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
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

    // 수정시 비밀번호 안변경하면 null로 보내야해서 NotNull 사용X
    @Length(min=8, max=16, message = "비밀번호는 8자 이상,  16자 이하로 입력해주세요.")
    private String pw;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotNull(message = "전화 번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호는 하이픈(-)을 포함한 올바른 형식(예: 010-1234-5678)으로 입력해주세요.")
    private String phone;

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
        dto.setPw(user.getPw());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStoreId(user.getStore().getStoreId());
        return dto;
    }

    private static String decode(String encoded) {
        if (encoded == null) return null;
        return new String(Base64.getDecoder().decode(encoded));
    }

}
