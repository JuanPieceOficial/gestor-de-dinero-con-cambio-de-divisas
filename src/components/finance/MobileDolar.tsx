"use client"

import { useState, useEffect, useCallback } from "react";
import { RefreshCw, DollarSign } from "lucide-react";

type BolivarRate = {
  oficial: number;
  paralelo: number;
  binance: { compra: number; venta: number; promedio: number } | null;
  euro: number;
};

const RATE_COLORS: Record<string, string> = {
  "Dólar BCV": "text-[#2275C0]",
  "Dólar USDT": "text-[#F59E0B]",
  "Dólar Promedio": "text-[#8B5CF6]",
  Euro: "text-[#06B6D4]",
  Brecha: "text-[#22C55E]",
};

function formatPrice(n: number) {
  return new Intl.NumberFormat("es-ES", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(n);
}

export function MobileDolar() {
  const [rate, setRate] = useState<BolivarRate | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [usdText, setUsdText] = useState("");
  const [vesText, setVesText] = useState("");
  const [selectedIdx, setSelectedIdx] = useState(1);
  const [focusedField, setFocusedField] = useState<"usd" | "ves" | null>(null);

  const rateList = rate
    ? [
        { label: "Dólar BCV", value: rate.oficial },
        ...(rate.binance ? [{ label: "Dólar USDT", value: rate.binance.promedio }] : []),
        { label: "Dólar Promedio", value: rate.paralelo },
        ...(rate.euro > 0 ? [{ label: "Euro", value: rate.euro }] : []),
      ]
    : [];

  const currentRate = rateList[selectedIdx]?.value || 0;

  // Derive the other field when usdText changes and usd was the last focused field
  useEffect(() => {
    if (focusedField !== "usd" || !rate) return;
    const u = parseFloat(usdText);
    if (!isNaN(u) && currentRate > 0) {
      setVesText(formatPrice(u * currentRate));
    } else if (usdText === "") {
      setVesText("");
    }
  }, [usdText, focusedField, currentRate, rate]);

  // Derive the other field when vesText changes and ves was the last focused field
  useEffect(() => {
    if (focusedField !== "ves" || !rate) return;
    const v = parseFloat(vesText);
    if (!isNaN(v) && currentRate > 0) {
      setUsdText(formatPrice(v / currentRate));
    } else if (vesText === "") {
      setUsdText("");
    }
  }, [vesText, focusedField, currentRate, rate]);

  const fetchRates = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const res = await fetch("https://api.alcambio.app/graphql", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          query: `query { getBinanceP2PAverages { sellAverage buyAverage } getCountryConversions(payload: {countryCode: "VE"}) { conversionRates { type official baseValue rateCurrency { code } } } }`,
        }),
      });
      const json = await res.json();
      const conversions = json.data?.getCountryConversions?.conversionRates || [];

      const bcvRate =
        conversions.find(
          (c: any) => c.type === "SECONDARY" && c.official && c.rateCurrency.code === "USD"
        )?.baseValue ||
        conversions.find(
          (c: any) => c.type === "OTHER" && c.official && c.rateCurrency.code === "USD"
        )?.baseValue ||
        0;

      const euroRate =
        conversions.find(
          (c: any) => c.type === "OTHER" && c.official && c.rateCurrency.code === "EUR"
        )?.baseValue || 0;

      const binanceAvg = json.data?.getBinanceP2PAverages;
      const binance = binanceAvg
        ? {
            compra: binanceAvg.buyAverage,
            venta: binanceAvg.sellAverage,
            promedio: (binanceAvg.buyAverage + binanceAvg.sellAverage) / 2,
          }
        : null;

      const usdt = binance?.promedio || bcvRate;
      const result: BolivarRate = {
        oficial: bcvRate,
        paralelo: (bcvRate + usdt) / 2,
        binance,
        euro: euroRate,
      };

      setRate(result);
      localStorage.setItem("gestorfacil_cached_rate", JSON.stringify(result));
    } catch {
      const cached = localStorage.getItem("gestorfacil_cached_rate");
      if (cached) setRate(JSON.parse(cached));
      else setError(true);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    const cached = localStorage.getItem("gestorfacil_cached_rate");
    if (cached) setRate(JSON.parse(cached));
    fetchRates();
  }, [fetchRates]);

  return (
    <div className="flex flex-col gap-3 pb-4">
      {/* Rates Card */}
      {rate && (
        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
          <p className="text-sm font-semibold text-muted-foreground mb-3">Tasas del Día</p>
          <div className="space-y-2.5">
            {rateList.map((r) => (
              <div key={r.label} className="flex justify-between items-center">
                <span className="text-sm font-medium">{r.label}</span>
                <span className={`text-sm font-bold ${RATE_COLORS[r.label] || "text-foreground"}`}>
                  Bs. {formatPrice(r.value)}
                </span>
              </div>
            ))}
            {rate.binance && (
              <>
                <div className="border-t border-border/30 my-2" />
                <div className="flex justify-between items-center">
                  <span className="text-xs text-muted-foreground">Brecha BCV ↔ USDT</span>
                  <span className="text-xs font-bold text-accent">
                    Bs. {formatPrice(rate.binance.promedio - rate.oficial)} (
                    {formatPrice(((rate.binance.promedio - rate.oficial) / rate.oficial) * 100)}%)
                  </span>
                </div>
              </>
            )}
            <p className="text-[10px] text-muted-foreground/50 mt-2">Fuente: Al Cambio</p>
          </div>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center">
          Error de conexión. Verifica tu internet.
        </div>
      )}

      {/* Empty state */}
      {!rate && !loading && !error && (
        <div className="text-center py-10">
          <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mx-auto mb-3">
            <DollarSign className="w-6 h-6 text-muted-foreground" />
          </div>
          <p className="text-xs text-muted-foreground">Obteniendo tasas...</p>
        </div>
      )}

      {/* Loading */}
      {loading && !rate && (
        <div className="flex justify-center py-10">
          <RefreshCw className="w-6 h-6 animate-spin text-primary" />
        </div>
      )}

      {/* Converter */}
      {rate && (
        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
          <p className="text-sm font-semibold text-muted-foreground mb-3">Calculadora</p>

          {/* Chip Selector */}
          <div className="flex gap-1 bg-muted p-1 rounded-xl mb-4 overflow-x-auto">
            {rateList.map((r, i) => (
              <button
                key={r.label}
                onClick={() => { setSelectedIdx(i); setUsdText(""); setVesText(""); }}
                className={`flex-1 text-[10px] py-2 px-1 rounded-lg font-medium transition-all whitespace-nowrap ${
                  i === selectedIdx
                    ? "bg-card shadow-sm text-foreground"
                    : "text-muted-foreground"
                }`}
              >
                {r.label}
                <br />
                <span className="text-[9px] opacity-70">Bs. {formatPrice(r.value)}</span>
              </button>
            ))}
          </div>

          <div className="space-y-3">
            <input
              type="number"
              placeholder="$ 0.00"
              value={usdText}
              onFocus={() => setFocusedField("usd")}
              onChange={(e) => setUsdText(e.target.value)}
              className="w-full h-12 px-4 rounded-xl bg-muted border border-border/50 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/30"
            />
            <input
              type="number"
              placeholder="Bs. 0.00"
              value={vesText}
              onFocus={() => setFocusedField("ves")}
              onChange={(e) => setVesText(e.target.value)}
              className="w-full h-12 px-4 rounded-xl bg-muted border border-border/50 text-sm font-medium outline-none focus:ring-2 focus:ring-primary/30"
            />
          </div>
        </div>
      )}

      {/* Refresh button */}
      <button
        onClick={fetchRates}
        disabled={loading}
        className="self-end w-12 h-12 rounded-full bg-primary text-white shadow-lg shadow-primary/30 flex items-center justify-center active:scale-90 transition-transform disabled:opacity-50"
      >
        <RefreshCw className={`w-5 h-5 ${loading ? "animate-spin" : ""}`} />
      </button>
    </div>
  );
}
