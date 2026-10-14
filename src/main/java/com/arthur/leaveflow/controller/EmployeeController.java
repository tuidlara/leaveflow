package com.arthur.leaveflow.controller;

import com.arthur.leaveflow.dto.EmployeeRequest;
import com.arthur.leaveflow.dto.EmployeeResponse;
import com.arthur.leaveflow.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse createEmployee (@Valid @RequestBody EmployeeRequest request) {
        return employeeService.criarFuncionario(request);
    }

    @GetMapping
    public Page <EmployeeResponse> listAllEmployees(Pageable pageable) {
        return employeeService.listarFuncionarios(pageable);
    }
}
