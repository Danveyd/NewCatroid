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

public class CreateVideoBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateVideoBrick() {
        addAllowedBrickField(Brick.BrickField.NAME, R.id.create_video_name);
        addAllowedBrickField(Brick.BrickField.FILE, R.id.create_video_file);
        addAllowedBrickField(Brick.BrickField.POSX, R.id.create_video_x);
        addAllowedBrickField(Brick.BrickField.POSY, R.id.create_video_y);
        addAllowedBrickField(Brick.BrickField.WIDTH, R.id.create_video_width);
        addAllowedBrickField(Brick.BrickField.HEIGHT, R.id.create_video_height);
        addAllowedBrickField(BrickField.LOOPED, R.id.create_video_loop);
        addAllowedBrickField(BrickField.CONTROLS, R.id.create_video_control);
    }

    public CreateVideoBrick(String name, String file, Integer x, Integer y, Integer width, Integer height, Integer looped, Integer controls) {
        this(new Formula(name), new Formula(file), new Formula(x), new Formula(y), new Formula(width), new Formula(height), new Formula(looped), new Formula(controls));
    }

    public CreateVideoBrick(Formula name, Formula file, Formula x, Formula y, Formula width, Formula height, Formula looped, Formula controls) {
        this();
        setFormulaWithBrickField(BrickField.NAME, name);
        setFormulaWithBrickField(BrickField.FILE, file);
        setFormulaWithBrickField(BrickField.POSX, x);
        setFormulaWithBrickField(BrickField.POSY, y);
        setFormulaWithBrickField(BrickField.WIDTH, width);
        setFormulaWithBrickField(BrickField.HEIGHT, height);
        setFormulaWithBrickField(BrickField.LOOPED, looped);
        setFormulaWithBrickField(BrickField.CONTROLS, controls);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_video;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .videoAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.FILE), getFormulaWithBrickField(BrickField.POSX), getFormulaWithBrickField(BrickField.POSY), getFormulaWithBrickField(BrickField.WIDTH), getFormulaWithBrickField(BrickField.HEIGHT),
                        getFormulaWithBrickField(BrickField.LOOPED), getFormulaWithBrickField(BrickField.CONTROLS)));
    }
}
