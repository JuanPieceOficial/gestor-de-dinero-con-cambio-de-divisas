"use client"

import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip, Legend } from 'recharts';
import { Target } from "lucide-react";

interface BudgetOverviewProps {
  budgetsWithSpent: Array<{
    category: string;
    limit: number;
    spent: number;
  }>;
}

export function BudgetOverview({ budgetsWithSpent }: BudgetOverviewProps) {
  const chartData = budgetsWithSpent
    .filter(b => b.spent > 0)
    .map(b => ({ name: b.category, value: b.spent }));

  const COLORS = ['#2275C0', '#29996E', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#6B7280'];

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(val);
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card className="shadow-md border-primary/5">
        <CardHeader>
          <CardTitle className="text-xl flex items-center gap-2">
            <Target className="w-5 h-5 text-primary" />
            Control de Presupuestos
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {budgetsWithSpent.map(budget => {
            const percentage = Math.min((budget.spent / budget.limit) * 100, 100);
            const isNearLimit = percentage > 85;
            const isOver = budget.spent > budget.limit;

            return (
              <div key={budget.category} className="space-y-2">
                <div className="flex justify-between text-sm font-medium">
                  <span>{budget.category}</span>
                  <span className={isOver ? "text-destructive" : isNearLimit ? "text-orange-500" : "text-muted-foreground"}>
                    {formatCurrency(budget.spent)} / {formatCurrency(budget.limit)}
                  </span>
                </div>
                <Progress 
                  value={percentage} 
                  className={`h-2 ${isOver ? "[&>div]:bg-destructive" : isNearLimit ? "[&>div]:bg-orange-500" : ""}`} 
                />
              </div>
            );
          })}
        </CardContent>
      </Card>

      <Card className="shadow-md border-primary/5">
        <CardHeader>
          <CardTitle className="text-xl">Distribución de Gastos</CardTitle>
        </CardHeader>
        <CardContent className="h-[300px]">
          {chartData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {chartData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <RechartsTooltip 
                  formatter={(value: number) => formatCurrency(value)}
                  contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                />
                <Legend iconType="circle" />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-full flex items-center justify-center text-muted-foreground italic">
              Sin datos de gastos para mostrar
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
