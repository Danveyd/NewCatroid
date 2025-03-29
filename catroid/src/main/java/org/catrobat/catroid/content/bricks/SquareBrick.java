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

import static org.catrobat.catroid.content.bricks.Brick.BrickField.BORDERS;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.COLOR;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.HEIGHT;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.NAME;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.ROTATION;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.TRANSPARENCY;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.WIDTH;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.X_POSITION;
import static org.catrobat.catroid.content.bricks.Brick.BrickField.Y_POSITION;

public class SquareBrick extends VisualPlacementBrick {

    private static final long serialVersionUID = 1L;

    public SquareBrick() {
        addAllowedBrickField(X_POSITION, R.id.brick_square_edit_x);
        addAllowedBrickField(Y_POSITION, R.id.brick_square_edit_y);
        addAllowedBrickField(WIDTH, R.id.brick_square_edit_w);
        addAllowedBrickField(HEIGHT, R.id.brick_square_edit_h);
        addAllowedBrickField(NAME, R.id.brick_square_edit_name);
        addAllowedBrickField(COLOR, R.id.brick_square_edit_color);
        addAllowedBrickField(TRANSPARENCY, R.id.brick_square_edit_trans);
        addAllowedBrickField(ROTATION, R.id.brick_square_edit_rot);
        addAllowedBrickField(BORDERS, R.id.brick_square_edit_borders);
    }

    public SquareBrick(String name, String color, float x, float y, float w, float h, float trans, float rot, float borders) {
        this(new Formula(name), new Formula(color), new Formula(x), new Formula(y), new Formula(w), new Formula(h), new Formula(trans), new Formula(rot), new Formula(borders));
    }

    public SquareBrick(Formula name, Formula color, Formula x, Formula y, Formula w, Formula h, Formula trans, Formula rot, Formula borders) {
        this();
        setFormulaWithBrickField(X_POSITION, x);
        setFormulaWithBrickField(Y_POSITION, y);
        setFormulaWithBrickField(WIDTH, w);
        setFormulaWithBrickField(HEIGHT, h);
        setFormulaWithBrickField(NAME, name);
        setFormulaWithBrickField(COLOR, color);
        setFormulaWithBrickField(TRANSPARENCY, trans);
        setFormulaWithBrickField(ROTATION, rot);
        setFormulaWithBrickField(BORDERS, borders);
    }

    @Override
    public BrickField getDefaultBrickField() {
        return X_POSITION;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_square;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSquareAction(sprite, sequence,
                getFormulaWithBrickField(NAME),
                getFormulaWithBrickField(COLOR),
                getFormulaWithBrickField(X_POSITION),
                getFormulaWithBrickField(Y_POSITION),
                getFormulaWithBrickField(WIDTH),
                getFormulaWithBrickField(HEIGHT),
                getFormulaWithBrickField(TRANSPARENCY),
                getFormulaWithBrickField(ROTATION),
                getFormulaWithBrickField(BORDERS)));
    }

    @Override
    public BrickField getXBrickField() {
        return BrickField.X_POSITION;
    }

    @Override
    public BrickField getYBrickField() {
        return BrickField.Y_POSITION;
    }

    @Override
    public int getXEditTextId() {
        return R.id.brick_square_edit_x;
    }

    @Override
    public int getYEditTextId() {
        return R.id.brick_square_edit_y;
    }
}
