package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PtOpBrick extends FormulaBrick {
    private int opSelection = 0;

    public PtOpBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_pt_op_res);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_pt_op_a);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_pt_op_b);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_pt_op;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Spinner spinner = view.findViewById(R.id.brick_pt_op_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.pt_ops_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int safeSelection = opSelection;
        if (safeSelection < 0 || safeSelection >= adapter.getCount()) {
            safeSelection = 0;
        }
        spinner.setSelection(safeSelection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                opSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        String[] ops = CatroidApplication.getAppContext().getResources().getStringArray(R.array.pt_ops_array);
        int safeSelection = opSelection;
        if (safeSelection < 0 || safeSelection >= ops.length) {
            safeSelection = 0;
        }
        sequence.addAction(sprite.getActionFactory().createPtOpAction( sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3),
                ops[safeSelection]
        ));
    }
}