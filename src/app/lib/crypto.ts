// Lógica de cartera crypto para la web: saldos on-chain vía RPC público
// y precios vía CoinGecko (misma fuente de datos que la wallet Android).
// Solo usa direcciones públicas — nunca claves privadas.

export type CryptoToken = {
  symbol: string;
  address: string; // contrato ERC-20 (vacío para el nativo)
  decimals: number;
};

export type CryptoChain = {
  id: number;
  name: string;
  shortName: string;
  nativeSymbol: string;
  nativeDecimals: number;
  rpcUrls: string[];
  explorerUrl: string;
  coinGeckoId: string; // id nativo en CoinGecko
  coinGeckoChain: string; // plataforma para token_price (vacío si no soporta)
  tokens: CryptoToken[]; // tokens ERC-20 principales (sin el nativo)
};

export const CRYPTO_CHAINS: CryptoChain[] = [
  {
    id: 1,
    name: "Ethereum",
    shortName: "ETH",
    nativeSymbol: "ETH",
    nativeDecimals: 18,
    rpcUrls: ["https://ethereum-rpc.publicnode.com", "https://cloudflare-eth.com", "https://eth.llamarpc.com"],
    explorerUrl: "https://etherscan.io",
    coinGeckoId: "ethereum",
    coinGeckoChain: "ethereum",
    tokens: [
      { symbol: "USDT", address: "0xdAC17F958D2ee523a2206206994597C13D831ec7", decimals: 6 },
      { symbol: "USDC", address: "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", decimals: 6 },
      { symbol: "DAI", address: "0x6B175474E89094C44Da98b954EedeAC495271d0F", decimals: 18 },
      { symbol: "WBTC", address: "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", decimals: 8 },
    ],
  },
  {
    id: 56,
    name: "BNB Smart Chain",
    shortName: "BNB",
    nativeSymbol: "BNB",
    nativeDecimals: 18,
    rpcUrls: ["https://bsc-dataseed.binance.org", "https://bsc-dataseed1.binance.org", "https://bsc-dataseed2.binance.org"],
    explorerUrl: "https://bscscan.com",
    coinGeckoId: "binancecoin",
    coinGeckoChain: "binance-smart-chain",
    tokens: [
      { symbol: "USDT", address: "0x55d398326f99059fF775485246999027B3197955", decimals: 18 },
      { symbol: "USDC", address: "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d", decimals: 18 },
      { symbol: "BUSD", address: "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56", decimals: 18 },
    ],
  },
  {
    id: 137,
    name: "Polygon",
    shortName: "POL",
    nativeSymbol: "POL",
    nativeDecimals: 18,
    rpcUrls: ["https://polygon-rpc.com", "https://polygon-bor-rpc.publicnode.com"],
    explorerUrl: "https://polygonscan.com",
    coinGeckoId: "polygon-ecosystem-token",
    coinGeckoChain: "polygon-pos",
    tokens: [
      { symbol: "USDT", address: "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", decimals: 6 },
      { symbol: "USDC", address: "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174", decimals: 6 },
      { symbol: "WETH", address: "0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619", decimals: 18 },
    ],
  },
  {
    id: 42161,
    name: "Arbitrum One",
    shortName: "ARB",
    nativeSymbol: "ETH",
    nativeDecimals: 18,
    rpcUrls: ["https://arb1.arbitrum.io/rpc", "https://arbitrum-one-rpc.publicnode.com"],
    explorerUrl: "https://arbiscan.io",
    coinGeckoId: "ethereum",
    coinGeckoChain: "arbitrum-one",
    tokens: [
      { symbol: "USDT", address: "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9", decimals: 6 },
      { symbol: "USDC", address: "0xaf88d065e77c8cC2239327C5EDb3A432268e5831", decimals: 6 },
      { symbol: "ARB", address: "0x912CE59144191C1204E64559FE8253a0e49E6548", decimals: 18 },
    ],
  },
  {
    id: 10,
    name: "Optimism",
    shortName: "OP",
    nativeSymbol: "ETH",
    nativeDecimals: 18,
    rpcUrls: ["https://mainnet.optimism.io", "https://optimism-rpc.publicnode.com"],
    explorerUrl: "https://optimistic.etherscan.io",
    coinGeckoId: "ethereum",
    coinGeckoChain: "optimism",
    tokens: [
      { symbol: "USDT", address: "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58", decimals: 6 },
      { symbol: "USDC", address: "0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85", decimals: 6 },
      { symbol: "OP", address: "0x4200000000000000000000000000000000000042", decimals: 18 },
    ],
  },
  {
    id: 8453,
    name: "Base",
    shortName: "BASE",
    nativeSymbol: "ETH",
    nativeDecimals: 18,
    rpcUrls: ["https://mainnet.base.org", "https://base-rpc.publicnode.com"],
    explorerUrl: "https://basescan.org",
    coinGeckoId: "ethereum",
    coinGeckoChain: "base",
    tokens: [
      { symbol: "USDC", address: "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", decimals: 6 },
    ],
  },
  {
    id: 43114,
    name: "Avalanche C-Chain",
    shortName: "AVAX",
    nativeSymbol: "AVAX",
    nativeDecimals: 18,
    rpcUrls: ["https://api.avax.network/ext/bc/C/rpc", "https://avalanche-c-chain-rpc.publicnode.com"],
    explorerUrl: "https://snowtrace.io",
    coinGeckoId: "avalanche-2",
    coinGeckoChain: "avalanche",
    tokens: [
      { symbol: "USDT", address: "0x9702230A8E5d1Aa1C752Cd4cB01E34EbE6C4e07F", decimals: 6 },
      { symbol: "USDC", address: "0xB97EF9Ef8734C71904D8002F8b6Bc66Dd9c48a6E", decimals: 6 },
    ],
  },
];

