CREATE TABLE IF NOT EXISTS gamers (
    id BIGSERIAL PRIMARY KEY,
    login TEXT NOT NULL,
    password TEXT NOT NULL,
    email TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gamers_login ON gamers(login);
-- PostgreSQL позволяет множественные NULL в уникальном индексе
CREATE INDEX  IF NOT EXISTS idx_gamers_email ON gamers(email) WHERE email IS NOT NULL;

-- таблица restore_passwords и ее индексы
CREATE TABLE IF NOT EXISTS restore_passwords (
     login TEXT NOT NULL,
     code INTEGER NOT NULL,
     created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_restore_passwords_login ON restore_passwords(login);
