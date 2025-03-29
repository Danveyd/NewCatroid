/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

public class CreateTableBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateTableBrick() {
        addAllowedBrickField(BrickField.TABLE_NAME, R.id.brick_create_table_edit_name);
        addAllowedBrickField(BrickField.SIZE_X, R.id.brick_create_table_edit_x);
        addAllowedBrickField(BrickField.SIZE_Y, R.id.brick_create_table_edit_y);
    }

    public CreateTableBrick(String value, Integer value2, Integer value3) {
        this(new Formula(value), new Formula(value2), new Formula(value3));
    }

    public CreateTableBrick(Formula formula, Formula formula2, Formula formula3) {
        this();
        setFormulaWithBrickField(BrickField.TABLE_NAME, formula);
        setFormulaWithBrickField(BrickField.SIZE_X, formula2);
        setFormulaWithBrickField(BrickField.SIZE_Y, formula3);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_table;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateTableAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TABLE_NAME), getFormulaWithBrickField(BrickField.SIZE_X), getFormulaWithBrickField(BrickField.SIZE_Y)));
    }
}
