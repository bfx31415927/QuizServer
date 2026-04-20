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
