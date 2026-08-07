"use client"

import { useState } from "react";
import { Moon, Sun, LogOut, LogIn, User, Trash2, Download, FileText } from "lucide-react";
import type { CurrencyCode, Transaction, Budget } from "@/app/lib/finance-store";

interface MobileSettingsProps {
  user?: { email?: string } | null;
  onShowAuth?: () => void;
  selectedCurrency: CurrencyCode;
  onCurrencyChange: (c: CurrencyCode) => void;
  useDarkMode: boolean;
  onToggleDarkMode: () => void;
  onSignOut: () => void;
  categories: { name: string; type: 'income' | 'expense'; is_default: boolean; id: string }[];
  addCategory: (name: string, type: 'income' | 'expense') => Promise<void>;
  deleteCategory: (id: string) => Promise<void>;
  transactions: Transaction[];
  budgets: Budget[];
  formatCurrency: (val: number) => string;
}

const CURRENCIES: { code: CurrencyCode; label: string }[] = [
  { code: "EUR", label: "Euro (€)" },
  { code: "USD", label: "Dólar ($)" },
  { code: "VES", label: "Bolívar (Bs.)" },
  { code: "COP", label: "Peso Colombiano (CO$)" },
  { code: "ARS", label: "Peso Argentino (AR$)" },
  { code: "MXN", label: "Peso Mexicano (MX$)" },
  { code: "BRL", label: "Real Brasileño (R$)" },
];

