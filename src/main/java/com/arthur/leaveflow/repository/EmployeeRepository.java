package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdWithLock(@Param("id") Long id);
}
