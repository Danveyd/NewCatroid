package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.scenes.scene2d.Action;
import org.catrobat.catroid.content.ActionFactory;

public class AsyncRepeatBrick extends FormulaBrick implements CompositeBrick {
    private static final long serialVersionUID = 1L;
    private transient EndBrick endBrick = new EndBrick(this);
    private List<Brick> loopBricks = new ArrayList<>();

    public AsyncRepeatBrick() {
        addAllowedBrickField(BrickField.TIMES_TO_REPEAT, R.id.brick_async_repeat_edit_text);
    }

    public AsyncRepeatBrick(Formula times) {
        this();
        setFormulaWithBrickField(BrickField.TIMES_TO_REPEAT, times);
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ScriptSequenceAction loopSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
        for (Brick brick : loopBricks) {
            if (!brick.isCommentedOut()) {
                brick.addActionToSequence(sprite, loopSequence);
            }
        }
        Action action = sprite.getActionFactory().createAsyncRepeatAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.TIMES_TO_REPEAT), loopSequence);
        sequence.addAction(action);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_async_repeat;
    }

    // --- Дальше идет стандартная реализация CompositeBrick, скопированная из RepeatBrick ---

    @Override public boolean hasSecondaryList() { return false; }
    @Override public List<Brick> getNestedBricks() { return loopBricks; }
    @Override public List<Brick> getSecondaryNestedBricks() { return null; }
    public boolean addBrick(Brick brick) { return loopBricks.add(brick); }

    @Override public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : loopBricks) { brick.setCommentedOut(commentedOut); }
    }

    @Override public Brick clone() throws CloneNotSupportedException {
        AsyncRepeatBrick clone = (AsyncRepeatBrick) super.clone();
        clone.endBrick = new EndBrick(clone);
        clone.loopBricks = new ArrayList<>();
        for (Brick brick : loopBricks) { clone.addBrick(brick.clone()); }
        return clone;
    }

    @Override public boolean consistsOfMultipleParts() { return true; }
    @Override public List<Brick> getAllParts() {
        List<Brick> bricks = new ArrayList<>();
        bricks.add(this);
        bricks.add(endBrick);
        return bricks;
    }
    @Override public void addToFlatList(List<Brick> bricks) {
        super.addToFlatList(bricks);
        for (Brick brick : loopBricks) { brick.addToFlatList(bricks); }
        bricks.add(endBrick);
    }
    @Override public void setParent(Brick parent) {
        super.setParent(parent);
        for (Brick brick : loopBricks) { brick.setParent(this); }
    }
    @Override public List<Brick> getDragAndDropTargetList() { return loopBricks; }
    @Override public boolean removeChild(Brick brick) {
        if (loopBricks.remove(brick)) { return true; }
        for (Brick childBrick : loopBricks) { if (childBrick.removeChild(brick)) { return true; } }
        return false;
    }
    @Override public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : loopBricks) { brick.addRequiredResources(requiredResourcesSet); }
    }
}