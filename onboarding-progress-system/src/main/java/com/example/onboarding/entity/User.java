package com.example.onboarding.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(length = 100)
    private String department;

    @Column(name = "joined_on")
    private LocalDate joinedOn;

    /** BCrypt hash of the password. null means the user cannot log in. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.MEMBER;

    /** SHA-256 hash of the personal link token (/join/{token}). null means no link. */
    @Column(name = "access_token_hash", length = 64)
    private String accessTokenHash;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public void setJoinedOn(LocalDate joinedOn) {
        this.joinedOn = joinedOn;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** True when a password is set, i.e. the user can log in. */
    public boolean isLoginEnabled() {
        return passwordHash != null;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public String getAccessTokenHash() {
        return accessTokenHash;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessLink(String tokenHash, Instant expiresAt) {
        this.accessTokenHash = tokenHash;
        this.accessTokenExpiresAt = expiresAt;
    }

    public void clearAccessLink() {
        setAccessLink(null, null);
    }

    public boolean hasAccessLink() {
        return accessTokenHash != null;
    }

    /** True when a personal link exists and has not expired yet. */
    public boolean hasValidAccessLink(Instant now) {
        return accessTokenHash != null && accessTokenExpiresAt != null && now.isBefore(accessTokenExpiresAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
