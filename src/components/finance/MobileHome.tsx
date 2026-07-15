"use client"

import { useMemo } from "react";
import { PieChart, Pie, Cell, ResponsiveContainer } from "recharts";
import { ArrowUpRight, ArrowDownRight, Wallet } from "lucide-react";
import type { Transaction } from "@/app/lib/finance-store";

interface MobileHomeProps {
  transactions: Transaction[];
  totals: { income: number; expense: number; balance: number };
  formatCurrency: (val: number) => string;
}

const COLORS = [
  "#2275C0", "#29996E", "#F59E0B", "#8B5CF6",
  "#EF4444", "#EC4899", "#06B6D4", "#84CC16",
];

export function MobileHome({ transactions, totals, formatCurrency }: MobileHomeProps) {
  const expenseByCategory = useMemo(() => {
    const map = new Map<string, number>();
    transactions
      .filter((t) => t.type === "expense")
      .forEach((t) => {
        map.set(t.category, (map.get(t.category) || 0) + Math.abs(t.amount));
      });
    return Array.from(map.entries())
      .map(([category, amount]) => ({ category, amount }))
      .sort((a, b) => b.amount - a.amount);
  }, [transactions]);

  const tips = [
    "Ahorra al menos el 20% de tus ingresos cada mes.",
    "Lleva un registro de cada gasto, por pequeño que sea.",
    "Usa la regla 50/30/20: 50% necesidades, 30% deseos, 20% ahorro.",
    "Revisa tus suscripciones mensuales y cancela las que no uses.",
    "El mejor momento para empezar a ahorrar fue ayer.",
  ];

  const tip = useMemo(() => tips[Math.floor(Math.random() * tips.length)], []);

  return (
    <div className="flex flex-col gap-4 pb-4">
      {/* Balance Card */}
      <div className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground rounded-2xl p-5 shadow-lg shadow-primary/20">
        <p className="text-sm font-medium opacity-80 uppercase tracking-wider">Balance Total</p>
        <p className="text-4xl font-bold mt-1 tracking-tight">
          {formatCurrency(totals.balance)}
        </p>
        <div className="mt-4 flex gap-4">
          <div className="flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2">
            <ArrowUpRight className="w-4 h-4 text-accent" />
            <div>
              <p className="text-[10px] opacity-70 uppercase tracking-wide">Ingresos</p>
              <p className="text-sm font-semibold">{formatCurrency(totals.income)}</p>
            </div>
          </div>
          <div className="flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2">
            <ArrowDownRight className="w-4 h-4" />
            <div>
              <p className="text-[10px] opacity-70 uppercase tracking-wide">Gastos</p>
              <p className="text-sm font-semibold">{formatCurrency(totals.expense)}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Pie Chart */}
      {expenseByCategory.length > 0 && (
        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
          <p className="text-sm font-semibold text-muted-foreground mb-3">Gastos por Categoría</p>
          <div className="flex items-center gap-4">
            <div className="w-36 h-36 shrink-0">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={expenseByCategory}
                    dataKey="amount"
                    nameKey="category"
                    cx="50%"
                    cy="50%"
                    innerRadius={28}
                    outerRadius={55}
                    paddingAngle={2}
                  >
                    {expenseByCategory.map((_, i) => (
                      <Cell key={i} fill={COLORS[i % COLORS.length]} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="flex-1 space-y-1.5">
              {expenseByCategory.slice(0, 5).map((item, i) => (
                <div key={item.category} className="flex items-center gap-2 text-xs">
                  <div
                    className="w-2.5 h-2.5 rounded-full shrink-0"
                    style={{ backgroundColor: COLORS[i % COLORS.length] }}
                  />
                  <span className="text-muted-foreground flex-1 truncate">{item.category}</span>
                  <span className="font-medium">{formatCurrency(item.amount)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Tip */}
      <div className="bg-card rounded-2xl p-4 border border-primary/10 shadow-sm">
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary shrink-0 mt-0.5">
            <Wallet className="w-4 h-4" />
          </div>
          <div>
            <p className="text-sm font-semibold">💡 Consejo del día</p>
            <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{tip}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
