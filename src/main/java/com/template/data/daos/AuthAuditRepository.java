package com.template.data.daos;

import com.template.data.entities.core.audit.AuthAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAuditRepository extends JpaRepository<AuthAudit, Long> {
}
