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
        final View brickView = super.getView(context);

        Spinner stateSpinner = brickView.findViewById(R.id.brick_set_physics_state_spinner);
        Spinner shapeSpinner = brickView.findViewById(R.id.brick_set_physics_shape_spinner);

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(context, R.array.brick_physics_states, android.R.layout.simple_spinner_item);
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(stateAdapter);

        ArrayAdapter<CharSequence> shapeAdapter = ArrayAdapter.createFromResource(context, R.array.brick_physics_shapes, android.R.layout.simple_spinner_item);
        shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shapeSpinner.setAdapter(shapeAdapter);

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                stateSelection = position;
                updateVisibility(brickView);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        shapeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                shapeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        stateSpinner.setSelection(stateSelection);
        shapeSpinner.setSelection(shapeSelection);
        updateVisibility(brickView);

        return brickView;
    }

    private void updateVisibility(View root) {
        if (root == null) {
            return;
        }
        LinearLayout shapeLayout = root.findViewById(R.id.brick_set_physics_shape_layout);
        LinearLayout massLayout = root.findViewById(R.id.brick_set_physics_state_mass_layout);

        if (shapeLayout == null || massLayout == null) {
            return;
        }
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
