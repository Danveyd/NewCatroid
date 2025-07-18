/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class CreateTextFieldBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public CreateTextFieldBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.create_textfield_name);
        addAllowedBrickField(BrickField.TEXT, R.id.create_textfield_default);
        addAllowedBrickField(BrickField.X_POSITION, R.id.create_textfield_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.create_textfield_y);
        addAllowedBrickField(BrickField.WIDTH, R.id.create_textfield_width);
        addAllowedBrickField(BrickField.HEIGHT, R.id.create_textfield_height);
        addAllowedBrickField(BrickField.TEXTSIZE, R.id.create_textfield_textsize);
        addAllowedBrickField(BrickField.TEXTCOLOR, R.id.create_textfield_textcolor);
        addAllowedBrickField(BrickField.BGCOLOR, R.id.create_textfield_bgcolor);
        addAllowedBrickField(BrickField.HINT_TEXT, R.id.create_textfield_hinttext);
        addAllowedBrickField(BrickField.HINT_COLOR, R.id.create_textfield_hintcolor);
        addAllowedBrickField(BrickField.ALIGNMENT, R.id.create_textfield_alignment);
        addAllowedBrickField(BrickField.PASSWORD, R.id.create_textfield_password);
        addAllowedBrickField(BrickField.FILE, R.id.create_textfield_font);
        addAllowedBrickField(BrickField.CORNER, R.id.create_textfield_corner);
        addAllowedBrickField(BrickField.MAX_LEN, R.id.create_textfield_length);
        addAllowedBrickField(BrickField.TYPE, R.id.create_textfield_type);
    }

    public CreateTextFieldBrick(String name, String defaultV, Integer x, Integer y, Integer w, Integer h, Integer ts, String tc, String bc, String ht, String hc, String alignment, Integer password, Integer corner, Integer max, String type, String font) {
        this(new Formula(name), new Formula(defaultV), new Formula(x), new Formula(y), new Formula(w), new Formula(h), new Formula(ts), new Formula(tc), new Formula(bc), new Formula(ht), new Formula(hc), new Formula(alignment), new Formula(password), new Formula(corner), new Formula(max), new Formula(type), new Formula(font));
    }

    // --- ИСПРАВЛЕНИЕ 1: Основной конструктор, принимающий Формулы ---
    // Раньше он устанавливал только имя. Теперь он устанавливает ВСЕ поля.
    private CreateTextFieldBrick(Formula name, Formula def, Formula x, Formula y, Formula w, Formula h, Formula ts, Formula tc, Formula bc, Formula ht, Formula hc, Formula align, Formula pass, Formula corner, Formula max, Formula type, Formula font) {
        this(); // Вызываем конструктор по умолчанию, чтобы добавить все AllowedBrickField
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.TEXT, def);
        setFormulaWithBrickField(BrickField.X_POSITION, x);
        setFormulaWithBrickField(BrickField.Y_POSITION, y);
        setFormulaWithBrickField(BrickField.WIDTH, w);
        setFormulaWithBrickField(BrickField.HEIGHT, h);
        setFormulaWithBrickField(BrickField.TEXTSIZE, ts);
        setFormulaWithBrickField(BrickField.TEXTCOLOR, tc);
        setFormulaWithBrickField(BrickField.BGCOLOR, bc);
        setFormulaWithBrickField(BrickField.HINT_TEXT, ht);
        setFormulaWithBrickField(BrickField.HINT_COLOR, hc);
        setFormulaWithBrickField(BrickField.ALIGNMENT, align);
        setFormulaWithBrickField(BrickField.PASSWORD, pass);
        setFormulaWithBrickField(BrickField.FILE, font);
        setFormulaWithBrickField(BrickField.CORNER, corner);
        setFormulaWithBrickField(BrickField.MAX_LEN, max);
        setFormulaWithBrickField(BrickField.TYPE, type);
    }

    // Конструктор для создания кирпичика с уже выбранной переменной.
    // Он был почти правильным, мы просто используем исправленный основной конструктор.
    public CreateTextFieldBrick(Formula name, Formula def, Formula x, Formula y, Formula w, Formula h, Formula ts, Formula tc, Formula bc, Formula ht, Formula hc, Formula align, Formula pass, Formula corner, Formula max, Formula type, Formula font, UserVariable userVariable) {
        this(name, def, x, y, w, h, ts, tc, bc, ht, hc, align, pass, corner, max, type, font);
        this.userVariable = userVariable;
    }

    // --- ИСПРАВЛЕНИЕ 2: Конструктор для установки значения из сенсора ---
    // Раньше он вызывал несуществующий конструктор. Теперь он правильно устанавливает
    // поле TEXT (начальное значение) на формулу сенсора.
    public CreateTextFieldBrick(Sensors defaultValue) {
        this(); // Вызываем конструктор по умолчанию
        // Устанавливаем поле начального текста (TEXT) на формулу с сенсором
        setFormulaWithBrickField(BrickField.TEXT, new Formula(new FormulaElement(FormulaElement.ElementType.SENSOR, defaultValue.name(), null)));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_textfield;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_create_textfield_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createTextfield(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.TEXT),
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.WIDTH),
                getFormulaWithBrickField(BrickField.HEIGHT),
                getFormulaWithBrickField(BrickField.TEXTSIZE),
                getFormulaWithBrickField(BrickField.TEXTCOLOR),
                getFormulaWithBrickField(BrickField.BGCOLOR),
                getFormulaWithBrickField(BrickField.HINT_TEXT),
                getFormulaWithBrickField(BrickField.HINT_COLOR),
                getFormulaWithBrickField(BrickField.ALIGNMENT),
                getFormulaWithBrickField(BrickField.PASSWORD),
                getFormulaWithBrickField(BrickField.CORNER),
                getFormulaWithBrickField(BrickField.MAX_LEN),
                getFormulaWithBrickField(BrickField.TYPE),
                getFormulaWithBrickField(BrickField.FILE),
                userVariable // Передаем переменную, унаследованную от UserVariableBrickWithFormula
        ));
    }
}
