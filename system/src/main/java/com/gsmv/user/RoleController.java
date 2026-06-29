package com.gsmv.user;

import com.gsmv.common.ApiResponse;
import com.gsmv.common.PageResponse;
import com.gsmv.user.dto.PermissionOption;
import com.gsmv.user.dto.RoleCreateRequest;
import com.gsmv.user.dto.RoleDetailView;
import com.gsmv.user.dto.RoleUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RoleDetailView>> listRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(roleService.listRoles(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleDetailView> getRole(@PathVariable Long id) {
        return ApiResponse.success(roleService.getRole(id));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionOption>> listPermissions() {
        return ApiResponse.success(roleService.listPermissions());
    }

    @PostMapping
    public ApiResponse<RoleDetailView> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return ApiResponse.success(roleService.createRole(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleDetailView> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return ApiResponse.success(roleService.updateRole(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success();
    }
}
