package com.needitbe.domain.useraction.repository;

import com.needitbe.domain.useraction.entity.UserActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActionLogRepository
        extends JpaRepository<UserActionLog, Long> {

    boolean existsByEventId(String eventId);
}