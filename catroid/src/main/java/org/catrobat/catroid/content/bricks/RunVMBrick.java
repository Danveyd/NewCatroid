package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class RunVMBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RunVMBrick() {
        addAllowedBrickField(BrickField.VM_MEMORY, R.id.brick_memory_edit);
        addAllowedBrickField(BrickField.VM_CPU, R.id.brick_cpu_edit);
        addAllowedBrickField(BrickField.VM_HDA, R.id.brick_hda_edit);
        addAllowedBrickField(BrickField.VM_CDROM, R.id.brick_cdrom_edit);
    }

    public RunVMBrick(String memory, String cpu, String hda, String cdrom) {
        this(new Formula(memory), new Formula(cpu), new Formula(hda), new Formula(cdrom));
    }

    public RunVMBrick(Formula memory, Formula cpu, Formula hda, Formula cdrom) {
        this();
        setFormulaWithBrickField(BrickField.VM_MEMORY, memory);
        setFormulaWithBrickField(BrickField.VM_CPU, cpu);
        setFormulaWithBrickField(BrickField.VM_HDA, hda);
        setFormulaWithBrickField(BrickField.VM_CDROM, cdrom);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_run_vm;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createRunVMAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VM_MEMORY),
                getFormulaWithBrickField(BrickField.VM_CPU),
                getFormulaWithBrickField(BrickField.VM_HDA),
                getFormulaWithBrickField(BrickField.VM_CDROM)
        ));
    }
}