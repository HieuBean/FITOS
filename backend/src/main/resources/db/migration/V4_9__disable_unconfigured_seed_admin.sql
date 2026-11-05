-- An unchanged seed password is public knowledge and must not grant access.
-- Accounts with a changed password retain their existing active state.
UPDATE users
SET is_active = FALSE
WHERE email = 'admin@fitos.com'
  AND password_hash = '$2a$10$VtLeWIzr3aQO2ZZPpanOkuypePoSDRDJ7CuDyYlOA1tRuVaJnP7iC';
