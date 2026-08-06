package com.cryptowallet.app.data.api

import com.cryptowallet.app.data.model.Chain

object Chains {

    val all: List<Chain> = listOf(
        Chain(
            id = 1L,
            name = "Ethereum",
            shortName = "ETH",
            nativeSymbol = "ETH",
            nativeName = "Ethereum",
            rpcUrls = listOf(
                "https://ethereum-rpc.publicnode.com",
                "https://cloudflare-eth.com",
                "https://eth.llamarpc.com"
            ),
            explorerUrl = "https://etherscan.io",
            coinGeckoId = "ethereum",
            coinGeckoChain = "ethereum"
        ),
        Chain(
            id = 56L,
            name = "BNB Smart Chain",
            shortName = "BNB",
            nativeSymbol = "BNB",
            nativeName = "BNB",
            rpcUrls = listOf(
                "https://bsc-dataseed.binance.org",
                "https://bsc-dataseed1.binance.org",
                "https://bsc-dataseed2.binance.org"
            ),
            explorerUrl = "https://bscscan.com",
            coinGeckoId = "binancecoin",
            coinGeckoChain = "binance-smart-chain"
        ),
        Chain(
            id = 137L,
            name = "Polygon",
            shortName = "POL",
            nativeSymbol = "POL",
            nativeName = "Polygon",
            rpcUrls = listOf(
                "https://polygon-rpc.com",
                "https://polygon-bor-rpc.publicnode.com"
            ),
            explorerUrl = "https://polygonscan.com",
            coinGeckoId = "polygon-ecosystem-token",
            coinGeckoChain = "polygon-pos"
        ),
        Chain(
            id = 42161L,
            name = "Arbitrum One",
            shortName = "ARB",
            nativeSymbol = "ETH",
            nativeName = "Ethereum (Arbitrum)",
            rpcUrls = listOf(
                "https://arb1.arbitrum.io/rpc",
                "https://arbitrum-one-rpc.publicnode.com"
            ),
            explorerUrl = "https://arbiscan.io",
            coinGeckoId = "ethereum",
            coinGeckoChain = "arbitrum-one"
        ),
        Chain(
            id = 10L,
            name = "Optimism",
            shortName = "OP",
            nativeSymbol = "ETH",
            nativeName = "Ethereum (Optimism)",
            rpcUrls = listOf(
                "https://mainnet.optimism.io",
                "https://optimism-rpc.publicnode.com"
            ),
            explorerUrl = "https://optimistic.etherscan.io",
            coinGeckoId = "ethereum",
            coinGeckoChain = "optimism"
        ),
        Chain(
            id = 8453L,
            name = "Base",
            shortName = "BASE",
            nativeSymbol = "ETH",
            nativeName = "Ethereum (Base)",
            rpcUrls = listOf(
                "https://mainnet.base.org",
                "https://base-rpc.publicnode.com"
            ),
            explorerUrl = "https://basescan.org",
            coinGeckoId = "ethereum",
            coinGeckoChain = "base"
        ),
        Chain(
            id = 43114L,
            name = "Avalanche C-Chain",
            shortName = "AVAX",
            nativeSymbol = "AVAX",
            nativeName = "Avalanche",
            rpcUrls = listOf(
                "https://api.avax.network/ext/bc/C/rpc",
                "https://avalanche-c-chain-rpc.publicnode.com"
            ),
            explorerUrl = "https://snowtrace.io",
            coinGeckoId = "avalanche-2",
            coinGeckoChain = "avalanche"
        )
    )

    val testnets: List<Chain> = listOf(
        Chain(
            id = 11155111L,
            name = "Ethereum Sepolia",
            shortName = "SEPOLIA",
            nativeSymbol = "ETH",
            nativeName = "Ethereum (Sepolia)",
            rpcUrls = listOf(
                "https://rpc.sepolia.org",
                "https://ethereum-sepolia-rpc.publicnode.com"
            ),
            explorerUrl = "https://sepolia.etherscan.io",
            coinGeckoId = "",
            coinGeckoChain = ""
        ),
        Chain(
            id = 97L,
            name = "BNB Smart Chain Testnet",
            shortName = "BSC-T",
            nativeSymbol = "tBNB",
            nativeName = "BNB (Testnet)",
            rpcUrls = listOf(
                "https://data-seed-prebsc-1-s1.binance.org:8545",
                "https://data-seed-prebsc-2-s1.binance.org:8545",
                "https://bsc-testnet-rpc.publicnode.com"
            ),
            explorerUrl = "https://testnet.bscscan.com",
            coinGeckoId = "",
            coinGeckoChain = ""
        ),
        Chain(
            id = 80002L,
            name = "Polygon Amoy",
            shortName = "AMOY",
            nativeSymbol = "POL",
            nativeName = "Polygon (Amoy)",
            rpcUrls = listOf(
                "https://rpc-amoy.polygon.technology"
            ),
            explorerUrl = "https://amoy.polygonscan.com",
            coinGeckoId = "",
            coinGeckoChain = ""
        )
    )

