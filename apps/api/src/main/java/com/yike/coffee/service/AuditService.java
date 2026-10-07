package com.yike.coffee.service;

import com.yike.coffee.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(String action, String targetType, String targetId, String details) {
        jdbc.update("INSERT INTO audit_log(id,operator_id,action,target_type,target_id,details) VALUES(?,?,?,?,?,?)",
            UUID.randomUUID().toString(), CurrentUser.required().userId(), action, targetType, targetId, details);
    }
}
