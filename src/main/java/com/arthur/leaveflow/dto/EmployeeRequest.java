package com.arthur.leaveflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeRequest(

        @NotBlank(message = "Nome não pode ser vazio.")
        String nome,

        @Email(message = "Email inválido.")
        @NotBlank(message = "Email não pode ser vazio.")
        String email,

        @NotBlank(message = "Departamento não pode ser vazio.")
        String department
) {
}
