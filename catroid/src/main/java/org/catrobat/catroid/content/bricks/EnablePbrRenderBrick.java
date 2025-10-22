/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */

package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class EnablePbrRenderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int renderState = 1; // 0: Off, 1: On. По умолчанию "On"

    public EnablePbrRenderBrick() {
    }

    public EnablePbrRenderBrick(int renderState) {
        this.renderState = renderState;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_enable_pbr_render;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner spinner = view.findViewById(R.id.brick_enable_pbr_render_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context, R.array.brick_pbr_render_states, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderState = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinner.setSelection(renderState);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createEnablePbrRenderAction(sprite, sequence, renderState));
    }
}