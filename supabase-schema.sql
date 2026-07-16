-- 1. Agregar columna user_id para asociar transacciones al usuario autenticado
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions (user_id);

-- 2. Activar RLS y crear política para que cada usuario solo vea sus datos
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Allow own device" ON transactions;
DROP POLICY IF EXISTS "Allow all anon" ON transactions;
DROP POLICY IF EXISTS "Users own data" ON transactions;
CREATE POLICY "Users own data" ON transactions
  FOR ALL USING (auth.uid() = user_id);

-- 3. DESPUÉS de iniciar sesión la primera vez, ejecuta esto para migrar tus transacciones viejas:
-- UPDATE transactions SET user_id = 'TU-USER-ID-AQUI' WHERE user_id IS NULL;
