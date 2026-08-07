"use client"

import { useMemo, useState } from "react";
import type { Transaction } from "@/app/lib/finance-store";
import { Trash2, ShoppingCart, Home, Car, Utensils, HeartPulse, GraduationCap, Briefcase, TrendingUp, CircleEllipsis, Search, ArrowUpRight, ArrowDownRight } from "lucide-react";

interface MobileTransactionsProps {
  transactions: Transaction[];
  onDelete: (id: string) => void;
  onEdit: (tx: Transaction) => void;
  formatCurrency: (val: number) => string;
}

const CATEGORY_ICONS: Record<string, any> = {
  Alimentación: Utensils,
  Transporte: Car,
  Ocio: ShoppingCart,
  Hogar: Home,
  Salud: HeartPulse,
  Educación: GraduationCap,
  Salario: Briefcase,
  Freelance: TrendingUp,
  Inversión: TrendingUp,
  Otros: CircleEllipsis,
};

export function MobileTransactions({ transactions, onDelete, onEdit, formatCurrency }: MobileTransactionsProps) {
  const [search, setSearch] = useState("");

  // Resumen compacto del registro de movimientos (no toca el saldo guardado)
  const summary = useMemo(() => {
    const income = transactions
      .filter((t) => t.type === "income")
      .reduce((s, t) => s + Math.abs(t.amount), 0);
    const expense = transactions
      .filter((t) => t.type === "expense")
      .reduce((s, t) => s + Math.abs(t.amount), 0);
    return { income, expense, balance: income - expense };
  }, [transactions]);

  const filtered = search.trim()
    ? transactions.filter(
        (t) =>
          t.description.toLowerCase().includes(search.toLowerCase()) ||
          t.category.toLowerCase().includes(search.toLowerCase())
      )
    : transactions;

  return (
    <div className="flex flex-col gap-3 pb-4">
      {/* Balance del registro */}
      <div className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground rounded-2xl p-5 shadow-lg shadow-primary/20">
        <p className="text-sm font-medium opacity-80 uppercase tracking-wider">Balance del registro</p>
        <p className="text-3xl font-bold mt-1 tracking-tight">
          {formatCurrency(summary.balance)}
        </p>
        <div className="mt-4 flex gap-4">
          <div className="flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2">
            <ArrowUpRight className="w-4 h-4 text-accent" />
            <div>
              <p className="text-[10px] opacity-70 uppercase tracking-wide">Ingresos</p>
              <p className="text-sm font-semibold">{formatCurrency(summary.income)}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2">
            <ArrowDownRight className="w-4 h-4" />
            <div>
              <p className="text-[10px] opacity-70 uppercase tracking-wide">Gastos</p>
              <p className="text-sm font-semibold">{formatCurrency(summary.expense)}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Search bar */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <input
          type="text"
          placeholder="Buscar por descripción o categoría..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full h-10 pl-9 pr-4 rounded-xl bg-muted border border-border/50 text-sm outline-none focus:ring-2 focus:ring-primary/30"
        />
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <div className="w-16 h-16 rounded-full bg-muted flex items-center justify-center mb-4">
            <ShoppingCart className="w-7 h-7 text-muted-foreground" />
          </div>
          <p className="text-sm font-medium text-muted-foreground">
            {search ? "Sin resultados" : "No hay movimientos"}
          </p>
          <p className="text-xs text-muted-foreground/70 mt-1">
            {search ? "Probá con otro término" : "Tocá + para registrar tu primer movimiento"}
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {filtered.map((t) => {
            const Icon = CATEGORY_ICONS[t.category] || CircleEllipsis;
            return (
              <div
                key={t.id}
                onClick={() => onEdit(t)}
                className="bg-card rounded-2xl px-4 py-3.5 border border-border/50 shadow-sm flex items-center gap-3 active:bg-muted/50 transition-colors cursor-pointer"
              >
                <div
                  className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                    t.type === "income"
                      ? "bg-accent/10 text-accent"
                      : "bg-primary/5 text-primary"
                  }`}
                >
                  <Icon className="w-5 h-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{t.description}</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-[11px] text-muted-foreground">{t.category}</span>
                    <span className="text-[10px] text-muted-foreground/50">·</span>
                    <span className="text-[11px] text-muted-foreground">
                      {new Date(t.date).toLocaleDateString("es-ES", {
                        day: "2-digit",
                        month: "short",
                      })}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <p
                    className={`text-sm font-bold ${
                      t.type === "income" ? "text-accent" : "text-foreground"
                    }`}
                  >
                    {t.type === "income" ? "+" : "-"}
                    {formatCurrency(Math.abs(t.amount))}
                  </p>
                  <button
                    onClick={(e) => { e.stopPropagation(); onDelete(t.id); }}
                    className="w-8 h-8 rounded-full flex items-center justify-center text-muted-foreground hover:text-destructive hover:bg-destructive/10 active:scale-90 transition-all"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
