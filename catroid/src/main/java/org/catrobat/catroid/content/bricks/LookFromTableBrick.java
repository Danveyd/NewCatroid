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

public class LookFromTableBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public LookFromTableBrick() {
        addAllowedBrickField(Brick.BrickField.RED, R.id.brick_look_from_table_edit_r);
        addAllowedBrickField(Brick.BrickField.GREEN, R.id.brick_look_from_table_edit_g);
        addAllowedBrickField(Brick.BrickField.BLUE, R.id.brick_look_from_table_edit_b);
        addAllowedBrickField(Brick.BrickField.ALPHA, R.id.brick_look_from_table_edit_a);
    }

    public LookFromTableBrick(String r, String g, String b, String a) {
        this(new Formula(r), new Formula(g), new Formula(b), new Formula(a));
    }

    public LookFromTableBrick(Formula r, Formula g, Formula b, Formula a) {
        this();
        setFormulaWithBrickField(BrickField.RED, r);
        setFormulaWithBrickField(BrickField.GREEN, g);
        setFormulaWithBrickField(BrickField.BLUE, b);
        setFormulaWithBrickField(BrickField.ALPHA, a);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_look_from_table;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createLookFromTableAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.RED), getFormulaWithBrickField(BrickField.GREEN), getFormulaWithBrickField(BrickField.BLUE), getFormulaWithBrickField(BrickField.ALPHA)));
    }
}
