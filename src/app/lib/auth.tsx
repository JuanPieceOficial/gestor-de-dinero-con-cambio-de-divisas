"use client"

import { useEffect, useState } from "react"
import { supabase } from "./supabase"
import type { User } from "@supabase/supabase-js"

let storeUser: User | null = null
let storeLoading = true
let initialized = false
let listeners: Array<() => void> = []

function subscribe(fn: () => void) {
  listeners.push(fn)
  return () => { listeners = listeners.filter(l => l !== fn) }
}

function notify() {
  listeners.forEach(l => l())
}

if (!initialized) {
  initialized = true
  supabase.auth.getSession().then(({ data: { session } }) => {
    storeUser = session?.user ?? null
  }).catch(() => {}).finally(() => {
    storeLoading = false
    notify()
  })
  supabase.auth.onAuthStateChange((_event, session) => {
    storeUser = session?.user ?? null
    notify()
  })
}

export function useAuth() {
  const [, tick] = useState(0)

  useEffect(() => subscribe(() => tick(n => n + 1)), [])

  const signIn = async (email: string, password: string) => {
    const { error } = await supabase.auth.signInWithPassword({ email, password })
    return error?.message ?? null
  }

  const signUp = async (email: string, password: string) => {
    const { error } = await supabase.auth.signUp({ email, password })
    return error?.message ?? null
  }

  const signOut = async () => {
    await supabase.auth.signOut()
  }

  return { user: storeUser, loading: storeLoading, signIn, signUp, signOut }
}
