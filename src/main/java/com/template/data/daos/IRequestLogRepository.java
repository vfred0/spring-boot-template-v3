package com.template.data.daos;

import com.template.data.entities.core.RequestLog;
import org.springframework.stereotype.Repository;

@Repository
public interface IRequestLogRepository extends IRepository<RequestLog, Long> {
}
