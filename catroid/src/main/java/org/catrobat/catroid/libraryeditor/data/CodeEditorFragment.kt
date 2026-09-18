/*package org.catrobat.catroid.libraryeditor.data

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.catrobat.catroid.R
import org.catrobat.catroid.utils.lunoscript.LunoSyntaxError
import org.catrobat.catroid.utils.lunoscript.Token
import org.catrobat.catroid.utils.lunoscript.TokenType

class CodeEditorFragment : Fragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private lateinit var editText: EditText
    private var textChangeJob: Job? = null
    private var ignoreTextChange = false
    private val tab = "    "

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_code_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText = view.findViewById(R.id.code_editor_edit_text)
        setupObservers()
        setupTextWatcher()
    }

    private fun setupObservers() {
        viewModel.libraryDraft.observe(viewLifecycleOwner) { draft ->
            if (editText.text.toString() != draft.code) {
                ignoreTextChange = true
                editText.setText(draft.code)
                ignoreTextChange = false
            }
        }

        viewModel.lexedTokens.observe(viewLifecycleOwner) { tokens ->
            applyHighlighting(tokens, null)
        }

        viewModel.syntaxError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                applyHighlighting(emptyList(), error)
            }
        }
    }

    private fun setupTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreTextChange) return
                if (before == 0 && count == 1 && s?.get(start) == '\n') {
                    handleAutoIndent(start)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreTextChange) return

                textChangeJob?.cancel()
                textChangeJob = lifecycleScope.launch {
                    delay(300)
                    s?.let { viewModel.updateCode(it.toString()) }
                }
            }
        })
    }

    private fun handleAutoIndent(cursorPosition: Int) {
        val text = editText.text.toString()
        val prevLineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1

        val prevLine = text.substring(prevLineStart, cursorPosition)
        val indentBuilder = StringBuilder()
        for (char in prevLine) {
            if (char == ' ' || char == '\t') {
                indentBuilder.append(char)
            } else {
                break
            }
        }

        if (prevLine.trim().endsWith('{')) {
            indentBuilder.append(tab)
        }

        if (indentBuilder.isNotEmpty()) {
            ignoreTextChange = true
            editText.text.insert(cursorPosition + 1, indentBuilder.toString())
            ignoreTextChange = false
        }
    }

    private fun applyHighlighting(tokens: List<Token>, error: LunoSyntaxError?) {
        ignoreTextChange = true
        val originalText = editText.text.toString()
        val spannable = SpannableString(originalText)

        for (token in tokens) {
            val color = getColorForToken(token.type)
            val spanStart = calculateAbsolutePosition(originalText, token.line, token.position)
            val spanEnd = spanStart + token.lexeme.length
            if (spanStart >= 0 && spanEnd <= spannable.length) {
                spannable.setSpan(ForegroundColorSpan(color), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        error?.let {
            val lineStart = calculateAbsolutePosition(originalText, it.line, 0)
            if (lineStart >= 0) {
                var lineEnd = originalText.indexOf('\n', lineStart)
                if (lineEnd == -1) lineEnd = originalText.length
                spannable.setSpan(BackgroundColorSpan(Color.argb(50, 255, 0, 0)), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd
        editText.setText(spannable, TextView.BufferType.SPANNABLE)
        editText.setSelection(selectionStart, selectionEnd)
        ignoreTextChange = false
    }

    private fun calculateAbsolutePosition(text: String, line: Int, position: Int): Int {
        var currentLine = 1
        var absolutePosition = 0
        while (currentLine < line && absolutePosition < text.length) {
            val nextNewline = text.indexOf('\n', absolutePosition)
            if (nextNewline == -1) return -1
            absolutePosition = nextNewline + 1
            currentLine++
        }
        return if (absolutePosition + position <= text.length) absolutePosition + position else -1
    }

    private fun getColorForToken(type: TokenType): Int {
        return when (type) {
            TokenType.VAR, TokenType.FUN, TokenType.IF, TokenType.ELSE, TokenType.RETURN,
            TokenType.WHILE, TokenType.FOR, TokenType.IMPORT, TokenType.CLASS, TokenType.TRY,
            TokenType.CATCH, TokenType.FINALLY -> Color.parseColor("#CF5717")

            TokenType.NUMBER_LITERAL, TokenType.FLOAT_LITERAL, TokenType.TRUE,
            TokenType.FALSE, TokenType.NULL -> Color.parseColor("#8F4CBA")

            TokenType.STRING_LITERAL -> Color.parseColor("#6B9C49")

            TokenType.IDENTIFIER -> Color.parseColor("#D3D3D3")

            TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
            TokenType.PLUS, TokenType.MINUS, TokenType.ASSIGN -> Color.parseColor("#FCCB41")

            TokenType.COMMENT -> Color.parseColor("#808080")

            else -> Color.parseColor("#A9B7C6")
        }
    }
}*/

package org.catrobat.catroid.libraryeditor.data

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.catrobat.catroid.R
import org.catrobat.catroid.utils.lunoscript.LunoSyntaxError
import org.catrobat.catroid.utils.lunoscript.Token
import org.catrobat.catroid.utils.lunoscript.TokenType

class CodeEditorFragment : Fragment() {

    private val viewModel: LibraryEditorViewModel by activityViewModels()
    private lateinit var editText: EditText
    private lateinit var lineNumbersView: TextView
    private var textChangeJob: Job? = null
    private var ignoreTextChange = false
    private val tab = "    "

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_code_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText = view.findViewById(R.id.code_editor_edit_text)
        lineNumbersView = view.findViewById(R.id.line_numbers_view)

