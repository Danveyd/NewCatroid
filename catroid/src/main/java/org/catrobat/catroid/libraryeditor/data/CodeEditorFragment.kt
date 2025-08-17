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
    private val tab = "    " // 4 пробела

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

            // --- ЛОГИКА АВТОТАБУЛЯЦИИ ---
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (ignoreTextChange) return
                // Проверяем, что был добавлен один символ и это символ новой строки
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
        // Ищем начало предыдущей строки
        val prevLineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1

        // Находим отступ предыдущей строки
        val prevLine = text.substring(prevLineStart, cursorPosition)
        val indentBuilder = StringBuilder()
        for (char in prevLine) {
            if (char == ' ' || char == '\t') {
                indentBuilder.append(char)
            } else {
                break
            }
        }

        // Если предыдущая строка заканчивалась на '{', добавляем еще один отступ
        if (prevLine.trim().endsWith('{')) {
            indentBuilder.append(tab)
        }

        // Вставляем отступ на новую строку
        if (indentBuilder.isNotEmpty()) {
            // Используем флаг, чтобы избежать рекурсивного вызова TextWatcher
            ignoreTextChange = true
            editText.text.insert(cursorPosition + 1, indentBuilder.toString())
            ignoreTextChange = false
        }
    }

    private fun applyHighlighting(tokens: List<Token>, error: LunoSyntaxError?) {
        ignoreTextChange = true
        val originalText = editText.text.toString()
        val spannable = SpannableString(originalText)

        // Подсветка токенов
        for (token in tokens) {
            val color = getColorForToken(token.type)
            val spanStart = calculateAbsolutePosition(originalText, token.line, token.position)
            val spanEnd = spanStart + token.lexeme.length
            if (spanStart >= 0 && spanEnd <= spannable.length) {
                spannable.setSpan(ForegroundColorSpan(color), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        // Подсветка ошибки
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
            if (nextNewline == -1) return -1 // Строка не найдена
            absolutePosition = nextNewline + 1
            currentLine++
        }
        return if (absolutePosition + position <= text.length) absolutePosition + position else -1
    }

    private fun getColorForToken(type: TokenType): Int {
        return when (type) {
            // Ключевые слова (оранжевый)
            TokenType.VAR, TokenType.FUN, TokenType.IF, TokenType.ELSE, TokenType.RETURN,
            TokenType.WHILE, TokenType.FOR, TokenType.IMPORT, TokenType.CLASS, TokenType.TRY,
            TokenType.CATCH, TokenType.FINALLY -> Color.parseColor("#CF5717")

            // Литералы (голубой/фиолетовый)
            TokenType.NUMBER_LITERAL, TokenType.FLOAT_LITERAL, TokenType.TRUE,
            TokenType.FALSE, TokenType.NULL -> Color.parseColor("#8F4CBA")

            // Строки (зеленый)
            TokenType.STRING_LITERAL -> Color.parseColor("#6B9C49")

            // Основной текст (белый/светло-серый)
            TokenType.IDENTIFIER -> Color.parseColor("#D3D3D3")

            // Операторы и символы (желтый)
            TokenType.LPAREN, TokenType.RPAREN, TokenType.LBRACE, TokenType.RBRACE,
            TokenType.PLUS, TokenType.MINUS, TokenType.ASSIGN -> Color.parseColor("#FCCB41")

            TokenType.COMMENT -> Color.parseColor("#808080")

            else -> Color.parseColor("#A9B7C6") // Запасной цвет
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
    private lateinit var lineNumbersView: TextView // <-- ДОБАВИЛИ
    private var textChangeJob: Job? = null
    private var ignoreTextChange = false
    private val tab = "    "

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_code_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editText = view.findViewById(R.id.code_editor_edit_text)
        lineNumbersView = view.findViewById(R.id.line_numbers_view) // <-- НАШЛИ VIEW

        // --- ИСПРАВЛЕНИЕ ДЛЯ КЛАВИАТУРЫ ---
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
                // Обновляем подсветку и номера строк после установки текста
                applyHighlighting(viewModel.lexedTokens.value ?: emptyList(), viewModel.syntaxError.value)
                updateLineNumbers() // <-- ОБНОВЛЯЕМ НОМЕРА СТРОК
                ignoreTextChange = false
            }
        }

        viewModel.lexedTokens.observe(viewLifecycleOwner) { tokens ->
            if (!ignoreTextChange) { // Чтобы не было двойной подсветки
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

                updateLineNumbers() // <-- ОБНОВЛЯЕМ НОМЕРА СТРОК ПРИ ИЗМЕНЕНИИ

                textChangeJob?.cancel()
                textChangeJob = lifecycleScope.launch {
                    delay(300)
                    s?.let { viewModel.updateCode(it.toString()) }
                }
            }
        })
    }

    // --- НОВАЯ ФУНКЦИЯ ДЛЯ ОБНОВЛЕНИЯ НОМЕРОВ СТРОК ---
    private fun updateLineNumbers() {
        // Запускаем в post, чтобы lineCount был актуальным после изменений в тексте
        editText.post {
            val lineCount = editText.lineCount
            val numbersText = StringBuilder()
            for (i in 1..lineCount) {
                numbersText.append(i).append("\n")
            }
            lineNumbersView.text = numbersText.toString()
        }
    }

    // ... (остальной ваш код: handleAutoIndent, applyHighlighting, и т.д. остается без изменений)
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
}