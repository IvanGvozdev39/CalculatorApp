package com.test.calculatorapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Float.max
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private val ADD = Constants.ADD
    private val SUBTRACT = Constants.SUBTRACT
    private val MULTIPLY = Constants.MULTIPLY
    private val DIVIDE = Constants.DIVIDE
    private lateinit var slideLeftAnim: Animation
    private lateinit var slideUpAnim: Animation
    private var canSlideUp = true
    private var canAddOperation = false
    private var canAddDecimal = false
    private var canAddPercent = false
    private var canAddNumber = true
    private var errorShown = false
    private var currentResult: BigDecimal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        slideLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_left_anim)
        slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_anim)

        showAdBanner()
    }

    fun showAdBanner() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_ad)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val dialogBtnConfirm = dialog.findViewById<MaterialButton>(R.id.button_confirm)
        val dialogBtnCancel = dialog.findViewById<AppCompatButton>(R.id.button_cancel)
        val dialogImage = dialog.findViewById<ImageView>(R.id.banner_image)

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.show()
        }, 1000)

        dialogBtnConfirm.setOnClickListener {
            googlePlayIntent()
        }

        dialogImage.setOnClickListener {
            googlePlayIntent()
        }

        dialogBtnCancel.setOnClickListener { dialog.dismiss() }
    }

    fun googlePlayIntent() {
        val url = "https://play.google.com/store/apps/developer?id=Ivan+Gvozdev&hl=ru&gl=US"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    fun numberAction(view: View) {
        if (view is Button) {
            var digitsOps = digitsOperatorsSeparate()
            if (view.text == ".") {
                if (canAddDecimal) {
                    workingsET.append(view.text)
                    canAddDecimal = false
                    canAddOperation = false
                    canAddPercent = false
                    canAddNumber = true
                    if (digitsOps.size <= 2)
                        workingsET.startAnimation(slideLeftAnim)
                    canSlideUp = true

                    workingsET.setSelection(workingsET.text.length)
                }
            } else if (view.text == "%") {
                if (!errorShown) {
                    if (workingsET.text.isEmpty())
                        return
                    else if (canAddPercent) {
                        workingsET.append(view.text)
                        canAddPercent = false
                        canAddOperation = true
                        canAddDecimal = false
                        canAddNumber = false
                        if (digitsOps.size <= 2)
                            if (digitsOps.size <= 2)
                                workingsET.startAnimation(slideLeftAnim)
                        canSlideUp = true
                        workingsET.setSelection(workingsET.text.length)
                    }
                }
            } else if (canAddNumber) {
                if (workingsET.text.length == 1 && workingsET.text[0] == '0') {
                    workingsET.setText("")
                }
                workingsET.append(view.text)
                if (digitsOps.size <= 2)
                    workingsET.startAnimation(slideLeftAnim)
                canSlideUp = true
                workingsET.setSelection(workingsET.text.length)

                canAddOperation = true

                val currentText = workingsET.text.toString()
                val lastNumber = currentText.split(Regex("[+\\-x÷]")).last()
                canAddDecimal = !lastNumber.contains(".")
                canAddPercent = true
                canAddNumber = true
            }
            resultsTV.text = resultsTVformat(calculateResults())
            adjustTextSize(resultsTV, resultsTV.text.toString())
        }
    }

    fun resultsTVformat(s: String): String {
        if (s == getString(R.string.cant_divide_by_zero))
            return s

        val bigDecimal = BigDecimal(s)

        return if (s.length > 17) {
            val df = DecimalFormat("0.######E0")
            df.format(bigDecimal)
        } else s
    }

    fun operationAction(view: View) {
        if (!errorShown) {
            var digitsOperators = digitsOperatorsSeparate()
            if (view is Button && canAddOperation) {
                val currentText = workingsET.text.toString()
                if (currentText.isNotEmpty() && (currentText.last()
                        .isDigit() || currentText.last() == '%')
                ) {
                    if (digitsOperators.size == 2 && digitsOperators[1] == '%') {
                        workingsET.setText("")
                        digitsOperators[0] =
                            (digitsOperators[0].toString().toFloat() / 100).toBigDecimal()
                        digitsOperators.removeLast()

                        for (el in digitsOperators)
                            workingsET.append(el.toString())
                    }
                    workingsET.append(view.text)
                    canAddOperation = false
                    canAddDecimal = false
                    canAddPercent = false
                    canAddNumber = true
                    var digitsOps = digitsOperatorsSeparate()
                    if (digitsOps.size <= 2)
                        workingsET.startAnimation(slideLeftAnim)
                    canSlideUp = true
                    workingsET.setSelection(workingsET.text.length)

                    performPendingCalculation(view.text.toString())
                }
            } else {
                if (digitsOperators.size > 0) {
                    val operators = setOf(ADD.toString(), SUBTRACT.toString(), MULTIPLY.toString(), DIVIDE.toString())
                    if (digitsOperators.last().toString() in operators) {
                        digitsOperators.removeLast()
                        workingsET.setText("")
                        for (i in digitsOperators)
                            workingsET.append(i.toString())
                        if (view is Button)
                            workingsET.append(view.text)
                    }
                }
            }
        }
    }

    fun allClearAction(view: View) {
        workingsET.setText("")
        resultsTV.text = ""
        canAddOperation = false
        canAddDecimal = false
        canAddPercent = false
        canAddNumber = true
        currentResult = null
        errorShown = false
    }

    fun equalsAction(view: View) {
        if (workingsET.text.isNotEmpty() &&
            (workingsET.text.last().isDigit() || workingsET.text.last() == '%') &&
                    !errorShown
        ) {
            workingsET.setText(calculateResults())
            workingsET.setSelection(workingsET.text.length)
            if (canSlideUp) {
                workingsET.startAnimation(slideUpAnim)
                canSlideUp = false
            }
            workingsET.setSelection(workingsET.text.length)
            resultsTV.text = ""
        }
    }

    @SuppressLint("SetTextI18n")
    private fun performPendingCalculation(value: String) {
        val digitsOperators = digitsOperatorsSeparate()
        if (digitsOperators.size >= 3) {
            val result = evaluateExpression(digitsOperators)
            workingsET.setText(result?.stripTrailingZeros()?.toPlainString() + value)
            resultsTV.setText(result?.stripTrailingZeros()?.toPlainString())
        }
    }

    private fun calculateResults(): String {
        val digitsOperators = digitsOperatorsSeparate()
        if (digitsOperators.isEmpty())
            return ""

        val result = evaluateExpression(digitsOperators)
        if (result == null) {
            resultsTV.textSize = 32f
            errorShown = true
            return getString(R.string.cant_divide_by_zero)
        } else {
            resultsTV.textSize = 48f
            errorShown = false

            val resultString = result.stripTrailingZeros().toPlainString()

            return resultString
        }
    }

    fun adjustTextSize(textView: TextView, text: String) {
        if (text == getString(R.string.cant_divide_by_zero))
            return
        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val maxTextSize = 48f
        val minTextSize = 12f

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, maxTextSize)
        textView.text = text

        textView.measure(0, 0)
        val textWidth = textView.measuredWidth

        if (textWidth > screenWidth) {
            val ratio = screenWidth / textWidth.toFloat()
            val newSize = maxTextSize * ratio
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, max(minTextSize, newSize))
        }
    }


    private fun evaluateExpression(tokens: MutableList<Any>): BigDecimal? {

        //Proceeding tokens finding negative numbers and saving them as one element
        var j = 0
        while (j < tokens.size) {
            if (j == 0 && tokens[j] == SUBTRACT) {
                tokens[j] = tokens[j + 1].toString().toBigDecimal().negate()
                for (ind in 1 until tokens.size - 1) {
                    tokens[ind] = tokens[ind + 1]
                    if (ind == tokens.size - 2)
                        tokens.removeLast()
                }
            } else if (tokens[j] == SUBTRACT) {
                if (!((tokens[j - 1] is BigDecimal && j + 1 < tokens.size && tokens[j + 1] is BigDecimal)
                            || (tokens[j - 1] is BigDecimal && j == tokens.size - 1))
                ) {
                    for (ind in j until tokens.size - 1) {
                        if (ind == j)
                            tokens[ind] = tokens[ind + 1].toString().toBigDecimal().negate()
                        else
                            tokens[ind] = tokens[ind + 1]
                        if (ind == tokens.size - 2)
                            tokens.removeLast()
                    }
                }
            }
            j++
        }


        var result = tokens[0] as BigDecimal?
        var i = 1

        while (i < tokens.size - 1) {
            var operator = tokens[i] as Char
            if (i + 2 < tokens.size && tokens[i + 2] == '%') {
                val nextValue = tokens[i + 1] as BigDecimal
                result =
                    result?.let { applyOperation(it, nextValue, operator, applyPercentOperation = true) }
                i += 3
            } else {
                val nextValue = tokens[i + 1] as BigDecimal
                result =
                    result?.let { applyOperation(it, nextValue, operator, applyPercentOperation = false) }
                i += 2
            }
        }

        if (tokens.size == 2 && tokens[1] == '%')
            result = (tokens[0].toString().toFloat() / 100).toBigDecimal()

        currentResult = result
        return result
    }

    private fun applyOperation(
        a: BigDecimal,
        b: BigDecimal,
        op: Char,
        applyPercentOperation: Boolean
    ): BigDecimal? {
        if (applyPercentOperation) {
            val af = a.toFloat()
            val bf = b.toFloat()
            return when (op) {
                ADD -> (af + (af * bf / 100)).toBigDecimal()
                SUBTRACT -> (af - (af * bf / 100)).toBigDecimal()
                MULTIPLY -> (af * bf / 100).toBigDecimal()
                DIVIDE -> if (b.toFloat() == 0f) null else (af / bf * 100).toBigDecimal()
                else -> BigDecimal.ZERO
            }
        } else return when (op) {
            ADD -> a.add(b)
            SUBTRACT -> a.subtract(b)
            MULTIPLY -> a.multiply(b)
            DIVIDE -> if (b.toFloat() == 0f) null else a.divide(b, 10, RoundingMode.HALF_UP)
            else -> BigDecimal.ZERO
        }
    }

    private fun digitsOperatorsSeparate(): MutableList<Any> {
        val list = mutableListOf<Any>()
        var currentDigit = ""

        for (character in workingsET.text) {
            if (character.isDigit() || character == '.') {
                currentDigit += character
            } else {
                if (currentDigit.isNotEmpty()) {
                    list.add(currentDigit.toBigDecimal())
                    currentDigit = ""
                }
                list.add(character)
            }
        }

        if (currentDigit.isNotEmpty()) {
            list.add(currentDigit.toBigDecimal())
        }

        return list
    }

    @SuppressLint("SetTextI18n")
    fun negativeInversionAction(view: View) {
        val digitsOperators = digitsOperatorsSeparate()
        if (digitsOperators.isNotEmpty()) {
            if (digitsOperators.size == 1) {
                workingsET.setText("0-${digitsOperators[0]}")
                resultsTV.text = resultsTVformat(calculateResults())
                adjustTextSize(resultsTV, resultsTV.text.toString())
            } else if (digitsOperators.last() is BigDecimal || digitsOperators.last() == '%') {
                val previousIndex = digitsOperators.lastIndex -
                        if (digitsOperators.last() != '%') 1 else 2

                when (digitsOperators[previousIndex]) {
                    ADD -> digitsOperators[previousIndex] = SUBTRACT
                    SUBTRACT -> {
                        if (previousIndex - 1 >= 0 && digitsOperators[previousIndex - 1].toString()
                                .last().isDigit()
                        )
                            digitsOperators[previousIndex] = ADD
                        else {
                            for (i in previousIndex until digitsOperators.size - 1) {
                                digitsOperators[i] = digitsOperators[i + 1]
                                if (i == digitsOperators.size - 2)
                                    digitsOperators.removeLast()
                            }
                        }
                    }
                    else -> {
                        if (digitsOperators.last() != '%') {
                            digitsOperators[digitsOperators.lastIndex] =
                                (digitsOperators.last() as BigDecimal).negate()
                        } else {
                            digitsOperators[digitsOperators.lastIndex - 1] =
                                (digitsOperators[digitsOperators.lastIndex - 1] as BigDecimal).negate()
                        }
                    }
                }

                var newText = ""
                for (el in digitsOperators)
                    newText += el.toString()
                workingsET.setText(newText)
                resultsTV.text = resultsTVformat(calculateResults())
                adjustTextSize(resultsTV, resultsTV.text.toString())
            }
        }
    }
}
