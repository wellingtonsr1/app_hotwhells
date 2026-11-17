package br.com.hotwheelscollector.domain.model

/**
 * Valor monetário em centavos com moeda junto do valor na camada de apresentação.
 *
 * Centavos em `Long`: nunca `Double`, nunca `REAL` no banco (Art. I.11, D12).
 * O limite de 9.999.999,99 vem de FR-076.
 */
@JvmInline
public value class MoneyCents(public val cents: Long) {

    init {
        require(cents >= 0L) { "Valor monetário não pode ser negativo." }
        require(cents <= MAX_CENTS) { "Valor acima do limite permitido (9.999.999,99)." }
    }

    public operator fun plus(other: MoneyCents): MoneyCents = MoneyCents(cents + other.cents)

    public operator fun minus(other: MoneyCents): MoneyCents = MoneyCents(cents - other.cents)

    public operator fun compareTo(other: MoneyCents): Int = cents.compareTo(other.cents)

    /** Formata como `1.234,56`, o formato que o colecionador espera ver. */
    public fun formatPtBr(): String {
        val units = cents / 100
        val fraction = (cents % 100).toString().padStart(2, '0')
        return "${units.toString().groupThousands()},$fraction"
    }

    public companion object {
        public const val MAX_CENTS: Long = 999_999_999L

        public val ZERO: MoneyCents = MoneyCents(0L)

        /**
         * Interpreta o que o usuário digita no teclado brasileiro, aceitando as duas
         * convenções comuns: `1.234,56` e `1234.56`.
         *
         * Regra explícita, para não adivinhar errado:
         * - com vírgula e ponto, o separador mais à direita é o decimal;
         * - só vírgula: sempre decimal (`12,50` = 1250 centavos);
         * - só ponto com exatamente 3 dígitos após ele e nenhum outro separador:
         *   milhar (`1.234` = 1234 reais), não decimal;
         * - demais casos com ponto: decimal (`12.5`, `12.50`).
         *
         * Retorna `null` quando o texto não é um valor válido — o chamador mostra a
         * mensagem em pt-BR, e nada de sentinela `0` (Art. I.9).
         */
        public fun parsePtBrOrNull(raw: String): MoneyCents? {
            val cleaned = raw.trim()
                .removePrefix("R$")
                .replace("\u00A0", "")
                .replace(" ", "")
            if (cleaned.isEmpty()) return null
            if (!cleaned.matches(Regex("""^[0-9.,]+$"""))) return null

            val lastComma = cleaned.lastIndexOf(',')
            val lastDot = cleaned.lastIndexOf('.')
            val integerPart: String
            val fractionPart: String

            when {
                lastComma >= 0 && lastDot >= 0 -> {
                    if (lastComma > lastDot) {
                        integerPart = cleaned.substring(0, lastComma)
                        fractionPart = cleaned.substring(lastComma + 1)
                    } else {
                        integerPart = cleaned.substring(0, lastDot)
                        fractionPart = cleaned.substring(lastDot + 1)
                    }
                }
                lastComma >= 0 -> {
                    integerPart = cleaned.substring(0, lastComma)
                    fractionPart = cleaned.substring(lastComma + 1)
                }
                lastDot >= 0 -> {
                    val afterDot = cleaned.substring(lastDot + 1)
                    val isThousands = afterDot.length == 3 && cleaned.count { it == '.' } == 1
                    if (isThousands) {
                        integerPart = cleaned
                        fractionPart = ""
                    } else {
                        integerPart = cleaned.substring(0, lastDot)
                        fractionPart = afterDot
                    }
                }
                else -> {
                    integerPart = cleaned
                    fractionPart = ""
                }
            }

            val digits = integerPart.replace(".", "").replace(",", "")
            if (digits.isEmpty() || !digits.all { it.isDigit() }) return null
            if (fractionPart.length > 2 || !fractionPart.all { it.isDigit() }) return null

            val units = digits.toLongOrNull() ?: return null
            val fraction = fractionPart.padEnd(2, '0').ifEmpty { "00" }.toLongOrNull() ?: return null
            val total = units * 100 + fraction
            return if (total in 0L..MAX_CENTS) MoneyCents(total) else null
        }
    }
}

private fun String.groupThousands(): String {
    val reversed = this.reversed()
    val grouped = reversed.chunked(3).joinToString(".")
    return grouped.reversed()
}
