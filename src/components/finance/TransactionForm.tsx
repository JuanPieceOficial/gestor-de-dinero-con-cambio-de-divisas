"use client"

import { useState } from 'react';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { PlusCircle, ArrowDownCircle, ArrowUpCircle } from "lucide-react";

interface TransactionFormProps {
  onAdd: (transaction: any) => void;
  categories: string[];
}

export function TransactionForm({ onAdd, categories }: TransactionFormProps) {
  const [type, setType] = useState<'income' | 'expense'>('expense');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!description || !amount || !category) return;

    onAdd({
      type,
      description,
      amount: type === 'expense' ? -Math.abs(Number(amount)) : Math.abs(Number(amount)),
      category,
      date
    });

    setDescription('');
    setAmount('');
    setCategory('');
  };

  return (
    <Card className="shadow-md border-primary/10">
      <CardHeader>
        <CardTitle className="text-xl flex items-center gap-2">
          <PlusCircle className="w-5 h-5 text-primary" />
          Nueva Transacción
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-2 p-1 bg-muted rounded-lg">
            <button
              type="button"
              onClick={() => setType('expense')}
              className={`flex items-center justify-center gap-2 py-2 text-sm font-medium rounded-md transition-all ${
                type === 'expense' 
                  ? 'bg-background shadow-sm text-destructive' 
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <ArrowDownCircle className="w-4 h-4" />
              Gasto
            </button>
            <button
              type="button"
              onClick={() => setType('income')}
              className={`flex items-center justify-center gap-2 py-2 text-sm font-medium rounded-md transition-all ${
                type === 'income' 
                  ? 'bg-background shadow-sm text-accent' 
                  : 'text-muted-foreground hover:text-foreground'
              }`}
            >
              <ArrowUpCircle className="w-4 h-4" />
              Ingreso
            </button>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Descripción</Label>
            <Input 
              id="description" 
              placeholder="Ej: Compra supermercado" 
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="amount">Monto ($)</Label>
              <Input 
                id="amount" 
                type="number" 
                placeholder="0.00" 
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="date">Fecha</Label>
              <Input 
                id="date" 
                type="date" 
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="category">Categoría</Label>
            <Select onValueChange={setCategory} value={category} required>
              <SelectTrigger>
                <SelectValue placeholder="Selecciona categoría" />
              </SelectTrigger>
              <SelectContent>
                {categories.map(cat => (
                  <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <Button type="submit" className="w-full font-semibold">
            Guardar Transacción
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
