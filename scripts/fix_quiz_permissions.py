import mysql.connector

conn = mysql.connector.connect(host='localhost', user='root', password='123456', database='gsmv', charset='utf8mb4')
cursor = conn.cursor()

# 1. 给 STUDENT 和 PUBLIC 角色添加 QUIZ_READ 权限
sql = """
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.code IN ('STUDENT', 'PUBLIC', 'RESEARCHER', 'VIEWER')
  AND p.code = 'QUIZ_READ'
"""
try:
    cursor.execute(sql)
    conn.commit()
    print(f'OK - inserted {cursor.rowcount} role-permission mappings for QUIZ_READ')
except Exception as e:
    conn.rollback()
    print(f'ERROR: {e}')

# 2. 验证
print('\n=== Verification ===')
cursor.execute("""
    SELECT r.code, GROUP_CONCAT(p.code ORDER BY p.code SEPARATOR ', ') as permissions
    FROM sys_role r
    LEFT JOIN sys_role_permission rp ON r.id = rp.role_id
    LEFT JOIN sys_permission p ON rp.permission_id = p.id
    WHERE r.code IN ('STUDENT', 'PUBLIC', 'RESEARCHER', 'VIEWER', 'ADMIN')
    GROUP BY r.code
""")
for row in cursor.fetchall():
    print(f'  {row[0]:12s}: {row[1]}')

cursor.close()
conn.close()
print('\nDone!')
