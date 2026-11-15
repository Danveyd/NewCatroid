package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;

public class UnknownBrick extends BrickBaseType {
    private static final long serialVersionUID = 1L;

    public static transient String lastUnknownClassName = "UnknownBrick";

    private String unknownClassName;

    public UnknownBrick() {
        this.unknownClassName = lastUnknownClassName;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_unknow;
    }

    @Override
    public View getView(Context context) {
        View view = super.getView(context);
        replaceLabelText(view);
        return view;
    }

    @Override
    public View getPrototypeView(Context context) {
        View view = super.getPrototypeView(context);
        replaceLabelText(view);
        return view;
    }

    private void replaceLabelText(View view) {
        TextView tv = findFirstTextView(view);
        if (tv != null) {
            tv.setText(view.getContext().getString(R.string.ub_unknown_block, getShortClassName(unknownClassName)));
        }
    }

    private String getShortClassName(String fullName) {
        if (fullName == null) return "Unknown";
        int idx = fullName.lastIndexOf('.');
        if (idx != -1) {
            return fullName.substring(idx + 1);
        }
        return fullName;
    }

    private TextView findFirstTextView(View view) {
        if (view instanceof TextView && !(view instanceof EditText) && !(view instanceof android.widget.CheckBox)) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView tv = findFirstTextView(group.getChildAt(i));
                if (tv != null) return tv;
            }
        }
        return null;
    }

    @Override
    public void addRequiredResources(ResourcesSet requiredResourcesSet) {
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
