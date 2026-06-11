package com.template.data.daos;

import com.template.data.entities.core.rbac.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r JOIN FETCH r.permissions WHERE ur.keycloakSub = :sub")
    Optional<UserRole> findByKeycloakSub(@Param("sub") String keycloakSub);

    boolean existsByRoleId(Long roleId);

    long deleteByKeycloakSub(String keycloakSub);
}
