package com.arthur.leaveflow.service;

import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.UserAlreadyExistsException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import com.arthur.leaveflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeRepository employeeRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeRepository = employeeRepository;
    }

    public User criarUsuario(String email, String senha, Role role, Long employeeId) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Já existe uma conta com esse e-mail.");
        }

        Employee employee = null;

        if (role == Role.ADMIN) {
            if (employeeId != null) {
                throw new IllegalArgumentException(
                        "Usuários ADMIN não devem ser vinculados a funcionários."
                );
            }
        } else {
            if (employeeId == null) {
                throw new IllegalArgumentException(
                        "É necessário informar um funcionário para esse perfil."
                );
            }

            employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Funcionário não encontrado."
                    ));
        }

        String senhaCriptografada = passwordEncoder.encode(senha);

        User user = new User(email, senhaCriptografada, role, employee);

        return userRepository.save(user);
    }

}
