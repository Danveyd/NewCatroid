Данный проект - Ch-engine. Форк Catrobat/Catroid

Это универсальный конструктор приложений / игр с визуальным программированием через блоки

Вот основной Readme с описанием проекта:

<p align="center">
  <img src="assets/logo.jpg" alt="Ch-engine Logo" width="128"/>
</p>

<h1 align="center">Ch-engine</h1>

<p align="center">
  Расширенная версия <a href="https://github.com/Catrobat/Catroid">Catrobat/Catroid</a> — с поддержкой 3D, AI, Qemu, Python и др.
</p>

<p align="center">
  <a href="https://t.me/New_Catroid">Telegram</a> • 
  <a href="https://apps.rustore.ru/app/org.DanVexTeam.ChEngine">RuStore</a>  • 
  <a href="#license">License</a>
</p>

**Основные улучшения:**
*   **Расширенное 3D:** Полноценная поддержка 3D-объектов, PBR-материалов, постобработкой и встроенный редактор сцен (с ECS и иерархией).
*   **Интеграция с ИИ:** Возможность использовать нейросетевые модели (импорт `.onnx`) и доступ к API Gemini.
*   **Сетевые функции:** Готовые инструменты для работы с Firebase и локальным сервером.
*   **Интеграция с Git:** Возможность разрабатывать проекты вместе.
*   **Библиотеки:** Создание своих блоков и формул.
*   **Плагины:** Кастомизация основного интерфейса.
*   **Qemu:** Эмуляция x86_64 прямо через блоки. (ARM64)
*   **Python 3.12:** Полный Python 3.12, возможность устанавливать библиотеки через pip (ARM64)
*   **LunoScript:** Собственный язык, похожий на **Kotlin**, который дает огромный функционал (работа с Java обьектами, наследование от них и это все в рантайме). <a href="https://github.com/Danveyd/LunoScript">Документация LunoScript</a>
*   **Файлы проекта:** Каждый проект имеет свое изолированное хранилище для ресурсов. (так же имеется командная строка)
*   ...и множество других улучшений! (ОГРОООМНОЕ МНОЖЕСТВО, я просто не напишу столько)

# Теперь гайд по проекту #

я буду называть пути внутри синхронизированного проекта Android Studio. Я предпологаю. что вы уже настроили проект.

`res/values` - тут все глобальные значения: цвета, строки и др.

`res/values/strings` - языки, основные английский и русский. Английский обновлять обязательно, без него не будет работать. английский язык никак не обозначен, русский подписан ru

`res/layout` - все лайауты, тобеж xml файлы, где прописано как выглядят блоки, менюшки и др.

`assets` - ассеты. то что зеленое - тестовое, при релизном билде не будет включено (так везде)

`kotlin+java/org.catrobat.catroid/` - основные скрипты, тут будут все .java, .kt файлы.

`kotlin+java/org.catrobat.catroid/content` - основной контент (блоки, действия блоков и др.), там также есть много контроллеров (для микрофона, Gemini и все остальное, что я создавал)

`kotlin+java/org.catrobat.catroid/content/actions` - действия блоков, тобеж по сути их код.

`kotlin+java/org.catrobat.catroid/content/bricks` - классы блоков, они соединяют действия, лайауты и т.д. воедино.

`kotlin+java/org.catrobat.catroid/content/ActionFactory.java` - "Фабрика" блоков, тут мы берем дейтвие, запихиваем в него введенные значения и возвращаем готовое к выполнению действие.

`kotlin+java/org.catrobat.catroid/content/GlobalManager.kt` - Глобальный мэнеджер, можете использовать его как штуку для хранения глобальных переменных. сейчас там есть переменные stopSounds и saveScenes (если отключить stopSounds - звуки не будут останавливаться при переходе на новую сцену, а если отключить saveScenes - сцены не будут сохраняться. иногда полезно)

`kotlin+java/org.catrobat.catroid/raptor` - Все для 3D, классы, компоненты. `ThreeDManager` - основной класс, отвечающий за 3D, `SceneManager` - его обертка более высокого уровня, постепенно я переношу всю работу на SceneManager, так как он лучше (система компонентов, сцен и т.д.)

`kotlin+java/org.catrobat.catroid/editor` - 3D редактор, все классы для него - тут.

`kotlin+java/org.catrobat.catroid/utils/lunoscript` - Все исхоники LunoScript, в Interpreter находятся обьявления всех встроенных функций.


# гайд на добавление какого-либо блока:
о файлах проекта: в проектах есть свое отдельное хранилище - файлы проекта. туда пользователь может класть любые файлы. например загрузить все для пайтона, в коде подключить это в окружение и запустить того же телеграм бота.
получить файл можно через scope?.project?.getFile(String)

а теперь покажу как добавлять блоки

1. создать Action
   вот пример
   AddEditAction.kt
