"use client"

import { useState, useEffect, useCallback } from 'react';
import { supabase } from './supabase';
import type { User } from '@supabase/supabase-js';

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

export type Category = {
  id: string;
  name: string;
  type: 'income' | 'expense';
  icon?: string;
  color?: string;
  is_default: boolean;
  user_id: string;
};

export type CurrencyCode = 'EUR' | 'USD' | 'VES' | 'COP' | 'ARS' | 'MXN' | 'BRL';

const DEFAULT_CATEGORIES: Omit<Category, 'id' | 'user_id'>[] = [
  { name: 'Alimentación', type: 'expense', is_default: true },
  { name: 'Transporte', type: 'expense', is_default: true },
  { name: 'Ocio', type: 'expense', is_default: true },
  { name: 'Hogar', type: 'expense', is_default: true },
  { name: 'Salud', type: 'expense', is_default: true },
  { name: 'Educación', type: 'expense', is_default: true },
  { name: 'Salario', type: 'income', is_default: true },
  { name: 'Freelance', type: 'income', is_default: true },
  { name: 'Inversión', type: 'income', is_default: true },
  { name: 'Otros', type: 'expense', is_default: true },
];

const INITIAL_BUDGETS: Budget[] = DEFAULT_CATEGORIES
  .filter(c => c.type === 'expense')
  .map(cat => ({ category: cat.name, limit: 500 }));

const CURRENCY_DATA: Record<CurrencyCode, { locale: string; symbol: string }> = {
  EUR: { locale: 'es-ES', symbol: '€' },
  USD: { locale: 'en-US', symbol: '$' },
  VES: { locale: 'es-VE', symbol: 'Bs.' },
  COP: { locale: 'es-CO', symbol: '$' },
  ARS: { locale: 'es-AR', symbol: '$' },
  MXN: { locale: 'es-MX', symbol: '$' },
  BRL: { locale: 'pt-BR', symbol: 'R$' },
};

