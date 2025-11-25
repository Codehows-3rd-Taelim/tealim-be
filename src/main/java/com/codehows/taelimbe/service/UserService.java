package com.codehows.taelimbe.service;

import com.codehows.taelimbe.dto.UserDto;
import com.codehows.taelimbe.entity.Store;
import com.codehows.taelimbe.entity.User;
import com.codehows.taelimbe.repository.StoreRepository;
import com.codehows.taelimbe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserDto dto) {
        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장 정보 없음"));

        User user = User.createUser(dto, store, passwordEncoder);
        return userRepository.save(user);
    }
}