```kotlin
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

package org.catrobat.catroid.content.actions

import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.NewDialogManager

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList

class AddEditAction() : TemporalAction() {
var scope: Scope? = null
var name: Formula? = null
var text: Formula? = null

    override fun update(percent: Float) {
        val name_str = name?.interpretString(scope) ?: "myDialog"
        val title_str = text?.interpretString(scope) ?: ""

        NewDialogManager.addEditText(name_str, title_str)
    }
}
```

2. добавляем переводы (я использую только русский, английский и бразильский португальский)
```xml
   <string formatted="false" name="dialog_default">Default text</string>
   <string formatted="false" name="dialog_positive">Add button \"Yes\"</string>
   <string formatted="false" name="dialog_negative">Add button \"No\"</string>
   <string formatted="false" name="dialog_neutral">Add button \"Later\"</string>
   <string formatted="false" name="dialog_edit">Add edit text</string>
   <string formatted="false" name="dialog_radio">Add radio</string>

<string formatted="false" name="dialog_default">Текст по умолчанию</string>
<string formatted="false" name="dialog_positive">Добавить кнопку \"Да\"</string>
<string formatted="false" name="dialog_negative">Добавить кнопку \"Нет\"</string>
<string formatted="false" name="dialog_neutral">Добавить кнопку \"Позже\"</string>
<string formatted="false" name="dialog_edit">Добавить поле ввода</string>
<string formatted="false" name="dialog_radio">Добавить выбор</string>
```

ВАЖНО: текст в блоке должен быть максимально коротким, иначе не влезет на экран

