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

public class AskGemini2Brick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public AskGemini2Brick() {
        addAllowedBrickField(BrickField.QUESTION, R.id.brick_ask_gemini2_edit_url);
        addAllowedBrickField(BrickField.MODEL, R.id.brick_ask_gemini2_edit_model);
    }

    public AskGemini2Brick(String val1, String val2) {
        this(new Formula(val1), new Formula(val2), null);
    }

    public AskGemini2Brick(Formula question, Formula model, UserVariable userVariable) {
        this();
        setFormulaWithBrickField(BrickField.QUESTION, question);
        setFormulaWithBrickField(BrickField.MODEL, model);
        this.userVariable = userVariable;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_ask_gemini2;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_ask_gemini2_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createAskGemini2Action(sprite, sequence,
                getFormulaWithBrickField(BrickField.QUESTION), getFormulaWithBrickField(BrickField.MODEL), userVariable));
    }

}
