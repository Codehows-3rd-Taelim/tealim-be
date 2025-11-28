package com.codehows.taelimbe.user.service;

import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

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

    public void deleteUser(Long userId) {
        userRepository.findById(userId)
                .ifPresentOrElse(
                        user -> userRepository.delete(user),
                        () -> { throw new IllegalArgumentException("해당 ID의 직원을 찾을 수 없습니다: " + userId); }
                );
    }
}
