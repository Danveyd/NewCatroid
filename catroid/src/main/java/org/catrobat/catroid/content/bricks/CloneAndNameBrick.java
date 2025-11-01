package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class CloneAndNameBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Sprite> {

    private static final long serialVersionUID = 1L;

    private Sprite objectToClone;
    private transient BrickSpinner<Sprite> spinner;

    public CloneAndNameBrick() {
        addAllowedBrickField(BrickField.CLONE_NAME, R.id.brick_clone_name_edit_text);
    }

    public Sprite getSelectedItem() {
        return objectToClone;
    }

    public void resetSpinner() {
        spinner.setSelection(0);
        objectToClone = null;
    }

    public CloneAndNameBrick(String cloneName) {
        this();
        setFormulaWithBrickField(BrickField.CLONE_NAME, new Formula(cloneName));
    }

    public CloneAndNameBrick(Formula cloneNameFormula) {
        this();
        setFormulaWithBrickField(BrickField.CLONE_NAME, cloneNameFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_clone_and_name;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        List<Nameable> items = new ArrayList<>();
        items.add(new StringOption(context.getString(R.string.brick_clone_this)));
        items.addAll(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());
        items.remove(ProjectManager.getInstance().getCurrentlyEditedScene().getBackgroundSprite());
        items.remove(ProjectManager.getInstance().getCurrentSprite());

        spinner = new BrickSpinner<>(R.id.brick_clone_spinner, view, items);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(objectToClone);

        return view;
    }

    @Override public void onNewOptionSelected(Integer spinnerId) {}
    @Override public void onEditOptionSelected(Integer spinnerId) {}
    @Override public void onStringOptionSelected(Integer spinnerId, String string) {
        objectToClone = null;
    }
    @Override public void onItemSelected(Integer spinnerId, @Nullable Sprite item) {
        objectToClone = item;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        Sprite spriteToClone = (objectToClone != null) ? objectToClone : sprite;
        Formula cloneNameFormula = getFormulaWithBrickField(BrickField.CLONE_NAME);

        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

        sequence.addAction(sprite.getActionFactory()
                .createCloneAndNameAction(spriteToClone, scope, cloneNameFormula));
    }
}