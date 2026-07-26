"use client"

import { useState } from "react";
import { Moon, Sun, LogOut, Plus, Trash2, Edit2 } from "lucide-react";
import type { CurrencyCode } from "@/app/lib/finance-store";

interface MobileSettingsProps {
  selectedCurrency: CurrencyCode;
  onCurrencyChange: (c: CurrencyCode) => void;
  useDarkMode: boolean;
  onToggleDarkMode: () => void;
  onSignOut: () => void;
  categories: { name: string; type: 'income' | 'expense'; is_default: boolean; id: string }[];
  addCategory: (name: string, type: 'income' | 'expense') => Promise<void>;
  deleteCategory: (id: string) => Promise<void>;
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

export function MobileSettings({
  selectedCurrency,
  onCurrencyChange,
  useDarkMode,
  onToggleDarkMode,
  onSignOut,
  categories,
  addCategory,
  deleteCategory,
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

      <button
        onClick={onSignOut}
        className="w-full h-11 rounded-xl bg-destructive/10 text-destructive font-medium text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
      >
        <LogOut className="w-4 h-4" />
        Cerrar sesión
      </button>
    </div>
  );
}