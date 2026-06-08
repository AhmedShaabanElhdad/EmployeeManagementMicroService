package com.example.authservice.repo;

import com.example.authservice.entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAccountRepo extends JpaRepository<UserAccount, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.username = :username")
    Optional<UserAccount> findByUsernameWithLock(String username);

    Optional<UserAccount> findByUsername(String username);
    
    long countByRole(UserAccount.ROLE role);
    long countByEnabled(boolean enabled);
    long countByAccountLocked(boolean locked);
}
