package com.arthur.leaveflow.dto;

import com.arthur.leaveflow.enums.Role;

public record EmployeeRegistrationResponse(

        Long id,
        String nome,
        String email,
        String department,
        Role role
) {
}
