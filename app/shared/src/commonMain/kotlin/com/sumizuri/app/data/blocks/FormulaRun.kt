package com.sumizuri.app.data.blocks

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_err_count_list
import com.sumizuri.app.shared.resources.script_err_div_zero
import com.sumizuri.app.shared.resources.script_err_function
import com.sumizuri.app.shared.resources.script_err_inputs
import com.sumizuri.app.shared.resources.script_err_name
import com.sumizuri.app.shared.resources.script_err_rem_zero
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

private val Comparisons = setOf("==", "!=", "<", "<=", ">", ">=")

// Works out the parts of a formula. Names are looked up when they are read, so the side of a ? b : c that is not taken costs nothing. A function that takes a condition, such as filter, works it out once for each item of its list, where the names of the item, such as unread or genres, come before the others.
internal class FormulaRun(val context: BlockContext?, private val names: (String) -> Value?) {
    fun eval(expr: Expr): Value = when (expr) {
        is Expr.Num -> Value.Num(expr.value)

        is Expr.Str -> Value.Txt(expr.value)

        is Expr.Name -> name(expr)

        is Expr.Unary -> if (expr.op ==
            "!"
        ) {
            Value.Flag(!eval(expr.operand).asBool())
        } else {
            Value.Num(-eval(expr.operand).asNumber())
        }

        is Expr.Choose -> if (eval(expr.test).asBool()) eval(expr.yes) else eval(expr.no)

        is Expr.Binary -> binary(expr)

        is Expr.Call -> call(expr)
    }

    // The same formula with the names of one item first, for a condition or a value worked out per item.
    fun forItem(item: Any): FormulaRun {
        val fields = itemFields(item, context)
        return FormulaRun(context) { name -> fields(name) ?: names(name) }
    }

    private fun name(expr: Expr.Name): Value = names(expr.name) ?: when (expr.name.lowercase()) {
        "pi" -> Value.Num(PI)
        "e" -> Value.Num(E)
        "true", "yes" -> Value.Flag(true)
        "false", "no" -> Value.Flag(false)
        else -> throw ScriptError(Res.string.script_err_name, expr.name, expr.at + 1)
    }

    private fun binary(expr: Expr.Binary): Value {
        when (expr.op) {
            "&&" -> return Value.Flag(eval(expr.left).asBool() && eval(expr.right).asBool())
            "||" -> return Value.Flag(eval(expr.left).asBool() || eval(expr.right).asBool())
        }
        val left = eval(expr.left)
        val right = eval(expr.right)
        if (expr.op == "+" &&
            (left is Value.Txt || right is Value.Txt)
        ) {
            return Value.Txt(left.asText() + right.asText())
        }
        if (left is Value.Txt && right is Value.Txt &&
            expr.op in Comparisons
        ) {
            return Value.Flag(compareTexts(expr.op, left.value, right.value))
        }
        val a = left.asNumber()
        val b = right.asNumber()
        return when (expr.op) {
            "+" -> Value.Num(a + b)
            "-" -> Value.Num(a - b)
            "*" -> Value.Num(a * b)
            "/" -> if (b == 0.0) throw ScriptError(Res.string.script_err_div_zero) else Value.Num(a / b)
            "%" -> if (b == 0.0) throw ScriptError(Res.string.script_err_rem_zero) else Value.Num(a % b)
            "^" -> Value.Num(a.pow(b))
            "==" -> Value.Flag(a == b)
            "!=" -> Value.Flag(a != b)
            "<" -> Value.Flag(a < b)
            "<=" -> Value.Flag(a <= b)
            ">" -> Value.Flag(a > b)
            else -> Value.Flag(a >= b)
        }
    }

    private fun compareTexts(op: String, a: String, b: String): Boolean = when (op) {
        "==" -> a.equals(b, ignoreCase = true)
        "!=" -> !a.equals(b, ignoreCase = true)
        "<" -> a < b
        "<=" -> a <= b
        ">" -> a > b
        else -> a >= b
    }

    // The numbers a function of many works on: the values of the one series it was given, or all it was given.
    private fun numbers(args: List<Expr>): List<Double> {
        val values = args.map(::eval)
        return when (val only = values.singleOrNull()) {
            is Value.Series -> only.points.map { it.value }
            is Value.Titles, is Value.Reads -> throw ScriptError(Res.string.script_err_count_list)
            else -> values.map { it.asNumber() }
        }
    }

    private fun call(expr: Expr.Call): Value {
        val args = expr.args
        fun arg(index: Int): Value =
            eval(args.getOrNull(index) ?: throw ScriptError(Res.string.script_err_inputs, expr.name, index + 1))
        fun num(index: Int): Double = arg(index).asNumber()
        listCall(expr)?.let { return it }
        return when (expr.name) {
            "if" -> if (arg(0).asBool()) arg(1) else arg(2)
            "min" -> Value.Num(numbers(args).minOrNull() ?: 0.0)
            "max" -> Value.Num(numbers(args).maxOrNull() ?: 0.0)
            "sum" -> Value.Num(numbers(args).sum())
            "avg", "average", "mean" -> Value.Num(numbers(args).let { if (it.isEmpty()) 0.0 else it.average() })
            "abs" -> Value.Num(abs(num(0)))
            "floor" -> Value.Num(floor(num(0)))
            "ceil" -> Value.Num(ceil(num(0)))
            "sqrt" -> Value.Num(sqrt(num(0)))
            "pow" -> Value.Num(num(0).pow(num(1)))
            "log" -> Value.Num(ln(num(0)))
            "log10" -> Value.Num(log10(num(0)))
            "exp" -> Value.Num(exp(num(0)))
            "round" -> Value.Num(halfUp(num(0), 10.0.pow(if (args.size > 1) num(1) else 0.0)))
            "clamp" -> Value.Num(num(0).coerceIn(num(1), maxOf(num(1), num(2))))
            "percent" -> num(1).let { whole -> Value.Num(if (whole == 0.0) 0.0 else num(0) / whole * 100) }
            "len" -> Value.Num(arg(0).size().toDouble())
            "upper" -> Value.Txt(arg(0).asText().uppercase())
            "lower" -> Value.Txt(arg(0).asText().lowercase())
            "trim" -> Value.Txt(arg(0).asText().trim())
            "contains", "has" -> Value.Flag(arg(0).asText().contains(arg(1).asText(), ignoreCase = true))
            "text" -> Value.Txt(if (args.size > 1) numberText(num(0), num(1).toInt()) else arg(0).asText())
            "number" -> Value.Num(arg(0).asNumber())
            else -> throw ScriptError(Res.string.script_err_function, expr.name, expr.at + 1)
        }
    }
}
