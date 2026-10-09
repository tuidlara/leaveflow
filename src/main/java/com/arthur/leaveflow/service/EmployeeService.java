package com.arthur.leaveflow.service;

import com.arthur.leaveflow.dto.EmployeeRequest;
import com.arthur.leaveflow.dto.EmployeeResponse;
import com.arthur.leaveflow.entity.Employee;
import com.arthur.leaveflow.exception.EmailAlreadyExistsException;
import com.arthur.leaveflow.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository){
        this.employeeRepository = employeeRepository;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getNome(),
                employee.getEmail(),
                employee.getDepartment()
        );
    }

    private Employee toEntity(EmployeeRequest request) {
        return new Employee(
                request.nome(),
                request.email(),
                request.department());
    }

    public EmployeeResponse criarFuncionario(EmployeeRequest request){
        Employee employee = toEntity(request);
        if(employeeRepository.existsByEmail(employee.getEmail())){
            throw new EmailAlreadyExistsException("Email já existe");
        }
        Employee funcionarioSalvo = employeeRepository.save(employee);
        return toResponse(funcionarioSalvo);
    }

}
