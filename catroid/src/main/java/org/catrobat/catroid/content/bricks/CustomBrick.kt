// org/catrobat/catroid/content/bricks/CustomBrick.kt
package org.catrobat.catroid.content.bricks

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import com.thoughtworks.xstream.annotations.XStreamAlias
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.InternToExternGenerator
import org.catrobat.catroid.libraries.CustomBrickManager
import org.catrobat.catroid.libraries.ParameterType
import java.lang.reflect.Field

@XStreamAlias("CustomBrick")
class CustomBrick(
    var definitionId: String = ""
) : FormulaBrick() { // Наследуемся от самого базового Brick

    var parameterFormulas: MutableList<Formula> = mutableListOf()

    /**
     * Этот метод вызывается XStream'ом после десериализации.
     * Он "чинит" объекты Formula, которые XStream не смог восстановить правильно.
     */
    /*private fun readResolve(): Any {
        if (parameterFormulas.isNullOrEmpty()) {
            return this // Если формул нет, делать нечего
        }

        // Мы не можем доверять методам "сломанных" объектов Formula.
        // Поэтому мы используем рефлексию, чтобы напрямую "вытащить"
        // их внутреннее строковое представление.
        try {
            // Получаем доступ к приватному полю `formulaTree` внутри класса Formula
            val formulaTreeField: Field = Formula::class.java.getDeclaredField("formulaTree")
            formulaTreeField.isAccessible = true // Делаем его доступным

            val restoredFormulas = mutableListOf<Formula>()
            for (possiblyBrokenFormula in parameterFormulas) {
                // Получаем "сломанный" объект formulaTree
                val brokenFormulaTree = formulaTreeField.get(possiblyBrokenFormula) as? FormulaElement

                // Получаем строковое представление из этого объекта
                val formulaString = brokenFormulaTree?.getInternTokenList()?.joinToString(separator = "") { it.tokenStringValue
                    ?: "" } ?: ""

                // Создаем новый, "здоровый" объект Formula
                restoredFormulas.add(Formula(formulaString))
            }
            // Заменяем старый список на новый
            this.parameterFormulas = restoredFormulas*/
    /*private fun readResolve(): Any {
        if (parameterFormulas.isNullOrEmpty()) {
            return this
        }

        try {
            val formulaTreeField: Field = Formula::class.java.getDeclaredField("formulaTree")
            formulaTreeField.isAccessible = true

            val restoredFormulas = mutableListOf<Formula>()
            for (possiblyBrokenFormula in parameterFormulas) {
                val brokenFormulaTree = formulaTreeField.get(possiblyBrokenFormula) as? FormulaElement

                val tokenList = brokenFormulaTree?.getInternTokenList()

                if (tokenList != null) {
                    // --- НАЧАЛО ИСПРАВЛЕНИЙ ---
                    // 1. Создаем генератор с контекстом приложения.
                    val generator = InternToExternGenerator(CatroidApplication.getAppContext())

                    // 2. Вызываем метод, который сгенерирует строку и сохранит ее внутри объекта.
                    generator.generateExternStringAndMapping(tokenList)

                    // 3. Забираем готовую, правильно отформатированную строку.
                    val formulaString = generator.generatedExternFormulaString

                    // 4. Создаем новую, "здоровую" формулу.
                    restoredFormulas.add(Formula(formulaString))
                    // --- КОНЕЦ ИСПРАВЛЕНИЙ ---
                } else {
                    restoredFormulas.add(Formula(""))
                }
            }
            this.parameterFormulas = restoredFormulas

        } catch (e: Exception) {
            Log.e("CustomBrick", "Ошибка восстановления формул через рефлексию", e)
            // Если рефлексия не удалась, создаем пустые формулы, чтобы избежать падения
            val emptyFormulas = mutableListOf<Formula>()
            for (i in parameterFormulas.indices) {
                emptyFormulas.add(Formula(""))
            }
            this.parameterFormulas = emptyFormulas
        }

        return this // Обязательно возвращаем this
    }*/

    private fun readResolve(): Any {
        if (parameterFormulas.isNullOrEmpty()) {
            return this
        }

        try {
            // 1. Получаем доступ к приватному полю `formulaTree` класса Formula
            val formulaTreeField: Field = Formula::class.java.getDeclaredField("formulaTree")
            formulaTreeField.isAccessible = true

            val restoredFormulas = mutableListOf<Formula>()
            for (possiblyBrokenFormula in parameterFormulas) {
                // 2. Извлекаем "сломанный", но содержащий данные FormulaElement
                val brokenFormulaTree = formulaTreeField.get(possiblyBrokenFormula) as? FormulaElement

                if (brokenFormulaTree != null) {
                    // 3. Создаем НОВЫЙ, "здоровый" объект Formula, используя конструктор,
                    // который принимает FormulaElement. Это гарантирует правильную инициализацию.
                    restoredFormulas.add(Formula(brokenFormulaTree))
                } else {
                    // На случай, если даже formulaTree == null
                    restoredFormulas.add(Formula(""))
                }
            }
            // 4. Заменяем старый список на новый, полностью рабочий
            this.parameterFormulas = restoredFormulas

        } catch (e: Exception) {
            Log.e("CustomBrick", "Ошибка восстановления формул через рефлексию", e)
            // Запасной план остается тем же
            val emptyFormulas = mutableListOf<Formula>()
            for (i in parameterFormulas.indices) {
                emptyFormulas.add(Formula(""))
            }
            this.parameterFormulas = emptyFormulas
        }

        return this
    }


    override fun getViewResource(): Int {
        val definition = CustomBrickManager.findDefinitionById(definitionId)
        return when (definition?.parameters?.size) {
            0 -> R.layout.brick_custom_0_param // Макет без параметров
            1 -> R.layout.brick_custom_1_param
            2 -> R.layout.brick_custom_2_param
            3 -> R.layout.brick_custom_3_param
            4 -> R.layout.brick_custom_4_param
            5 -> R.layout.brick_custom_5_param
            6 -> R.layout.brick_custom_6_param
            7 -> R.layout.brick_custom_7_param
            8 -> R.layout.brick_custom_8_param
            else -> R.layout.brick_unknow
        }
    }

    // --- ВОТ ГЛАВНАЯ МАГИЯ ---
    override fun getView(context: Context): View {
        // 1. Создаем View из нужного XML-макета, используя стандартный механизм
        super.getView(context) // Важно вызвать super, он заполнит поле `view`

        // 2. Получаем определение нашего блока, чтобы знать, что отображать
        val definition = CustomBrickManager.findDefinitionById(definitionId)
        if (definition == null) {
            // Если блок не найден, можно показать ошибку в самом блоке
            view.findViewById<TextView>(R.id.custom_brick_header_part_1)?.text =
                "Ошибка: блок '$definitionId' не найден"
            return view
        }

        // 3. Динамически настраиваем созданный View
        // Сначала все прячем, чтобы не было "мусора" от переиспользования
        resetDynamicViews()

        // 3.1. Заполняем текстовые части
        val headerParts = definition.headerText.split(Regex("\\{\\d+\\}"))
        val textViews = getHeaderTextViews()
        headerParts.forEachIndexed { index, text ->
            if (index < textViews.size) {
                textViews[index]?.apply {
                    this.text = text.trim()
                    visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
                }
            }
        }

        // 3.2. Настраиваем поля ввода для параметров
        definition.parameters.forEachIndexed { index, paramDef ->
            while (parameterFormulas.size <= index) {
                parameterFormulas.add(Formula(0.0))
            }
            val formula = parameterFormulas[index]

            // --- ГЛАВНОЕ ИЗМЕНЕНИЕ: ВЫБИРАЕМ УНИКАЛЬНЫЙ BRICKFIELD ---
            val brickField = getBrickFieldForIndex(index)
            if (brickField == null) {
                // Если параметров больше, чем мы определили в enum,
                // просто пропускаем их, чтобы не было сбоя.
                Log.e("CustomBrick", "Слишком много параметров для блока ${definition.id}. Параметр ${index+1} не будет отображен.")
                return@forEachIndexed // continue для forEach
            }

            when (paramDef.type) {
                ParameterType.TEXT_FIELD -> {
                    val formulaView = getFormulaView(index)
                    formulaView?.visibility = View.VISIBLE

                    // Используем наш уникальный BrickField
                    addAllowedBrickField(brickField, formulaView!!.id)
                    setFormulaWithBrickField(brickField, formula)
                }
                ParameterType.VARIABLE_DROPDOWN -> {
                    val spinner = getSpinnerView(index)
                    spinner?.visibility = View.VISIBLE
                    // TODO: Настроить спиннер. Ему тоже может понадобиться свой BrickField.
                }
                else -> TODO()
            }
        }

        // После настройки всех полей, вызываем метод, который вешает слушатели
        if (this is FormulaBrick) { // Если CustomBrick наследуется от FormulaBrick
            setClickListeners()
        }

        return view
    }

    private fun getBrickFieldForIndex(index: Int): Brick.BrickField? {
        return when (index) {
            0 -> Brick.BrickField.CUSTOM_PARAM_1
            1 -> Brick.BrickField.CUSTOM_PARAM_2
            2 -> Brick.BrickField.CUSTOM_PARAM_3
            3 -> Brick.BrickField.CUSTOM_PARAM_4
            4 -> Brick.BrickField.CUSTOM_PARAM_5
            5 -> Brick.BrickField.CUSTOM_PARAM_6
            6 -> Brick.BrickField.CUSTOM_PARAM_7
            7 -> Brick.BrickField.CUSTOM_PARAM_8
            8 -> Brick.BrickField.CUSTOM_PARAM_9
            9 -> Brick.BrickField.CUSTOM_PARAM_10
            else -> null // Больше не поддерживаем
        }
    }

    // Вспомогательные функции для поиска View внутри нашего `view`
    private fun resetDynamicViews() {
        getHeaderTextViews().forEach { it?.visibility = View.GONE }
        for (i in 0..4) { // Сбрасываем для 5 потенциальных параметров
            getFormulaView(i)?.visibility = View.GONE
            getSpinnerView(i)?.visibility = View.GONE
        }
    }

    private fun getHeaderTextViews(): List<TextView?> {
        return listOf(
            view.findViewById(R.id.custom_brick_header_part_1),
            view.findViewById(R.id.custom_brick_header_part_2),
            view.findViewById(R.id.custom_brick_header_part_3),
            view.findViewById(R.id.custom_brick_header_part_4),
            view.findViewById(R.id.custom_brick_header_part_5),
            view.findViewById(R.id.custom_brick_header_part_6),
            view.findViewById(R.id.custom_brick_header_part_7),
            view.findViewById(R.id.custom_brick_header_part_8),
            view.findViewById(R.id.custom_brick_header_part_9)
        )
    }

    private fun getFormulaView(index: Int): TextView? {
        val id = when (index) {
            0 -> R.id.custom_brick_formula_1
            1 -> R.id.custom_brick_formula_2
            2 -> R.id.custom_brick_formula_3
            3 -> R.id.custom_brick_formula_4
            4 -> R.id.custom_brick_formula_5
            5 -> R.id.custom_brick_formula_6
            6 -> R.id.custom_brick_formula_7
            7 -> R.id.custom_brick_formula_8
            8 -> R.id.custom_brick_formula_9
            else -> -1
        }
        return if (id != -1) view.findViewById(id) else null
    }

    private fun getSpinnerView(index: Int): Spinner? { /* ... аналогично ... */ return null }

    // Логика выполнения остается прежней - она создает CustomAction
    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        val definition = CustomBrickManager.findDefinitionById(definitionId) ?: return

        val customAction = sprite.actionFactory.createCustomAction(
            sprite,
            sequence,
            definition,
            parameterFormulas
        )
        sequence.addAction(customAction)
    }

    override fun clone(): Brick {
        val newBrick = CustomBrick(definitionId)
        // Глубокое копирование формул
        newBrick.parameterFormulas = parameterFormulas.map { it.clone() }.toMutableList()
        return newBrick
    }
}