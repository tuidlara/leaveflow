package com.arthur.leaveflow.config;

import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.repository.UserRepository;
import com.arthur.leaveflow.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserService userService;
    private final UserRepository userRepository;

    public AdminInitializer(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        String email = System.getenv("ADMIN_EMAIL");
        String senha = System.getenv("ADMIN_PASSWORD");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("A variável de ambiente ADMIN_EMAIL não foi configurada.");
        }

        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException("A variável de ambiente ADMIN_PASSWORD não foi configurada.");
        }

        userService.criarUsuario(email, senha, Role.ADMIN, null);

    }
}
