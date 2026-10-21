package com.arthur.leaveflow.service;

import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.UserAlreadyExistsException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import com.arthur.leaveflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                employeeRepository
        );
    }

    @Test
    void deveCriarUsuarioComSenhaCriptografada() {
        String email = "arthur@leaveflow.com";
        String senha = "senha123";
        String senhaCriptografada = "hash-da-senha";
        Long employeeId = 1L;
        Role role = Role.EMPLOYEE;

        Employee employee = new Employee("Arthur", email, "TI");

        User userSalvo = new User(email, senhaCriptografada, role, employee);

        when(userRepository.existsByEmail(email)).thenReturn(false);

        when(employeeRepository.findById(employeeId))
                .thenReturn(Optional.of(employee));

        when(passwordEncoder.encode(senha)).thenReturn(senhaCriptografada);

        when(userRepository.save(any(User.class))).thenReturn(userSalvo);

        User resultado = userService.criarUsuario(email, senha, role, employeeId);

        assertEquals(email, resultado.getEmail());
        assertEquals(senhaCriptografada, resultado.getSenha());
        assertEquals(role, resultado.getRole());
        assertEquals(employee, resultado.getEmployee());

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
                () -> userService.criarUsuario(email, senha, Role.EMPLOYEE, 1L)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    void deveCriarAdminSemFuncionarioVinculado() {
        String email = "admin@leaveflow.com";
        String senha = "senha123";
        String senhaCriptografada = "hash-da-senha";

        User adminSalvo = new User(email, senhaCriptografada, Role.ADMIN);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(senha)).thenReturn(senhaCriptografada);
        when(userRepository.save(any(User.class))).thenReturn(adminSalvo);

        User resultado = userService.criarUsuario(email, senha, Role.ADMIN, null);

        assertEquals(Role.ADMIN, resultado.getRole());
        assertNull(resultado.getEmployee());
        assertEquals(senhaCriptografada, resultado.getSenha());

        verify(employeeRepository, never()).findById(anyLong());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoQuandoEmployeeNaoTemIdVinculado() {
        String email = "employee@leaveflow.com";
        String senha = "senha123";

        when(userRepository.existsByEmail(email)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.criarUsuario(email, senha, Role.EMPLOYEE, null)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(employeeRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deveLancarExcecaoQuandoFuncionarioNaoExiste() {
        String email = "employee@leaveflow.com";
        String senha = "senha123";
        Long employeeId = 99L;

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.criarUsuario(email, senha, Role.EMPLOYEE, employeeId)
        );

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
