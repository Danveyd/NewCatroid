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

public class SetParticleBufferRenderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int modeSelection = 0;

    public SetParticleBufferRenderBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_particle_buffer_instance_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_particle_buffer_name);
    }

    public SetParticleBufferRenderBrick(String instanceId, String bufferName, int mode) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        setFormulaWithBrickField(BrickField.TEXT, new Formula(bufferName));
        this.modeSelection = mode;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_particle_buffer_render;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner modeSpinner = view.findViewById(R.id.brick_particle_buffer_mode_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.particle_buffer_modes, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        modeSpinner.setSelection(modeSelection);
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetParticleBufferRenderAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.TEXT),
                modeSelection
        ));
    }
}
