package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class SetScreenOrientationBrick extends BrickBaseType {
    private static final long serialVersionUID = 1L;

    private int orientation = 0;

    @Override
    public int getViewResource() {
        return R.layout.brick_screen_orientation;
    }

    @Override
    public View getView(Context context) {
        View view = super.getView(context);
        Spinner spinner = view.findViewById(R.id.brick_screen_orientation_value);
        spinner.setAdapter(ArrayAdapter.createFromResource(
                context, R.array.screen_orientations, android.R.layout.simple_spinner_dropdown_item));
        spinner.setSelection(Math.max(0, Math.min(3, orientation)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                orientation = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetScreenOrientationAction(
                sprite, sequence, orientation));
    }
}
