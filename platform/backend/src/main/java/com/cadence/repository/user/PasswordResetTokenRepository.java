package com.cadence.repository.user;

import com.cadence.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user u JOIN FETCH u.role WHERE t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Query("SELECT t FROM PasswordResetToken t WHERE t.user.userId = :userId AND t.usedAt IS NULL")
    List<PasswordResetToken> findUnusedByUserId(@Param("userId") UUID userId);
}
