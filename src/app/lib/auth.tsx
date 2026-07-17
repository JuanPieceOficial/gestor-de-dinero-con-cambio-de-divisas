"use client"

export function useAuth() {
  return {
    user: null,
    loading: false,
    signIn: async () => "Login deshabilitado",
    signUp: async () => "Registro deshabilitado",
    signOut: async () => {},
  }
}
