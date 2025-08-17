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
    private int stateSelection = 0; // 0: None, 1: Static, 2: Dynamic. По умолчанию "None"

    public SetPhysicsStateBrick() {
        // Добавляем поля для формул как обычно
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_physics_state_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_physics_state_mass_value);
    }

    public SetPhysicsStateBrick(String objectId, int selection, double mass) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(objectId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(mass));
        this.stateSelection = selection;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_physics_state;
    }

    @Override
    public View getView(Context context) {
        super.getView(context); // Важно вызвать, чтобы FormulaBrick настроил свои поля

        Spinner spinner = view.findViewById(R.id.brick_set_physics_state_spinner);
        LinearLayout massLayout = view.findViewById(R.id.brick_set_physics_state_mass_layout);

        // Настраиваем адаптер из ресурсов
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.brick_physics_states, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Устанавливаем слушатель для сохранения выбора и управления видимостью
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                stateSelection = position;
                // Показываем поле массы только если выбрано "Dynamic" (позиция 2)
                massLayout.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

// Убедитесь, что эта строка тоже обновлена
        spinner.setSelection(stateSelection);
        massLayout.setVisibility(stateSelection == 2 ? View.VISIBLE : View.GONE);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetPhysicsStateAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        stateSelection,
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}