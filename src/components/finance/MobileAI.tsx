"use client"

import { useState } from "react";
import { Sparkles, Loader2, Lightbulb } from "lucide-react";
import { budgetOptimizationSuggestions } from "@/ai/flows/budget-optimization-suggestions";

interface MobileAIProps {
  transactions: any[];
  budgets: any[];
}

export function MobileAI({ transactions, budgets }: MobileAIProps) {
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const result = await budgetOptimizationSuggestions({
        transactions: transactions.map((t) => ({
          date: t.date,
          description: t.description,
          amount: t.amount,
          category: t.category,
        })),
        budgets: budgets.map((b) => ({
          category: b.category,
          limit: b.limit,
          spent: b.spent,
        })),
        currentFinancialSummary: "Usuario buscando optimizar sus ahorros mensuales.",
      });
      setSuggestions(result.suggestions);
    } catch (error) {
      console.error("AI Error:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-4 pb-4">
      <div className="bg-card rounded-2xl p-5 border border-primary/10 shadow-sm">
        <div className="flex items-start gap-3 mb-4">
          <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary shrink-0">
            <Sparkles className="w-5 h-5" />
          </div>
          <div className="flex-1">
            <p className="text-sm font-bold">Asistente Financiero IA</p>
            <p className="text-xs text-muted-foreground mt-0.5">
              Analiza tus patrones de gasto y recibe sugerencias
            </p>
          </div>
        </div>

        <button
          onClick={handleGenerate}
          disabled={loading || transactions.length === 0}
          className="w-full py-3 rounded-xl bg-primary text-primary-foreground font-semibold text-sm flex items-center justify-center gap-2 active:scale-[0.98] transition-transform disabled:opacity-50"
        >
          {loading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Lightbulb className="w-4 h-4" />
          )}
          {loading ? "Analizando..." : "Obtener Sugerencias"}
        </button>
      </div>

      {suggestions.length > 0 && (
        <div className="space-y-2">
          {suggestions.map((s, i) => (
            <div
              key={i}
              className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm flex gap-3"
            >
              <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center shrink-0 text-primary font-bold text-xs">
                {i + 1}
              </div>
              <p className="text-sm leading-relaxed">{s}</p>
            </div>
          ))}
        </div>
      )}

      {suggestions.length === 0 && !loading && (
        <div className="text-center py-10">
          <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mx-auto mb-3">
            <Lightbulb className="w-6 h-6 text-muted-foreground" />
          </div>
          <p className="text-xs text-muted-foreground">
            {transactions.length === 0
              ? "Registra transacciones para recibir consejos personalizados."
              : "Toca el botón para generar sugerencias con IA."}
          </p>
        </div>
      )}
    </div>
  );
}
