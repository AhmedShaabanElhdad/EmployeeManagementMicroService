package com.example.authservice.repo;

import com.example.authservice.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepo extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);
    
    long countByRole(UserAccount.ROLE role);
    long countByEnabled(boolean enabled);
    long countByAccountLocked(boolean locked);
}
