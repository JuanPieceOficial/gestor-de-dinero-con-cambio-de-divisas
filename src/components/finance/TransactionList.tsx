"use client"

import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Trash2, ShoppingCart, Home, Car, Utensils, HeartPulse, GraduationCap, CircleEllipsis } from "lucide-react";

interface TransactionListProps {
  transactions: any[];
  onDelete: (id: string) => void;
}

const CATEGORY_ICONS: Record<string, any> = {
  'Alimentación': Utensils,
  'Transporte': Car,
  'Ocio': ShoppingCart,
  'Hogar': Home,
  'Salud': HeartPulse,
  'Educación': GraduationCap,
  'Otros': CircleEllipsis,
};

export function TransactionList({ transactions, onDelete }: TransactionListProps) {
  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(val);
  };

  return (
    <div className="rounded-md border bg-card shadow-sm overflow-hidden">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead>Fecha</TableHead>
            <TableHead>Categoría</TableHead>
            <TableHead>Descripción</TableHead>
            <TableHead className="text-right">Monto</TableHead>
            <TableHead className="w-[50px]"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {transactions.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center py-8 text-muted-foreground italic">
                No hay transacciones registradas todavía.
              </TableCell>
            </TableRow>
          ) : (
            transactions.map((t) => {
              const Icon = CATEGORY_ICONS[t.category] || CircleEllipsis;
              return (
                <TableRow key={t.id} className="group transition-colors">
                  <TableCell className="text-xs font-medium text-muted-foreground">
                    {new Date(t.date).toLocaleDateString('es-ES', { day: '2-digit', month: 'short' })}
                  </TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 rounded-full bg-primary/5 text-primary">
                        <Icon className="w-3.5 h-3.5" />
                      </div>
                      <span className="text-sm font-medium">{t.category}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-sm">{t.description}</TableCell>
                  <TableCell className={`text-right font-bold ${t.type === 'income' ? 'text-accent' : 'text-foreground'}`}>
                    {t.type === 'income' ? '+' : ''}{formatCurrency(t.amount)}
                  </TableCell>
                  <TableCell>
                    <Button 
                      variant="ghost" 
                      size="icon" 
                      onClick={() => onDelete(t.id)}
                      className="opacity-0 group-hover:opacity-100 text-destructive hover:bg-destructive/10 transition-all"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </TableCell>
                </TableRow>
              )
            })
          )}
        </TableBody>
      </Table>
    </div>
  );
}
