package com.gestorfacil.app.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SupabaseProvider {
    private const val URL = "https://xfailvysvwqieicdpnid.supabase.co"
    private const val KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhmYWlsdnlzdndxaWVpY2RwbmlkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxNjA0MjYsImV4cCI6MjA5OTczNjQyNn0.O9lii_2vfv9jEOLBk0jjUzhzZComE63GePKg26fcknM"

    val client: SupabaseClient = createSupabaseClient(URL, KEY) {
        install(Auth)
        install(Postgrest)
    }

    val auth get() = client.auth
    val db get() = client.postgrest
}
