"use client";

// Historial on-chain vía Blockscout API v2 (público, sin API key).
// Mismo origen de datos que la wallet Android (ExplorerClient).
// Endpoints: /api/v2/addresses/{address}/transactions y .../token-transfers

import type { CryptoChain } from "./crypto";

export type OnChainTx = {
  chainId: number;
  chainName: string;
  explorerUrl: string;
  hash: string;
  from: string;
  to: string;
  tokenAddress: string | null;
  tokenSymbol: string;
  amount: string; // unidades del token
  amountRaw: string; // wei / unidades mínimas
  feeWei: string;
  status: string;
  timestamp: number; // epoch ms
  type: "send" | "receive";
};

const HOSTS: Record<number, string> = {
  1: "https://eth.blockscout.com",
  56: "https://bsc.blockscout.com",
  137: "https://polygon.blockscout.com",
  42161: "https://arbitrum.blockscout.com",
  10: "https://optimism.blockscout.com",
  8453: "https://base.blockscout.com",
  43114: "https://avalanche.blockscout.com",
  11155111: "https://eth-sepolia.blockscout.com",
  97: "https://bsc-testnet.blockscout.com",
  80002: "https://amoy-testnet.blockscout.com",
};

async function getItems(url: string): Promise<Record<string, unknown>[] | null> {
  try {
    const res = await fetch(url);
    if (!res.ok) return null;
    const json = (await res.json()) as { items?: unknown };
    return Array.isArray(json.items) ? (json.items as Record<string, unknown>[]) : null;
  } catch {
    return null;
  }
}

function str(v: unknown): string {
  return typeof v === "string" ? v : "";
}

function hashOf(obj: Record<string, unknown> | undefined): string {
  if (!obj || typeof obj !== "object") return "";
  return str((obj as Record<string, unknown>)["hash"]);
}

function parseTimestamp(raw: unknown): number {
  const s = str(raw);
  if (!s) return 0;
  const t = Date.parse(s);
  return Number.isNaN(t) ? 0 : t;
}

function formatAmount(raw: string, decimals: number): string {
  try {
    const n = BigInt(raw);
    const neg = n < BigInt(0);
    const abs = neg ? -n : n;
    const s = abs.toString().padStart(decimals + 1, "0");
    const int = s.slice(0, s.length - decimals);
    const frac = s.slice(s.length - decimals).replace(/0+$/, "");
    const out = frac ? `${int}.${frac}` : int;
    return (neg ? "-" : "") + out;
  } catch {
    return "0";
  }
}

async function fetchNativeTxs(
  host: string,
  chain: CryptoChain,
  address: string
): Promise<OnChainTx[]> {
  const items = await getItems(`${host}/api/v2/addresses/${address}/transactions`);
  if (!items) return [];
  const out: OnChainTx[] = [];
  for (const el of items) {
    const hash = str(el["hash"]);
    if (!hash) continue;
    const from = hashOf(el["from"] as Record<string, unknown>);
    const to = hashOf(el["to"] as Record<string, unknown>);
    const valueWei = str(el["value"]) || "0";
    const feeObj = el["fee"] as Record<string, unknown> | undefined;
    const feeWei = str(feeObj?.["value"]);
    const status = str(el["status"]) === "error" ? "failed" : "success";
    const timestamp = parseTimestamp(el["timestamp"]);
    const isSend = from.toLowerCase() === address.toLowerCase();
    out.push({
      chainId: chain.id,
      chainName: chain.name,
      explorerUrl: chain.explorerUrl,
      hash,
      from,
      to,
      tokenAddress: null,
      tokenSymbol: chain.nativeSymbol,
      amount: formatAmount(valueWei, chain.nativeDecimals),
      amountRaw: valueWei,
      feeWei,
      status,
      timestamp,
      type: isSend ? "send" : "receive",
    });
  }
  return out;
}

async function fetchTokenTransfers(
  host: string,
  chain: CryptoChain,
  address: string
): Promise<OnChainTx[]> {
  const items = await getItems(`${host}/api/v2/addresses/${address}/token-transfers`);
  if (!items) return [];
  const out: OnChainTx[] = [];
  for (const el of items) {
    const hash = str(el["transaction_hash"]);
    if (!hash) continue;
    const from = hashOf(el["from"] as Record<string, unknown>);
    const to = hashOf(el["to"] as Record<string, unknown>);
    const token = el["token"] as Record<string, unknown> | undefined;
    const symbol = str(token?.["symbol"]) || "TOKEN";
    const decimals = Number.parseInt(str(token?.["decimals"]), 10) || 18;
    const total = el["total"] as Record<string, unknown> | undefined;
    const rawValue = str(total?.["value"]) || "0";
    const tokenAddress = str(token?.["address"]) || null;
    const timestamp = parseTimestamp(el["timestamp"]);
    const isSend = from.toLowerCase() === address.toLowerCase();
    out.push({
      chainId: chain.id,
      chainName: chain.name,
      explorerUrl: chain.explorerUrl,
      hash,
      from,
      to,
      tokenAddress,
      tokenSymbol: symbol,
      amount: formatAmount(rawValue, decimals),
      amountRaw: rawValue,
      feeWei: "",
      status: "success",
      timestamp,
      type: isSend ? "send" : "receive",
    });
  }
  return out;
}

export async function fetchChainActivity(
  chain: CryptoChain,
  address: string
): Promise<OnChainTx[]> {
  const host = HOSTS[chain.id];
  if (!host) return [];
  const records: OnChainTx[] = [];
  try {
    records.push(...(await fetchNativeTxs(host, chain, address)));
  } catch {
    // cadena sin explorador disponible
  }
  try {
    records.push(...(await fetchTokenTransfers(host, chain, address)));
  } catch {
    // igual
  }
  return records;
}

export async function fetchAllActivity(
  chains: CryptoChain[],
  address: string
): Promise<OnChainTx[]> {
  const groups = await Promise.all(
    chains.map((c) => fetchChainActivity(c, address))
  );
  return groups.flat().sort((a, b) => b.timestamp - a.timestamp);
}
