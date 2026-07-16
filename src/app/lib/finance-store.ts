"use client"

import { useState, useEffect } from 'react';
import { supabase } from './supabase';

export type Transaction = {
  id: string;
  date: string;
  description: string;
  amount: number;
  category: string;
  type: 'income' | 'expense';
};

export type Budget = {
  category: string;
  limit: number;
};

export type CurrencyCode = 'EUR' | 'USD' | 'VES' | 'COP' | 'ARS' | 'MXN' | 'BRL';

const DEFAULT_CATEGORIES = [
  'Alimentación', 'Transporte', 'Ocio', 'Hogar',
  'Salud', 'Educación', 'Salario', 'Freelance', 'Inversión', 'Otros'
];

const INITIAL_BUDGETS: Budget[] = DEFAULT_CATEGORIES.map(cat => ({
  category: cat, limit: 500
}));

const CURRENCY_DATA: Record<CurrencyCode, { locale: string; symbol: string }> = {
  EUR: { locale: 'es-ES', symbol: '€' },
  USD: { locale: 'en-US', symbol: '$' },
  VES: { locale: 'es-VE', symbol: 'Bs.' },
  COP: { locale: 'es-CO', symbol: '$' },
  ARS: { locale: 'es-AR', symbol: '$' },
  MXN: { locale: 'es-MX', symbol: '$' },
  BRL: { locale: 'pt-BR', symbol: 'R$' },
};

export function useFinanceData() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>(INITIAL_BUDGETS);
  const [isLoaded, setIsLoaded] = useState(false);
  const [selectedCurrency, setSelectedCurrencyState] = useState<CurrencyCode>('EUR');
  const [useDarkMode, setUseDarkModeState] = useState(false);
  const [editTransaction, setEditTransaction] = useState<Transaction | null>(null);

  // Load from Supabase + localStorage fallback
  useEffect(() => {
    (async () => {
      const { data } = await supabase.from('transactions').select('*').order('date', { ascending: false });
      if (data && data.length > 0) {
        setTransactions(data.map((r: any) => ({
          id: String(r.id),
          date: r.date,
          description: r.description,
          amount: r.amount,
          category: r.category,
          type: r.type as 'income' | 'expense',
        })));
      } else {
        const saved = localStorage.getItem('gestorfacil_transactions');
        if (saved) setTransactions(JSON.parse(saved));
      }
      const savedBudgets = localStorage.getItem('gestorfacil_budgets');
      const savedCurrency = localStorage.getItem('gestorfacil_currency');
      const savedDarkMode = localStorage.getItem('gestorfacil_dark_mode');
      if (savedBudgets) setBudgets(JSON.parse(savedBudgets));
      if (savedCurrency) setSelectedCurrencyState(savedCurrency as CurrencyCode);
      if (savedDarkMode) setUseDarkModeState(savedDarkMode === 'true');
      setIsLoaded(true);
    })();
  }, []);

  // Sync to localStorage
  useEffect(() => {
    if (isLoaded) localStorage.setItem('gestorfacil_transactions', JSON.stringify(transactions));
  }, [transactions, isLoaded]);

  useEffect(() => {
    if (isLoaded) localStorage.setItem('gestorfacil_budgets', JSON.stringify(budgets));
  }, [budgets, isLoaded]);

  const syncToSupabase = async (txs: Transaction[]) => {
    try {
      const { error } = await supabase.from('transactions').upsert(
        txs.map(t => ({
          id: Number(t.id),
          date: t.date,
          description: t.description,
          amount: t.amount,
          category: t.category,
          type: t.type,
        })),
        { onConflict: 'id' }
      );
      if (error) console.warn('Supabase sync error:', error.message);
    } catch { /* offline */ }
  };

  const setSelectedCurrency = (c: CurrencyCode) => {
    setSelectedCurrencyState(c);
    localStorage.setItem('gestorfacil_currency', c);
  };

  const toggleDarkMode = () => {
    setUseDarkModeState(prev => {
      const next = !prev;
      localStorage.setItem('gestorfacil_dark_mode', String(next));
      if (next) document.documentElement.classList.add('dark');
      else document.documentElement.classList.remove('dark');
      return next;
    });
  };

  const formatCurrency = (val: number) => {
    const data = CURRENCY_DATA[selectedCurrency];
    return new Intl.NumberFormat(data.locale, {
      style: 'currency',
      currency: selectedCurrency,
    }).format(val);
  };

  const addTransaction = (transaction: Omit<Transaction, 'id'>) => {
    const newTransaction = { ...transaction, id: Math.random().toString(36).substring(2, 9) };
    setTransactions(prev => {
      const next = [newTransaction, ...prev];
      syncToSupabase(next);
      return next;
    });
  };

  const deleteTransaction = (id: string) => {
    setTransactions(prev => {
      const next = prev.filter(t => t.id !== id);
      supabase.from('transactions').delete().eq('id', Number(id)).then();
      return next;
    });
  };

  const updateTransaction = (id: string, data: Omit<Transaction, 'id'>) => {
    setTransactions(prev => {
      const next = prev.map(t => t.id === id ? { ...data, id } : t);
      syncToSupabase(next);
      return next;
    });
  };

  const updateBudget = (category: string, limit: number) => {
    setBudgets(prev => prev.map(b => b.category === category ? { ...b, limit } : b));
  };

  const getBudgetsWithSpent = () => {
    return budgets.map(budget => {
      const spent = transactions
        .filter(t => t.type === 'expense' && t.category === budget.category)
        .reduce((sum, t) => sum + Math.abs(t.amount), 0);
      return { ...budget, spent };
    });
  };

  const totals = transactions.reduce((acc, t) => {
    if (t.type === 'income') acc.income += t.amount;
    else acc.expense += Math.abs(t.amount);
    acc.balance = acc.income - acc.expense;
    return acc;
  }, { income: 0, expense: 0, balance: 0 });

  return {
    transactions,
    budgets,
    addTransaction,
    deleteTransaction,
    updateTransaction,
    updateBudget,
    getBudgetsWithSpent,
    totals,
    isLoaded,
    selectedCurrency,
    setSelectedCurrency,
    formatCurrency,
    useDarkMode,
    toggleDarkMode,
    editTransaction,
    setEditTransaction,
    categories: DEFAULT_CATEGORIES,
  };
}
