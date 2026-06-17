"use client"

import { Card, CardContent } from "@/components/ui/card";
import { Wallet, TrendingUp, TrendingDown } from "lucide-react";

interface DashboardSummaryProps {
  totals: {
    income: number;
    expense: number;
    balance: number;
  };
}

export function DashboardSummary({ totals }: DashboardSummaryProps) {
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(val);
  };

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card className="bg-primary text-primary-foreground shadow-lg border-none overflow-hidden relative group">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
          <Wallet className="w-24 h-24" />
        </div>
        <CardContent className="p-6">
          <div className="flex flex-col gap-1">
            <span className="text-sm font-medium opacity-80 uppercase tracking-wider">Balance Total</span>
            <span className="text-3xl font-bold">{formatCurrency(totals.balance)}</span>
          </div>
        </CardContent>
      </Card>

      <Card className="bg-white dark:bg-card border-accent/20 shadow-md">
        <CardContent className="p-6 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-accent/10 flex items-center justify-center text-accent">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Ingresos Mensuales</span>
            <span className="text-2xl font-bold text-accent">{formatCurrency(totals.income)}</span>
          </div>
        </CardContent>
      </Card>

      <Card className="bg-white dark:bg-card border-destructive/20 shadow-md">
        <CardContent className="p-6 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center text-destructive">
            <TrendingDown className="w-6 h-6" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Gastos Mensuales</span>
            <span className="text-2xl font-bold text-destructive">{formatCurrency(totals.expense)}</span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
