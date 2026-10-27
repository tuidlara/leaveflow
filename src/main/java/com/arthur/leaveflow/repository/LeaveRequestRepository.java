
package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByLeaveType(LeaveType leaveType, Pageable pageable);

    Page<LeaveRequest> findByStatusAndLeaveType(
            LeaveStatus status,
            LeaveType leaveType,
            Pageable pageable
    );

    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<LeaveRequest> findByEmployeeIdAndStatus(
            Long employeeId,
            LeaveStatus status,
            Pageable pageable
    );

    Page<LeaveRequest> findByEmployeeIdAndLeaveType(
            Long employeeId,
            LeaveType leaveType,
            Pageable pageable
    );

    Page<LeaveRequest> findByEmployeeIdAndStatusAndLeaveType(
            Long employeeId,
            LeaveStatus status,
            LeaveType leaveType,
            Pageable pageable
    );

    // consulta se ja existe outra solicitação aprovada do mesmo funcionario
    // com o mesmo tipo e com datas que se sobrepoem ao periodo informado

    @Query("""
            SELECT COUNT(l) > 0
            FROM LeaveRequest l
            WHERE l.employee.id = :employeeId
              AND l.leaveType = :leaveType
              AND l.status = :status
              AND l.startDate <= :endDate
              AND l.endDate >= :startDate
              AND l.id <> :requestId
            """)
    boolean existsOverlappingRequest(
            @Param("employeeId") Long employeeId,
            @Param("leaveType") LeaveType leaveType,
            @Param("status") LeaveStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("requestId") Long requestId
    );
}
