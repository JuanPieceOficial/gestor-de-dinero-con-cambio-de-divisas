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

-- 3. Crear tabla de categorías por usuario
CREATE TABLE IF NOT EXISTS categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('income', 'expense')),
  icon TEXT,
  color TEXT,
  "order" INTEGER DEFAULT 0,
  is_default BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índice para consultas rápidas
CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories (user_id);
CREATE INDEX IF NOT EXISTS idx_categories_user_type ON categories (user_id, type);

-- RLS para categorías
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users own categories" ON categories
  FOR ALL USING (auth.uid() = user_id);

-- 4. Trigger para actualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_categories_updated_at ON categories;
CREATE TRIGGER update_categories_updated_at
  BEFORE UPDATE ON categories
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5. Insertar categorías por defecto para nuevo usuario (se ejecuta al registrarse)
-- Nota: Esto se hará desde la app al detectar usuario sin categorías

-- 6. DESPUÉS de iniciar sesión la primera vez, ejecuta esto para migrar transacciones viejas:
-- UPDATE transactions SET user_id = 'TU-USER-ID-AQUI' WHERE user_id IS NULL;