-- Create transactions table for GestorFácil
CREATE TABLE IF NOT EXISTS transactions (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  date TEXT NOT NULL,
  description TEXT NOT NULL,
  amount DOUBLE PRECISION NOT NULL,
  category TEXT NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('income', 'expense'))
);

-- Enable Row Level Security (optional for anon key)
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;

-- Allow all operations for anon (public app)
CREATE POLICY "Allow all anon" ON transactions
  FOR ALL USING (true) WITH CHECK (true);
