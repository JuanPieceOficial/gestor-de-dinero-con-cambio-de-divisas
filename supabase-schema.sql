-- Añadir columna device_id a la tabla existente
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS device_id TEXT NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_transactions_device_id ON transactions (device_id);