function downloadFile(content: string, filename: string, mimeType: string) {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function handleExportCSV(transactions: Transaction[], budgets: Budget[], formatCurrency: (val: number) => string) {
  const headers = ["Fecha", "Descripción", "Monto", "Categoría", "Tipo"];
  const rows = transactions.map(t => [
    t.date,
    t.description,
    t.type === "income" ? t.amount : -Math.abs(t.amount),
    t.category,
    t.type === "income" ? "Ingreso" : "Gasto",
  ]);
  const csv = [headers.join(","), ...rows.map(r => r.map(v => `"${v}"`).join(","))].join("\n");
  downloadFile(csv, "gestorfacil-transacciones.csv", "text/csv");
}

function handleExportJSON(transactions: Transaction[], budgets: Budget[], categories: MobileSettingsProps["categories"]) {
  const data = {
    exportDate: new Date().toISOString(),
    version: "1.0",
    transactions,
    budgets,
    categories,
  };
  const json = JSON.stringify(data, null, 2);
  downloadFile(json, "gestorfacil-backup.json", "application/json");
}

export function MobileSettings({
  user,
  onShowAuth,
  selectedCurrency,
  onCurrencyChange,
  useDarkMode,
  onToggleDarkMode,
  onSignOut,
  categories,
  addCategory,
  deleteCategory,
  transactions,
  budgets,
  formatCurrency,
}: MobileSettingsProps) {
  const [showAddCategory, setShowAddCategory] = useState<{ type: 'income' | 'expense' } | null>(null);
  const [newCategoryName, setNewCategoryName] = useState("");

  const handleAddCategory = async (type: 'income' | 'expense') => {
    const name = newCategoryName.trim();
    if (!name) return;
    try {
      await addCategory(name, type);
      setNewCategoryName("");
      setShowAddCategory(null);
    } catch (e) {
      alert("Error al crear categoría");
    }
  };

  const handleDeleteCategory = async (id: string) => {
    if (!confirm("¿Eliminar esta categoría?")) return;
    try {
      await deleteCategory(id);
    } catch (e) {
      alert("No se puede eliminar (es por defecto o tiene transacciones)");
    }
  };

  const incomeCategories = categories.filter(c => c.type === 'income');
  const expenseCategories = categories.filter(c => c.type === 'expense');

  return (
    <div className="flex flex-col gap-4 pb-4">
      {/* Account / Sync */}
      <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
        <p className="text-sm font-semibold text-muted-foreground mb-3">Cuenta y sincronización</p>
        {user ? (
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3 min-w-0">
              <div className="w-9 h-9 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
                <User className="w-4 h-4 text-primary" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium truncate">{user.email || "Sesión iniciada"}</p>
                <p className="text-[11px] text-muted-foreground">Datos sincronizados con la nube</p>
              </div>
            </div>
            <button
              onClick={onSignOut}
              className="shrink-0 p-2 rounded-xl text-destructive bg-destructive/10 hover:bg-destructive/15 transition-colors"
              aria-label="Cerrar sesión"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
              <p className="text-xs text-muted-foreground">
                Modo local — tus datos se guardan en este dispositivo
              </p>
            </div>
            {onShowAuth && (
              <button
                onClick={onShowAuth}
                className="w-full h-11 rounded-xl bg-primary text-primary-foreground font-medium text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
              >
                <LogIn className="w-4 h-4" />
                Iniciar sesión para sincronizar
              </button>
            )}
          </div>
        )}
      </div>

      <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
        <p className="text-sm font-semibold text-muted-foreground mb-4">Ajustes</p>

        {/* Currency */}
        <div className="space-y-2">
          <p className="text-sm font-medium">Moneda principal</p>
          <select
            value={selectedCurrency}
            onChange={(e) => onCurrencyChange(e.target.value as CurrencyCode)}
            className="w-full h-12 px-3 rounded-xl bg-muted border border-border/50 text-sm outline-none focus:ring-2 focus:ring-primary/30"
          >
            {CURRENCIES.map((c) => (
              <option key={c.code} value={c.code}>
                {c.label}
              </option>
            ))}
          </select>
        </div>

        <div className="border-t border-border/30 my-4" />

        {/* Dark Mode */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            {useDarkMode ? (
              <Moon className="w-5 h-5 text-primary" />
            ) : (
              <Sun className="w-5 h-5 text-primary" />
            )}
            <span className="text-sm font-medium">Modo oscuro</span>
          </div>
          <button
            onClick={onToggleDarkMode}
            className={`relative w-12 h-6 rounded-full transition-colors ${
              useDarkMode ? "bg-primary" : "bg-muted-foreground/30"
            }`}
          >
            <div
              className={`absolute top-0.5 w-5 h-5 rounded-full bg-white shadow-sm transition-transform ${
                useDarkMode ? "translate-x-6.5" : "translate-x-0.5"
              }`}
            />
          </button>
        </div>
      </div>

      {/* Categories */}
      <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <p className="text-sm font-semibold text-muted-foreground">Categorías</p>
        </div>

        {/* Income */}
        <div className="mb-6">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-accent uppercase tracking-wider">Ingresos</span>
            <button
              onClick={() => setShowAddCategory({ type: 'income' })}
              className="text-sm text-primary font-medium"
            >
              + Nueva
            </button>
          </div>
          <div className="space-y-1">
            {incomeCategories.map(cat => (
              <div key={cat.id} className="flex items-center justify-between p-2 rounded-xl bg-muted/50">
                <span className="text-sm">{cat.name}</span>
                {!cat.is_default && (
                  <button
                    onClick={() => handleDeleteCategory(cat.id)}
                    className="p-1 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Expense */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-medium text-destructive uppercase tracking-wider">Gastos</span>
            <button
              onClick={() => setShowAddCategory({ type: 'expense' })}
              className="text-sm text-primary font-medium"
            >
              + Nueva
            </button>
          </div>
          <div className="space-y-1">
            {expenseCategories.map(cat => (
              <div key={cat.id} className="flex items-center justify-between p-2 rounded-xl bg-muted/50">
                <span className="text-sm">{cat.name}</span>
                {!cat.is_default && (
                  <button
                    onClick={() => handleDeleteCategory(cat.id)}
                    className="p-1 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Add Category Modal */}
        {showAddCategory && (
          <div className="fixed inset-0 bg-black/50 flex items-end z-50" onClick={() => setShowAddCategory(null)}>
            <div className="w-full bg-card rounded-t-2xl p-6 border-t border-border/50 shadow-xl" onClick={e => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-semibold">Nueva categoría de {showAddCategory.type === 'income' ? 'ingresos' : 'gastos'}</h3>
                <button onClick={() => setShowAddCategory(null)} className="p-1">
                  <Trash2 className="w-5 h-5" />
                </button>
              </div>
              <input
                value={newCategoryName}
                onChange={e => setNewCategoryName(e.target.value)}
                placeholder="Nombre de la categoría"
                className="w-full h-12 px-4 rounded-xl bg-muted border border-border/50 text-sm outline-none focus:ring-2 focus:ring-primary/30 mb-4"
                autoFocus
              />
              <button
                onClick={() => handleAddCategory(showAddCategory.type)}
                className="w-full h-11 rounded-xl bg-primary text-primary-foreground font-medium"
              >
                Crear
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Export */}
      <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
        <p className="text-sm font-semibold text-muted-foreground mb-3">Exportar datos</p>
        <div className="grid grid-cols-2 gap-3">
          <button
            onClick={() => handleExportCSV(transactions, budgets, formatCurrency)}
            className="h-11 rounded-xl bg-primary/10 text-primary font-medium text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <FileText className="w-4 h-4" />
            CSV
          </button>
          <button
            onClick={() => handleExportJSON(transactions, budgets, categories)}
            className="h-11 rounded-xl bg-primary/10 text-primary font-medium text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <Download className="w-4 h-4" />
            JSON
          </button>
        </div>
      </div>

    </div>
  );
}