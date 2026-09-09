package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyShaderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int presetSelection = 0;

    public ApplyShaderBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_apply_shader_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_apply_shader_target);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_apply_shader_strength);
    }

    public ApplyShaderBrick(String effectId, int preset, String target, double strength) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(effectId));
        setFormulaWithBrickField(BrickField.TEXT, new Formula(target));
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(strength));
        this.presetSelection = preset;
    }

    public ApplyShaderBrick(Formula effectId, int preset, Formula target, Formula strength) {
        this();
        setFormulaWithBrickField(BrickField.NAME, effectId);
        setFormulaWithBrickField(BrickField.TEXT, target);
        setFormulaWithBrickField(BrickField.VALUE_1, strength);
        this.presetSelection = preset;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_apply_shader;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner presetSpinner = view.findViewById(R.id.brick_apply_shader_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.brick_shader_presets, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                presetSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        presetSpinner.setSelection(presetSelection);
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createApplyShaderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        presetSelection,
                        getFormulaWithBrickField(BrickField.TEXT),
                        getFormulaWithBrickField(BrickField.VALUE_1)
                ));
    }
}
