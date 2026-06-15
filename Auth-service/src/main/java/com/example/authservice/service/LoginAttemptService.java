package com.example.authservice.service;

import com.example.authservice.entity.AuditLog;
import com.example.authservice.entity.UserAccount;
import com.example.authservice.repo.AuditLogRepo;
import com.example.authservice.repo.UserAccountRepo;
import com.example.shared.core.CustomResponseException;

import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 15;
    private final UserAccountRepo userAccountRepo;
    private final AuditLogRepo auditLogRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UserAccount processPreLogin(String username) {
        UserAccount userAccount = userAccountRepo.findByUsernameWithLock(username)
                .orElseThrow(CustomResponseException::BadCredential);

        if (userAccount.isAccountLocked()) {
            if (unlockWhenTimeExpired(userAccount)) {
                log.info("Account unlocked for user: {}", username);
            } else {
                throw new LockedException("Account is locked. Try again in " + LOCK_TIME_DURATION + " minutes.");
            }
        }
        return userAccount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String username) {
        userAccountRepo.findByUsernameWithLock(username).ifPresent(user -> {
            int newFailAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(newFailAttempts);
            if (newFailAttempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                log.warn("Account locked for user: {}", username);
            }
            userAccountRepo.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        userAccountRepo.findByUsernameWithLock(username).ifPresent(user -> {
            if (user.getFailedAttempts() > 0) {
                user.setFailedAttempts(0);
                user.setLockTime(null);
                userAccountRepo.save(user);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(String username, String action, String details) {
        auditLogRepo.save(AuditLog.builder()
                .username(username)
                .action(action)
                .details(details)
//                .timestamp(Instant.now())
                .build());
    }

    private boolean unlockWhenTimeExpired(UserAccount user) {
        if (user.getLockTime() != null &&
                user.getLockTime().plusMinutes(LOCK_TIME_DURATION).isBefore(LocalDateTime.now())) {
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setFailedAttempts(0);
            userAccountRepo.save(user);
            return true;
        }
        return false;
    }
}
