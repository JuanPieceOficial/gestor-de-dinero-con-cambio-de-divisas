import { createClient } from "@supabase/supabase-js";

const supabaseUrl = "https://xfailvysvwqieicdpnid.supabase.co";
const supabaseAnonKey =
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhmYWlsdnlzdndxaWVpY2RwbmlkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQxNjA0MjYsImV4cCI6MjA5OTczNjQyNn0.O9lii_2vfv9jEOLBk0jjUzhzZComE63GePKg26fcknM";

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
