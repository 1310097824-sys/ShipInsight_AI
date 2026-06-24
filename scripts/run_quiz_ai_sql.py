import mysql.connector

sql = """
CREATE TABLE IF NOT EXISTS quiz_ai_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_quiz_ai_chat_user_time (user_id, created_at, id),
    CONSTRAINT fk_quiz_ai_chat_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"""

conn = mysql.connector.connect(host='localhost', user='root', password='123456', database='gsmv', charset='utf8mb4')
cursor = conn.cursor()

try:
    cursor.execute(sql)
    conn.commit()
    print('OK - quiz_ai_chat_message table created')
except Exception as e:
    conn.rollback()
    print(f'ERROR: {e}')

# Verify
print('\n=== Verification ===')
cursor.execute("SHOW TABLES LIKE 'quiz_ai_chat_message'")
result = cursor.fetchone()
if result:
    print(f'Table exists: {result[0]}')
    cursor.execute('DESCRIBE quiz_ai_chat_message')
    for col in cursor.fetchall():
        print(f'  {col[0]:20s} {col[1]}')
else:
    print('Table NOT found!')

cursor.close()
conn.close()
print('\nDone!')
