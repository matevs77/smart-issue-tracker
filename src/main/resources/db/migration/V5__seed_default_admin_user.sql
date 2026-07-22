INSERT INTO tb_users (username, email, password_hash, role, active)
VALUES ('admin', 'admin@issuetracker.dev', '${admin_password_hash}', 'ADMIN', true);
