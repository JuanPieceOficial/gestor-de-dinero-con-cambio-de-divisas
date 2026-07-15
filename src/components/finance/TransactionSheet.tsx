"use client"

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { X, ArrowDownCircle, ArrowUpCircle } from "lucide-react";
import type { Transaction } from "@/app/lib/finance-store";

interface TransactionSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdd: (transaction: any) => void;
  categories: string[];
  editTransaction?: Transaction | null;
}

export function TransactionSheet({
  open,
  onOpenChange,
  onAdd,
  categories,
  editTransaction,
}: TransactionSheetProps) {
  const [type, setType] = useState<"income" | "expense">("expense");
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [category, setCategory] = useState("");
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);

  useEffect(() => {
    if (editTransaction) {
      setType(editTransaction.type);
      setDescription(editTransaction.description);
      setAmount(String(Math.abs(editTransaction.amount)));
      setCategory(editTransaction.category);
      setDate(editTransaction.date);
    } else {
      setType("expense");
      setDescription("");
      setAmount("");
      setCategory("");
      setDate(new Date().toISOString().split("T")[0]);
    }
  }, [editTransaction, open]);

  if (!open) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!description || !amount || !category) return;

    onAdd({
      type,
      description,
      amount: type === "expense" ? -Math.abs(Number(amount)) : Math.abs(Number(amount)),
      category,
      date,
    });

    setDescription("");
    setAmount("");
    setCategory("");
    onOpenChange(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-end">
      <div
        className="absolute inset-0 bg-black/40"
        onClick={() => onOpenChange(false)}
      />
      <div className="relative bg-card rounded-t-3xl shadow-2xl max-h-[90vh] overflow-y-auto animate-in slide-in-from-bottom duration-300">
        <div className="flex justify-center pt-3 pb-1">
          <div className="w-10 h-1 rounded-full bg-muted-foreground/30" />
        </div>
        <div className="flex items-center justify-between px-5 py-3 border-b border-border/50">
          <h2 className="text-lg font-bold">
            {editTransaction ? "Editar Transacción" : "Nueva Transacción"}
          </h2>
          <button
            onClick={() => onOpenChange(false)}
            className="w-8 h-8 rounded-full flex items-center justify-center text-muted-foreground hover:bg-muted active:scale-90 transition-all"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          {/* Type Toggle */}
          <div className="grid grid-cols-2 gap-2 p-1 bg-muted rounded-xl">
            <button
              type="button"
              onClick={() => setType("expense")}
              className={`flex items-center justify-center gap-2 py-3 text-sm font-medium rounded-lg transition-all ${
                type === "expense"
                  ? "bg-card shadow-sm text-destructive"
                  : "text-muted-foreground"
              }`}
            >
              <ArrowDownCircle className="w-4 h-4" />
              Gasto
            </button>
            <button
              type="button"
              onClick={() => setType("income")}
              className={`flex items-center justify-center gap-2 py-3 text-sm font-medium rounded-lg transition-all ${
                type === "income"
                  ? "bg-card shadow-sm text-accent"
                  : "text-muted-foreground"
              }`}
            >
              <ArrowUpCircle className="w-4 h-4" />
              Ingreso
            </button>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="sheet-desc" className="text-sm font-medium">Descripción</Label>
            <Input
              id="sheet-desc"
              placeholder="Ej: Compra supermercado"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="h-12 rounded-xl"
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="sheet-amount" className="text-sm font-medium">Monto</Label>
              <Input
                id="sheet-amount"
                type="number"
                placeholder="0.00"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="h-12 rounded-xl"
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="sheet-date" className="text-sm font-medium">Fecha</Label>
              <Input
                id="sheet-date"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="h-12 rounded-xl"
                required
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="sheet-cat" className="text-sm font-medium">Categoría</Label>
            <Select onValueChange={setCategory} value={category} required>
              <SelectTrigger className="h-12 rounded-xl">
                <SelectValue placeholder="Selecciona categoría" />
              </SelectTrigger>
              <SelectContent>
                {categories.map((cat) => (
                  <SelectItem key={cat} value={cat}>
                    {cat}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button type="submit" className="w-full h-12 rounded-xl text-base font-bold mt-2">
            {editTransaction ? "Actualizar" : "Guardar"}
          </Button>
        </form>
      </div>
    </div>
  );
}
