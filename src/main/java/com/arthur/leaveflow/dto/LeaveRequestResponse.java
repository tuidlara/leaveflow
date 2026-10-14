package com.arthur.leaveflow.dto;

import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(

        Long id,
        Long employeeId,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        LocalDateTime createdAt,
        String decisionReason,
        String decidedBy,
        LocalDateTime decidedAt
) {
}
