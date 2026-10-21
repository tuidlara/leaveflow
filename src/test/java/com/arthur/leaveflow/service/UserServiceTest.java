package com.arthur.leaveflow.service;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.UserAlreadyExistsException;
import com.arthur.leaveflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void deveCriarUsuarioComSenhaCriptografada() {
        String email = "arthur@leaveflow.com";
        String senha = "senha123";
        String senhaCriptografada = "hash-da-senha";

        Role role = Role.EMPLOYEE;

        User userSalvo = new User(email, senhaCriptografada, role);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(senha)).thenReturn(senhaCriptografada);
        when(userRepository.save(any(User.class))).thenReturn(userSalvo);

        User resultado = userService.criarUsuario(email, senha, role);

        assertEquals(email, resultado.getEmail());
        assertEquals(senhaCriptografada, resultado.getSenha());
        assertEquals(role, resultado.getRole());

        verify(passwordEncoder).encode(senha);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmailJaCadastrado() {
        String email = "arthur@leaveflow.com";
        String senha = "senha123";

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.criarUsuario(email, senha, Role.EMPLOYEE)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}