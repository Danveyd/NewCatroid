package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.CreateSpringConstraintAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateSpringConstraintBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public boolean springX = false;
    public boolean springY = true;
    public boolean springZ = false;

    public CreateSpringConstraintBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_spring_constraint_edit_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_spring_constraint_edit_a);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_spring_constraint_edit_b);

        // Pivots A
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_spring_constraint_edit_pax);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_spring_constraint_edit_pay);
        addAllowedBrickField(BrickField.VALUE_6, R.id.brick_spring_constraint_edit_paz);

        // Pivots B
        addAllowedBrickField(BrickField.VALUE_7, R.id.brick_spring_constraint_edit_pbx);
        addAllowedBrickField(BrickField.VALUE_8, R.id.brick_spring_constraint_edit_pby);
        addAllowedBrickField(BrickField.VALUE_9, R.id.brick_spring_constraint_edit_pbz);

        // Min Limits
        addAllowedBrickField(BrickField.VALUE_10, R.id.brick_spring_constraint_edit_min_x);
        addAllowedBrickField(BrickField.VALUE_11, R.id.brick_spring_constraint_edit_min_y);
        addAllowedBrickField(BrickField.VALUE_12, R.id.brick_spring_constraint_edit_min_z);

        // Max Limits
        addAllowedBrickField(BrickField.VALUE_13, R.id.brick_spring_constraint_edit_max_x);
        addAllowedBrickField(BrickField.VALUE_14, R.id.brick_spring_constraint_edit_max_y);
        addAllowedBrickField(BrickField.VALUE_15, R.id.brick_spring_constraint_edit_max_z);

        // Stiffness & Damping
        addAllowedBrickField(BrickField.VALUE_16, R.id.brick_spring_constraint_edit_stiffness);
        addAllowedBrickField(BrickField.VALUE_17, R.id.brick_spring_constraint_edit_damping);
    }

    public CreateSpringConstraintBrick(String constraintId, String objectA, String objectB,
                                       double pax, double pay, double paz,
                                       double pbx, double pby, double pbz,
                                       boolean sx, boolean sy, boolean sz,
                                       double minX, double minY, double minZ,
                                       double maxX, double maxY, double maxZ,
                                       double stiffness, double damping) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(constraintId));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(objectA));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(objectB));

        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(pax));
        setFormulaWithBrickField(BrickField.VALUE_5, new Formula(pay));
        setFormulaWithBrickField(BrickField.VALUE_6, new Formula(paz));

        setFormulaWithBrickField(BrickField.VALUE_7, new Formula(pbx));
        setFormulaWithBrickField(BrickField.VALUE_8, new Formula(pby));
        setFormulaWithBrickField(BrickField.VALUE_9, new Formula(pbz));

        this.springX = sx;
        this.springY = sy;
        this.springZ = sz;

        setFormulaWithBrickField(BrickField.VALUE_10, new Formula(minX));
        setFormulaWithBrickField(BrickField.VALUE_11, new Formula(minY));
        setFormulaWithBrickField(BrickField.VALUE_12, new Formula(minZ));

        setFormulaWithBrickField(BrickField.VALUE_13, new Formula(maxX));
        setFormulaWithBrickField(BrickField.VALUE_14, new Formula(maxY));
        setFormulaWithBrickField(BrickField.VALUE_15, new Formula(maxZ));

        setFormulaWithBrickField(BrickField.VALUE_16, new Formula(stiffness));
        setFormulaWithBrickField(BrickField.VALUE_17, new Formula(damping));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_spring_constraint;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        CheckBox cbX = view.findViewById(R.id.brick_spring_axis_x);
        CheckBox cbY = view.findViewById(R.id.brick_spring_axis_y);
        CheckBox cbZ = view.findViewById(R.id.brick_spring_axis_z);

        cbX.setChecked(springX);
        cbY.setChecked(springY);
        cbZ.setChecked(springZ);

        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            int id = buttonView.getId();
            if (id == R.id.brick_spring_axis_x) springX = isChecked;
            else if (id == R.id.brick_spring_axis_y) springY = isChecked;
            else if (id == R.id.brick_spring_axis_z) springZ = isChecked;
        };

        cbX.setOnCheckedChangeListener(listener);
        cbY.setOnCheckedChangeListener(listener);
        cbZ.setOnCheckedChangeListener(listener);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        CreateSpringConstraintAction action = (CreateSpringConstraintAction) sprite.getActionFactory()
                .createSpringConstraintAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.VALUE_5),
                        getFormulaWithBrickField(BrickField.VALUE_6),
                        getFormulaWithBrickField(BrickField.VALUE_7),
                        getFormulaWithBrickField(BrickField.VALUE_8),
                        getFormulaWithBrickField(BrickField.VALUE_9));

        action.setSpringX(springX);
        action.setSpringY(springY);
        action.setSpringZ(springZ);

        action.setMinX(getFormulaWithBrickField(BrickField.VALUE_10));
        action.setMinY(getFormulaWithBrickField(BrickField.VALUE_11));
        action.setMinZ(getFormulaWithBrickField(BrickField.VALUE_12));

        action.setMaxX(getFormulaWithBrickField(BrickField.VALUE_13));
        action.setMaxY(getFormulaWithBrickField(BrickField.VALUE_14));
        action.setMaxZ(getFormulaWithBrickField(BrickField.VALUE_15));

        action.setStiffness(getFormulaWithBrickField(BrickField.VALUE_16));
        action.setDamping(getFormulaWithBrickField(BrickField.VALUE_17));

        sequence.addAction(action);
    }
}
