package com.arthur.leaveflow.service;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.UserAlreadyExistsException;
import com.arthur.leaveflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User criarUsuario(String email, String senha, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Já existe uma conta com esse e-mail.");
        }

        String senhaCriptografada = passwordEncoder.encode(senha);

        User user = new User(email, senhaCriptografada, role);

        return userRepository.save(user);
    }

}
