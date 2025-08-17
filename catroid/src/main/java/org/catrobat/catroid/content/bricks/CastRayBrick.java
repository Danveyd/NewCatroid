package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CastRayBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CastRayBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_cast_ray_name);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_cast_ray_from_x);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_cast_ray_from_y);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_cast_ray_from_z);
        addAllowedBrickField(BrickField.VALUE_5, R.id.brick_cast_ray_dir_x);
        addAllowedBrickField(BrickField.VALUE_6, R.id.brick_cast_ray_dir_y);
        addAllowedBrickField(BrickField.VALUE_7, R.id.brick_cast_ray_dir_z);
    }

    public CastRayBrick(Formula rayName,
                        Formula fromX, Formula fromY, Formula fromZ,
                        Formula dirX, Formula dirY, Formula dirZ) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, rayName);
        setFormulaWithBrickField(BrickField.VALUE_2, fromX);
        setFormulaWithBrickField(BrickField.VALUE_3, fromY);
        setFormulaWithBrickField(BrickField.VALUE_4, fromZ);
        setFormulaWithBrickField(BrickField.VALUE_5, dirX);
        setFormulaWithBrickField(BrickField.VALUE_6, dirY);
        setFormulaWithBrickField(BrickField.VALUE_7, dirZ);
    }

    public CastRayBrick(String rayName,
                        double fromX, double fromY, double fromZ,
                        double dirX, double dirY, double dirZ) {
        this(new Formula(rayName),
                new Formula(fromX), new Formula(fromY), new Formula(fromZ),
                new Formula(dirX), new Formula(dirY), new Formula(dirZ));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_cast_ray;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCastRayAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3),
                        getFormulaWithBrickField(BrickField.VALUE_4),
                        getFormulaWithBrickField(BrickField.VALUE_5),
                        getFormulaWithBrickField(BrickField.VALUE_6),
                        getFormulaWithBrickField(BrickField.VALUE_7)
                ));
    }
}