        editText.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS// or
                //TYPE_TEXT_FLAG_FILTER

        setupObservers()
        setupTextWatcher()
    }

    private fun setupObservers() {
        viewModel.libraryDraft.observe(viewLifecycleOwner) { draft ->
            if (editText.text.toString() != draft.code) {
                ignoreTextChange = true
                editText.setText(draft.code)
                applyHighlighting(viewModel.lexedTokens.value ?: emptyList(), viewModel.syntaxError.value)
                updateLineNumbers()
                ignoreTextChange = false
            }
        }

        viewModel.lexedTokens.observe(viewLifecycleOwner) { tokens ->
            if (!ignoreTextChange) {
                applyHighlighting(tokens, viewModel.syntaxError.value)
            }
        }

        viewModel.syntaxError.observe(viewLifecycleOwner) { error ->
            if (!ignoreTextChange) {
                applyHighlighting(viewModel.lexedTokens.value ?: emptyList(), error)
            }
        }
    }

    private fun setupTextWatcher() {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreTextChange) return
                if (before == 0 && count == 1 && s?.get(start) == '\n') {
                    handleAutoIndent(start)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (ignoreTextChange) return

                updateLineNumbers()

                textChangeJob?.cancel()
                textChangeJob = lifecycleScope.launch {
                    delay(300)
                    s?.let { viewModel.updateCode(it.toString()) }
                }
            }
        })
    }

    private fun updateLineNumbers() {
        editText.post {
            val lineCount = editText.lineCount
            val numbersText = StringBuilder()
            for (i in 1..lineCount) {
                numbersText.append(i).append("\n")
            }
            lineNumbersView.text = numbersText.toString()
        }
    }

    private fun handleAutoIndent(cursorPosition: Int) {
        val text = editText.text.toString()
        val prevLineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1
        val prevLine = text.substring(prevLineStart, cursorPosition)
        val indentBuilder = StringBuilder()
        for (char in prevLine) {
            if (char == ' ' || char == '\t') {
                indentBuilder.append(char)
            } else {
                break
            }
        }
        if (prevLine.trim().endsWith('{')) {
            indentBuilder.append(tab)
        }
        if (indentBuilder.isNotEmpty()) {
            ignoreTextChange = true
            editText.text.insert(cursorPosition + 1, indentBuilder.toString())
            ignoreTextChange = false
        }
    }
    private fun applyHighlighting(tokens: List<Token>, error: LunoSyntaxError?) {
        ignoreTextChange = true
        val originalText = editText.text.toString()
        val spannable = SpannableString(originalText)
        for (token in tokens) {
            val color = getColorForToken(token.type)
            val spanStart = calculateAbsolutePosition(originalText, token.line, token.position)
            val spanEnd = spanStart + token.lexeme.length
            if (spanStart >= 0 && spanEnd <= spannable.length) {
                spannable.setSpan(ForegroundColorSpan(color), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        error?.let {
            val lineStart = calculateAbsolutePosition(originalText, it.line, 0)
            if (lineStart >= 0) {
                var lineEnd = originalText.indexOf('\n', lineStart)
                if (lineEnd == -1) lineEnd = originalText.length
                spannable.setSpan(BackgroundColorSpan(Color.argb(50, 255, 0, 0)), lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val selectionStart = editText.selectionStart
        val selectionEnd = editText.selectionEnd
        editText.setText(spannable, TextView.BufferType.SPANNABLE)
        editText.setSelection(selectionStart, selectionEnd)
        ignoreTextChange = false
    }
    private fun calculateAbsolutePosition(text: String, line: Int, position: Int): Int {
        var currentLine = 1
        var absolutePosition = 0
        while (currentLine < line && absolutePosition < text.length) {
            val nextNewline = text.indexOf('\n', absolutePosition)
            if (nextNewline == -1) return -1
            absolutePosition = nextNewline + 1
            currentLine++
        }
        return if (absolutePosition + position <= text.length) absolutePosition + position else -1
    }
    private fun getColorForToken(type: TokenType): Int {
        return when (type) {
            TokenType.VAR, TokenType.FUN, TokenType.CLASS, TokenType.STATIC,
            TokenType.IF, TokenType.ELSE, TokenType.WHILE, TokenType.FOR, TokenType.IN,
            TokenType.SWITCH, TokenType.CASE, TokenType.DEFAULT,
            TokenType.RETURN, TokenType.BREAK, TokenType.CONTINUE,
            TokenType.THIS, TokenType.SUPER,
            TokenType.IMPORT, TokenType.TRY, TokenType.CATCH, TokenType.FINALLY ->
                Color.parseColor("#CF5717")

            TokenType.NUMBER_LITERAL, TokenType.FLOAT_LITERAL,
            TokenType.TRUE, TokenType.FALSE, TokenType.NULL ->
                Color.parseColor("#8F4CBA")

            TokenType.STRING_LITERAL, TokenType.F_STRING ->
                Color.parseColor("#6B9C49")

            TokenType.IDENTIFIER ->
                Color.parseColor("#D3D3D3")

            TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
            TokenType.LBRACKET, TokenType.RBRACKET,
            TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO,
            TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN, TokenType.MODULO_ASSIGN,
            TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE,
            TokenType.AND, TokenType.OR, TokenType.BANG,
            TokenType.DOT, TokenType.COMMA, TokenType.COLON, TokenType.SEMICOLON,
            TokenType.ARROW ->
                Color.parseColor("#FCCB41")

            TokenType.COMMENT ->
                Color.parseColor("#808080")

            else ->
                Color.parseColor("#A9B7C6")
        }
    }
}
