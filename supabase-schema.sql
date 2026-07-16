-- 1. Añadir columna user_id
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions (user_id);

-- 2. Asignar tus transacciones existentes a tu usuario (ejecuta esto DESPUÉS de iniciar sesión la primera vez)
-- UPDATE transactions SET user_id = 'TU-AUTH-USER-ID-AQUI' WHERE user_id IS NULL;

-- 3. RLS: cada usuario solo ve sus datos
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow own device" ON transactions;
DROP POLICY IF EXISTS "Allow all anon" ON transactions;
CREATE POLICY "Users own data" ON transactions
  FOR ALL USING (auth.uid() = user_id);
