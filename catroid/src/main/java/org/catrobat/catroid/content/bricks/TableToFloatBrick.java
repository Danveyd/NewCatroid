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

public class TableToFloatBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public TableToFloatBrick() {
        addAllowedBrickField(BrickField.TABLE_NAME, R.id.brick_table_to_float_table);
        addAllowedBrickField(BrickField.FLOAT_ARRAY, R.id.brick_table_to_float_float);
    }

    public TableToFloatBrick(String value, String val2) {
        this(new Formula(value), new Formula(val2));
    }

    public TableToFloatBrick(Formula formula, Formula form2) {
        this();
        setFormulaWithBrickField(BrickField.TABLE_NAME, formula);
        setFormulaWithBrickField(BrickField.FLOAT_ARRAY, form2);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_table_to_float;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createTableToFloat(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TABLE_NAME), getFormulaWithBrickField(BrickField.FLOAT_ARRAY)));
    }
}
