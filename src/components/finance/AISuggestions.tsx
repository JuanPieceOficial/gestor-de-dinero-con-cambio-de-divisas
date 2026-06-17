"use client"

import { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from "@/components/ui/card";
import { Sparkles, Loader2, Lightbulb } from "lucide-react";
import { budgetOptimizationSuggestions } from "@/ai/flows/budget-optimization-suggestions";

interface AISuggestionsProps {
  transactions: any[];
  budgets: any[];
}

export function AISuggestions({ transactions, budgets }: AISuggestionsProps) {
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    try {
      // Mapping local state to AI flow input
      const aiInput = {
        transactions: transactions.map(t => ({
          date: t.date,
          description: t.description,
          amount: t.amount,
          category: t.category
        })),
        budgets: budgets.map(b => ({
          category: b.category,
          limit: b.limit,
          spent: b.spent
        })),
        currentFinancialSummary: "Usuario buscando optimizar sus ahorros mensuales."
      };

      const result = await budgetOptimizationSuggestions(aiInput);
      setSuggestions(result.suggestions);
    } catch (error) {
      console.error("AI Error:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="border-primary/20 bg-primary/5 shadow-md">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <CardTitle className="text-xl flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-primary animate-pulse" />
              Análisis Inteligente
            </CardTitle>
            <CardDescription>
              Usa IA para analizar tus patrones y ahorrar más
            </CardDescription>
          </div>
          <Button 
            onClick={handleGenerate} 
            disabled={loading || transactions.length === 0}
            className="font-semibold"
          >
            {loading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Lightbulb className="w-4 h-4 mr-2" />}
            Obtener Sugerencias
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {suggestions.length > 0 ? (
          <ul className="space-y-4">
            {suggestions.map((s, i) => (
              <li key={i} className="flex gap-3 p-3 bg-background rounded-lg border border-primary/10 transition-all hover:translate-x-1">
                <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center shrink-0 text-primary font-bold text-xs">
                  {i + 1}
                </div>
                <p className="text-sm leading-relaxed">{s}</p>
              </li>
            ))}
          </ul>
        ) : (
          <div className="text-center py-8 text-muted-foreground">
            {transactions.length === 0 
              ? "Registra algunas transacciones para recibir consejos personalizados."
              : "Haz clic en el botón superior para generar sugerencias con IA."}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
