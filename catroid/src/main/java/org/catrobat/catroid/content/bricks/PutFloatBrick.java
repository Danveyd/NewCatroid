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

public class PutFloatBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PutFloatBrick() {
        addAllowedBrickField(BrickField.FLOAT_ARRAY, R.id.brick_put_float_float);
        addAllowedBrickField(BrickField.VALUE, R.id.brick_put_float_value);
        addAllowedBrickField(BrickField.LOOK_INDEX, R.id.brick_put_float_index);
    }

    public PutFloatBrick(String value, Integer val2, Integer val3) {
        this(new Formula(value), new Formula(val2), new Formula(val3));
    }

    public PutFloatBrick(Formula formula, Formula form2, Formula form3) {
        this();
        setFormulaWithBrickField(BrickField.FLOAT_ARRAY, formula);
        setFormulaWithBrickField(BrickField.VALUE, form2);
        setFormulaWithBrickField(BrickField.LOOK_INDEX, form3);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_put_float;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPutFloat(sprite, sequence,
                        getFormulaWithBrickField(BrickField.FLOAT_ARRAY), getFormulaWithBrickField(BrickField.VALUE), getFormulaWithBrickField(BrickField.LOOK_INDEX)));
    }
}
