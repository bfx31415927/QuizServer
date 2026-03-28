-- V3__Change_id_to_bigserial.sql
-- Правильная миграция (последовательность уже существует)

BEGIN;

-- 1. Убираем DEFAULT (разрываем связь с последовательностью)
ALTER TABLE gamers ALTER COLUMN id DROP DEFAULT;

-- 2. Меняем тип столбца с INTEGER на BIGINT
ALTER TABLE gamers ALTER COLUMN id TYPE BIGINT;

-- 3. Обновляем последовательность (если нужно)
--    Устанавливаем следующее значение на основе максимального ID
SELECT setval('gamers_id_seq', COALESCE((SELECT MAX(id) FROM gamers), 0) + 1);

-- 4. Восстанавливаем DEFAULT
ALTER TABLE gamers ALTER COLUMN id SET DEFAULT nextval('gamers_id_seq');

-- 5. Проверяем привязку последовательности (должна сохраниться)
--    Но на всякий случай пересоздаем привязку
ALTER SEQUENCE gamers_id_seq OWNED BY gamers.id;

COMMIT;
