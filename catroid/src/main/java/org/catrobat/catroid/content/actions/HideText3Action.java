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

package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.utils.Array;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.stage.ShowTextActor;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

public class HideText3Action extends TemporalAction {

    private UserVariable variableToHide;

    private String name;
    private Sprite sprite;
    private Scope scope;
    private AndroidStringProvider androidStringProvider;

    @Override
    protected void begin() {
        if (scope == null) {
            Log.e("HideText", "Scope is null");
            return; // Прекращаем выполнение, если scope не установлен
        }
        
        variableToHide = new UserVariable(name);
        if (StageActivity.stageListener != null) {
            Array<Actor> stageActors = StageActivity.stageListener.getStage().getActors();
            ShowTextActor dummyActor = new ShowTextActor(true, new UserVariable("dummyActor"),
                    0, 0, 1.0f, null, sprite, androidStringProvider);
            for (Actor actor : stageActors) {
                if (actor.getClass().equals(dummyActor.getClass())) {
                    ShowTextActor showTextActor = (ShowTextActor) actor;
                    if (showTextActor.getVariableNameToCompare().equals(variableToHide.getName())
                            && showTextActor.getSprite().equals(sprite)) {
                        actor.remove();
                    }
                }
            }
        }
        variableToHide.setVisible(false);
    }

    @Override
    protected void update(float percent) {
    }

    public void setName(Formula name) {
        try {
            this.name = name.interpretString(this.scope);
        } catch (InterpretationException e) {
            // Логируем или обрабатываем исключение. Например, можно вывести сообщение об ошибке.
            //System.err.println("Ошибка интерпретации имени: " + e.getMessage());
            // Или можно установить имя в значение по умолчанию
            Log.e("HideText", "InterpretationException: " + e.getMessage());
            this.name = "defaultName"; // Замените на ваше значение по умолчанию
        }
    }


    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public void setAndroidStringProvider(AndroidStringProvider androidStringProvider) {
        this.androidStringProvider = androidStringProvider;
    }
}
