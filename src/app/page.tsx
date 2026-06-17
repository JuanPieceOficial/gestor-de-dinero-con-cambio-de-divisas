"use client"

import { useFinanceData } from "@/app/lib/finance-store";
import { DashboardSummary } from "@/components/finance/DashboardSummary";
import { TransactionForm } from "@/components/finance/TransactionForm";
import { TransactionList } from "@/components/finance/TransactionList";
import { BudgetOverview } from "@/components/finance/BudgetOverview";
import { AISuggestions } from "@/components/finance/AISuggestions";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LayoutDashboard, ReceiptText, PieChart as ChartIcon, BrainCircuit } from "lucide-react";

export default function Home() {
  const { 
    transactions, 
    addTransaction, 
    deleteTransaction, 
    getBudgetsWithSpent, 
    totals,
    isLoaded 
  } = useFinanceData();

  if (!isLoaded) return null;

  const categories = [
    'Alimentación', 'Transporte', 'Ocio', 'Hogar', 'Salud', 'Educación', 'Otros'
  ];

  const budgetsWithSpent = getBudgetsWithSpent();

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-30 w-full border-b bg-background/80 backdrop-blur-md">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-white shadow-lg shadow-primary/20">
              <ChartIcon className="w-6 h-6" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-primary">Gestor<span className="text-foreground">Fácil</span></h1>
          </div>
          <div className="hidden md:block">
            <p className="text-sm text-muted-foreground font-medium italic">
              "Tu salud financiera, nuestra prioridad"
            </p>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <div className="flex flex-col gap-8">
          
          <DashboardSummary totals={totals} />

          <Tabs defaultValue="dashboard" className="space-y-6">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <TabsList className="bg-muted p-1 rounded-xl">
                <TabsTrigger value="dashboard" className="rounded-lg gap-2 px-6">
                  <LayoutDashboard className="w-4 h-4" />
                  Dashboard
                </TabsTrigger>
                <TabsTrigger value="transactions" className="rounded-lg gap-2 px-6">
                  <ReceiptText className="w-4 h-4" />
                  Transacciones
                </TabsTrigger>
                <TabsTrigger value="budgets" className="rounded-lg gap-2 px-6">
                  <ChartIcon className="w-4 h-4" />
                  Presupuestos
                </TabsTrigger>
                <TabsTrigger value="ai" className="rounded-lg gap-2 px-6">
                  <BrainCircuit className="w-4 h-4" />
                  IA Insights
                </TabsTrigger>
              </TabsList>
            </div>

            <TabsContent value="dashboard" className="space-y-8 animate-in fade-in slide-in-from-bottom-2">
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-1">
                  <TransactionForm onAdd={addTransaction} categories={categories} />
                </div>
                <div className="lg:col-span-2 space-y-6">
                  <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold">Actividad Reciente</h2>
                  </div>
                  <TransactionList transactions={transactions.slice(0, 5)} onDelete={deleteTransaction} />
                </div>
              </div>
            </TabsContent>

            <TabsContent value="transactions" className="space-y-6 animate-in fade-in slide-in-from-bottom-2">
              <div className="flex flex-col gap-6">
                <div className="flex items-center justify-between">
                  <h2 className="text-2xl font-bold">Historial de Transacciones</h2>
                </div>
                <TransactionList transactions={transactions} onDelete={deleteTransaction} />
              </div>
            </TabsContent>

            <TabsContent value="budgets" className="space-y-6 animate-in fade-in slide-in-from-bottom-2">
              <div className="flex flex-col gap-6">
                <h2 className="text-2xl font-bold">Análisis de Presupuestos</h2>
                <BudgetOverview budgetsWithSpent={budgetsWithSpent} />
              </div>
            </TabsContent>

            <TabsContent value="ai" className="space-y-6 animate-in fade-in slide-in-from-bottom-2">
              <div className="flex flex-col gap-6 max-w-4xl mx-auto">
                <h2 className="text-2xl font-bold">Asistente Financiero AI</h2>
                <AISuggestions transactions={transactions} budgets={budgetsWithSpent} />
              </div>
            </TabsContent>
          </Tabs>
        </div>
      </main>

      {/* Footer */}
      <footer className="mt-auto border-t py-8 bg-muted/30">
        <div className="container mx-auto px-4 text-center">
          <p className="text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} GestorFácil. Diseñado para simplificar tu vida financiera.
          </p>
        </div>
      </footer>
    </div>
  );
}
