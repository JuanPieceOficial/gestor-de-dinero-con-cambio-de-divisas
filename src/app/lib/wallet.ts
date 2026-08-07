"use client";

// Billetera no custodiada para la web: crea/importa una billetera BIP-39,
// cifra la frase de recuperación en localStorage con AES-GCM usando un PIN
// (derivado con PBKDF2). Las llaves nunca salen del navegador.
import { ethers } from "ethers";

const STORAGE_KEY = "gestorfacil_webwallet_v1";
const PBKDF2_ITERATIONS = 150_000;

type StoredWallet = {
  salt: string; // hex
  iv: string; // hex
  ciphertext: string; // hex (frase cifrada)
  address: string; // dirección derivada (pública)
  createdAt: number;
};

function toHex(buf: ArrayBuffer | Uint8Array<ArrayBuffer>): string {
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

function fromHex(hex: string): Uint8Array<ArrayBuffer> {
  const out = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  }
  return out;
}

function getStored(): StoredWallet | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredWallet) : null;
  } catch {
    return null;
  }
}

function webCryptoAvailable(): boolean {
  return typeof crypto !== "undefined" && !!crypto.subtle;
}

async function deriveKey(pin: string, salt: Uint8Array<ArrayBuffer>): Promise<CryptoKey> {
  const material = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(`gestorfacil::${pin}`),
    "PBKDF2",
    false,
    ["deriveKey"]
  );
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" },
    material,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"]
  );
}

async function saveWallet(phrase: string, pin: string): Promise<string> {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await deriveKey(pin, salt);
  const ct = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    new TextEncoder().encode(phrase)
  );
  const wallet = ethers.Wallet.fromPhrase(phrase);
  const stored: StoredWallet = {
    salt: toHex(salt),
    iv: toHex(iv),
    ciphertext: toHex(ct),
    address: wallet.address,
    createdAt: Date.now(),
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(stored));
  return wallet.address;
}

export function isWebCryptoAvailable(): boolean {
  return webCryptoAvailable();
}

export function hasWallet(): boolean {
  if (typeof window === "undefined") return false;
  return !!localStorage.getItem(STORAGE_KEY);
}

export function generatePhrase(): string {
  // 32 bytes de entropía = frase BIP-39 de 24 palabras (igual que la app Android)
  return ethers.Mnemonic.fromEntropy(ethers.randomBytes(32)).phrase;
}

export async function persistWallet(phrase: string, pin: string): Promise<string> {
  return saveWallet(normalizePhrase(phrase), pin);
}

export function normalizePhrase(phrase: string): string {
  return phrase
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
    .join(" ");
}

export function isValidPhrase(phrase: string): boolean {
  const count = normalizePhrase(phrase).split(" ").length;
  return [12, 15, 18, 21, 24].includes(count);
}

export async function importWallet(phrase: string, pin: string): Promise<{ address: string }> {
  const normalized = normalizePhrase(phrase);
  if (!isValidPhrase(normalized)) {
    throw new Error("La frase debe tener 12, 15, 18, 21 o 24 palabras.");
  }
  // Valida el checksum BIP-39 antes de guardar (ethers lanza si es inválida)
  try {
    ethers.Wallet.fromPhrase(normalized);
  } catch {
    throw new Error("Frase de recuperación inválida (checksum incorrecto).");
  }
  const address = await saveWallet(normalized, pin);
  return { address };
}

export async function unlockWallet(pin: string): Promise<string | null> {
  const stored = getStored();
  if (!stored) return null;
  try {
    const key = await deriveKey(pin, fromHex(stored.salt));
    await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: fromHex(stored.iv) },
      key,
      fromHex(stored.ciphertext)
    );
    return stored.address;
  } catch {
    return null; // PIN incorrecto
  }
}

async function decryptPhrase(pin: string): Promise<string | null> {
  const stored = getStored();
  if (!stored) return null;
  try {
    const key = await deriveKey(pin, fromHex(stored.salt));
    const pt = await crypto.subtle.decrypt(
      { name: "AES-GCM", iv: fromHex(stored.iv) },
      key,
      fromHex(stored.ciphertext)
    );
    return new TextDecoder().decode(pt);
  } catch {
    return null; // PIN incorrecto
  }
}

export async function revealPhrase(pin: string): Promise<string | null> {
  return decryptPhrase(pin);
}

// Clave privada en memoria para firmar transacciones (solo durante la sesión desbloqueada)
export async function getPrivateKey(pin: string): Promise<string | null> {
  const phrase = await decryptPhrase(pin);
  if (!phrase) return null;
  try {
    return ethers.Wallet.fromPhrase(phrase).privateKey;
  } catch {
    return null;
  }
}

export async function changePin(oldPin: string, newPin: string): Promise<boolean> {
  const phrase = await decryptPhrase(oldPin);
  if (!phrase) return false;
  try {
    const salt = crypto.getRandomValues(new Uint8Array(16));
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const key = await deriveKey(newPin, salt);
    const ct = await crypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      new TextEncoder().encode(phrase)
    );
    const stored = getStored();
    if (!stored) return false;
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        ...stored,
        salt: toHex(salt),
        iv: toHex(iv),
        ciphertext: toHex(ct),
      })
    );
    return true;
  } catch {
    return false;
  }
}

export function deleteWallet(): void {
  localStorage.removeItem(STORAGE_KEY);
}
