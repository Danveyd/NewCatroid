package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.badlogic.gdx.Input;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenMouseButtonClickedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class WhenMouseButtonClickedBrick extends ScriptBrickBaseType {
    private static final long serialVersionUID = 1L;
    private WhenMouseButtonClickedScript script;

    public WhenMouseButtonClickedBrick() {
        this(new WhenMouseButtonClickedScript(Input.Buttons.LEFT));
    }

    public WhenMouseButtonClickedBrick(WhenMouseButtonClickedScript script) {
        this.script = script;
        this.script.setScriptBrick(this);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_mouse_button_clicked;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Spinner spinner = view.findViewById(R.id.mouse_button_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.mouse_buttons, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        switch(script.getButtonCode()) {
            case Input.Buttons.RIGHT: spinner.setSelection(1); break;
            case Input.Buttons.MIDDLE: spinner.setSelection(2); break;
            default: spinner.setSelection(0); break;
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedButtonCode;
                switch (position) {
                    case 1: selectedButtonCode = Input.Buttons.RIGHT; break;
                    case 2: selectedButtonCode = Input.Buttons.MIDDLE; break;
                    default: selectedButtonCode = Input.Buttons.LEFT; break;
                }
                ((WhenMouseButtonClickedScript) script).setButtonCode(selectedButtonCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    @Override public Script getScript() { return script; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {}
    @Override public Brick clone() throws CloneNotSupportedException {
        WhenMouseButtonClickedBrick clone = (WhenMouseButtonClickedBrick) super.clone();
        clone.script = (WhenMouseButtonClickedScript) script.clone();
        clone.script.setScriptBrick(clone);
        return clone;
    }
}