export type BalanceItem = {
  symbol: string;
  balance: number; // unidades del token
  price: number; // en la moneda seleccionada
  fiatValue: number; // balance * price
};

export type ChainBalance = {
  chain: CryptoChain;
  items: BalanceItem[];
  chainFiat: number;
};

export function isValidAddress(addr: string): boolean {
  return /^0x[a-fA-F0-9]{40}$/.test(addr.trim());
}

// --- JSON-RPC ---

async function rpc(url: string, method: string, params: unknown[]): Promise<unknown> {
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method, params }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json = await res.json();
  if (json.error) throw new Error(json.error.message || "RPC error");
  return json.result;
}

async function rpcWithFallback(chain: CryptoChain, method: string, params: unknown[]): Promise<unknown> {
  let lastErr: unknown = null;
  for (const url of chain.rpcUrls) {
    try {
      return await rpc(url, method, params);
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr ?? new Error("Sin RPC disponible");
}

const BALANCE_OF_SELECTOR = "0x70a08231";

async function getNativeBalance(chain: CryptoChain, address: string): Promise<bigint> {
  const hex = (await rpcWithFallback(chain, "eth_getBalance", [address, "latest"])) as string;
  return BigInt(hex);
}

async function getTokenBalance(chain: CryptoChain, token: CryptoToken, address: string): Promise<bigint> {
  const data = BALANCE_OF_SELECTOR + address.toLowerCase().slice(2).padStart(64, "0");
  const hex = (await rpcWithFallback(chain, "eth_call", [{ to: token.address, data }, "latest"])) as string;
  return BigInt(hex);
}

// --- Precios CoinGecko ---

const COINGECKO = "https://api.coingecko.com/api/v3";

export async function fetchPrices(
  currency: string
): Promise<{ natives: Map<string, number>; tokens: Map<string, Map<string, number>> }> {
  const natives = new Map<string, number>();
  const tokens = new Map<string, Map<string, number>>();
  const vs = currency.toLowerCase();

  const nativeIds = [...new Set(CRYPTO_CHAINS.map((c) => c.coinGeckoId).filter(Boolean))].join(",");
  try {
    const res = await fetch(`${COINGECKO}/simple/price?ids=${nativeIds}&vs_currencies=${vs}`);
    if (res.ok) {
      const json = await res.json();
      for (const [id, v] of Object.entries(json)) {
        const obj = v as Record<string, number>;
        natives.set(id, obj[vs] ?? 0);
      }
    }
  } catch {
    // los precios nativos quedan en 0 y el total sigue mostrando saldos
  }

  await Promise.all(
    CRYPTO_CHAINS.filter((c) => c.coinGeckoChain && c.tokens.length > 0).map(async (chain) => {
      const addrs = [...new Set(chain.tokens.map((t) => t.address.toLowerCase()))].join(",");
      try {
        const res = await fetch(
          `${COINGECKO}/simple/token_price/${chain.coinGeckoChain}?contract_addresses=${addrs}&vs_currencies=${vs}`
        );
        if (res.ok) {
          const json = await res.json();
          const map = new Map<string, number>();
          for (const [addr, v] of Object.entries(json)) {
            const obj = v as Record<string, number>;
            map.set(addr.toLowerCase(), obj[vs] ?? 0);
          }
          tokens.set(chain.id.toString(), map);
        }
      } catch {
        // sin precios de tokens en esta cadena
      }
    })
  );

  return { natives, tokens };
}

// --- Cartera ---

export async function fetchPortfolio(
  address: string,
  currency: string
): Promise<{ byChain: ChainBalance[]; totalFiat: number }> {
  const addr = address.trim();
  const { natives, tokens } = await fetchPrices(currency);

  const results = await Promise.all(
    CRYPTO_CHAINS.map(async (chain): Promise<ChainBalance | null> => {
      try {
        const nativeRaw = await getNativeBalance(chain, addr);
        const nativePrice = natives.get(chain.coinGeckoId) ?? 0;
        const items: BalanceItem[] = [];

        const addItem = (symbol: string, raw: bigint, decimals: number, price: number) => {
          const balance = Number(raw) / 10 ** decimals;
          if (balance > 0) {
            items.push({ symbol, balance, price, fiatValue: balance * price });
          }
        };

        addItem(chain.nativeSymbol, nativeRaw, chain.nativeDecimals, nativePrice);

        if (chain.tokens.length > 0) {
          const priceMap = tokens.get(chain.id.toString()) ?? new Map<string, number>();
          const tokenResults = await Promise.all(
            chain.tokens.map(async (t) => {
              try {
                const raw = await getTokenBalance(chain, t, addr);
                return { t, raw };
              } catch {
                return { t, raw: BigInt(0) };
              }
            })
          );
          for (const { t, raw } of tokenResults) {
            addItem(t.symbol, raw, t.decimals, priceMap.get(t.address.toLowerCase()) ?? 0);
          }
        }

        if (items.length === 0) return null;

        const chainFiat = items.reduce((s, i) => s + i.fiatValue, 0);
        return { chain, items, chainFiat };
      } catch {
        return null;
      }
    })
  );

  const byChain = results.filter((r): r is ChainBalance => r !== null);
  const totalFiat = byChain.reduce((s, c) => s + c.chainFiat, 0);
  return { byChain, totalFiat };
}

export function formatCryptoAmount(value: number, maxDecimals = 6): string {
  if (value === 0) return "0";
  if (value >= 1000) return value.toLocaleString("es-ES", { maximumFractionDigits: 2 });
  return value.toLocaleString("es-ES", { maximumFractionDigits: maxDecimals });
}

export function shortAddress(addr: string, tail = 6): string {
  if (addr.length < 12) return addr;
  return `${addr.slice(0, 8)}…${addr.slice(-tail)}`;
}