3. добавляем лайаут
   brick_add_edit.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
  ~ Catroid: An on-device visual programming system for Android devices
  ~ Copyright (C) 2010-2022 The Catrobat Team
  ~ (<http://developer.catrobat.org/credits>)
  ~
  ~ This program is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU Affero General Public License as
  ~ published by the Free Software Foundation, either version 3 of the
  ~ License, or (at your option) any later version.
  ~
  ~ An additional term exception under section 7 of the GNU Affero
  ~ General Public License, version 3, is available at
  ~ http://developer.catrobat.org/license_additional_term
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  ~ GNU Affero General Public License for more details.
  ~
  ~ You should have received a copy of the GNU Affero General Public License
  ~ along with this program.  If not, see <http://www.gnu.org/licenses/>.
  -->

<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
xmlns:app="http://schemas.android.com/apk/res-auto"
android:layout_width="match_parent"
android:layout_height="wrap_content"
android:gravity="center_vertical"
android:orientation="horizontal">

    <CheckBox
        android:id="@+id/brick_checkbox"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:visibility="gone" />
*вот этот чекбокс очень важен! без него - вылеты

    <org.catrobat.catroid.ui.BrickLayout
        android:id="@+id/brick_add_edit_layout"
        style="@style/BrickContainer.Look.Medium">

        <include layout="@layout/icon_brick_category_look" />

        <TextView
            style="@style/BrickText.SingleLine"
            android:text="@string/dialog_edit" />

        <TextView
            style="@style/BrickText.SingleLine"
            android:text="@string/dialog_name"
            app:layout_newLine="true"/>

        <TextView
            android:id="@+id/brick_add_edit_edit_name"
            style="@style/BrickEditText" />

        <TextView
            style="@style/BrickText.SingleLine"
            android:text="@string/dialog_default"
            app:layout_newLine="true"/>

        <TextView
            android:id="@+id/brick_add_edit_edit_text"
            style="@style/BrickEditText" />
    </org.catrobat.catroid.ui.BrickLayout>

</LinearLayout>
```

*в качестве категории и иконки используй категорию, которую я скажу, а размер такой:
1 параметр: Small
2-3 параметра: Medium
4 и более - Big

так же еще советую каждый параметр ставить на новую строчку. чтобы было:
Value1 "ввод"
Value2 "ввод2"
Value3 "ввод3"

ну ты понял.


4. добавляем в ActionFactory
```java
   public Action createEditAction(Sprite sprite, SequenceAction sequence,
       Formula name, Formula text) {
       AddEditAction action = action(AddEditAction.class);
       Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
       action.setScope(scope);
       action.setName(name);
       action.setText(text);
       return action;
   }
```

5. создаем финальный Brick
   AddEditBrick.java
```java
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

public class AddEditBrick extends FormulaBrick {
private static final long serialVersionUID = 1L;

    public AddEditBrick() {
        addAllowedBrickField(Brick.BrickField.NAME, R.id.brick_add_edit_edit_name);
        addAllowedBrickField(Brick.BrickField.TEXT, R.id.brick_add_edit_edit_text);
    }

    public AddEditBrick(String value, String value2) {
        this(new Formula(value), new Formula(value2));
    }

    public AddEditBrick(Formula formula, Formula formula2) {
        this();
        setFormulaWithBrickField(BrickField.NAME, formula);
        setFormulaWithBrickField(BrickField.TEXT, formula2);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_add_edit;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createEditAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.TEXT)));
    }
}
```
*обязательно создай конструктор, чтобы можно было создать блок из обычных строк / чисел, а не только из формул.

6. Далее мы добавляем описание блока в BrickInfo (нужно написать 3 описания: на русском, английском и бразильском португальском):
   add(AddEditBrick.class, "Добавляет поле ввода в диалог (возвращает: написанное значение)");


7. добавляем в XStreamSerializer:
   xstream.alias("brick", AddEditBrick.class);


8. Ну и в CategoryBricksFactory:
   looksBrickList.add(AddEditBrick("myDialog", "это текстовое поле"))

Так же можно добавлять, к примеру, выпадающие списки как тут:
```java
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetPhysicsStateBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int stateSelection = 0; // 0: None, 1: Static, 2: Dynamic
    private int shapeSelection = 0; // 0: Box, 1: Sphere, 2: Capsule

    public SetPhysicsStateBrick() {

        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_physics_state_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_physics_state_mass_value);
    }

    public SetPhysicsStateBrick(String objectId, int state, int shape, double mass) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(mass));
        this.stateSelection = state;
        this.shapeSelection = shape;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_physics_state;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner stateSpinner = view.findViewById(R.id.brick_set_physics_state_spinner);
        Spinner shapeSpinner = view.findViewById(R.id.brick_set_physics_shape_spinner);
        LinearLayout shapeLayout = view.findViewById(R.id.brick_set_physics_shape_layout);
        LinearLayout massLayout = view.findViewById(R.id.brick_set_physics_state_mass_layout);

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context, R.array.brick_physics_states, android.R.layout.simple_spinner_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(stateAdapter);

        ArrayAdapter<CharSequence> shapeAdapter = ArrayAdapter.createFromResource(context, R.array.brick_physics_shapes, android.R.layout.simple_spinner_item);
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shapeSpinner.setAdapter(shapeAdapter);

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stateSelection = position;
                updateVisibility();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        shapeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                shapeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        stateSpinner.setSelection(stateSelection);
        shapeSpinner.setSelection(shapeSelection);
        updateVisibility();

        return view;
    }

    private void updateVisibility() {
        LinearLayout shapeLayout = view.findViewById(R.id.brick_set_physics_shape_layout);
        LinearLayout massLayout = view.findViewById(R.id.brick_set_physics_state_mass_layout);

        shapeLayout.setVisibility(stateSelection == 1 || stateSelection == 2 ? View.VISIBLE : View.GONE);
        massLayout.setVisibility(stateSelection == 2 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetPhysicsStateAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        stateSelection,
                        shapeSelection,
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}

```

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center_vertical"
    android:orientation="horizontal">

    <CheckBox
        android:id="@+id/brick_checkbox"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone" />

    <org.catrobat.catroid.ui.BrickLayout
        android:id="@+id/brick_set_physics_state_layout"
        style="@style/BrickContainer.Threed.Big">

        <include layout="@layout/icon_brick_category_threed" />

        <TextView
            style="@style/BrickText.SingleLine"
            android:text="@string/brick_set_physics_state_of" />
        <TextView
            android:id="@+id/brick_set_physics_state_id"
            style="@style/BrickEditText"
            app:layout_newLine="true"/>

        <TextView
            style="@style/BrickText.SingleLine"
            android:text="@string/brick_set_physics_state_to"
            app:layout_newLine="true" />
        <Spinner
            android:id="@+id/brick_set_physics_state_spinner"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textDirection="locale" />

        <LinearLayout
            android:id="@+id/brick_set_physics_shape_layout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            app:layout_newLine="true"
            android:visibility="gone">

            <TextView
                style="@style/BrickText.SingleLine"
                android:text="@string/brick_set_physics_state_shape" />
            <Spinner
                android:id="@+id/brick_set_physics_shape_spinner"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/brick_set_physics_state_mass_layout"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            app:layout_newLine="true"
            android:visibility="gone">

            <TextView
                style="@style/BrickText.SingleLine"
                android:text="@string/brick_set_physics_state_mass" />
            <TextView
                android:id="@+id/brick_set_physics_state_mass_value"
                style="@style/BrickEditText" />
        </LinearLayout>

    </org.catrobat.catroid.ui.BrickLayout>
</LinearLayout>
```

```java
package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

public class SetPhysicsStateAction extends TemporalAction {
    public Scope scope;
    public Formula objectId;
    public int stateSelection; // 0: None, 1: Static, 2: Dynamic
    public int shapeSelection; // 0: Box, 1: Sphere, 2: Capsule
    public Formula mass;

    @Override
    protected void update(float percent) {
        var threeDManager = StageActivity.getActiveStageListener().getThreeDManager();
        if (threeDManager == null) return;

        try {
            String id = objectId.interpretString(scope);
            if (id.isEmpty()) return;

            float m = mass.interpretFloat(scope);

            ThreeDManager.PhysicsState state;
            switch (stateSelection) {
                case 0: state = ThreeDManager.PhysicsState.NONE; break;
                case 1: state = ThreeDManager.PhysicsState.STATIC; break;
                case 2: state = ThreeDManager.PhysicsState.DYNAMIC; break;
                case 3: state = ThreeDManager.PhysicsState.MESH_STATIC; break;
                case 4: state = ThreeDManager.PhysicsState.KINEMATIC; break;
                default: return;
            }

            ThreeDManager.PhysicsShape shape;
            switch (shapeSelection) {
                case 1: shape = ThreeDManager.PhysicsShape.SPHERE; break;
                case 2: shape = ThreeDManager.PhysicsShape.CAPSULE; break;
                case 0:
                default: shape = ThreeDManager.PhysicsShape.BOX; break;
            }

            threeDManager.setPhysicsState(id, state, shape, m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```



# Гайд как добавлять формулы:
Вот как добавлять формулы:
1. добавляем переводы (русский и английский)
```xml
   <string name="formula_pt_argmax" formatted="false">PT_argmax</string>
```
*пробелы заменяем на нижние подчеркивания

2. в тех же переводах делаем строку параметров:
```xml
   <string name="formula_pt_argmax_param" formatted="false">(\'tensor\')</string>
```

3. добавляем в Functions.java:
```java   
  package org.catrobat.catroid.formulaeditor;

import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.utils.EnumUtils;

import java.util.EnumSet;

@LunoClass
public enum Functions {

	SIN, COS, TAN, LN, LOG, SQRT, RAND, ROUND, ROUNDTO, ABS, PI, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, POWER, FLOOR, CEIL,
	MAX,
	MIN, TRUE, FALSE, LENGTH,
	LETTER, SUBTEXT, FILE, TO_HEX, TO_DEC, CLAMP, DISTAN, UPPER, LOWER, REVERSE, VAR, VARNAME, VARVALUE, RANDOM_STR, REPLACE, CONTAINS_STR, REPEAT, TABLE_X, TABLE_Y, TABLE_ELEMENT, TABLE_JOIN, JOIN, JOIN3, DISTANCE, JOINNUMBER, REGEX, LIST_ITEM, CONTAINS, INDEX_OF_ITEM, NUMBER_OF_ITEMS,
	LUA, FLOATARRAY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT, VIDEO_PLAYING, VIDEO_TIME, JSON_GET, JSON_SET, JSON_IS_VALID,
	ARDUINOANALOG,
	ARDUINODIGITAL, RASPIDIGITAL,
	MULTI_FINGER_X, MULTI_FINGER_Y, MULTI_FINGER_TOUCHED, INDEX_CURRENT_TOUCH, COLLIDES_WITH_COLOR,

	COLOR_TOUCHES_COLOR, COLOR_AT_XY, COLOR_EQUALS_COLOR, TEXT_BLOCK_X, TEXT_BLOCK_Y,
	TEXT_BLOCK_SIZE, TEXT_BLOCK_FROM_CAMERA, TEXT_BLOCK_LANGUAGE_FROM_CAMERA, IF_THEN_ELSE, FLATTEN, CONNECT, FIND, TOUCHES_OBJECT_BY_NAME,

	// -- 3D ---
	GET_3D_POSITION_X,
	GET_3D_POSITION_Y,
	GET_3D_POSITION_Z,
	GET_3D_ROTATION_YAW,
	GET_3D_ROTATION_PITCH,
	GET_3D_ROTATION_ROLL,
	GET_3D_SCALE_X,
	GET_3D_SCALE_Y,
	GET_3D_SCALE_Z,
	GET_3D_DISTANCE,
	GET_DIRECTION_X,
	GET_DIRECTION_Y,
	GET_ANGLE,
	GET_RAY_DISTANCE,
	GET_RAY_HIT_OBJECT,
	GET_CAMERA_POS_X, GET_CAMERA_POS_Y, GET_CAMERA_POS_Z,
	GET_CAMERA_DIR_X, GET_CAMERA_DIR_Y, GET_CAMERA_DIR_Z,
	GET_CAMERA_ROTATION_YAW, GET_CAMERA_ROTATION_PITCH, GET_CAMERA_ROTATION_ROLL,
	GET_3D_VELOCITY_X, GET_3D_VELOCITY_Y, GET_3D_VELOCITY_Z,
	OBJECT_TOUCHES_OBJECT, OBJECT_INTERSECTS_OBJECT,
	GET_RAY_HIT_X, GET_RAY_HIT_Y, GET_RAY_HIT_Z, GET_RAY_HIT_NORMAL_X, GET_RAY_HIT_NORMAL_Y, GET_RAY_HIT_NORMAL_Z, RAY_DID_HIT,

	ID_OF_DETECTED_OBJECT, OBJECT_WITH_ID_VISIBLE,
	IS_MOUSE_BUTTON_DOWN,
	RAY_DID_HIT2,
	RAY_HIT_SPRITE_NAME,
	RAY_HIT_X,
	RAY_HIT_Y,
	RAY_HIT_DISTANCE, FILES_PATH, ALL_FILES, FILE_SIZE, DELTA, COLLISION_LIST, INTERSECT_LIST, PT_ARGMAX, PT_VALUE, PT_VALUEND, PT_SHAPE, PT_DUMP, PT_TOTALSIZE;

	private static final String TAG = Functions.class.getSimpleName();
	public static EnumSet<Functions> TEXT = EnumSet.of(INTERSECT_LIST, COLLISION_LIST, DELTA, LENGTH, LETTER, SUBTEXT, CLAMP, TO_HEX, TO_DEC, DISTAN, UPPER, LOWER, REVERSE, VAR, VARNAME, VARVALUE, JOIN, JOIN3, REPLACE, CONTAINS_STR, REPEAT, RANDOM_STR, JOINNUMBER,
			REGEX, TABLE_X, TABLE_Y, TABLE_ELEMENT, TABLE_JOIN, FLOATARRAY, LUA, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT, VIDEO_PLAYING, VIDEO_TIME, FILE, FILES_PATH, ALL_FILES, FILE_SIZE,
			GET_3D_POSITION_X, GET_3D_POSITION_Y, GET_3D_POSITION_Z, GET_3D_ROTATION_YAW, GET_3D_ROTATION_PITCH, GET_3D_ROTATION_ROLL, GET_3D_SCALE_X, GET_3D_SCALE_Y, GET_3D_SCALE_Z, GET_3D_DISTANCE, GET_DIRECTION_X, GET_DIRECTION_Y, GET_ANGLE,
			GET_RAY_DISTANCE, GET_RAY_HIT_OBJECT, GET_CAMERA_POS_X, GET_CAMERA_POS_Y, GET_CAMERA_POS_Z, GET_CAMERA_DIR_X, GET_CAMERA_DIR_Y, GET_CAMERA_DIR_Z,
			GET_3D_VELOCITY_X, GET_3D_VELOCITY_Y, GET_3D_VELOCITY_Z, GET_CAMERA_ROTATION_YAW, GET_CAMERA_ROTATION_PITCH, GET_CAMERA_ROTATION_ROLL, OBJECT_TOUCHES_OBJECT, JSON_GET, JSON_SET, JSON_IS_VALID, GET_RAY_HIT_X, GET_RAY_HIT_Y, GET_RAY_HIT_Z, GET_RAY_HIT_NORMAL_X, GET_RAY_HIT_NORMAL_Y, GET_RAY_HIT_NORMAL_Z,
			IS_MOUSE_BUTTON_DOWN, OBJECT_INTERSECTS_OBJECT, PT_ARGMAX, PT_VALUE, PT_VALUEND, PT_SHAPE, PT_DUMP, PT_TOTALSIZE);
	public static final EnumSet<Functions> LIST = EnumSet.of(LIST_ITEM, CONTAINS, INDEX_OF_ITEM,
			NUMBER_OF_ITEMS, FLATTEN, CONNECT, FIND);
	public static final EnumSet<Functions> BOOLEAN = EnumSet.of(TRUE, FALSE, CONTAINS,
			MULTI_FINGER_TOUCHED, COLLIDES_WITH_COLOR, COLOR_TOUCHES_COLOR, COLOR_EQUALS_COLOR, RAY_DID_HIT, OBJECT_INTERSECTS_OBJECT, OBJECT_TOUCHES_OBJECT);

	public static boolean isFunction(String value) {
		return EnumUtils.isValidEnum(Functions.class, value);
	}

	public static boolean isBoolean(Functions function) {
		return BOOLEAN.contains(function);
	}

	public static Functions getFunctionByValue(String value) {
		return EnumUtils.getEnum(Functions.class, value);
	}

	public static void addText(Functions func) {
		TEXT.add(func);
	}
}
```


4. добавляем в InternFormulaKeyboardAdapter (там огромный switch case)
```java
   case R.id.formula_editor_keyboard_4:
   return buildNumber("4");
   case R.id.formula_editor_keyboard_5:
   return buildNumber("5");
   case R.id.formula_editor_keyboard_6:
   return buildNumber("6");
   case R.id.formula_editor_keyboard_7:
   return buildNumber("7");
   case R.id.formula_editor_keyboard_8:
   return buildNumber("8");
   case R.id.formula_editor_keyboard_9:
   return buildNumber("9");

   		case R.string.formula_editor_function_sin:
   			return buildSingleParameterFunction(Functions.SIN, NUMBER, "90");
   		case R.string.formula_editor_function_cos:
   			return buildSingleParameterFunction(Functions.COS, NUMBER, "360");
   		case R.string.formula_editor_function_tan:
   			return buildSingleParameterFunction(Functions.TAN, NUMBER, "45");
   		case R.string.formula_editor_function_ln:
   			return buildSingleParameterFunction(Functions.LN, NUMBER, "2.718281828459");
   		case R.string.formula_editor_function_log:
   			return buildSingleParameterFunction(Functions.LOG, NUMBER, "10");
   		case R.string.formula_editor_function_pi:
   			return buildFunctionWithoutParametersAndBrackets(Functions.PI);
   		case R.string.formula_editor_function_sqrt:
   			return buildSingleParameterFunction(Functions.SQRT, NUMBER, "4");
   		case R.string.formula_editor_function_rand:
   			return buildDoubleParameterFunction(Functions.RAND,
   					NUMBER, "1",
   					NUMBER, "6");
   		case R.string.formula_editor_function_abs:
   			return buildSingleParameterFunction(Functions.ABS, NUMBER, "1");
   		case R.string.formula_editor_function_round:
   			return buildSingleParameterFunction(Functions.ROUND, NUMBER, "1.6");
   		case R.string.formula_editor_function_roundto:
   			return buildDoubleParameterFunction(Functions.ROUNDTO, NUMBER, "123.456", NUMBER, "1");
   		case R.string.formula_editor_function_mod:
   			return buildDoubleParameterFunction(Functions.MOD,
   					NUMBER, "3",
   					NUMBER, "2");
   		case R.string.formula_editor_function_arcsin:
   			return buildSingleParameterFunction(Functions.ARCSIN, NUMBER, "0.5");
   		case R.string.formula_editor_function_arccos:
   			return buildSingleParameterFunction(Functions.ARCCOS, NUMBER, "0");
   		case R.string.formula_editor_function_arctan:
   			return buildSingleParameterFunction(Functions.ARCTAN, NUMBER, "1");
   		case R.string.formula_editor_function_arctan2:
   			return buildDoubleParameterFunctionWithNegativeValues(Functions.ARCTAN2,
   					true, NUMBER, "1",
   					false, NUMBER, "0");
   		case R.string.formula_editor_function_exp:
   			return buildSingleParameterFunction(Functions.EXP, NUMBER, "1");
   		case R.string.formula_editor_function_power:
   			return buildDoubleParameterFunction(Functions.POWER,
   					NUMBER, "2",
   					NUMBER, "3");
   		case R.string.formula_editor_function_floor:
   			return buildSingleParameterFunction(Functions.FLOOR, NUMBER, "0.7");
   		case R.string.formula_editor_function_ceil:
   			return buildSingleParameterFunction(Functions.CEIL, NUMBER, "0.3");
   		case R.string.formula_editor_function_max:
   			return buildDoubleParameterFunction(Functions.MAX,
   					NUMBER, "5",
   					NUMBER, "4");
   		case R.string.formula_editor_function_touches_object_by_name:
   			return buildSingleParameterFunction(Functions.TOUCHES_OBJECT_BY_NAME, STRING, "object name");
   		case R.string.formula_editor_function_min:
   			return buildDoubleParameterFunction(Functions.MIN,
   					NUMBER, "7",
   					NUMBER, "2");
   		case R.string.formula_editor_function_if_then_else:
   			return buildTripleParameterFunction(Functions.IF_THEN_ELSE,
   					FUNCTION_NAME, Functions.getFunctionByValue("FALSE").toString(),
   					NUMBER, "2",
   					NUMBER, "3");
   		case R.string.formula_editor_function_true:
   			return buildFunctionWithoutParametersAndBrackets(Functions.TRUE);
   		case R.string.formula_editor_function_false:
   			return buildFunctionWithoutParametersAndBrackets(Functions.FALSE);
   		case R.string.formula_editor_function_letter:
   			return buildDoubleParameterFunction(Functions.LETTER, NUMBER, "1",
   					STRING, "hello world");
   		case R.string.formula_editor_function_subtext:
   			return buildTripleParameterFunction(Functions.SUBTEXT, NUMBER, "3", NUMBER, "5",
   					STRING, "hello world");
   		case R.string.formula_editor_function_file:
   			return buildSingleParameterFunction(Functions.FILE, STRING, "variable.txt");
   		case R.string.formula_editor_function_files_path:
   			return buildFunctionWithoutParametersAndBrackets(Functions.FILES_PATH);
   		case R.string.formula_editor_function_all_files:
   			return buildFunctionWithoutParametersAndBrackets(Functions.ALL_FILES);
   		case R.string.formula_editor_function_file_size:
   			return buildSingleParameterFunction(Functions.FILE_SIZE, STRING, "variable.txt");
   		case R.string.formula_editor_function_lua:
   			return buildSingleParameterFunction(Functions.LUA, STRING, "return math.sqrt(25)");
   		case R.string.formula_editor_function_to_hex:
   			return buildSingleParameterFunction(Functions.TO_HEX, NUMBER, "175");
   		case R.string.formula_editor_function_to_dec:
   			return buildSingleParameterFunction(Functions.TO_DEC, STRING, "AF");
   		case R.string.formula_editor_function_random_str:
   			return buildSingleParameterFunction(Functions.RANDOM_STR, NUMBER, "15");
   		case R.string.formula_editor_function_repeat:
   			return buildDoubleParameterFunction(Functions.REPEAT, STRING, "повторение - признак безумия ", NUMBER, "10");
   		case R.string.formula_editor_function_replace:
   			return buildTripleParameterFunction(Functions.REPLACE, STRING, "pandas love bamboo", STRING, "love",
   					STRING, "eat");
   		case R.string.formula_editor_function_contains_str:
   			return buildDoubleParameterFunction(Functions.CONTAINS_STR, STRING, "bats are cutest!", STRING, "cute");
   		case R.string.formula_editor_function_distan:
   			return buildFourParameterFunction(Functions.DISTAN, NUMBER, "100", NUMBER, "200",
   					NUMBER, "-100", NUMBER, "-200");
   		case R.string.formula_editor_function_clamp:
   			return buildTripleParameterFunction(Functions.CLAMP, NUMBER, "-2", NUMBER, "2",
   					NUMBER, "10");
   		case R.string.formula_editor_function_upper:
   			return buildSingleParameterFunction(Functions.UPPER, STRING, "aboudna");
   		case R.string.formula_editor_function_lower:
   			return buildSingleParameterFunction(Functions.LOWER, STRING, "SIGMA");
   		case R.string.formula_editor_function_reverse:
   			return buildSingleParameterFunction(Functions.REVERSE, STRING,"olleH");
   		case R.string.formula_editor_function_var:
   			return buildSingleParameterFunction(Functions.VAR, STRING,"variable1");
   		case R.string.formula_editor_function_var_name:
   			return buildSingleParameterFunction(Functions.VARNAME, NUMBER,"0");
   		case R.string.formula_editor_function_var_value:
   			return buildSingleParameterFunction(Functions.VARVALUE, NUMBER,"0");
   		case R.string.formula_editor_function_table_x:
   			return buildSingleParameterFunction(Functions.TABLE_X, STRING,"myTable");
   		case R.string.view_x:
   			return buildSingleParameterFunction(Functions.VIEW_X, STRING,"myView");
   		case R.string.view_y:
   			return buildSingleParameterFunction(Functions.VIEW_Y, STRING,"myView");
   		case R.string.view_width:
   			return buildSingleParameterFunction(Functions.VIEW_WIDTH, STRING,"myView");
   		case R.string.view_height:
   			return buildSingleParameterFunction(Functions.VIEW_HEIGHT, STRING,"myView");
   		case R.string.is_video_playing:
   			return buildSingleParameterFunction(Functions.VIDEO_PLAYING, STRING,"myVideoPlayer");
   		case R.string.video_time:
   			return buildSingleParameterFunction(Functions.VIDEO_TIME, STRING,"myVideoPlayer");
   		case R.string.formula_3d_pos_x:
   			return buildSingleParameterFunction(Functions.GET_3D_POSITION_X, STRING, "myObject");
            case R.string.formula_pt_totalsize:
                return buildSingleParameterFunction(Functions.PT_TOTALSIZE, STRING, "tensor");
            case R.string.formula_pt_shape:
                return buildSingleParameterFunction(Functions.PT_SHAPE, STRING, "tensor");
            case R.string.formula_pt_dump:
                return buildSingleParameterFunction(Functions.PT_DUMP, STRING, "tensor");
            case R.string.formula_pt_argmax:
                return buildSingleParameterFunction(Functions.PT_ARGMAX, STRING, "tensor");
            case R.string.formula_pt_value:
                return buildDoubleParameterFunction(Functions.PT_VALUE, STRING, "tensor", NUMBER, "0");
            case R.string.formula_pt_valuend:
                return buildDoubleParameterFunction(Functions.PT_VALUEND, STRING, "tensor", STRING, "12,2");
            case R.string.formula_collision_list:
                return buildSingleParameterFunction(Functions.COLLISION_LIST, STRING, "myObject");
            case R.string.formula_intersect_list:
                return buildSingleParameterFunction(Functions.INTERSECT_LIST, STRING, "myObject");
            case R.string.formula_delta:
                return buildFunctionWithoutParametersAndBrackets(Functions.DELTA);
   		case R.string.formula_3d_touches:
   			return buildDoubleParameterFunction(Functions.OBJECT_TOUCHES_OBJECT, STRING, "object1", STRING, "object2");
   		case R.string.formula_3d_intersects:
   			return buildDoubleParameterFunction(Functions.OBJECT_INTERSECTS_OBJECT, STRING, "player", STRING, "trigger_zone");
   		case R.string.formula_3d_velo_x:
   			return buildSingleParameterFunction(Functions.GET_3D_VELOCITY_X, STRING, "myObject");
   		case R.string.formula_3d_velo_y:
   			return buildSingleParameterFunction(Functions.GET_3D_VELOCITY_Y, STRING, "myObject");
   		case R.string.formula_3d_velo_z:
   			return buildSingleParameterFunction(Functions.GET_3D_VELOCITY_Z, STRING, "myObject");
   		case R.string.formula_3d_pos_y:
```

5. в InternToExternGenerator
```java
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.CONNECT.name(), R.string.formula_editor_function_connect);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.FIND.name(), R.string.formula_editor_function_find);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.TABLE_X.name(), R.string.formula_editor_function_table_x);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.TABLE_Y.name(), R.string.formula_editor_function_table_y);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIEW_X.name(), R.string.view_x);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIEW_Y.name(), R.string.view_y);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIEW_WIDTH.name(), R.string.view_width);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIEW_HEIGHT.name(), R.string.view_height);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIDEO_PLAYING.name(), R.string.is_video_playing);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.VIDEO_TIME.name(), R.string.video_time);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.FLOATARRAY.name(), R.string.formula_editor_function_floatarray);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.TABLE_ELEMENT.name(), R.string.formula_editor_function_table_element);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.TABLE_JOIN.name(), R.string.formula_editor_function_table_join);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_DIRECTION_X.name(), R.string.formula_vector_dir_x);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_DIRECTION_Y.name(), R.string.formula_vector_dir_y);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_ANGLE.name(), R.string.formula_vector_angle);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_VALUE.name(), R.string.formula_pt_value);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_3D_POSITION_X.name(), R.string.formula_3d_pos_x);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_TOTALSIZE.name(), R.string.formula_pt_totalsize);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_DUMP.name(), R.string.formula_pt_dump);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_ARGMAX.name(), R.string.formula_pt_argmax);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_SHAPE.name(), R.string.pt_shape);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.PT_VALUEND.name(), R.string.formula_pt_valuend);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.INTERSECT_LIST.name(), R.string.formula_intersect_list);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.COLLISION_LIST.name(), R.string.formula_collision_list);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.DELTA.name(), R.string.formula_delta);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.OBJECT_TOUCHES_OBJECT.name(), R.string.formula_3d_touches);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.OBJECT_INTERSECTS_OBJECT.name(), R.string.formula_3d_intersects);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_CAMERA_ROTATION_YAW.name(), R.string.formula_cam_rot_yaw);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_CAMERA_ROTATION_ROLL.name(), R.string.formula_cam_rot_roll);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_CAMERA_ROTATION_PITCH.name(), R.string.formula_cam_rot_pitch);
   INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.put(Functions.GET_3D_POSITION_Y.name(), R.string.formula_3d_pos_y);
   ```


6. в CategoryListFragment (я добавил специально новые списки для нашего Fast2D)
```java
   private static final List<Integer> LIST_FUNCTIONS = asList(R.string.formula_editor_function_number_of_items,
   R.string.formula_editor_function_list_item, R.string.formula_editor_function_contains,
   R.string.formula_editor_function_index_of_item, R.string.formula_editor_function_flatten, R.string.formula_editor_function_connect, R.string.formula_editor_function_find);
   private static final List<Integer> LIST_PARAMS = asList(R.string.formula_editor_function_number_of_items_parameter,
   R.string.formula_editor_function_list_item_parameter,
   R.string.formula_editor_function_contains_parameter,
   R.string.formula_editor_function_index_of_item_parameter,
   R.string.formula_editor_function_flatten_parameter,
   R.string.formula_editor_function_connect_parameter,
   R.string.formula_editor_function_find_parameter);

   private static final List<Integer> PT_FUNCTIONS = asList(
   R.string.formula_pt_argmax, R.string.formula_pt_value, R.string.formula_pt_valuend,
   R.string.formula_pt_shape, R.string.formula_pt_dump, R.string.formula_pt_totalsize
   );
   private static final List<Integer> PT_PARAMS = asList(
   R.string.formula_pt_argmax_param, R.string.formula_pt_value_param, R.string.formula_pt_valuennd_param,
   R.string.formula_pt_shape_param, R.string.formula_pt_dump_param, R.string.formula_pt_totalsize_param
   );

   private static final List<Integer> F2D_FUNCTIONS = asList(

   );
   private static final List<Integer> F2D_PARAMS = asList(

   );
```


7. основной кейс в FormulaElement
```java
   case GET_DIRECTION_Y: {
   double angleDegrees = tryInterpretDoubleValue(arguments.get(0));
   return (double) com.badlogic.gdx.math.MathUtils.sinDeg((float) angleDegrees);
   }
   case GET_ANGLE: {
   double x = tryInterpretDoubleValue(arguments.get(0));
   double y = tryInterpretDoubleValue(arguments.get(1));
   return (double) com.badlogic.gdx.math.MathUtils.atan2Deg((float) y, (float) x);
   }
   case GET_3D_VELOCITY_X: {
   ThreeDManager manager = getThreeDManager();
   if (manager == null) return 0.0;
   String id = String.valueOf(arguments.get(0));
   return (double) manager.getVelocity(id).x;
   }
   case GET_3D_VELOCITY_Y: {
   ThreeDManager manager = getThreeDManager();
   if (manager == null) return 0.0;
   String id = String.valueOf(arguments.get(0));
   return (double) manager.getVelocity(id).y;
   }
   case GET_3D_VELOCITY_Z: {
   ThreeDManager manager = getThreeDManager();
   if (manager == null) return 0.0;
   String id = String.valueOf(arguments.get(0));
   return (double) manager.getVelocity(id).z;
   }
   case GET_3D_POSITION_X: {
   ThreeDManager manager = getThreeDManager();
   if (manager == null) return 0.0;
   String id = String.valueOf(arguments.get(0));
   Vector3 pos = manager.getPosition(id);
   return (pos != null) ? pos.x : 0.0;
   }
   case PT_ARGMAX: {
   return MLBridge.nativeArgMax(String.valueOf(arguments.get(0)));
   }
```
