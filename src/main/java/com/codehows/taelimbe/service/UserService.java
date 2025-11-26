package com.codehows.taelimbe.service;

import com.codehows.taelimbe.entity.User;
import com.codehows.taelimbe.repository.UserRepository;
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

}
