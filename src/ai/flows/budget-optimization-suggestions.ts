'use server';
/**
 * @fileOverview An AI tool for analyzing spending patterns and providing budget optimization suggestions.
 *
 * - budgetOptimizationSuggestions - A function that analyzes user financial data and provides personalized budget optimization suggestions.
 * - BudgetOptimizationSuggestionsInput - The input type for the budgetOptimizationSuggestions function.
 * - BudgetOptimizationSuggestionsOutput - The return type for the budgetOptimizationSuggestions function.
 */

import { ai } from '@/ai/genkit';
import { z } from 'genkit';

const TransactionSchema = z.object({
  date: z.string().describe('The date of the transaction in YYYY-MM-DD format.'),
  description: z.string().describe('A brief description of the transaction.'),
  amount: z.number().describe('The amount of the transaction. Positive for income, negative for expense.'),
  category: z.string().describe('The category of the transaction (e.g., Alimentación, Transporte, Ocio).'),
});

const BudgetSchema = z.object({
  category: z.string().describe('The category for which the budget is set.'),
  limit: z.number().describe('The maximum amount allocated for this category.'),
  spent: z.number().describe('The amount already spent in this category.'),
});

const BudgetOptimizationSuggestionsInputSchema = z.object({
  transactions: z.array(TransactionSchema).describe('A list of user transactions including date, description, amount, and category.'),
  budgets: z.array(BudgetSchema).describe('A list of user-defined budgets per category, including limit and spent amount.'),
  currentFinancialSummary: z.string().optional().describe('An optional summary of the user\'s current financial status.'),
});
export type BudgetOptimizationSuggestionsInput = z.infer<typeof BudgetOptimizationSuggestionsInputSchema>;

const BudgetOptimizationSuggestionsOutputSchema = z.object({
  suggestions: z.array(z.string()).describe('A list of personalized suggestions to optimize the user\'s budget.'),
});
export type BudgetOptimizationSuggestionsOutput = z.infer<typeof BudgetOptimizationSuggestionsOutputSchema>;

export async function budgetOptimizationSuggestions(input: BudgetOptimizationSuggestionsInput): Promise<BudgetOptimizationSuggestionsOutput> {
  return budgetOptimizationSuggestionsFlow(input);
}

const budgetOptimizationPrompt = ai.definePrompt({
  name: 'budgetOptimizationPrompt',
  input: { schema: BudgetOptimizationSuggestionsInputSchema },
  output: { schema: BudgetOptimizationSuggestionsOutputSchema },
  prompt: `Eres un asesor financiero experto. Tu tarea es analizar los patrones de gasto del usuario y sus presupuestos para proporcionar sugerencias personalizadas y accionables para optimizar su presupuesto y mejorar sus finanzas.

Aquí tienes la información financiera del usuario:

Resumen Financiero Actual (si disponible): {{{currentFinancialSummary}}}

Transacciones:
{{#each transactions}}
- Fecha: {{this.date}}, Descripción: {{this.description}}, Monto: {{this.amount}}, Categoría: {{this.category}}
{{/each}}

Presupuestos:
{{#each budgets}}
- Categoría: {{this.category}}, Límite: {{this.limit}}, Gastado: {{this.spent}}
{{/each}}

Basado en esta información, identifica patrones de gasto, áreas de posible ahorro y cualquier desviación del presupuesto. Luego, genera una lista de 3 a 5 sugerencias concretas y personalizadas para que el usuario optimice su presupuesto y gestione mejor sus finanzas. Las sugerencias deben ser realistas y fáciles de implementar.
`,
});

const budgetOptimizationSuggestionsFlow = ai.defineFlow(
  {
    name: 'budgetOptimizationSuggestionsFlow',
    inputSchema: BudgetOptimizationSuggestionsInputSchema,
    outputSchema: BudgetOptimizationSuggestionsOutputSchema,
  },
  async (input) => {
    const { output } = await budgetOptimizationPrompt(input);
    return output!;
  },
);
