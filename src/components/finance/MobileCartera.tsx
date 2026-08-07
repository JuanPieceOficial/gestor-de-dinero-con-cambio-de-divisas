"use client";

import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import QRCode from "qrcode";
import {
  RefreshCw,
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
  Send as SendIcon,
  ArrowDownLeft,
  ArrowUpRight,
  History,
  Settings as SettingsIcon,
  ChevronDown,
  QrCode as QrCodeIcon,
  Lock,
} from "lucide-react";
import { formatUnits } from "ethers";
import {
  CRYPTO_CHAINS,
  fetchPortfolio,
  shortAddress,
  formatCryptoAmount,
  type CryptoChain,
} from "@/app/lib/crypto";
import * as walletLib from "@/app/lib/wallet";
import { fetchAllActivity, type OnChainTx } from "@/app/lib/blockscout";
import {
  estimateNativeSend,
  estimateTokenSend,
  sendNative,
  sendToken,
  isValidRecipient,
  type FeeEstimate,
} from "@/app/lib/send";

interface MobileCarteraProps {
  formatCurrency: (val: number) => string;
}

type Stage = "welcome" | "create" | "import" | "unlock" | "portfolio" | "reveal";
type CreateStep = "phrase" | "pin";
type View = "portfolio" | "send" | "receive" | "activity" | "settings";

type Portfolio = {
  byChain: { chain: CryptoChain; items: { symbol: string; balance: number; price: number; fiatValue: number }[]; chainFiat: number }[];
  totalFiat: number;
};

type TokenOption = {
  key: string;
  chainId: number;
  symbol: string;
  decimals: number;
  balance: number;
  tokenAddress: string | null;
};

const VALID_PIN = /^\d{4,8}$/;

