package com.arthur.leaveflow.dto;

import com.arthur.leaveflow.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeRegistrationRequest(

        @NotBlank(message = "Nome não pode ser vazio.")
        String nome,

        @Email(message = "Email inválido.")
        @NotBlank(message = "Email não pode ser vazio.")
        String email,

        @NotBlank(message = "Departamento não pode ser vazio.")
        String department,

        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        @NotBlank(message = "A senha é obrigatória.")
        String senha,

        @NotNull(message = "O perfil de acesso é obrigatório.")
        Role role


) {
}
