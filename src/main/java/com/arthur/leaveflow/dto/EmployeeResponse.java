package com.arthur.leaveflow.dto;

public record EmployeeResponse(

        Long id,
        String nome,
        String email,
        String department
) {
}
