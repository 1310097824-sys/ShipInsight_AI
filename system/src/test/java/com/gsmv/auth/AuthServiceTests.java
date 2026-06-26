package com.gsmv.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gsmv.audit.service.AuditService;
import com.gsmv.auth.dto.CaptchaResponse;
import com.gsmv.auth.dto.RegisterRequest;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.security.JwtService;
import com.gsmv.user.mapper.PermissionMapper;
import com.gsmv.user.mapper.RoleMapper;
import com.gsmv.user.mapper.UserMapper;
import com.gsmv.user.model.SysRole;
import com.gsmv.user.model.SysUser;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTests {

    @Test
    void registerVerifiesCaptchaBeforeCreatingPendingUser() {
        UserMapper userMapper = mock(UserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuditService auditService = mock(AuditService.class);
        CaptchaService captchaService = fixedCaptchaService("ABCD2");
        CaptchaResponse captcha = captchaService.generate();
        SysRole role = new SysRole();
        role.setId(7L);
        role.setCode("OPERATOR");

        when(userMapper.findByUsername("student01")).thenReturn(null);
        when(roleMapper.findByCode("OPERATOR")).thenReturn(role);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        doAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(11L);
            return null;
        }).when(userMapper).insert(any(SysUser.class));

        service(userMapper, roleMapper, permissionMapper, passwordEncoder, jwtService, auditService, captchaService)
                .register(new RegisterRequest(
                        "student01",
                        "secret",
                        "学生一号",
                        "student@example.com",
                        "13800000000",
                        "operator",
                        captcha.captchaId(),
                        "abcd2"
                ));

        verify(userMapper).insert(any(SysUser.class));
        verify(roleMapper).insertUserRoles(11L, List.of(7L));
        verify(auditService).record(eq(11L), eq("AUTH"), eq("REGISTER"), eq("SYS_USER"), eq(11L), eq(true), any(String.class));
        assertEquals(0, captchaService.size());
    }

    @Test
    void registerRejectsInvalidCaptchaBeforeUserLookup() {
        UserMapper userMapper = mock(UserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuditService auditService = mock(AuditService.class);
        CaptchaService captchaService = fixedCaptchaService("ABCD2");
        CaptchaResponse captcha = captchaService.generate();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service(userMapper, roleMapper, permissionMapper, passwordEncoder, jwtService, auditService, captchaService)
                        .register(new RegisterRequest(
                                "student01",
                                "secret",
                                "学生一号",
                                null,
                                null,
                                "STUDENT",
                                captcha.captchaId(),
                                "WRONG"
                        ))
        );

        assertEquals(ErrorCode.BAD_REQUEST, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("验证码错误或已过期"));
        verifyNoInteractions(userMapper, roleMapper, permissionMapper, passwordEncoder, jwtService, auditService);
    }

    private AuthService service(
            UserMapper userMapper,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService,
            CaptchaService captchaService
    ) {
        return new AuthService(userMapper, roleMapper, permissionMapper, passwordEncoder, jwtService, auditService, captchaService);
    }

    private CaptchaService fixedCaptchaService(String code) {
        return new CaptchaService(
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(5),
                () -> code
        );
    }
}
