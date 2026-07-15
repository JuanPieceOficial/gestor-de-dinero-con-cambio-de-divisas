"use client"

import { useState } from "react";
import type { Transaction } from "@/app/lib/finance-store";
import { Trash2, ShoppingCart, Home, Car, Utensils, HeartPulse, GraduationCap, Briefcase, TrendingUp, CircleEllipsis, Search } from "lucide-react";

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

  const filtered = search.trim()
    ? transactions.filter(
        (t) =>
          t.description.toLowerCase().includes(search.toLowerCase()) ||
          t.category.toLowerCase().includes(search.toLowerCase())
      )
    : transactions;

  return (
    <div className="flex flex-col gap-3 pb-4">
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
