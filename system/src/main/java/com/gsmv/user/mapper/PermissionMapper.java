package com.gsmv.user.mapper;

import com.gsmv.user.model.SysPermission;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PermissionMapper {

    @Select("""
            SELECT DISTINCT p.code
            FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId}
            ORDER BY p.code
            """)
    List<String> findPermissionCodesByUserId(Long userId);

    @Select("SELECT * FROM sys_permission ORDER BY id")
    List<SysPermission> findAll();

    @Select("SELECT * FROM sys_permission WHERE id = #{id}")
    SysPermission findById(Long id);

    @Select("""
            SELECT p.* FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id
            WHERE rp.role_id = #{roleId}
            ORDER BY p.id
            """)
    List<SysPermission> findByRoleId(Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id)
            VALUES
            <foreach collection='permissionIds' item='permId' separator=','>
              (#{roleId}, #{permId})
            </foreach>
            </script>
            """)
    void insertRolePermissions(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    void deleteRolePermissions(Long roleId);
}