export function MobileCartera({ formatCurrency }: MobileCarteraProps) {
  const [stage, setStage] = useState<Stage>("welcome");
  const [createStep, setCreateStep] = useState<CreateStep>("phrase");
  const [view, setView] = useState<View>("portfolio");
  const [address, setAddress] = useState("");
  const [pk, setPk] = useState<string | null>(null);
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
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [copied, setCopied] = useState(false);

  // ---- Enviar ----
  const [sendStep, setSendStep] = useState<0 | 1 | 2>(0);
  const [selTokenKey, setSelTokenKey] = useState("");
  const [recipient, setRecipient] = useState("");
  const [amountText, setAmountText] = useState("");
  const [fee, setFee] = useState<FeeEstimate | null>(null);
  const [txHash, setTxHash] = useState("");
  const [sendBusy, setSendBusy] = useState(false);

  // ---- Recibir ----
  const [recvChain, setRecvChain] = useState<CryptoChain>(CRYPTO_CHAINS[0]);
  const [recvChainOpen, setRecvChainOpen] = useState(false);
  const [qrData, setQrData] = useState("");

  // ---- Actividad ----
  const [activity, setActivity] = useState<OnChainTx[] | null>(null);
  const [activityLoading, setActivityLoading] = useState(false);
  const [selTx, setSelTx] = useState<OnChainTx | null>(null);

  // ---- Ajustes (cambiar PIN) ----
  const [oldPin, setOldPin] = useState("");
  const [newPin, setNewPin] = useState("");
  const [newPin2, setNewPin2] = useState("");
  const [info, setInfo] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

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
      setError("No se pudo copiar automáticamente.");
    }
  };

  const resetFlow = () => {
    setPin("");
    setPin2("");
    setImportText("");
    setError(null);
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

  const enterWallet = (addr: string, privateKey: string) => {
    setAddress(addr);
    setPk(privateKey);
    setView("portfolio");
    setStage("portfolio");
    setConfirmDelete(false);
    loadPortfolio(addr);
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
      const privateKey = await walletLib.getPrivateKey(pin);
      if (!privateKey) throw new Error("No se pudo derivar la clave.");
      enterWallet(addr, privateKey);
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
      const privateKey = await walletLib.getPrivateKey(pin);
      if (!privateKey) throw new Error("No se pudo derivar la clave.");
      enterWallet(addr, privateKey);
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
      const privateKey = await walletLib.getPrivateKey(pin);
      enterWallet(addr, privateKey ?? "");
      if (!privateKey) setError("No se pudo derivar la clave. Volvé a intentar.");
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

  const lockWallet = () => {
    setPk(null);
    setActivity(null);
    setConfirmDelete(false);
    setView("portfolio");
    resetFlow();
    setStage("unlock");
  };

  const doDeleteWallet = () => {
    walletLib.deleteWallet();
    setPk(null);
    setAddress("");
    setPortfolio(null);
    setActivity(null);
    setConfirmDelete(false);
    resetFlow();
    setStage("welcome");
  };

  const confirmChangePin = async () => {
    if (!VALID_PIN.test(newPin) || newPin !== newPin2) {
      setError("El nuevo PIN debe tener entre 4 y 8 dígitos y coincidir en ambos campos.");
      return;
    }
    setBusy(true);
    setError(null);
    const ok = await walletLib.changePin(oldPin, newPin);
    if (ok) {
      setOldPin("");
      setNewPin("");
      setNewPin2("");
      setError(null);
      setInfo("PIN actualizado correctamente.");
    } else {
      setError("El PIN actual es incorrecto.");
    }
    setBusy(false);
  };

  // ---------- Enviar ----------
  const tokenOptions = useMemo<TokenOption[]>(() => {
    if (!portfolio) return [];
    const opts: TokenOption[] = [];
    for (const g of portfolio.byChain) {
      const native = g.items.find((i) => i.symbol === g.chain.nativeSymbol);
      if (native) {
        opts.push({
          key: `${g.chain.id}:native`,
          chainId: g.chain.id,
          symbol: g.chain.nativeSymbol,
          decimals: g.chain.nativeDecimals,
          balance: native.balance,
          tokenAddress: null,
        });
      }
      for (const t of g.chain.tokens) {
        const item = g.items.find((i) => i.symbol === t.symbol);
        if (item && item.balance > 0) {
          opts.push({
            key: `${g.chain.id}:${t.address}`,
            chainId: g.chain.id,
            symbol: t.symbol,
            decimals: t.decimals,
            balance: item.balance,
            tokenAddress: t.address,
          });
        }
      }
    }
    return opts;
  }, [portfolio]);

  const selToken = useMemo(
    () => tokenOptions.find((t) => t.key === selTokenKey) ?? tokenOptions[0],
    [tokenOptions, selTokenKey]
  );
  const selChain = useMemo(
    () => CRYPTO_CHAINS.find((c) => c.id === selToken?.chainId) ?? CRYPTO_CHAINS[0],
    [selToken]
  );

  const openSend = () => {
    if (tokenOptions.length === 0) {
      setError("No tenés saldos para enviar todavía.");
      return;
    }
    setSelTokenKey(tokenOptions[0].key);
    setRecipient("");
    setAmountText("");
    setFee(null);
    setTxHash("");
    setError(null);
    setSendStep(0);
    setView("send");
  };

  const continueSend = async () => {
    if (!selToken) return;
    const to = recipient.trim();
    if (!isValidRecipient(to)) {
      setError("Dirección del destinatario inválida (debe ser 0x + 40 hex).");
      return;
    }
    let parsed: bigint;
    try {
      parsed = ethersParseUnits(amountText, selToken.decimals);
    } catch {
      setError("Importe inválido.");
      return;
    }
    if (parsed <= BigInt(0)) {
      setError("El importe debe ser mayor que cero.");
      return;
    }
    if (Number(amountText) > selToken.balance) {
      setError(`Saldo insuficiente. Disponible: ${formatCryptoAmount(selToken.balance)} ${selToken.symbol}`);
      return;
    }
    setBusy(true);
    setError(null);
    const est = selToken.tokenAddress
      ? await estimateTokenSend(selChain, { address: selToken.tokenAddress, symbol: selToken.symbol, decimals: selToken.decimals }, address, to, amountText)
      : await estimateNativeSend(selChain, address, to, amountText);
    setBusy(false);
    setFee(est);
    if (!est.error) {
      setSendStep(1);
    } else {
      setError(`No se pudo estimar: ${est.error}`);
    }
  };

  const confirmSend = async () => {
    if (!selToken || !pk) return;
    setSendBusy(true);
    setError(null);
    try {
      const hash = selToken.tokenAddress
        ? await sendToken(selChain, { address: selToken.tokenAddress, symbol: selToken.symbol, decimals: selToken.decimals }, pk, recipient.trim(), amountText)
        : await sendNative(selChain, pk, recipient.trim(), amountText);
      setTxHash(hash);
      setSendStep(2);
      loadPortfolio(address);
    } catch (e) {
      setError(e instanceof Error ? e.message : "La transacción falló.");
    }
    setSendBusy(false);
  };

  // ---------- Recibir (QR) ----------
  useEffect(() => {
    if (stage !== "portfolio" || view !== "receive") return;
    let alive = true;
    QRCode.toDataURL(address, {
      width: 220,
      margin: 1,
      errorCorrectionLevel: "M",
      color: { dark: "#0B1220", light: "#FFFFFF" },
    })
      .then((url) => {
        if (alive) setQrData(url);
      })
      .catch(() => {
        if (alive) setQrData("");
      });
    return () => {
      alive = false;
    };
  }, [stage, view, address]);

  // ---------- Actividad ----------
  useEffect(() => {
    if (stage !== "portfolio" || view !== "activity") return;
    let alive = true;
    setActivityLoading(true);
    fetchAllActivity(CRYPTO_CHAINS, address)
      .then((rows) => {
        if (alive) setActivity(rows);
      })
      .catch(() => {
        if (alive) setActivity([]);
      })
      .finally(() => {
        if (alive) setActivityLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [stage, view, address, refreshKey]);

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

  // ---------- Pantallas con PIN (crear paso 2 / importar / desbloquear / revelar) ----------
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
              else if (stage === "reveal") {
                setStage("portfolio");
                setView("settings");
              } else setStage("welcome");
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

  // ---------- Pantalla: revelar frase (autenticado) ----------
  if (stage === "reveal" && showPhrase) {
    const words = phrase.split(" ");
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => { setShowPhrase(false); setPin(""); setStage("portfolio"); setView("settings"); }} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
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

  // =================================================================
  //  WALLET AUTENTICADA: Cartera / Enviar / Recibir / Actividad / Ajustes
  // =================================================================

  // ---------- Enviar: paso 2 (resultado) ----------
  if (view === "send" && sendStep === 2) {
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => { setView("portfolio"); }} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver a la cartera
        </button>
        <div className="flex flex-col items-center py-8 text-center gap-3">
          <div className="w-16 h-16 rounded-full bg-accent/10 flex items-center justify-center">
            <CheckCircle2 className="w-8 h-8 text-accent" />
          </div>
          <h3 className="text-lg font-bold">Transacción enviada</h3>
          <p className="text-xs text-muted-foreground max-w-[280px]">
            Tu transacción está en la red {selChain?.name} y pendiente de confirmación.
          </p>
          <p className="text-xs font-mono bg-muted/60 rounded-lg px-3 py-1.5 max-w-full break-all">
            {shortAddress(txHash, 14)}
          </p>
          <div className="flex gap-2">
            <a
              href={`${selChain?.explorerUrl}/tx/${txHash}`}
              target="_blank"
              rel="noreferrer"
              className="h-11 px-4 rounded-xl bg-card border border-border/60 text-sm font-semibold flex items-center gap-2 active:scale-95 transition-transform"
            >
              <ExternalLink className="w-4 h-4" /> Ver en el explorador
            </a>
            <button
              onClick={() => setView("portfolio")}
              className="h-11 px-4 rounded-xl bg-primary text-white text-sm font-semibold active:scale-95 transition-transform"
            >
              Listo
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ---------- Enviar: paso 1 (confirmación) ----------
  if (view === "send" && sendStep === 1 && selToken) {
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => { setSendStep(0); setError(null); }} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm space-y-3">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Enviar</span>
            <span className="font-bold">{amountText} {selToken.symbol}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Destinatario</span>
            <span className="font-mono text-xs break-all max-w-[55%] text-right">{shortAddress(recipient, 14)}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Red</span>
            <span className="font-semibold">{selChain?.name}</span>
          </div>
          {fee && !fee.error && (
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Comisión estimada</span>
              <span className="font-semibold">
                {formatUnits(fee.feeWei, selChain!.nativeDecimals)} {selChain?.nativeSymbol}
              </span>
            </div>
          )}
        </div>

        {error && (
          <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
            <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        <button
          onClick={confirmSend}
          disabled={sendBusy}
          className="w-full h-12 rounded-xl bg-primary text-white font-semibold shadow-lg shadow-primary/25 active:scale-[0.98] transition-transform disabled:opacity-50"
        >
          {sendBusy ? "Enviando..." : "Confirmar y enviar"}
        </button>
      </div>
    );
  }

  // ---------- Enviar: paso 0 (formulario) ----------
  if (view === "send") {
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => setView("portfolio")} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <SendIcon className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base font-bold">Enviar cripto</h3>
            <p className="text-[11px] text-muted-foreground">Firmado localmente con tu clave</p>
          </div>
        </div>

        {/* Token selector */}
        <div className="relative">
          <label className="text-xs text-muted-foreground mb-1 block">Token</label>
          <select
            value={selToken?.key ?? ""}
            onChange={(e) => { setSelTokenKey(e.target.value); setAmountText(""); setFee(null); }}
            className="w-full h-11 px-3 rounded-xl bg-card border border-border/60 text-sm font-semibold outline-none focus:ring-2 focus:ring-primary/30 appearance-none"
          >
            {tokenOptions.map((t) => (
              <option key={t.key} value={t.key}>
                {t.symbol} · {formatCryptoAmount(t.balance)} · {CRYPTO_CHAINS.find((c) => c.id === t.chainId)?.shortName}
              </option>
            ))}
          </select>
          <ChevronDown className="w-4 h-4 absolute right-3 top-[30px] text-muted-foreground pointer-events-none" />
        </div>

        {selToken && (
          <p className="text-[11px] text-muted-foreground -mt-2">
            Disponible: <b>{formatCryptoAmount(selToken.balance)} {selToken.symbol}</b>
          </p>
        )}

        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Dirección del destinatario (0x...)</label>
          <input
            type="text"
            placeholder="0x..."
            value={recipient}
            onChange={(e) => setRecipient(e.target.value)}
            className="w-full h-11 px-3 rounded-xl bg-card border border-border/60 text-xs font-mono outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>

        <div>
          <label className="text-xs text-muted-foreground mb-1 block">Cantidad</label>
          <div className="flex gap-2">
            <input
              type="text"
              inputMode="decimal"
              placeholder="0.0"
              value={amountText}
              onChange={(e) => setAmountText(e.target.value)}
              className="flex-1 h-11 px-3 rounded-xl bg-card border border-border/60 text-sm font-semibold outline-none focus:ring-2 focus:ring-primary/30"
            />
            <button
              onClick={() => selToken && setAmountText(formatCryptoAmount(selToken.balance, 8))}
              className="h-11 px-4 rounded-xl bg-primary/10 text-primary text-xs font-bold hover:bg-primary/20 active:scale-95 transition-all"
            >
              MAX
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
            <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        <button
          onClick={continueSend}
          disabled={busy || !recipient.trim() || !amountText || !selToken}
          className="w-full h-12 rounded-xl bg-primary text-white font-semibold shadow-lg shadow-primary/25 active:scale-[0.98] transition-transform disabled:opacity-40 disabled:shadow-none"
        >
          {busy ? "Estimando comisión..." : "Continuar"}
        </button>
      </div>
    );
  }

  // ---------- Recibir ----------
  if (view === "receive") {
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => setView("portfolio")} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <ArrowDownLeft className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-base font-bold">Depositar cripto</h3>
            <p className="text-[11px] text-muted-foreground">Tu dirección sirve en las 7 cadenas EVM</p>
          </div>
        </div>

        {/* Selector de red */}
        <div className="relative">
          <label className="text-xs text-muted-foreground mb-1 block">Red de depósito</label>
          <button
            onClick={() => setRecvChainOpen((o) => !o)}
            className="w-full h-11 px-3 rounded-xl bg-card border border-border/60 text-sm font-semibold flex items-center justify-between outline-none"
          >
            <span>{recvChain.name}</span>
            <span className="flex items-center gap-1 text-[11px] text-primary">
              {recvChain.nativeSymbol} <ChevronDown className="w-4 h-4" />
            </span>
          </button>
          {recvChainOpen && (
            <div className="absolute z-20 w-full mt-1 bg-card border border-border/60 rounded-xl shadow-xl overflow-hidden">
              {CRYPTO_CHAINS.map((c) => (
                <button
                  key={c.id}
                  onClick={() => { setRecvChain(c); setRecvChainOpen(false); }}
                  className="w-full px-3 py-2.5 text-left text-sm hover:bg-muted/60 transition-colors flex justify-between items-center"
                >
                  <span className="font-semibold">{c.name}</span>
                  <span className="text-[11px] text-muted-foreground">{c.nativeSymbol}</span>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* QR + dirección */}
        <div className="flex flex-col items-center gap-3 py-2">
          <div className="bg-white rounded-2xl p-3 shadow-sm">
            {qrData ? (
              <img src={qrData} alt="Código QR de la dirección" className="w-52 h-52 rounded-lg" />
            ) : (
              <div className="w-52 h-52 flex items-center justify-center text-muted-foreground">
                <QrCodeIcon className="w-8 h-8" />
              </div>
            )}
          </div>
          <button
            onClick={() => copy(address)}
            className="w-full flex items-center justify-between gap-2 bg-card border border-border/60 rounded-xl px-4 py-3 active:scale-[0.99] transition-transform"
          >
            <span className="font-mono text-xs font-semibold break-all">{shortAddress(address, 16)}</span>
            {copied ? <CheckCircle2 className="w-4 h-4 text-accent shrink-0" /> : <Copy className="w-4 h-4 text-muted-foreground shrink-0" />}
          </button>
          <p className="text-[11px] text-muted-foreground">Tocá la dirección para copiarla</p>

          <div className="bg-accent/10 text-accent text-[11px] rounded-xl p-3 flex gap-2 leading-relaxed">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <span>
              Enviá únicamente activos compatibles a través de la red{" "}
              <b>{recvChain.name}</b>. Usar una red distinta puede perder fondos.
            </span>
          </div>

          <a
            href={`https://buy.moonpay.com?walletAddress=${address}`}
            target="_blank"
            rel="noreferrer"
            className="w-full h-11 rounded-xl bg-card border border-border/60 text-sm font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform hover:bg-muted/40"
          >
            <Coins className="w-4 h-4" /> Comprar con tarjeta (MoonPay)
          </a>
        </div>
      </div>
    );
  }

  // ---------- Actividad ----------
  if (view === "activity") {
    return (
      <div className="flex flex-col gap-3 pb-4">
        <div className="flex items-center justify-between">
          <button onClick={() => setView("portfolio")} className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-4 h-4" /> Volver
          </button>
          <div className="flex items-center gap-2">
            <h3 className="text-base font-bold">Actividad</h3>
          </div>
          <button
            onClick={() => setRefreshKey((k) => k + 1)}
            className="w-8 h-8 rounded-full bg-card border border-border/60 flex items-center justify-center text-muted-foreground"
            aria-label="Recargar actividad"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {activityLoading && !activity ? (
          <div className="flex justify-center py-12">
            <RefreshCw className="w-6 h-6 animate-spin text-primary" />
          </div>
        ) : activity && activity.length > 0 ? (
          <div className="flex flex-col gap-2">
            {activity.map((tx, i) => (
              <button
                key={`${tx.chainId}:${tx.hash}:${i}`}
                onClick={() => setSelTx(tx)}
                className="bg-card rounded-2xl px-4 py-3 border border-border/50 shadow-sm flex items-center gap-3 text-left active:bg-muted/50 transition-colors"
              >
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${
                    tx.type === "receive" ? "bg-accent/10 text-accent" : "bg-primary/10 text-primary"
                  }`}
                >
                  {tx.type === "receive" ? <ArrowDownLeft className="w-4 h-4" /> : <ArrowUpRight className="w-4 h-4" />}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium">
                    {tx.type === "receive" ? "Recibido" : "Enviado"} · {tx.tokenSymbol}
                    {tx.status === "failed" && <span className="text-destructive text-[10px] ml-1">(falló)</span>}
                  </p>
                  <p className="text-[11px] text-muted-foreground">
                    {tx.chainName} · {formatTxTime(tx.timestamp)}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className={`text-sm font-bold ${tx.type === "receive" ? "text-accent" : ""}`}>
                    {tx.type === "receive" ? "+" : "-"}{formatCryptoAmount(Number(tx.amount), 6)} {tx.tokenSymbol}
                  </p>
                </div>
              </button>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mx-auto mb-3">
              <History className="w-6 h-6 text-muted-foreground" />
            </div>
            <p className="text-xs text-muted-foreground">
              {activityLoading ? "Cargando..." : "Todavía no hay movimientos on-chain. Enviá o recibí fondos para verlos aquí."}
            </p>
          </div>
        )}

        {/* Detalle */}
        {selTx && (
          <div className="fixed inset-0 z-40 bg-black/50 flex items-end justify-center" onClick={() => setSelTx(null)}>
            <div
              className="bg-background w-full max-w-lg rounded-t-3xl p-5 max-h-[80dvh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between mb-4">
                <h4 className="text-base font-bold">Detalle de transacción</h4>
                <button onClick={() => setSelTx(null)} className="w-8 h-8 rounded-full bg-muted flex items-center justify-center text-muted-foreground">
                  ✕
                </button>
              </div>
              <div className="space-y-3 text-sm">
                <DetailRow label="Estado" value={
                  <span className={selTx.status === "failed" ? "text-destructive font-semibold" : "text-accent font-semibold"}>
                    {selTx.status === "failed" ? "Fallida" : "Confirmada"}
                  </span>
                } />
                <DetailRow label="Red" value={selTx.chainName} />
                <DetailRow label="Tipo" value={selTx.type === "receive" ? "Recibido" : "Enviado"} />
                <DetailRow label="Cantidad" value={`${selTx.type === "receive" ? "+" : "-"}${formatCryptoAmount(Number(selTx.amount), 8)} ${selTx.tokenSymbol}`} />
                <DetailRow label="Fecha" value={formatTxTime(selTx.timestamp, true)} />
                {selTx.feeWei && chainBy(selTx.chainId) && (
                  <DetailRow label="Comisión" value={`${formatUnits(BigInt(selTx.feeWei), chainBy(selTx.chainId)!.nativeDecimals)} ${chainBy(selTx.chainId)!.nativeSymbol}`} />
                )}
                <div>
                  <p className="text-[11px] text-muted-foreground uppercase tracking-wide mb-1">Desde</p>
                  <p className="font-mono text-[11px] break-all bg-muted/50 rounded-lg p-2">{selTx.from}</p>
                </div>
                <div>
                  <p className="text-[11px] text-muted-foreground uppercase tracking-wide mb-1">Hacia</p>
                  <p className="font-mono text-[11px] break-all bg-muted/50 rounded-lg p-2">{selTx.to}</p>
                </div>
                <div>
                  <p className="text-[11px] text-muted-foreground uppercase tracking-wide mb-1">Hash</p>
                  <div className="flex items-center gap-2 bg-muted/50 rounded-lg p-2">
                    <p className="font-mono text-[11px] break-all flex-1">{selTx.hash}</p>
                    <button onClick={() => copy(selTx.hash)} className="text-muted-foreground hover:text-foreground shrink-0">
                      {copied ? <CheckCircle2 className="w-4 h-4 text-accent" /> : <Copy className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
              </div>
              <a
                href={`${selTx.explorerUrl}/tx/${selTx.hash}`}
                target="_blank"
                rel="noreferrer"
                className="mt-4 w-full h-11 rounded-xl bg-primary text-white text-sm font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
              >
                <ExternalLink className="w-4 h-4" /> Ver en el explorador
              </a>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ---------- Ajustes de la wallet ----------
  if (view === "settings") {
    return (
      <div className="flex flex-col gap-4 pb-4">
        <button onClick={() => setView("portfolio")} className="self-start flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-4 h-4" /> Volver
        </button>

        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <SettingsIcon className="w-4 h-4" />
          </div>
          <h3 className="text-base font-bold">Ajustes de la billetera</h3>
        </div>

        {info && (
          <div className="bg-accent/10 text-accent text-sm p-3 rounded-xl text-center">{info}</div>
        )}
        {error && (
          <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
            <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        {/* Cambiar PIN */}
        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm space-y-3">
          <p className="text-sm font-semibold flex items-center gap-2">
            <Lock className="w-4 h-4 text-primary" /> Cambiar PIN
          </p>
          <input
            type="password"
            inputMode="numeric"
            placeholder="PIN actual"
            value={oldPin}
            onChange={(e) => setOldPin(e.target.value.replace(/\D/g, "").slice(0, 8))}
            className="w-full h-11 px-3 rounded-xl bg-muted border border-border/50 text-center text-lg tracking-[0.3em] font-bold outline-none focus:ring-2 focus:ring-primary/30"
          />
          <input
            type="password"
            inputMode="numeric"
            placeholder="Nuevo PIN (4-8 dígitos)"
            value={newPin}
            onChange={(e) => setNewPin(e.target.value.replace(/\D/g, "").slice(0, 8))}
            className="w-full h-11 px-3 rounded-xl bg-muted border border-border/50 text-center text-lg tracking-[0.3em] font-bold outline-none focus:ring-2 focus:ring-primary/30"
          />
          <input
            type="password"
            inputMode="numeric"
            placeholder="Repetí el nuevo PIN"
            value={newPin2}
            onChange={(e) => setNewPin2(e.target.value.replace(/\D/g, "").slice(0, 8))}
            className="w-full h-11 px-3 rounded-xl bg-muted border border-border/50 text-center text-lg tracking-[0.3em] font-bold outline-none focus:ring-2 focus:ring-primary/30"
          />
          <button
            onClick={confirmChangePin}
            disabled={busy || !oldPin || !newPin || newPin !== newPin2}
            className="w-full h-11 rounded-xl bg-primary text-white text-sm font-semibold active:scale-[0.98] transition-transform disabled:opacity-40"
          >
            {busy ? "Guardando..." : "Actualizar PIN"}
          </button>
        </div>

        {/* Seguridad */}
        <div className="bg-card rounded-2xl p-4 border border-border/50 shadow-sm space-y-2">
          <button
            onClick={() => { setPin(""); setError(null); setInfo(null); setStage("reveal"); }}
            className="w-full h-11 rounded-xl bg-muted/60 text-sm font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <Eye className="w-4 h-4" /> Ver frase de recuperación
          </button>
          <button
            onClick={lockWallet}
            className="w-full h-11 rounded-xl bg-muted/60 text-sm font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <LogOut className="w-4 h-4" /> Bloquear billetera
          </button>
          <button
            onClick={() => setConfirmDelete((c) => !c)}
            className="w-full h-11 rounded-xl bg-destructive/10 text-destructive text-sm font-semibold flex items-center justify-center gap-2 active:scale-[0.98] transition-transform"
          >
            <Trash2 className="w-4 h-4" /> Borrar billetera
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
      </div>
    );
  }

  // ---------- Cartera (portfolio) ----------
  const total = portfolio?.totalFiat ?? 0;

  return (
    <div className="flex flex-col gap-4 pb-4">
      {/* Total card */}
      <div className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground rounded-2xl p-5 shadow-lg shadow-primary/20">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium opacity-80 uppercase tracking-wider">Patrimonio Crypto</p>
          <button
            onClick={() => setView("settings")}
            className="opacity-80 hover:opacity-100 transition-opacity w-8 h-8 rounded-full bg-white/15 flex items-center justify-center"
            aria-label="Ajustes de la billetera"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
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
      </div>

      {/* Acciones rápidas */}
      <div className="grid grid-cols-3 gap-2">
        <button
          onClick={openSend}
          className="h-14 rounded-2xl bg-card border border-border/50 shadow-sm flex flex-col items-center justify-center gap-1 active:scale-95 transition-transform hover:border-primary/30"
        >
          <SendIcon className="w-5 h-5 text-primary" />
          <span className="text-[11px] font-semibold">Enviar</span>
        </button>
        <button
          onClick={() => { setError(null); setView("receive"); }}
          className="h-14 rounded-2xl bg-card border border-border/50 shadow-sm flex flex-col items-center justify-center gap-1 active:scale-95 transition-transform hover:border-primary/30"
        >
          <ArrowDownLeft className="w-5 h-5 text-accent" />
          <span className="text-[11px] font-semibold">Recibir</span>
        </button>
        <button
          onClick={() => { setError(null); setView("activity"); }}
          className="h-14 rounded-2xl bg-card border border-border/50 shadow-sm flex flex-col items-center justify-center gap-1 active:scale-95 transition-transform hover:border-primary/30"
        >
          <History className="w-5 h-5 text-muted-foreground" />
          <span className="text-[11px] font-semibold">Actividad</span>
        </button>
      </div>

      {error && (
        <div className="bg-destructive/10 text-destructive text-sm p-3 rounded-xl text-center flex items-center justify-center gap-2">
          <TriangleAlert className="w-4 h-4 shrink-0" /> {error}
        </div>
      )}

      {/* Cadenas */}
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
            Sin saldos en estas cadenas. Usá "Recibir" para obtener tu dirección
            y enviarte fondos.
          </p>
        </div>
      )}

      {/* Refrescar */}
      <button
        onClick={() => loadPortfolio(address)}
        disabled={loading}
        className="self-end w-12 h-12 rounded-full bg-primary text-white shadow-lg shadow-primary/30 flex items-center justify-center active:scale-90 transition-transform disabled:opacity-50"
        aria-label="Actualizar saldos"
      >
        <RefreshCw className={`w-5 h-5 ${loading ? "animate-spin" : ""}`} />
      </button>
    </div>
  );
}

// ---------- Helpers ----------

function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex justify-between items-center gap-3">
      <span className="text-muted-foreground text-xs">{label}</span>
      <span className="text-right">{value}</span>
    </div>
  );
}

function chainBy(chainId: number): CryptoChain | undefined {
  return CRYPTO_CHAINS.find((c) => c.id === chainId);
}

function ethersParseUnits(amount: string, decimals: number): bigint {
  // convierte sin pérdida de precisión (evita Number())
  const parts = amount.trim().split(".");
  const int = parts[0].replace(/[^0-9]/g, "");
  const frac = (parts[1] ?? "").replace(/[^0-9]/g, "").slice(0, decimals).padEnd(decimals, "0");
  if (!int && !frac.replace(/0/g, "")) throw new Error("vacío");
  return BigInt(int + frac);
}

function formatTxTime(ts: number, full = false): string {
  if (!ts) return "—";
  const d = new Date(ts);
  return d.toLocaleString("es-ES", full
    ? { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" }
    : { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}
