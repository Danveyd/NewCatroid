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
import org.catrobat.catroid.formulaeditor.UserVariable;

public class BigAskBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public BigAskBrick() {
        addAllowedBrickField(BrickField.ASK_QUESTION, R.id.brick_big_ask_question);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_big_ask_clue);
        addAllowedBrickField(BrickField.OK, R.id.brick_big_ask_ok);
        addAllowedBrickField(BrickField.CANEL, R.id.brick_big_ask_canel);
        addAllowedBrickField(BrickField.DEFAULT, R.id.brick_big_ask_default);
    }

    public BigAskBrick(String questionText, String value2, String value3, String value4, String value5) {
        this(new Formula(questionText), new Formula(value2), new Formula(value3), new Formula(value4), new Formula(value5));
    }

    public BigAskBrick(Formula questionFormula, Formula value2, Formula value3, Formula value4, Formula value5, UserVariable answerVariable) {
        this(questionFormula, value2, value3, value4, value5);
        userVariable = answerVariable;
    }

    public BigAskBrick(Formula questionFormula, Formula value2, Formula value3, Formula value4, Formula value5) {
        this();
        setFormulaWithBrickField(BrickField.ASK_QUESTION, questionFormula);
        setFormulaWithBrickField(BrickField.TEXT, value2);
        setFormulaWithBrickField(BrickField.OK, value3);
        setFormulaWithBrickField(BrickField.CANEL, value4);
        setFormulaWithBrickField(BrickField.DEFAULT, value5);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_big_ask;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_big_ask_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createBigAskAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.ASK_QUESTION),
                        getFormulaWithBrickField(BrickField.TEXT),
                        getFormulaWithBrickField(BrickField.OK),
                        getFormulaWithBrickField(BrickField.CANEL),
                        getFormulaWithBrickField(BrickField.DEFAULT),
                        userVariable));
    }
}
