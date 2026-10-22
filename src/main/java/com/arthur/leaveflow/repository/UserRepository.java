package com.arthur.leaveflow.repository;

import com.arthur.leaveflow.entity.User;
import com.arthur.leaveflow.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

}
