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

public class SetParticlePropertyBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int propertySelection = 0;

    public SetParticlePropertyBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_particle_property_instance_id);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_particle_property_value);
    }

    public SetParticlePropertyBrick(String instanceId, int propertySelection, double value) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(instanceId));
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(value));
        this.propertySelection = propertySelection;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_particle_property;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner spinner = view.findViewById(R.id.brick_particle_property_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.particle_properties_array, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                propertySelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinner.setSelection(propertySelection);
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetParticlePropertyAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                propertySelection,
                getFormulaWithBrickField(BrickField.VALUE_1)
        ));
    }
}
