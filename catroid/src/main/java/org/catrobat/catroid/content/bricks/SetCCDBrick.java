package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.AdapterViewOnItemSelectedListenerImpl;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetCCDBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    private int ccdSelection = 0; // 0 = enable, 1 = disable

    public SetCCDBrick() {
        addAllowedBrickField(BrickField.THREED_OBJECT_ID, R.id.brick_set_ccd_object_id);
    }

    public SetCCDBrick(String objectId, boolean isEnabled) {
        this();
        setFormulaWithBrickField(BrickField.THREED_OBJECT_ID, new Formula(objectId));
        this.ccdSelection = isEnabled ? 0 : 1;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_ccd;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner spinner = view.findViewById(R.id.brick_set_ccd_spinner);

        String[] options = {
                "Вкл.",
                "Выкл"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterViewOnItemSelectedListenerImpl(position -> {
            ccdSelection = position;
            return null;
        }));
        spinner.setSelection(ccdSelection);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        Formula objectIdFormula = getFormulaWithBrickField(BrickField.THREED_OBJECT_ID);
        Formula enabledFormula;
        if (ccdSelection == 0) {
            enabledFormula = new Formula(1.0);
        } else {
            enabledFormula = new Formula(0.0);
        }

        sequence.addAction(sprite.getActionFactory().createSetCCDAction(
                sprite,
                sequence,
                objectIdFormula,
                enabledFormula
        ));
    }
}