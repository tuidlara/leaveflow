package com.arthur.leaveflow.entity;

import com.arthur.leaveflow.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @OneToOne
    @JoinColumn(name = "employee_id", unique = true, nullable = true)
    private Employee employee;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public User(String email, String senha, Role role, Employee employee) {
        this.email = email;
        this.senha = senha;
        this.role = role;
        this.employee = employee;
    }

    public User(String email, String senha, Role role) {
        this.email = email;
        this.senha = senha;
        this.role = role;
    }

    protected User() {
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getSenha() {
        return senha;
    }

    public Role getRole() {
        return role;
    }

    public Employee getEmployee() {
        return employee;
    }
}

