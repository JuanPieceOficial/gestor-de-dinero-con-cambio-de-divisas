"use client"

import { Target } from "lucide-react";

interface MobileBudgetsProps {
  budgetsWithSpent: Array<{ category: string; limit: number; spent: number }>;
  formatCurrency: (val: number) => string;
}

export function MobileBudgets({ budgetsWithSpent, formatCurrency }: MobileBudgetsProps) {
  return (
    <div className="flex flex-col gap-3 pb-4">
      <div className="bg-card rounded-2xl p-4 border border-primary/10 shadow-sm">
        <div className="flex items-center gap-2 mb-4">
          <Target className="w-5 h-5 text-primary" />
          <p className="text-sm font-bold">Control de Presupuestos</p>
        </div>
        <div className="space-y-4">
          {budgetsWithSpent.map((budget) => {
            const percentage = Math.min((budget.spent / budget.limit) * 100, 100);
            const isNearLimit = percentage > 85;
            const isOver = budget.spent > budget.limit;

            return (
              <div key={budget.category} className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <span className="font-medium">{budget.category}</span>
                  <span
                    className={`font-semibold ${
                      isOver
                        ? "text-destructive"
                        : isNearLimit
                          ? "text-orange-500"
                          : "text-muted-foreground"
                    }`}
                  >
                    {formatCurrency(budget.spent)} / {formatCurrency(budget.limit)}
                  </span>
                </div>
                <div className="h-2.5 bg-muted rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isOver
                        ? "bg-destructive"
                        : isNearLimit
                          ? "bg-orange-500"
                          : "bg-primary"
                    }`}
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
