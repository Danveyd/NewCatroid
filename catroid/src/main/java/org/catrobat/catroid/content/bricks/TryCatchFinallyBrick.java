package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TryCatchFinallyBrick extends BrickBaseType implements CompositeBrick {
    private static final long serialVersionUID = 1L;

    private transient CatchBrick catchBrickPart = new CatchBrick(this);
    private transient FinallyBrick finallyBrickPart = new FinallyBrick(this);
    private transient EndBrick endBrick = new EndBrick(this);

    protected List<Brick> tryBricks = new ArrayList<>();
    protected List<Brick> catchBricks = new ArrayList<>();
    protected List<Brick> finallyBricks = new ArrayList<>();

    public TryCatchFinallyBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_try;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ScriptSequenceAction trySequence = new ScriptSequenceAction(sequence.getScript());
        for (Brick brick : tryBricks) if (!brick.isCommentedOut()) brick.addActionToSequence(sprite, trySequence);

        ScriptSequenceAction catchSequence = new ScriptSequenceAction(sequence.getScript());
        for (Brick brick : catchBricks) if (!brick.isCommentedOut()) brick.addActionToSequence(sprite, catchSequence);

        ScriptSequenceAction finallySequence = new ScriptSequenceAction(sequence.getScript());
        for (Brick brick : finallyBricks) if (!brick.isCommentedOut()) brick.addActionToSequence(sprite, finallySequence);

        sequence.addAction(sprite.getActionFactory().createTryCatchFinallyAction(
                trySequence, catchSequence, finallySequence, catchBrickPart.userVariable
        ));
    }


    @Override
    public List<Brick> getDragAndDropTargetList() {
        return tryBricks;
    }

    @Override
    public List<Brick> getNestedBricks() {
        return tryBricks;
    }

    @Override
    public boolean hasSecondaryList() {
        return true;
    }

    @Override
    public List<Brick> getSecondaryNestedBricks() {
        return catchBricks;
    }

    public List<Brick> getThirdNestedBricks() {
        return finallyBricks;
    }

    @Override
    public boolean consistsOfMultipleParts() {
        return true;
    }

    @Override
    public List<Brick> getAllParts() {
        List<Brick> parts = new ArrayList<>();
        parts.add(this);
        parts.add(catchBrickPart);
        parts.add(finallyBrickPart);
        parts.add(endBrick);
        return parts;
    }

    @Override
    public void addToFlatList(List<Brick> bricks) {
        super.addToFlatList(bricks);
        for (Brick brick : tryBricks) brick.addToFlatList(bricks);

        bricks.add(catchBrickPart);
        for (Brick brick : catchBricks) brick.addToFlatList(bricks);

        bricks.add(finallyBrickPart);
        for (Brick brick : finallyBricks) brick.addToFlatList(bricks);

        bricks.add(endBrick);
    }

    @Override
    public void setParent(Brick parent) {
        super.setParent(parent);
        for (Brick brick : tryBricks) brick.setParent(this);
        for (Brick brick : catchBricks) brick.setParent(catchBrickPart);
        for (Brick brick : finallyBricks) brick.setParent(finallyBrickPart);
        endBrick.setParent(this);
    }

    @Override
    public boolean removeChild(Brick brick) {
        if (tryBricks.remove(brick) || catchBricks.remove(brick) || finallyBricks.remove(brick)) {
            return true;
        }
        for (Brick child : tryBricks) if (child.removeChild(brick)) return true;
        for (Brick child : catchBricks) if (child.removeChild(brick)) return true;
        for (Brick child : finallyBricks) if (child.removeChild(brick)) return true;
        return false;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        TryCatchFinallyBrick clone = (TryCatchFinallyBrick) super.clone();
        clone.catchBrickPart = new CatchBrick(clone);
        clone.finallyBrickPart = new FinallyBrick(clone);
        clone.endBrick = new EndBrick(clone);
        clone.tryBricks = new ArrayList<>();
        for (Brick brick : tryBricks) clone.tryBricks.add(brick.clone());
        clone.catchBricks = new ArrayList<>();
        for (Brick brick : catchBricks) clone.catchBricks.add(brick.clone());
        clone.finallyBricks = new ArrayList<>();
        for (Brick brick : finallyBricks) clone.finallyBricks.add(brick.clone());
        clone.catchBrickPart.userVariable = this.catchBrickPart.userVariable;
        return clone;
    }

    @Override
    public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : tryBricks) brick.setCommentedOut(commentedOut);
        for (Brick brick : catchBricks) brick.setCommentedOut(commentedOut);
        for (Brick brick : finallyBricks) brick.setCommentedOut(commentedOut);
    }

    @Override
    public void addRequiredResources(ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : tryBricks) brick.addRequiredResources(requiredResourcesSet);
        for (Brick brick : catchBricks) brick.addRequiredResources(requiredResourcesSet);
        for (Brick brick : finallyBricks) brick.addRequiredResources(requiredResourcesSet);
    }

    public class CatchBrick extends UserVariableBrick {
        CatchBrick(TryCatchFinallyBrick parent) { this.parent = parent; }
        @Override public int getViewResource() { return R.layout.brick_catch; }
        @Override protected int getSpinnerId() { return R.id.catch_error_variable_spinner; }
        @Override public List<Brick> getDragAndDropTargetList() { return ((TryCatchFinallyBrick) parent).catchBricks; }
        @Override public boolean isCommentedOut() { return parent.isCommentedOut(); }
        @Override public boolean consistsOfMultipleParts() { return true; }
        @Override public List<Brick> getAllParts() { return parent.getAllParts(); }
        @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {}
        @Override public UUID getBrickID() { return parent.getBrickID(); }
    }

    public class FinallyBrick extends BrickBaseType {
        FinallyBrick(TryCatchFinallyBrick parent) { this.parent = parent; }
        @Override public int getViewResource() { return R.layout.brick_finally; }
        @Override public List<Brick> getDragAndDropTargetList() { return ((TryCatchFinallyBrick) parent).finallyBricks; }
        @Override public boolean isCommentedOut() { return parent.isCommentedOut(); }
        @Override public boolean consistsOfMultipleParts() { return true; }
        @Override public List<Brick> getAllParts() { return parent.getAllParts(); }
        @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {}
        @Override public UUID getBrickID() { return parent.getBrickID(); }
    }
}