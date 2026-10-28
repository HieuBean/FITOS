-- Repair only the invalid hash shipped with the original seeded admin.
-- Keep any password that an administrator has already changed.
UPDATE users
SET password_hash = '$2a$10$VtLeWIzr3aQO2ZZPpanOkuypePoSDRDJ7CuDyYlOA1tRuVaJnP7iC'
WHERE email = 'admin@fitos.com'
  AND password_hash = '$2a$10$WpQjY/7rG4p4q3m4E7wL3.cI1X6y8U4Z3i8V5M2z7B9Q0k1L2N3O.';
