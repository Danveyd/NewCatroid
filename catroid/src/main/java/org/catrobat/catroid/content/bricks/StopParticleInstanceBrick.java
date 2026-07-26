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

public class StopParticleInstanceBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int modeSelection = 0; // 0: Smooth, 1: Immediate

    public StopParticleInstanceBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_particle_stop_instance_id);
    }

    public StopParticleInstanceBrick(String instanceId, int modeSelection) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        this.modeSelection = modeSelection;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_stop_particle_instance;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner spinner = view.findViewById(R.id.brick_particle_stop_mode_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.particle_stop_modes, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinner.setSelection(modeSelection);
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createStopParticleInstanceAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                modeSelection
        ));
    }
}
