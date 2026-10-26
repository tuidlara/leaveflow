package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.EmployeeRegistrationRequest;
import com.arthur.leaveflow.dto.EmployeeRegistrationResponse;
import com.arthur.leaveflow.dto.EmployeeResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.enums.Role;
import com.arthur.leaveflow.exception.EmailAlreadyExistsException;
import com.arthur.leaveflow.exception.EmployeeNotFoundException;
import com.arthur.leaveflow.exception.InvalidRoleException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserService userService;

    public EmployeeService(EmployeeRepository employeeRepository, UserService userService) {
        this.employeeRepository = employeeRepository;
        this.userService = userService;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getNome(),
                employee.getEmail(),
                employee.getDepartment()
        );
    }

    private EmployeeRegistrationResponse toRegistrationResponse(Employee employee, Role role) {
        return new EmployeeRegistrationResponse(
                employee.getId(),
                employee.getNome(),
                employee.getEmail(),
                employee.getDepartment(),
                role
        );
    }

    private Employee toEntity(EmployeeRegistrationRequest request) {
        return new Employee(
                request.nome(),
                request.email(),
                request.department());
    }

    @Transactional
    public EmployeeRegistrationResponse criarFuncionario(EmployeeRegistrationRequest request) {
        Employee employee = toEntity(request);
        if (employeeRepository.existsByEmail(employee.getEmail())) {
            throw new EmailAlreadyExistsException("Email já existe!");
        }

        if (request.role() == Role.ADMIN) {
            throw new InvalidRoleException("Não é permitido criar usuários ADMIN por esse endpoint.");
        }
        Employee funcionarioSalvo = employeeRepository.save(employee);

        userService.criarUsuario(
                request.email(),
                request.senha(),
                request.role(),
                funcionarioSalvo.getId()
        );
        return toRegistrationResponse(funcionarioSalvo, request.role());
    }

    public Page<EmployeeResponse> listarFuncionarios(Pageable pageable) {
        Page<Employee> funcionarios;
        funcionarios = employeeRepository.findAll(pageable);
        return funcionarios.map(this::toResponse);
    }

    public EmployeeResponse buscarFuncionarioPorId(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Funcionário não encontrado!"));
        return toResponse(employee);
    }

}
