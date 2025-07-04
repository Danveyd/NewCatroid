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

public class NormalizeImgBrick extends FormulaBrick {

    private static final long serialVersionUID = 1L;

    public NormalizeImgBrick() {
        addAllowedBrickField(BrickField.FILE, R.id.brick_normalize_img_file);
        addAllowedBrickField(BrickField.RED, R.id.brick_normalize_img_r);
        addAllowedBrickField(BrickField.GREEN, R.id.brick_normalize_img_g);
        addAllowedBrickField(BrickField.BLUE, R.id.brick_normalize_img_b);
    }

    public NormalizeImgBrick(String val1, String val2, String val3, String val4) {
        this(new Formula(val1), new Formula(val2), new Formula(val3), new Formula(val4));
    }

    public NormalizeImgBrick(Formula code, Formula r, Formula g, Formula b) {
        this();
        setFormulaWithBrickField(BrickField.FILE, code);
        setFormulaWithBrickField(BrickField.RED, r);
        setFormulaWithBrickField(BrickField.GREEN, g);
        setFormulaWithBrickField(BrickField.BLUE, b);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_normalize_img;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createNormalizeImg(sprite, sequence,
                getFormulaWithBrickField(BrickField.FILE), getFormulaWithBrickField(BrickField.RED), getFormulaWithBrickField(BrickField.GREEN), getFormulaWithBrickField(BrickField.BLUE)));
    }

}
