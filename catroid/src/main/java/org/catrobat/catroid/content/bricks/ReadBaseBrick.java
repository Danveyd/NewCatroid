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

public class ReadBaseBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public ReadBaseBrick() {
        addAllowedBrickField(BrickField.FIREBASE_ID, R.id.brick_read_base_edit_base);
        addAllowedBrickField(BrickField.FIREBASE_KEY, R.id.brick_read_base_edit_key);
    }

    public ReadBaseBrick(String base, String key) {
        this(new Formula(base), new Formula(key));
    }

    public ReadBaseBrick(Formula base, Formula key, UserVariable userVariable) {
        this(base, key);
        this.userVariable = userVariable;
    }

    private ReadBaseBrick(Formula base, Formula key) {
        this();
        setFormulaWithBrickField(BrickField.FIREBASE_ID, base);
        setFormulaWithBrickField(BrickField.FIREBASE_KEY, key);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_read_base;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_read_base_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createReadBaseAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FIREBASE_ID), getFormulaWithBrickField(BrickField.FIREBASE_KEY), userVariable));
    }
}
