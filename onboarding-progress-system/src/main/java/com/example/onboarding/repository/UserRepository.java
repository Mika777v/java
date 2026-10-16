package com.example.onboarding.repository;

import com.example.onboarding.entity.Role;
import com.example.onboarding.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    Optional<User> findByAccessTokenHash(String accessTokenHash);
}
