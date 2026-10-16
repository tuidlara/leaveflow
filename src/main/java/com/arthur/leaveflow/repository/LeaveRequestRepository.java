package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.LeaveRequest;
import com.arthur.leaveflow.enums.LeaveStatus;
import com.arthur.leaveflow.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    Page<LeaveRequest> findByLeaveType(LeaveType leaveType, Pageable pageable);

    Page<LeaveRequest> findByStatusAndLeaveType(
            LeaveStatus status,
            LeaveType leaveType,
            Pageable pageable
    );

}