    fun byId(id: Long): Chain? = all.firstOrNull { it.id == id } ?: testnets.firstOrNull { it.id == id }

    val defaultTokenList: Map<Long, List<Triple<String, String, Int>>> = mapOf(
        1L to listOf(
            Triple("ETH", "Ethereum", 18),
            Triple("USDT", "Tether USD", 6),
            Triple("USDC", "USD Coin", 6),
            Triple("DAI", "Dai Stablecoin", 18),
            Triple("WBTC", "Wrapped Bitcoin", 8)
        ),
        56L to listOf(
            Triple("BNB", "BNB", 18),
            Triple("USDT", "Tether USD", 18),
            Triple("USDC", "USD Coin", 18),
            Triple("BUSD", "Binance USD", 18),
            Triple("WBNB", "Wrapped BNB", 18)
        ),
        137L to listOf(
            Triple("POL", "Polygon", 18),
            Triple("USDT", "Tether USD", 6),
            Triple("USDC", "USD Coin", 6),
            Triple("WETH", "Wrapped Ether", 18)
        ),
        42161L to listOf(
            Triple("ETH", "Ethereum", 18),
            Triple("USDT", "Tether USD", 6),
            Triple("USDC", "USD Coin", 6),
            Triple("ARB", "Arbitrum", 18)
        ),
        10L to listOf(
            Triple("ETH", "Ethereum", 18),
            Triple("USDT", "Tether USD", 6),
            Triple("USDC", "USD Coin", 6),
            Triple("OP", "Optimism", 18)
        ),
        8453L to listOf(
            Triple("ETH", "Ethereum", 18),
            Triple("USDC", "USD Coin", 6)
        ),
        43114L to listOf(
            Triple("AVAX", "Avalanche", 18),
            Triple("USDT", "Tether USD", 6),
            Triple("USDC", "USD Coin", 6),
            Triple("WAVAX", "Wrapped AVAX", 18)
        )
    )

    val tokenAddresses: Map<Long, Map<String, String>> = mapOf(
        1L to mapOf(
            "USDT" to "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            "USDC" to "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
            "DAI" to "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            "WBTC" to "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599"
        ),
        56L to mapOf(
            "USDT" to "0x55d398326f99059fF775485246999027B3197955",
            "USDC" to "0x8AC76a51cc950d9822D68b83fE1Ad97B32Cd580d",
            "BUSD" to "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56",
            "WBNB" to "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c"
        ),
        137L to mapOf(
            "USDT" to "0xc2132D05D31c914a87C6611C10748AEb04B58e8F",
            "USDC" to "0x2791Bca1f2de4661ED88A30C99A7a9449Aa84174",
            "WETH" to "0x7ceB23fD6bC0adD59E62ac25578270cFf1b9f619"
        ),
        42161L to mapOf(
            "USDT" to "0xFd086bC7CD5C481DCC9C85ebE478A1C0b69FCbb9",
            "USDC" to "0xaf88d065e77c8cC2239327C5EDb3A432268e5831",
            "ARB" to "0x912CE59144191C1204E64559FE8253a0e49E6548"
        ),
        10L to mapOf(
            "USDT" to "0x94b008aA00579c1307B0EF2c499aD98a8ce58e58",
            "USDC" to "0x0b2C639c533813f4Aa9D7837CAf62653d097Ff85",
            "OP" to "0x4200000000000000000000000000000000000042"
        ),
        8453L to mapOf(
            "USDC" to "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"
        ),
        43114L to mapOf(
            "USDT" to "0x9702230A8E5d1Aa1C752Cd4cB01E34EbE6C4e07F",
            "USDC" to "0xB97EF9Ef8734C71904D8002F8b6Bc66Dd9c48a6E",
            "WAVAX" to "0xB31f66AA3C1e785363F0875A1B74E27b85FD66c7"
        )
    )
}
