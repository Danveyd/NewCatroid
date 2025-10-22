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
    private int shapeSelection = 0; // 0: Box, 1: Sphere, 2: Capsule

    public SetPhysicsStateBrick() {
        // Добавляем поля для формул как обычно
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

        // Настройка спиннера состояний
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context, R.array.brick_physics_states, android.R.layout.simple_spinner_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(stateAdapter);

        // Настройка спиннера форм
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

        // Устанавливаем начальные значения
        stateSpinner.setSelection(stateSelection);
        shapeSpinner.setSelection(shapeSelection);
        updateVisibility();

        return view;
    }

    private void updateVisibility() {
        LinearLayout shapeLayout = view.findViewById(R.id.brick_set_physics_shape_layout);
        LinearLayout massLayout = view.findViewById(R.id.brick_set_physics_state_mass_layout);

        // Показываем выбор формы для Static и Dynamic
        shapeLayout.setVisibility(stateSelection == 1 || stateSelection == 2 ? View.VISIBLE : View.GONE);
        // Показываем массу только для Dynamic
        massLayout.setVisibility(stateSelection == 2 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetPhysicsStateAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        stateSelection,
                        shapeSelection, // Передаем новый параметр
                        getFormulaWithBrickField(BrickField.VALUE_2)
                ));
    }
}