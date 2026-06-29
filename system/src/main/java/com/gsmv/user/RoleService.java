package com.gsmv.user;

import com.gsmv.audit.service.AuditService;
import com.gsmv.common.ErrorCode;
import com.gsmv.common.PageResponse;
import com.gsmv.common.exception.BusinessException;
import com.gsmv.common.exception.NotFoundException;
import com.gsmv.security.SecurityUtils;
import com.gsmv.user.dto.PermissionOption;
import com.gsmv.user.dto.RoleCreateRequest;
import com.gsmv.user.dto.RoleDetailView;
import com.gsmv.user.dto.RoleUpdateRequest;
import com.gsmv.user.mapper.PermissionMapper;
import com.gsmv.user.mapper.RoleMapper;
import com.gsmv.user.model.SysPermission;
import com.gsmv.user.model.SysRole;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuditService auditService;

    public RoleService(RoleMapper roleMapper, PermissionMapper permissionMapper, AuditService auditService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.auditService = auditService;
    }

    public PageResponse<RoleDetailView> listRoles(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;
        List<RoleDetailView> items = roleMapper.findPage(keyword, safeSize, offset).stream()
                .map(this::toDetailView)
                .toList();
        long total = roleMapper.count(keyword);
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    public RoleDetailView getRole(Long id) {
        return toDetailView(requireRole(id));
    }

    public List<PermissionOption> listPermissions() {
        return permissionMapper.findAll().stream()
                .map(this::toPermissionOption)
                .toList();
    }

    @Transactional
    public RoleDetailView createRole(RoleCreateRequest request) {
        if (roleMapper.findByCode(request.code().trim()) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色代码已存在", HttpStatus.CONFLICT);
        }
        SysRole role = new SysRole();
        role.setCode(request.code().trim());
        role.setName(request.name().trim());
        role.setDescription(request.description());
        roleMapper.insert(role);

        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            permissionMapper.insertRolePermissions(role.getId(), request.permissionIds());
        }

        auditService.record(SecurityUtils.requireCurrentUser().userId(), "ROLE", "CREATE", "SYS_ROLE", role.getId(), true,
                "{\"code\":\"" + role.getCode() + "\",\"name\":\"" + role.getName() + "\"}");
        return toDetailView(roleMapper.findById(role.getId()));
    }

    @Transactional
    public RoleDetailView updateRole(Long id, RoleUpdateRequest request) {
        SysRole role = requireRole(id);
        role.setName(request.name().trim());
        role.setDescription(request.description());
        roleMapper.update(role);

        // Replace permissions
        permissionMapper.deleteRolePermissions(id);
        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            permissionMapper.insertRolePermissions(id, request.permissionIds());
        }

        auditService.record(SecurityUtils.requireCurrentUser().userId(), "ROLE", "UPDATE", "SYS_ROLE", id, true,
                "{\"code\":\"" + role.getCode() + "\",\"name\":\"" + role.getName() + "\"}");
        return toDetailView(roleMapper.findById(id));
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = requireRole(id);
        if ("ADMIN".equals(role.getCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能删除系统管理员角色", HttpStatus.BAD_REQUEST);
        }
        // Check if any users still have this role
        int userCount = countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "该角色下还有 " + userCount + " 个用户，请先将用户移出此角色", HttpStatus.BAD_REQUEST);
        }
        permissionMapper.deleteRolePermissions(id);
        roleMapper.delete(id);
        auditService.record(SecurityUtils.requireCurrentUser().userId(), "ROLE", "DELETE", "SYS_ROLE", id, true,
                "{\"code\":\"" + role.getCode() + "\"}");
    }

    private SysRole requireRole(Long id) {
        SysRole role = roleMapper.findById(id);
        if (role == null) {
            throw new NotFoundException("角色不存在");
        }
        return role;
    }

    private int countUsersByRoleId(Long roleId) {
        return roleMapper.countUsersByRoleId(roleId);
    }

    private RoleDetailView toDetailView(SysRole role) {
        List<PermissionOption> perms = permissionMapper.findByRoleId(role.getId()).stream()
                .map(this::toPermissionOption)
                .toList();
        int userCount = countUsersByRoleId(role.getId());
        return new RoleDetailView(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                perms, userCount, role.getCreatedAt());
    }

    private PermissionOption toPermissionOption(SysPermission p) {
        return new PermissionOption(p.getId(), p.getCode(), p.getName(), p.getDescription());
    }
}
