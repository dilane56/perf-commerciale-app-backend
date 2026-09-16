-- Dev-only default admin account. Password: Admin@123 (BCrypt hash below). CHANGE IT on first login in every real environment.
INSERT INTO users (first_name, last_name, email, password_hash, active)
VALUES (N'Admin', N'Systeme', N'admin@cbcbourse.local', N'$2a$10$zoM7cZrxi3dZRzVmXlkvHuhBSU6txu9BFu0XuRCFmuGBU2XNSZ5WK', 1);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = N'ADMIN'
WHERE u.email = N'admin@cbcbourse.local';
