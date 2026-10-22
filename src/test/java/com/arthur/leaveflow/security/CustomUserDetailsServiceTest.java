package com.arthur.leaveflow.security;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void deveCarregarUsuarioQuandoEmailExiste() {
        User user = new User(
                "admin@leaveflow.com",
                "senha-criptografada",
                Role.ADMIN
        );

        when(userRepository.findByEmail("admin@leaveflow.com"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService
                .loadUserByUsername("admin@leaveflow.com");

        assertEquals("admin@leaveflow.com", userDetails.getUsername());
        assertEquals("senha-criptografada", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void deveLancarExcecaoQuandoEmailNaoExiste() {
        when(userRepository.findByEmail("inexistente@leaveflow.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(
                        "inexistente@leaveflow.com"
                )
        );
    }
}