"use client"

import { useState, useEffect } from 'react';

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

const DEFAULT_CATEGORIES = [
  'Alimentación',
  'Transporte',
  'Ocio',
  'Hogar',
  'Salud',
  'Educación',
  'Otros'
];

const INITIAL_BUDGETS: Budget[] = DEFAULT_CATEGORIES.map(cat => ({
  category: cat,
  limit: 500
}));

export function useFinanceData() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>(INITIAL_BUDGETS);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const savedTransactions = localStorage.getItem('gestorfacil_transactions');
    const savedBudgets = localStorage.getItem('gestorfacil_budgets');

    if (savedTransactions) setTransactions(JSON.parse(savedTransactions));
    if (savedBudgets) setBudgets(JSON.parse(savedBudgets));
    setIsLoaded(true);
  }, []);

  useEffect(() => {
    if (isLoaded) {
      localStorage.setItem('gestorfacil_transactions', JSON.stringify(transactions));
    }
  }, [transactions, isLoaded]);

  useEffect(() => {
    if (isLoaded) {
      localStorage.setItem('gestorfacil_budgets', JSON.stringify(budgets));
    }
  }, [budgets, isLoaded]);

  const addTransaction = (transaction: Omit<Transaction, 'id'>) => {
    const newTransaction = {
      ...transaction,
      id: Math.random().toString(36).substring(2, 9)
    };
    setTransactions(prev => [newTransaction, ...prev]);
  };

  const deleteTransaction = (id: string) => {
    setTransactions(prev => prev.filter(t => t.id !== id));
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
    updateBudget,
    getBudgetsWithSpent,
    totals,
    isLoaded
  };
}
