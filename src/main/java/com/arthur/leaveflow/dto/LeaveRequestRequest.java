package com.arthur.leaveflow.dto;

import com.arthur.leaveflow.enums.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record LeaveRequestRequest(

        @NotNull(message = "Tipo de solicitação precisa ser informado.")
        LeaveType leaveType,

        @NotNull(message = "Insira a data inicial.")
        LocalDate startDate,

        @NotNull(message = "Insira a data de término.")
        LocalDate endDate,

        @NotBlank(message = "O motivo da solicitação é obrigatório.")
        String reason
) {
}
