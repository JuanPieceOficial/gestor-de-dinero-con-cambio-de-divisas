"use client"

import { useState } from "react"
import { useAuth } from "@/app/lib/auth"
import { Wallet } from "lucide-react"

export function AuthPage() {
  const { signIn, signUp, loading } = useAuth()
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [mode, setMode] = useState<"login" | "register">("login")
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    const msg = mode === "login"
      ? await signIn(email, password)
      : await signUp(email, password)

    if (msg) setError(msg)
    setSubmitting(false)
  }

  if (loading) return null

  return (
    <div className="h-dvh flex flex-col items-center justify-center px-6 bg-background">
      <div className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center text-white shadow-xl shadow-primary/30 mb-6">
        <Wallet className="w-9 h-9" />
      </div>

      <h1 className="text-2xl font-bold tracking-tight mb-1">
        Gestor<span className="text-primary">Fácil</span>
      </h1>
      <p className="text-sm text-muted-foreground mb-8">
        {mode === "login" ? "Inicia sesión para continuar" : "Crea tu cuenta para empezar"}
      </p>

      <form onSubmit={handleSubmit} className="w-full max-w-sm space-y-4">
        <div>
          <label className="text-xs font-medium text-muted-foreground mb-1.5 block">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={e => setEmail(e.target.value)}
            className="w-full h-11 px-4 rounded-xl border border-input bg-background text-sm outline-none focus:border-primary transition-colors"
            placeholder="tu@email.com"
          />
        </div>

        <div>
          <label className="text-xs font-medium text-muted-foreground mb-1.5 block">Contraseña</label>
          <input
            type="password"
            required
            minLength={6}
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="w-full h-11 px-4 rounded-xl border border-input bg-background text-sm outline-none focus:border-primary transition-colors"
            placeholder="••••••"
          />
        </div>

        {error && (
          <p className="text-xs text-destructive text-center">{error}</p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="w-full h-11 rounded-xl bg-primary text-white font-semibold text-sm shadow-lg shadow-primary/20 active:scale-[0.98] transition-all disabled:opacity-50"
        >
          {submitting ? "..." : mode === "login" ? "Iniciar sesión" : "Crear cuenta"}
        </button>
      </form>

      <button
        onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(null) }}
        className="mt-6 text-sm text-muted-foreground underline underline-offset-2"
      >
        {mode === "login" ? "¿No tienes cuenta? Regístrate" : "¿Ya tienes cuenta? Inicia sesión"}
      </button>
    </div>
  )
}
