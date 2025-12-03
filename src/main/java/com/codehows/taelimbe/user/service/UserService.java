package com.codehows.taelimbe.user.service;

import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.dto.UserDTO;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public void saveUser(User user){
        validateDuplicateUser(user);
        userRepository.save(user);
    }

    public void validateDuplicateUser(User user)
    {
        boolean loginIdExists = userRepository.existsById(user.getId());
        if (loginIdExists)
        {
            throw new IllegalStateException ("이미 사용 중인 아이디입니다.");
        }
    }

    @Transactional
    public UserDTO updateUser(Long userId, UserDTO dto) {
        // 1. 기존 User 엔티티 조회
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "업데이트 대상 직원(UserId: " + userId + ")을 찾을 수 없습니다."
                ));

        // 2. 아이디 변경 (중복 확인)
        if (dto.getId() != null && !dto.getId().equals(target.getId())) {
            // 중복 확인
            if (userRepository.existsById(dto.getId())) {
                throw new IllegalStateException("이미 사용 중인 아이디입니다.");
            }
            target.setId(dto.getId());
        }

        // 3. 비밀번호 변경 (입력된 경우만)
        if (dto.getPw() != null && !dto.getPw().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(dto.getPw());
            target.setPw(encodedPassword);
        }

        // 4. 이름, 전화번호, 이메일 업데이트
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            target.setName(dto.getName());
        }

        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            target.setPhone(dto.getPhone());
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            target.setEmail(dto.getEmail());
        }

        // 5. Store (매장) 업데이트 처리
        if (dto.getStoreId() != null) {
            Store store = storeRepository.findById(dto.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "매장(StoreId: " + dto.getStoreId() + ")을 찾을 수 없습니다."
                    ));
            target.setStore(store);
        }

        // 6. Role (권한) 업데이트 처리
        if (dto.getRole() != null) {
            target.setRole(dto.getRole());
        }

        // 7. 업데이트된 엔티티 저장
        User updated = userRepository.save(target);

        // 8. 업데이트된 엔티티를 DTO로 변환하여 반환
        return UserDTO.from(updated);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .ifPresentOrElse(
                        user -> userRepository.delete(user),
                        () -> { throw new IllegalArgumentException("해당 ID의 직원을 찾을 수 없습니다: " + userId); }
                );
    }
}
