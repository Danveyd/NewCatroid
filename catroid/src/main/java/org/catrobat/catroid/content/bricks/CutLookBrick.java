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

public class CutLookBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CutLookBrick() {
        addAllowedBrickField(BrickField.X1, R.id.brick_cut_look_fromx);
        addAllowedBrickField(BrickField.Y1, R.id.brick_cut_look_fromy);
        addAllowedBrickField(BrickField.X2, R.id.brick_cut_look_tox);
        addAllowedBrickField(BrickField.Y2, R.id.brick_cut_look_toy);
    }

    public CutLookBrick(Integer x1, Integer y1, Integer x2, Integer y2) {
        this(new Formula(x1), new Formula(y1), new Formula(x2), new Formula(y2));
    }

    public CutLookBrick(Formula x1, Formula y1, Formula x2, Formula y2) {
        this();
        setFormulaWithBrickField(BrickField.X1, x1);
        setFormulaWithBrickField(BrickField.Y1, y1);
        setFormulaWithBrickField(BrickField.X2, x2);
        setFormulaWithBrickField(BrickField.Y2, y2);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_cut_look;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCutLook(sprite, sequence,
                        getFormulaWithBrickField(BrickField.X1), getFormulaWithBrickField(BrickField.Y1), getFormulaWithBrickField(BrickField.X2), getFormulaWithBrickField(BrickField.Y2)));
    }
}
