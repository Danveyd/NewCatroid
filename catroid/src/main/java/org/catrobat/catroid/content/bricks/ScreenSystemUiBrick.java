package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class ScreenSystemUiBrick extends BrickBaseType {
    private static final long serialVersionUID = 1L;

    private int visibilityAction = 1;
    private int target = 2;

    @Override
    public int getViewResource() {
        return R.layout.brick_screen_system_ui;
    }

    @Override
    public View getView(Context context) {
        View view = super.getView(context);
        Spinner actionSpinner = view.findViewById(R.id.brick_screen_ui_action);
        Spinner targetSpinner = view.findViewById(R.id.brick_screen_ui_target);

        actionSpinner.setAdapter(ArrayAdapter.createFromResource(
                context, R.array.screen_ui_actions, android.R.layout.simple_spinner_dropdown_item));
        targetSpinner.setAdapter(ArrayAdapter.createFromResource(
                context, R.array.screen_ui_targets, android.R.layout.simple_spinner_dropdown_item));

        actionSpinner.setSelection(Math.max(0, Math.min(1, visibilityAction)));
        targetSpinner.setSelection(Math.max(0, Math.min(2, target)));

        actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                visibilityAction = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View item, int position, long id) {
                target = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createScreenSystemUiAction(
                sprite, sequence, visibilityAction == 0, target));
    }
}
