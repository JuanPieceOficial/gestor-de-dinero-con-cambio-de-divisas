"use client"

import { useState, useEffect } from "react";
import { useAuth } from "@/app/lib/auth";
import { useFinanceData } from "@/app/lib/finance-store";
import { MobileHome } from "@/components/finance/MobileHome";
import { MobileTransactions } from "@/components/finance/MobileTransactions";
import { MobileDolar } from "@/components/finance/MobileDolar";
import { MobileSettings } from "@/components/finance/MobileSettings";
import { TransactionSheet } from "@/components/finance/TransactionSheet";
import { AuthPage } from "@/components/finance/AuthPage";
import { ErrorBoundary } from "@/components/ErrorBoundary";
import { Wallet, ReceiptText, DollarSign, Settings, Plus } from "lucide-react";

const TABS = [
  { id: "home", label: "Inicio", icon: Wallet },
  { id: "transactions", label: "Movimientos", icon: ReceiptText },
  { id: "dolar", label: "Dólar", icon: DollarSign },
  { id: "settings", label: "Ajustes", icon: Settings },
] as const;

type TabId = (typeof TABS)[number]["id"];

export default function Home() {
  const { user, loading: authLoading, signOut } = useAuth();
  const [activeTab, setActiveTab] = useState<TabId>("home");
  const [sheetOpen, setSheetOpen] = useState(false);
  const [editTx, setEditTx] = useState<any>(null);

  const {
    transactions,
    addTransaction,
    deleteTransaction,
    updateTransaction,
    totals,
    isLoaded,
    selectedCurrency,
    setSelectedCurrency,
    formatCurrency,
    useDarkMode,
    toggleDarkMode,
    categories,
  } = useFinanceData();

  // Apply dark mode class
  useEffect(() => {
    if (useDarkMode) document.documentElement.classList.add("dark");
    else document.documentElement.classList.remove("dark");
  }, [useDarkMode]);

  if (authLoading || !isLoaded) return null;
  if (!user) return <AuthPage />;

  const handleAddOrEdit = (data: any) => {
    if (editTx) {
      updateTransaction(editTx.id, data);
      setEditTx(null);
    } else {
      addTransaction(data);
    }
  };

  const tabContent = (tab: TabId) => {
    switch (tab) {
      case "home":
        return <MobileHome transactions={transactions} totals={totals} formatCurrency={formatCurrency} />;
      case "transactions":
        return (
          <MobileTransactions
            transactions={transactions}
            onDelete={deleteTransaction}
            onEdit={(tx) => { setEditTx(tx); setSheetOpen(true); }}
            formatCurrency={formatCurrency}
          />
        );
      case "dolar":
        return <MobileDolar />;
      case "settings":
        return (
          <MobileSettings
            selectedCurrency={selectedCurrency}
            onCurrencyChange={setSelectedCurrency}
            useDarkMode={useDarkMode}
            onToggleDarkMode={toggleDarkMode}
            onSignOut={signOut}
          />
        );
    }
  };

  return (
    <ErrorBoundary>
    <div className="h-dvh flex flex-col bg-background max-w-lg mx-auto relative overflow-hidden">
      {/* Header */}
      <header className="shrink-0 px-4 pt-5 pb-2">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white shadow-md shadow-primary/20">
            <Wallet className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight">
              Gestor<span className="text-primary">Fácil</span>
            </h1>
            <p className="text-[11px] text-muted-foreground -mt-0.5">
              Tu salud financiera
            </p>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 overflow-y-auto px-4 pb-2 scroll-smooth">
        {tabContent(activeTab)}
      </main>

      {/* FAB - only on home and transactions */}
      {(activeTab === "home" || activeTab === "transactions") && (
        <button
          onClick={() => { setEditTx(null); setSheetOpen(true); }}
          className="absolute right-5 bottom-20 z-20 w-14 h-14 rounded-full bg-primary text-white shadow-xl shadow-primary/30 flex items-center justify-center active:scale-90 transition-transform"
        >
          <Plus className="w-7 h-7" />
        </button>
      )}

      {/* Bottom Tab Bar */}
      <nav className="shrink-0 bg-card border-t border-border safe-area-bottom">
        <div className="flex items-center justify-around py-1.5">
          {TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex flex-col items-center gap-0.5 py-1 px-4 rounded-xl transition-all ${
                  isActive
                    ? "text-primary"
                    : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <Icon className="w-5 h-5" />
                <span className="text-[10px] font-medium leading-none">{tab.label}</span>
              </button>
            );
          })}
        </div>
      </nav>

      {/* Transaction Sheet */}
      <TransactionSheet
        open={sheetOpen}
        onOpenChange={(v) => { setSheetOpen(v); if (!v) setEditTx(null); }}
        onAdd={handleAddOrEdit}
        categories={categories}
        editTransaction={editTx}
      />
    </div>
    </ErrorBoundary>
  );
}
