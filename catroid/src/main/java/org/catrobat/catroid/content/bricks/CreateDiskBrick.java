package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class CreateDiskBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateDiskBrick() {
        addAllowedBrickField(BrickField.VM_DISK_NAME, R.id.brick_disk_name_edit);
        addAllowedBrickField(BrickField.VM_DISK_SIZE, R.id.brick_disk_size_edit);
    }

    public CreateDiskBrick(String diskName, String diskSize) {
        this(new Formula(diskName), new Formula(diskSize));
    }

    public CreateDiskBrick(Formula diskName, Formula diskSize) {
        this();
        setFormulaWithBrickField(BrickField.VM_DISK_NAME, diskName);
        setFormulaWithBrickField(BrickField.VM_DISK_SIZE, diskSize);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_disk;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createDiskAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VM_DISK_NAME),
                getFormulaWithBrickField(BrickField.VM_DISK_SIZE)
        ));
    }
}