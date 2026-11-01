package com.cadence.repository.user;

import com.cadence.entity.Invitation;
import com.cadence.entity.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("SELECT i FROM Invitation i JOIN FETCH i.role LEFT JOIN FETCH i.manager JOIN FETCH i.invitedBy "
            + "WHERE i.tokenHash = :tokenHash")
    Optional<Invitation> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Query("SELECT i FROM Invitation i JOIN FETCH i.role LEFT JOIN FETCH i.manager JOIN FETCH i.invitedBy "
            + "WHERE i.invitationId = :id")
    Optional<Invitation> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT i FROM Invitation i JOIN FETCH i.role LEFT JOIN FETCH i.manager JOIN FETCH i.invitedBy "
            + "ORDER BY i.createdAt DESC")
    List<Invitation> findAllWithDetails();

    @Query("SELECT i FROM Invitation i WHERE LOWER(i.email) = LOWER(:email) AND i.status = :status")
    List<Invitation> findByEmailAndStatus(@Param("email") String email, @Param("status") InvitationStatus status);
}
