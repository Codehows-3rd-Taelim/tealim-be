package com.codehows.taelimbe.entity;

import com.codehows.taelimbe.constant.Role;
import com.codehows.taelimbe.dto.UserDTO;
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

    @Column(name = "pw", length = 255, nullable = false)
    private String pw;

    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @Column(name = "phone", length = 11, nullable = false)
    private String phone;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    public static User createUser(UserDTO dto, PasswordEncoder encoder, Store store) {

        return User.builder()
                .id(dto.getId())
                .pw(encoder.encode(dto.getPw()))
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .role(dto.getRole())
                .store(store)
                .build();
    }
}