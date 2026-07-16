 "use client"

import { Moon, Sun, LogOut } from "lucide-react";
import type { CurrencyCode } from "@/app/lib/finance-store";

interface MobileSettingsProps {
  selectedCurrency: CurrencyCode;
  onCurrencyChange: (c: CurrencyCode) => void;
  useDarkMode: boolean;
  onToggleDarkMode: () => void;
  onSignOut: () => void;
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
}: MobileSettingsProps) {
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
