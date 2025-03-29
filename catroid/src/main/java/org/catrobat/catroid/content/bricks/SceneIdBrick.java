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

import android.util.Log;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class SceneIdBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SceneIdBrick() {
        addAllowedBrickField(BrickField.SCENE_ID, R.id.brick_scene_id_edit_text);
    }

    public SceneIdBrick(String value) {
        this(new Formula(value));
    }

    public SceneIdBrick(Formula formula) {
        this();
        setFormulaWithBrickField(BrickField.SCENE_ID, formula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_scene_id;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        Formula form = getFormulaWithBrickField(BrickField.SCENE_ID);
        try {
            Integer value = form.interpretInteger(scope);
            sequence.addAction(sprite.getActionFactory().createSceneIdAction(value, sprite));
        } catch (InterpretationException e) {
            Log.e("SceneId", e.toString());
        }
    }
}
