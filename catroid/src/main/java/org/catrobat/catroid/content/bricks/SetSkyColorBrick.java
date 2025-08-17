package org.catrobat.catroid.content.bricks;

// package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetSkyColorBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetSkyColorBrick() {
        // Связываем поля для формул с ID в XML-разметке.
        // Используем стандартные BrickField.VALUE_1, _2, _3
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_set_sky_color_r);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_set_sky_color_g);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_set_sky_color_b);
    }

    /**
     * Конструктор для удобного создания блока с уже заданными значениями.
     */
    public SetSkyColorBrick(double r, double g, double b) {
        this(); // Вызываем основной конструктор
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(r));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(g));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(b));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_sky_color;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetSkyColorAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3)
                ));
    }
}