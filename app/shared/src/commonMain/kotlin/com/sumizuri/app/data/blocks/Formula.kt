package com.sumizuri.app.data.blocks

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_err_empty
import com.sumizuri.app.shared.resources.script_err_expected
import com.sumizuri.app.shared.resources.script_err_no_quote
import com.sumizuri.app.shared.resources.script_err_not_number
import com.sumizuri.app.shared.resources.script_err_symbol
import com.sumizuri.app.shared.resources.script_err_too_soon
import com.sumizuri.app.shared.resources.script_err_unexpected

// The small language of the formulas in a block script. It has numbers, text in quotes, yes and no, names such as stats.month, library and the names the script gave its values, the sums + - * / % and ^, comparisons, and, or, not, a ? b : c, and functions such as min, max, round, clamp, if, sum, avg, count and len. Text added to anything joins as text. A list, such as a series or titles, can go into sum, avg, min, max and count.
internal sealed interface Expr {
    class Num(val value: Double) : Expr
    class Str(val value: String) : Expr
    class Name(val name: String, val at: Int) : Expr
    class Unary(val op: String, val operand: Expr) : Expr
    class Binary(val op: String, val left: Expr, val right: Expr) : Expr
    class Choose(val test: Expr, val yes: Expr, val no: Expr) : Expr
    class Call(val name: String, val args: List<Expr>, val at: Int) : Expr
}

private enum class TokenKind { Number, Text, Name, Symbol, End }

private class Token(val kind: TokenKind, val text: String, val at: Int)

private val Symbols =
    listOf("==", "!=", "<=", ">=", "&&", "||", "+", "-", "*", "/", "%", "^", "(", ")", ",", "<", ">", "!", "?", ":")

private fun tokensOf(source: String): List<Token> {
    val out = ArrayList<Token>()
    var i = 0
    while (i < source.length) {
        val char = source[i]
        when {
            char.isWhitespace() -> i++

            char.isDigit() || (char == '.' && source.getOrNull(i + 1)?.isDigit() == true) -> {
                val start = i
                while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                out += Token(TokenKind.Number, source.substring(start, i), start)
            }

            char == '"' || char == '\'' -> {
                val start = i++
                val text = StringBuilder()
                while (i < source.length && source[i] != char) text.append(source[i++])
                if (i >= source.length) throw ScriptError(Res.string.script_err_no_quote, start + 1)
                i++
                out += Token(TokenKind.Text, text.toString(), start)
            }

            char.isLetter() || char == '_' -> {
                // A name may have dots in it, as stats.month has.
                val start = i
                while (i < source.length &&
                    (
                        source[i].isLetterOrDigit() || source[i] == '_' ||
                            (source[i] == '.' && source.getOrNull(i + 1)?.isLetter() == true)
                        )
                ) {
                    i++
                }
                out += Token(TokenKind.Name, source.substring(start, i), start)
            }

            else -> {
                val symbol =
                    Symbols.firstOrNull { source.startsWith(it, i) }
                        ?: throw ScriptError(Res.string.script_err_symbol, char, i + 1)
                out += Token(TokenKind.Symbol, symbol, i)
                i += symbol.length
            }
        }
    }
    out += Token(TokenKind.End, "", source.length)
    return out
}

// Reads a formula into its parts, from the loosest joining to the tightest: a ? b : c, or, and, not, comparisons, + and -, * / and %, a minus sign, ^, and last the single values.
private class FormulaParser(source: String) {
    private val tokens = tokensOf(source)
    private var place = 0
    private val next: Token get() = tokens[place]

    fun parse(): Expr {
        if (next.kind == TokenKind.End) throw ScriptError(Res.string.script_err_empty)
        val expr = choose()
        if (next.kind != TokenKind.End) throw ScriptError(Res.string.script_err_unexpected, next.text, next.at + 1)
        return expr
    }

    private fun takes(vararg texts: String): String? {
        val token = next
        val word = token.kind == TokenKind.Symbol || token.kind == TokenKind.Name
        return if (word && token.text in texts) token.text.also { place++ } else null
    }

    private fun expect(text: String) {
        if (takes(text) == null) throw ScriptError(Res.string.script_err_expected, text, next.at + 1)
    }

    private fun choose(): Expr {
        val test = or()
        if (takes("?") == null) return test
        val yes = choose()
        expect(":")
        return Expr.Choose(test, yes, choose())
    }

    private fun or(): Expr {
        var left = and()
        while (takes("||", "or") != null) left = Expr.Binary("||", left, and())
        return left
    }

    private fun and(): Expr {
        var left = not()
        while (takes("&&", "and") != null) left = Expr.Binary("&&", left, not())
        return left
    }

    private fun not(): Expr = if (takes("!", "not") != null) Expr.Unary("!", not()) else compare()

    private fun compare(): Expr {
        val left = sum()
        val op = takes("==", "!=", "<=", ">=", "<", ">") ?: return left
        return Expr.Binary(op, left, sum())
    }

    private fun sum(): Expr {
        var left = product()
        while (true) left = Expr.Binary(takes("+", "-") ?: return left, left, product())
    }

    private fun product(): Expr {
        var left = unary()
        while (true) left = Expr.Binary(takes("*", "/", "%") ?: return left, left, unary())
    }

    private fun unary(): Expr = if (takes("-") != null) Expr.Unary("-", unary()) else power()

    private fun power(): Expr {
        val base = single()
        return if (takes("^") != null) Expr.Binary("^", base, unary()) else base
    }

    private fun single(): Expr {
        val token = next
        place++
        return when (token.kind) {
            TokenKind.Number -> Expr.Num(
                token.text.toDoubleOrNull() ?: throw ScriptError(Res.string.script_err_not_number, token.text),
            )

            TokenKind.Text -> Expr.Str(token.text)

            TokenKind.Name -> if (takes("(") !=
                null
            ) {
                Expr.Call(token.text.lowercase(), arguments(), token.at)
            } else {
                Expr.Name(token.text, token.at)
            }

            TokenKind.Symbol -> if (token.text ==
                "("
            ) {
                choose().also { expect(")") }
            } else {
                throw ScriptError(
                    Res.string.script_err_unexpected,
                    token.text,
                    token.at + 1,
                )
            }

            TokenKind.End -> throw ScriptError(Res.string.script_err_too_soon)
        }
    }

    private fun arguments(): List<Expr> {
        if (takes(")") != null) return emptyList()
        val args = mutableListOf(choose())
        while (takes(",") != null) args += choose()
        expect(")")
        return args
    }
}

// Reading a formula is quick next to the data it works on, so it is read again each time it runs.
internal fun parseFormula(source: String): Expr = FormulaParser(source).parse()

// Works out a formula. Names are asked for when the formula reads them: the values the script gave names to and the data of the profile. Null means the name is not known.
fun evaluateFormula(source: String, context: BlockContext?, names: (String) -> Value?): Value =
    FormulaRun(context, names).eval(parseFormula(source))
