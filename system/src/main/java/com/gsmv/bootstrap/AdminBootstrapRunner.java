package com.gsmv.bootstrap;

import com.gsmv.config.AdminProperties;
import com.gsmv.user.mapper.RoleMapper;
import com.gsmv.user.mapper.UserMapper;
import com.gsmv.user.model.SysRole;
import com.gsmv.user.model.SysUser;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminProperties adminProperties;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            AdminProperties adminProperties,
            UserMapper userMapper,
            RoleMapper roleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.adminProperties = adminProperties;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!adminProperties.enabled()) {
            return;
        }

        SysRole adminRole = roleMapper.findByCode("ADMIN");
        if (adminRole == null) {
            log.warn("ADMIN role is missing, skip bootstrap admin setup.");
            return;
        }

        SysUser adminUser = userMapper.findByUsername(adminProperties.username());
        if (adminUser == null) {
            // 首次初始化：创建 admin 用户并分配角色
            SysUser user = new SysUser();
            user.setUsername(adminProperties.username());
            user.setDisplayName(adminProperties.displayName());
            user.setPasswordHash(passwordEncoder.encode(adminProperties.bootstrapPassword()));
            user.setStatus(1);
            user.setApprovalStatus("APPROVED");
            user.setReviewedAt(LocalDateTime.now());
            userMapper.insert(user);
            roleMapper.insertUserRoles(user.getId(), List.of(adminRole.getId()));
            log.info("Bootstrap admin created. username={}, password={}", adminProperties.username(), adminProperties.bootstrapPassword());
            return;
        }

        // admin 已存在：确保每次启动都补上可能缺失的 ADMIN 角色
        List<String> existingRoleCodes = roleMapper.findRoleCodesByUserId(adminUser.getId());
        if (existingRoleCodes == null || !existingRoleCodes.contains("ADMIN")) {
            List<Long> existingRoleIds = roleMapper.findRolesByUserId(adminUser.getId())
                    .stream().map(SysRole::getId).toList();
            if (!existingRoleIds.contains(adminRole.getId())) {
                roleMapper.insertUserRoles(adminUser.getId(), List.of(adminRole.getId()));
                log.info("Bootstrap assigned missing ADMIN role to existing admin user id={}", adminUser.getId());
            }
        }
    }
}
