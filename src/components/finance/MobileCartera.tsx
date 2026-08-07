"use client";

import { useCallback, useEffect, useState } from "react";
import {
  RefreshCw,
  Bitcoin,
  Wallet,
  ExternalLink,
  CircleCheck,
  TriangleAlert,
  Coins,
  Plus,
  Eye,
  EyeOff,
  Copy,
  Trash2,
  LogOut,
  ShieldCheck,
  ArrowLeft,
  CheckCircle2,
  KeyRound,
} from "lucide-react";
import {
  CRYPTO_CHAINS,
  fetchPortfolio,
  shortAddress,
  formatCryptoAmount,
} from "@/app/lib/crypto";
import * as walletLib from "@/app/lib/wallet";

interface MobileCarteraProps {
  formatCurrency: (val: number) => string;
}

type Stage = "welcome" | "create" | "import" | "unlock" | "portfolio" | "reveal";
type CreateStep = "phrase" | "pin";

type Portfolio = {
  byChain: { chain: (typeof CRYPTO_CHAINS)[number]; items: { symbol: string; balance: number; price: number; fiatValue: number }[]; chainFiat: number }[];
  totalFiat: number;
};

const VALID_PIN = /^\d{4,8}$/;

export function MobileCartera({ formatCurrency }: MobileCarteraProps) {
  const [stage, setStage] = useState<Stage>("welcome");
  const [createStep, setCreateStep] = useState<CreateStep>("phrase");
  const [address, setAddress] = useState("");
  const [phrase, setPhrase] = useState("");
  const [savedPhrase, setSavedPhrase] = useState(false);
  const [showPhrase, setShowPhrase] = useState(false);
  const [pin, setPin] = useState("");
  const [pin2, setPin2] = useState("");
  const [importText, setImportText] = useState("");
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!walletLib.isWebCryptoAvailable()) {
      setError("Tu navegador no soporta cifrado local. Necesitás HTTPS (o localhost).");
      return;
    }
    if (walletLib.hasWallet()) setStage("unlock");
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

  const copy = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      setInfo("No se pudo copiar automáticamente.");
    }
  };

  const resetFlow = () => {
    setPin("");
    setPin2("");
    setImportText("");
    setError(null);
    setInfo(null);
  };

  const startCreate = () => {
    resetFlow();
    setPhrase(walletLib.generatePhrase());
    setSavedPhrase(false);
    setShowPhrase(false);
    setCreateStep("phrase");
    setStage("create");
  };

  const startImport = () => {
    resetFlow();
    setStage("import");
  };

  const confirmCreate = async () => {
    if (!VALID_PIN.test(pin) || pin !== pin2) {
      setError("El PIN debe tener entre 4 y 8 dígitos y coincidir en ambos campos.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const addr = await walletLib.persistWallet(phrase, pin);
      setAddress(addr);
      setStage("portfolio");
      loadPortfolio(addr);
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo crear la billetera.");
    }
    setBusy(false);
  };

  const confirmImport = async () => {
    if (!walletLib.isValidPhrase(importText)) {
      setError("La frase debe tener 12, 15, 18, 21 o 24 palabras.");
      return;
    }
    if (!VALID_PIN.test(pin) || pin !== pin2) {
      setError("El PIN debe tener entre 4 y 8 dígitos y coincidir en ambos campos.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const { address: addr } = await walletLib.importWallet(importText, pin);
      setAddress(addr);
      setStage("portfolio");
      loadPortfolio(addr);
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo importar la billetera.");
    }
    setBusy(false);
  };

  const confirmUnlock = async () => {
    if (pin.length === 0) return;
    setBusy(true);
    setError(null);
    const addr = await walletLib.unlockWallet(pin);
    if (addr) {
      setAddress(addr);
      setStage("portfolio");
      loadPortfolio(addr);
    } else {
      setError("PIN incorrecto. Probá de nuevo.");
      setPin("");
    }
    setBusy(false);
  };

  const confirmReveal = async () => {
    setBusy(true);
    setError(null);
    const p = await walletLib.revealPhrase(pin);
    if (p) {
      setPhrase(p);
      setShowPhrase(true);
    } else {
      setError("PIN incorrecto.");
    }
    setBusy(false);
  };

  const doDeleteWallet = () => {
    walletLib.deleteWallet();
    setAddress("");
    setPortfolio(null);
    setConfirmDelete(false);
    resetFlow();
    setStage("welcome");
  };

  // ---------- Pantalla: bienvenida ----------
  if (stage === "welcome") {
    return (
      <div className="flex flex-col items-center justify-center text-center py-10 gap-6">
        <div className="w-20 h-20 rounded-full bg-gradient-to-br from-primary to-primary/70 flex items-center justify-center text-white shadow-lg shadow-primary/25">
          <ShieldCheck className="w-10 h-10" />
        </div>
        <div>
          <h2 className="text-2xl font-bold tracking-tight">Cartera Crypto</h2>
          <p className="text-sm text-muted-foreground mt-1.5 max-w-[260px]">
            Billetera no custodiada. Solo vos controlás tus llaves privadas,
            cifradas localmente con tu PIN.
          </p>
        </div>

        <div className="w-full space-y-3">
          <button
            onClick={startCreate}
            className="w-full h-12 rounded-xl bg-primary text-white font-semibold shadow-lg shadow-primary/25 flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <Plus className="w-5 h-5" /> Crear nueva billetera
          </button>
          <button
            onClick={startImport}
            className="w-full h-12 rounded-xl bg-card border border-border/60 text-foreground font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform hover:bg-muted/40"
          >
            <KeyRound className="w-5 h-5" /> Restaurar billetera existente
          </button>
        </div>

        <p className="text-[10px] text-muted-foreground/70 leading-relaxed max-w-[280px]">
          Tu frase de recuperación se cifra con AES-256 y nunca sale de este
          navegador. Compatible con cualquier wallet BIP-39 (MetaMask,
          Trust Wallet, la app Android...).
        </p>

        {error && (
          <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
            <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}
      </div>
    );
  }

  // ---------- Pantalla: crear (paso 1: frase) ----------
  if (stage === "create" && createStep === "phrase") {
    const words = phrase.split(" ");
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => setStage("welcome")} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="bg-destructive/10 text-destructive text-xs p-3.5 rounded-xl leading-relaxed flex gap-2">
          <TriangleAlert className="w-4 h-4 shrink-0 mt-0.5" />
          <span>
            Guardá estas <b>24 palabras en orden</b> en papel y guardalas en un
            lugar seguro. Si las perdés, <b>no hay forma de recuperar tu dinero</b>.
          </span>
        </div>

        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold">Frase de recuperación</p>
            <button
              onClick={() => setShowPhrase((s) => !s)}
              className="w-8 h-8 rounded-full flex items-center justify-center text-muted-foreground hover:bg-muted transition-colors"
              aria-label={showPhrase ? "Ocultar frase" : "Mostrar frase"}
            >
              {showPhrase ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
          {showPhrase ? (
            <div className="grid grid-cols-2 gap-1.5">
              {words.map((w, i) => (
                <div key={i} className="flex items-center gap-1.5 text-xs bg-muted/60 rounded-lg px-2 py-1.5">
                  <span className="text-muted-foreground/60 w-5 text-right font-mono">{i + 1}.</span>
                  <span className="font-semibold font-mono">{w}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 gap-2 text-muted-foreground">
              <Eye className="w-6 h-6" />
              <p className="text-xs">Tocá el ojo para revelar tu frase</p>
            </div>
          )}
        </div>

        <label className="flex items-start gap-2.5 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={savedPhrase}
            onChange={(e) => setSavedPhrase(e.target.checked)}
            className="mt-0.5 w-4 h-4 accent-primary"
          />
          <span className="text-xs text-muted-foreground leading-relaxed">
            Guardé mi frase de recuperación en papel y en orden.
          </span>
        </label>

        <button
          onClick={() => { setSavedPhrase(false); setCreateStep("pin"); }}
          disabled={!savedPhrase}
          className="w-full h-12 rounded-xl bg-primary text-white font-semibold shadow-lg shadow-primary/25 flex items-center justify-center gap-2 active:scale-[0.98] transition-transform disabled:opacity-40 disabled:shadow-none"
        >
          Continuar <ArrowLeft className="w-4 h-4 rotate-180" />
        </button>
      </div>
    );
  }

  // ---------- Pantalla: crear (paso 2: PIN) / importar / desbloquear / revelar ----------
  const needsPin =
    stage === "create" ||
    stage === "import" ||
    stage === "unlock" ||
    (stage === "reveal" && !showPhrase);

  const PIN_TITLES: Record<string, string> = {
    create: "Elegí tu PIN de desbloqueo",
    import: "Elegí tu PIN de desbloqueo",
    unlock: "Desbloquear billetera",
    reveal: "Confirmá tu PIN",
  };
  const pinTitle = PIN_TITLES[stage];

  const isNewPin = stage === "create" || stage === "import";

  if (needsPin) {
    return (
      <div className="flex flex-col gap-4 pb-4">
        {stage !== "unlock" && (
          <button
            onClick={() => {
              resetFlow();
              if (stage === "create") setCreateStep("phrase");
              else if (stage === "reveal") setStage("portfolio");
              else setStage("welcome");
            }}
            className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-4 h-4" /> Volver
          </button>
        )}

        <div className="flex flex-col items-center py-6 text-center">
          <div className="w-14 h-14 rounded-2xl bg-primary/10 flex items-center justify-center text-primary mb-3">
            <ShieldCheck className="w-7 h-7" />
          </div>
          <h3 className="text-lg font-bold">{pinTitle}</h3>
          {isNewPin ? (
            <p className="text-xs text-muted-foreground mt-1 max-w-[280px]">
              Este PIN cifra tu frase de recuperación en este navegador. No lo
              olvides: sin él no se puede desbloquear la billetera.
            </p>
          ) : (
            <p className="text-xs text-muted-foreground mt-1 max-w-[280px]">
              Ingresá el PIN que elegiste al crear la billetera.
            </p>
          )}
        </div>

        {stage === "import" && (
          <textarea
            value={importText}
            onChange={(e) => setImportText(e.target.value)}
            placeholder="Pegá tu frase de 12, 15, 18, 21 o 24 palabras separadas por espacios..."
            rows={3}
            className="w-full px-3 py-2.5 rounded-xl bg-muted border border-border/50 text-xs font-mono outline-none focus:ring-2 focus:ring-primary/30 resize-none"
          />
        )}

        <input
          type="password"
          inputMode="numeric"
          autoComplete="off"
          placeholder={isNewPin ? "PIN (4-8 dígitos)" : "Tu PIN"}
          value={pin}
          onChange={(e) => setPin(e.target.value.replace(/\D/g, "").slice(0, 8))}
          className="w-full h-12 px-4 rounded-xl bg-card border border-border/60 text-center text-xl tracking-[0.4em] font-bold outline-none focus:ring-2 focus:ring-primary/40"
        />

        {isNewPin && (
          <input
            type="password"
            inputMode="numeric"
            autoComplete="off"
            placeholder="Repetí el PIN"
            value={pin2}
            onChange={(e) => setPin2(e.target.value.replace(/\D/g, "").slice(0, 8))}
            className="w-full h-12 px-4 rounded-xl bg-card border border-border/60 text-center text-xl tracking-[0.4em] font-bold outline-none focus:ring-2 focus:ring-primary/40"
          />
        )}

        {error && (
          <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
            <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        <button
          onClick={stage === "create" ? confirmCreate : stage === "import" ? confirmImport : stage === "unlock" ? confirmUnlock : confirmReveal}
          disabled={busy || pin.length === 0 || (isNewPin && pin !== pin2)}
          className="w-full h-12 rounded-xl bg-primary text-white font-semibold shadow-lg shadow-primary/25 flex items-center justify-center gap-2 active:scale-[0.98] transition-transform disabled:opacity-40 disabled:shadow-none"
        >
          {busy ? "Un momento..." : stage === "create" ? "Crear billetera" : stage === "import" ? "Importar billetera" : stage === "unlock" ? "Desbloquear" : "Confirmar"}
        </button>

        {stage === "unlock" && (
          <div className="text-center">
            <button
              onClick={() => { resetFlow(); setConfirmDelete(true); }}
              className="text-xs text-muted-foreground/70 hover:text-destructive transition-colors"
            >
              Olvidé mi PIN — borrar billetera
            </button>
          </div>
        )}

        {confirmDelete && stage === "unlock" && (
          <div className="bg-destructive/10 border border-destructive/30 rounded-xl p-4 space-y-3">
            <p className="text-xs text-destructive leading-relaxed">
              Al borrar la billetera se elimina la frase cifrada de este
              navegador. <b>Si no tenés la frase guardada en papel, perderás el
              acceso a esos fondos para siempre.</b>
            </p>
            <div className="flex gap-2">
              <button
                onClick={doDeleteWallet}
                className="flex-1 h-10 rounded-xl bg-destructive text-white text-sm font-semibold active:scale-[0.98] transition-transform"
              >
                Borrar definitivamente
              </button>
              <button
                onClick={() => setConfirmDelete(false)}
                className="flex-1 h-10 rounded-xl bg-card border border-border/60 text-sm font-semibold"
              >
                Cancelar
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ---------- Pantalla: revelar frase (ya autenticado) ----------
  if (stage === "reveal" && showPhrase) {
    const words = phrase.split(" ");
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => { setShowPhrase(false); setPin(""); setStage("portfolio"); }} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="bg-destructive/10 text-destructive text-xs p-3.5 rounded-xl leading-relaxed flex gap-2">
          <TriangleAlert className="w-4 h-4 shrink-0 mt-0.5" />
          <span>Nunca compartas esta frase. Cualquiera que la tenga puede
            mover tus fondos. Guardala en papel, fuera de internet.</span>
        </div>

        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm">
          <div className="grid grid-cols-2 gap-1.5">
            {words.map((w, i) => (
              <div key={i} className="flex items-center gap-1.5 text-xs bg-muted/60 rounded-lg px-2 py-1.5">
                <span className="text-muted-foreground/60 w-5 text-right font-mono">{i + 1}.</span>
                <span className="font-semibold font-mono">{w}</span>
              </div>
            ))}
          </div>
        </div>

        <button
          onClick={() => copy(phrase)}
          className="w-full h-11 rounded-xl bg-primary text-white text-sm font-semibold shadow-md shadow-primary/20 flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
        >
          {copied ? <CheckCircle2 className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
          {copied ? "¡Copiada!" : "Copiar frase"}
        </button>
      </div>
    );
  }

  // ---------- Pantalla: cartera (autenticado) ----------
  const total = portfolio?.totalFiat ?? 0;

  return (
    <div className="flex flex-col gap-4 pb-4">
      {/* Total card */}
      <div className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground rounded-2xl p-5 shadow-lg shadow-primary/20">
        <p className="text-sm font-medium opacity-80 uppercase tracking-wider">Patrimonio Crypto</p>
        <p className="text-4xl font-bold mt-1 tracking-tight">{formatCurrency(total)}</p>
        <div className="mt-4 flex items-center gap-2 bg-white/15 rounded-xl px-3 py-2 w-fit">
          <Wallet className="w-4 h-4 shrink-0" />
          <p className="text-xs font-medium font-mono">{shortAddress(address)}</p>
          <button
            onClick={() => copy(address)}
            className="opacity-80 hover:opacity-100 transition-opacity"
            aria-label="Copiar dirección"
          >
            {copied ? <CheckCircle2 className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
          <a
            href={`https://etherscan.io/address/${address}`}
            target="_blank"
            rel="noreferrer"
            className="opacity-80 hover:opacity-100 transition-opacity"
            aria-label="Ver en el explorador"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
        <p className="text-[10px] opacity-70 mt-2">Tu dirección para recibir en cualquier cadena EVM</p>
      </div>

      {error && (
        <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
          <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
        </div>
      )}

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
            Sin saldos en estas cadenas. Enviá fondos a tu dirección para verlos aquí.
          </p>
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2 justify-end">
        <button
          onClick={() => loadPortfolio(address)}
          disabled={loading}
          className="w-12 h-12 rounded-full bg-primary text-white shadow-lg shadow-primary/30 flex items-center justify-center active:scale-90 transition-transform disabled:opacity-50"
          aria-label="Actualizar saldos"
        >
          <RefreshCw className={`w-5 h-5 ${loading ? "animate-spin" : ""}`} />
        </button>
        <button
          onClick={() => { setPin(""); setError(null); setStage("reveal"); }}
          className="h-12 px-4 rounded-full bg-card border border-border/60 text-sm font-semibold flex items-center gap-2 active:scale-95 transition-transform"
        >
          <Eye className="w-4 h-4" /> Ver frase
        </button>
        <button
          onClick={() => setStage("unlock")}
          className="h-12 px-4 rounded-full bg-card border border-border/60 text-sm font-semibold flex items-center gap-2 active:scale-95 transition-transform"
          title="Bloquear billetera"
        >
          <LogOut className="w-4 h-4" /> Bloquear
        </button>
        <button
          onClick={() => setConfirmDelete((c) => !c)}
          className="w-12 h-12 rounded-full bg-card border border-border/60 text-muted-foreground hover:text-destructive hover:border-destructive/40 flex items-center justify-center active:scale-90 transition-all"
          aria-label="Borrar billetera"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>

      {confirmDelete && (
        <div className="bg-destructive/10 border border-destructive/30 rounded-xl p-4 space-y-3">
          <p className="text-xs text-destructive leading-relaxed">
            ¿Borrar la billetera de este navegador? Se elimina la frase cifrada.
            <b> Si no la tenés guardada en papel, perderás el acceso a esos fondos.</b>
          </p>
          <div className="flex gap-2">
            <button
              onClick={doDeleteWallet}
              className="flex-1 h-10 rounded-xl bg-destructive text-white text-sm font-semibold active:scale-[0.98] transition-transform"
            >
              Borrar definitivamente
            </button>
            <button
              onClick={() => setConfirmDelete(false)}
              className="flex-1 h-10 rounded-xl bg-card border border-border/60 text-sm font-semibold"
            >
              Cancelar
            </button>
          </div>
        </div>
      )}

      {info && <p className="text-xs text-muted-foreground text-center">{info}</p>}
    </div>
  );
}

