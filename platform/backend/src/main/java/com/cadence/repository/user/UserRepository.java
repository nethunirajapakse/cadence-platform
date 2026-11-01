package com.cadence.repository.user;

import com.cadence.entity.User;
import com.cadence.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

    // Emails are matched case-insensitively everywhere (a unique index on
    // lower(email) backs this up).
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.userId = :userId")
    Optional<User> findByIdWithRole(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN u.role r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") RoleName roleName);

    @Query("SELECT u FROM User u JOIN FETCH u.role LEFT JOIN FETCH u.manager ORDER BY u.name")
    List<User> findAllWithRoleAndManager();

    // ---- manager scoping -------------------------------------------------

    @Query("SELECT u.userId FROM User u WHERE u.manager.userId = :managerId")
    List<UUID> findDirectReportIds(@Param("managerId") UUID managerId);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.manager.userId = :managerId ORDER BY u.name")
    List<User> findDirectReports(@Param("managerId") UUID managerId);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.manager.userId = :managerId AND u.active = true")
    List<User> findActiveDirectReports(@Param("managerId") UUID managerId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u "
            + "WHERE u.userId = :userId AND u.manager.userId = :managerId")
    boolean isDirectReportOf(@Param("userId") UUID userId, @Param("managerId") UUID managerId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.manager.userId = :managerId AND u.active = true")
    long countActiveDirectReports(@Param("managerId") UUID managerId);

    @Query("SELECT COUNT(u) FROM User u JOIN u.role r WHERE r.roleName = :roleName AND u.active = true")
    long countActiveByRoleName(@Param("roleName") RoleName roleName);
}
