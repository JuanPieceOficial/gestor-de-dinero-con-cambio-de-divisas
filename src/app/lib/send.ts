"use client";

// Envío de transacciones desde la web: estima gas y firma con ethers v6.
// La clave privada solo vive en memoria durante la sesión desbloqueada.
import { ethers } from "ethers";
import type { CryptoChain, CryptoToken } from "./crypto";

const ERC20_ABI = [
  "function transfer(address to, uint256 amount) returns (bool)",
  "function balanceOf(address owner) view returns (uint256)",
];

export async function makeProvider(chain: CryptoChain): Promise<ethers.JsonRpcProvider> {
  let lastErr: unknown = null;
  for (const url of chain.rpcUrls) {
    try {
      const provider = new ethers.JsonRpcProvider(url, chain.id);
      await provider.getBlockNumber();
      return provider;
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr ?? new Error("Sin RPC disponible");
}

export type FeeEstimate = {
  feeWei: bigint;
  gasLimit: bigint;
  gasPrice: bigint;
  error?: string;
};

async function estimateGas(
  chain: CryptoChain,
  fn: (provider: ethers.JsonRpcProvider) => Promise<bigint>
): Promise<FeeEstimate> {
  try {
    const provider = await makeProvider(chain);
    const gasLimit = await fn(provider);
    const feeData = await provider.getFeeData();
    const gasPrice = feeData.gasPrice ?? BigInt(0);
    return { feeWei: gasLimit * gasPrice, gasLimit, gasPrice };
  } catch (e) {
    return {
      feeWei: BigInt(0),
      gasLimit: BigInt(0),
      gasPrice: BigInt(0),
      error: e instanceof Error ? e.message : "No se pudo estimar la comisión",
    };
  }
}

export async function estimateNativeSend(
  chain: CryptoChain,
  from: string,
  to: string,
  amount: string
): Promise<FeeEstimate> {
  return estimateGas(chain, async (provider) => {
    const value = ethers.parseUnits(amount, chain.nativeDecimals);
    return provider.estimateGas({ from, to, value });
  });
}

export async function estimateTokenSend(
  chain: CryptoChain,
  token: CryptoToken,
  from: string,
  to: string,
  amount: string
): Promise<FeeEstimate> {
  return estimateGas(chain, async (provider) => {
    const contract = new ethers.Contract(token.address, ERC20_ABI, provider);
    const value = ethers.parseUnits(amount, token.decimals);
    return contract.transfer.estimateGas(to, value, { from });
  });
}

export async function sendNative(
  chain: CryptoChain,
  privateKey: string,
  to: string,
  amount: string
): Promise<string> {
  const provider = await makeProvider(chain);
  const wallet = new ethers.Wallet(privateKey, provider);
  const value = ethers.parseUnits(amount, chain.nativeDecimals);
  const tx = await wallet.sendTransaction({ to, value });
  return tx.hash;
}

export async function sendToken(
  chain: CryptoChain,
  token: CryptoToken,
  privateKey: string,
  to: string,
  amount: string
): Promise<string> {
  const provider = await makeProvider(chain);
  const wallet = new ethers.Wallet(privateKey, provider);
  const contract = new ethers.Contract(token.address, ERC20_ABI, wallet);
  const value = ethers.parseUnits(amount, token.decimals);
  const tx = await contract.transfer(to, value);
  return tx.hash;
}

export function isValidRecipient(addr: string): boolean {
  return ethers.isAddress(addr.trim());
}
