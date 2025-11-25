package com.codehows.taelimbe.entity;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.dto.UserDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "id", length = 20, unique = true, nullable = false)
    private String id;

    @Column(name = "pw", length = 50, nullable = false)
    private String pw;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "address", length = 20, nullable = false)
    private String address;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    public static User createUser(UserDto dto, PasswordEncoder encoder) {
        return User.builder()
                .id(dto.getId())
                .pw(dto.getPw())
                .name(dto.getName())
                .address(dto.getAddress())
                .email(dto.getEmail())
                .role(dto.getRole())
                .store(dto.getStoreId())
                .build();

    }
}