export function useFinanceData(user: User | null) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>(INITIAL_BUDGETS);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoaded, setIsLoaded] = useState(false);
  const [categoriesLoaded, setCategoriesLoaded] = useState(false);
  const [selectedCurrency, setSelectedCurrencyState] = useState<CurrencyCode>('EUR');
  const [useDarkMode, setUseDarkModeState] = useState(false);
  const [editTransaction, setEditTransaction] = useState<Transaction | null>(null);

  // Load categories from Supabase
  const loadCategories = useCallback(async () => {
    if (!user) {
      setCategories(DEFAULT_CATEGORIES.map((c, i) => ({ ...c, id: `default-${i}`, user_id: 'local' })) as Category[]);
      setCategoriesLoaded(true);
      return;
    }
    try {
      const { data } = await supabase
        .from('categories')
        .select('*')
        .eq('user_id', user.id)
        .order('type', { ascending: true })
        .order('name', { ascending: true });

      if (data && data.length > 0) {
        setCategories(data as Category[]);
      } else {
        // Seed defaults
        await seedDefaultCategories(user.id);
        const { data: seeded } = await supabase
          .from('categories')
          .select('*')
          .eq('user_id', user.id);
        if (seeded) setCategories(seeded as Category[]);
      }
    } catch (e) {
      console.error('Load categories error:', e);
      // Fallback to defaults
      setCategories(DEFAULT_CATEGORIES.map((c, i) => ({ ...c, id: `default-${i}`, user_id: user.id })) as Category[]);
    } finally {
      setCategoriesLoaded(true);
    }
  }, [user]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  // Load transactions + budgets + settings
  useEffect(() => {
    if (!categoriesLoaded) return;
    if (!user) {
      setIsLoaded(true);
      return;
    }
    (async () => {
      try {
        const { data: txData } = await supabase
          .from('transactions')
          .select('*')
          .eq('user_id', user.id)
          .order('date', { ascending: false });

        if (txData && txData.length > 0) {
          setTransactions(txData.map((r: any) => ({
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
      } catch (e) {
        // Supabase caído/error de red: seguir con datos locales
        console.error('Load transactions error:', e);
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
  }, [user, categoriesLoaded]);

  // Sync to localStorage
  useEffect(() => {
    if (isLoaded) localStorage.setItem('gestorfacil_transactions', JSON.stringify(transactions));
  }, [transactions, isLoaded]);

  useEffect(() => {
    if (isLoaded) localStorage.setItem('gestorfacil_budgets', JSON.stringify(budgets));
  }, [budgets, isLoaded]);

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

  // Category CRUD (funciona también sin login: modo local)
  const addCategory = async (name: string, type: 'income' | 'expense') => {
    if (!user) {
      const local: Category = {
        id: `local-${Date.now()}`,
        name,
        type,
        is_default: false,
        user_id: 'local',
      };
      setCategories(prev => [...prev, local]);
      return local;
    }
    const { data, error } = await supabase
      .from('categories')
      .insert({ name, type, user_id: user.id, is_default: false })
      .select()
      .single();
    if (!error && data) {
      setCategories(prev => [...prev, data as Category]);
      return data as Category;
    }
    throw error;
  };

  const deleteCategory = async (id: string) => {
    const cat = categories.find(c => c.id === id);
    if (cat?.is_default) throw new Error('No se puede eliminar categoría por defecto');
    if (!user) {
      setCategories(prev => prev.filter(c => c.id !== id));
      return;
    }
    const { error } = await supabase.from('categories').delete().eq('id', id).eq('user_id', user.id);
    if (!error) setCategories(prev => prev.filter(c => c.id !== id));
    else throw error;
  };

  const updateCategory = async (id: string, updates: Partial<Pick<Category, 'name' | 'icon' | 'color'>>) => {
    if (!user) return;
    const { error } = await supabase.from('categories').update(updates).eq('id', id).eq('user_id', user.id);
    if (!error) setCategories(prev => prev.map(c => c.id === id ? { ...c, ...updates } : c));
    else throw error;
  };

  const getCategoriesByType = (type: 'income' | 'expense') =>
    categories.filter(c => c.type === type).map(c => c.name);

  const addTransaction = async (tx: Omit<Transaction, 'id'>) => {
    if (!user) {
      const tmpId = `tmp_${Date.now()}`;
      setTransactions(prev => [{ ...tx, id: tmpId } as Transaction, ...prev]);
      return;
    }
    const { data, error } = await supabase.from('transactions').insert({
      date: tx.date,
      description: tx.description,
      amount: tx.amount,
      category: tx.category,
      type: tx.type,
      user_id: user.id,
    }).select().single();
    if (error) {
      const tmpId = `tmp_${Date.now()}`;
      setTransactions(prev => [{ ...tx, id: tmpId } as Transaction, ...prev]);
    } else if (data) {
      setTransactions(prev => [{
        id: String(data.id),
        date: data.date,
        description: data.description,
        amount: data.amount,
        category: data.category,
        type: data.type as 'income' | 'expense',
      }, ...prev]);
    }
  };

  const deleteTransaction = (id: string) => {
    const numId = parseInt(id, 10);
    if (user && !isNaN(numId)) supabase.from('transactions').delete().eq('id', numId).eq('user_id', user.id).then();
    setTransactions(prev => prev.filter(t => t.id !== id));
  };

  const updateTransaction = (id: string, data: Omit<Transaction, 'id'>) => {
    const numId = parseInt(id, 10);
    if (user && !isNaN(numId)) {
      supabase.from('transactions').update({
        date: data.date,
        description: data.description,
        amount: data.amount,
        category: data.category,
        type: data.type,
      }).eq('id', numId).eq('user_id', user.id).then();
    }
    setTransactions(prev => prev.map(t => t.id === id ? { ...data, id } : t));
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
    categories,
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
    addCategory,
    deleteCategory,
    updateCategory,
    getCategoriesByType,
  };
}

async function seedDefaultCategories(userId: string) {
  const rows = DEFAULT_CATEGORIES.map(c => ({ ...c, user_id: userId }));
  await supabase.from('categories').insert(rows);
}