"use client"

import { useState, useCallback, useEffect } from "react";
import {
  RefreshCw,
  Bitcoin,
  Wallet,
  ExternalLink,
  CircleCheck,
  TriangleAlert,
  Coins,
} from "lucide-react";
import {
  CRYPTO_CHAINS,
  fetchPortfolio,
  isValidAddress,
  shortAddress,
  formatCryptoAmount,
} from "@/app/lib/crypto";

interface MobileCarteraProps {
  formatCurrency: (val: number) => string;
}

const STORAGE_KEY = "gestorfacil_wallet_address";

export function MobileCartera({ formatCurrency }: MobileCarteraProps) {
  const [addressInput, setAddressInput] = useState("");
  const [savedAddress, setSavedAddress] = useState("");
  const [portfolio, setPortfolio] = useState<{
    byChain: { chain: (typeof CRYPTO_CHAINS)[number]; items: { symbol: string; balance: number; price: number; fiatValue: number }[]; chainFiat: number }[];
    totalFiat: number;
  } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [edited, setEdited] = useState(false);

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY) || "";
    setSavedAddress(saved);
    setAddressInput(saved);
  }, []);

  const loadPortfolio = useCallback(async (addr: string) => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchPortfolio(addr, "USD");
      setPortfolio(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error de red");
      setPortfolio(null);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    if (savedAddress) loadPortfolio(savedAddress);
  }, [savedAddress, loadPortfolio]);

  const saveAddress = () => {
    const addr = addressInput.trim();
    if (!isValidAddress(addr)) {
      setError("Dirección inválida. Debe ser 0x + 40 caracteres hexadecimales.");
      return;
    }
    localStorage.setItem(STORAGE_KEY, addr);
    setSavedAddress(addr);
    setEdited(false);
    loadPortfolio(addr);
  };

  const total = portfolio?.totalFiat ?? 0;
  const hasAddress = savedAddress.length > 0;

  return (
    <div className="flex flex-col gap-4 pb-4">
      {/* Address input card */}
      <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-primary">
            <Bitcoin className="w-4 h-4" />
          </div>
          <div>
            <p className="text-sm font-semibold">Mi cartera crypto</p>
            <p className="text-[10px] text-muted-foreground">
              Saldos en 7 cadenas EVM · solo dirección pública
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="0x..."
            value={addressInput}
            onChange={(e) => { setAddressInput(e.target.value); setEdited(true); setError(null); }}
            className="flex-1 h-11 px-3 rounded-xl bg-muted border border-border/50 text-xs font-mono font-medium outline-none focus:ring-2 focus:ring-primary/30"
          />
          <button
            onClick={saveAddress}
            disabled={loading}
            className="h-11 px-4 rounded-xl bg-primary text-white text-sm font-semibold shadow-md shadow-primary/20 active:scale-95 transition-transform disabled:opacity-50"
          >
            Guardar
          </button>
        </div>
        {edited && hasAddress && (
          <p className="text-[10px] text-muted-foreground mt-2 flex items-center gap-1">
            <TriangleAlert className="w-3 h-3" /> Pulsa Guardar para actualizar la dirección
          </p>
        )}
      </div>

      {error && (
        <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
          <TriangleAlert className="w-4 h-4" /> {error}
        </div>
      )}

      {hasAddress && !error && (
        <>
          {/* Total card */}
          <div className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground rounded-2xl p-5 shadow-lg shadow-primary/20">
            <p className="text-sm font-medium opacity-80 uppercase tracking-wider">Patrimonio Crypto</p>
            <p className="text-4xl font-bold mt-1 tracking-tight">
              {formatCurrency(total)}
            </p>
            <div className="mt-4 flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2 w-fit">
              <Wallet className="w-4 h-4" />
              <p className="text-xs font-medium font-mono">{shortAddress(savedAddress)}</p>
              <a
                href={`https://etherscan.io/address/${savedAddress}`}
                target="_blank"
                rel="noreferrer"
                className="opacity-80 hover:opacity-100 transition-opacity"
                aria-label="Ver en el explorador"
              >
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </div>
          </div>

          {/* Chains */}
          {loading && !portfolio ? (
            <div className="flex justify-center py-10">
              <RefreshCw className="w-6 h-6 animate-spin text-primary" />
            </div>
          ) : portfolio && portfolio.byChain.length > 0 ? (
            portfolio.byChain.map((group) => (
              <div key={group.chain.id} className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
                <div className="flex justify-between items-center mb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-xl bg-primary/10 flex items-center justify-center">
                      <Coins className="w-4 h-4 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold">{group.chain.name}</p>
                      <p className="text-[10px] text-muted-foreground">{group.chain.shortName}</p>
                    </div>
                  </div>
                  <span className="text-sm font-bold">{formatCurrency(group.chainFiat)}</span>
                </div>
                <div className="space-y-2">
                  {group.items.map((item) => (
                    <div key={item.symbol} className="flex justify-between items-center text-xs">
                      <span className="text-muted-foreground font-medium">
                        {item.symbol}
                        <span className="ml-1.5 text-foreground font-semibold">
                          {formatCryptoAmount(item.balance)}
                        </span>
                      </span>
                      <span className="text-muted-foreground">
                        {item.price > 0 ? formatCurrency(item.fiatValue) : "—"}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-8">
              <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mx-auto mb-3">
                <CircleCheck className="w-6 h-6 text-muted-foreground" />
              </div>
              <p className="text-xs text-muted-foreground">
                Sin saldos en estas cadenas. Los tokens con balance 0 se ocultan.
              </p>
            </div>
          )}
        </>
      )}

      {!hasAddress && !error && (
        <div className="text-center py-10">
          <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mx-auto mb-3">
            <Bitcoin className="w-6 h-6 text-muted-foreground" />
          </div>
          <p className="text-xs text-muted-foreground">
            Pega la dirección pública de tu wallet EVM (la que ves en la app en
            “Cartera → Ajustes → Mi dirección”) para ver tus saldos aquí.
          </p>
        </div>
      )}

      {/* Refresh */}
      {hasAddress && (
        <button
          onClick={() => loadPortfolio(savedAddress)}
          disabled={loading}
          className="self-end w-12 h-12 rounded-full bg-primary text-white shadow-lg shadow-primary/30 flex items-center justify-center active:scale-90 transition-transform disabled:opacity-50"
          aria-label="Actualizar saldos"
        >
          <RefreshCw className={`w-5 h-5 ${loading ? "animate-spin" : ""}`} />
        </button>
      )}
    </div>
  );
}
