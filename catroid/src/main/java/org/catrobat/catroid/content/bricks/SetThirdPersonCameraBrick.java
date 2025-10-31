package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetThirdPersonCameraBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetThirdPersonCameraBrick() {
        addAllowedBrickField(BrickField.OBJECT_ID, R.id.brick_third_person_camera_object_id);
        addAllowedBrickField(BrickField.DISTANCE, R.id.brick_third_person_camera_distance);
        addAllowedBrickField(BrickField.HEIGHT, R.id.brick_third_person_camera_height);
        addAllowedBrickField(BrickField.PITCH, R.id.brick_third_person_camera_pitch);
    }

    public SetThirdPersonCameraBrick(String objectId, double distance, double height, double pitch) {
        this(new Formula(objectId), new Formula(distance), new Formula(height), new Formula(pitch));
    }

    public SetThirdPersonCameraBrick(Formula objectId, Formula distance, Formula height, Formula pitch) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_ID, objectId);
        setFormulaWithBrickField(BrickField.DISTANCE, distance);
        setFormulaWithBrickField(BrickField.HEIGHT, height);
        setFormulaWithBrickField(BrickField.PITCH, pitch);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_third_person_camera;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetThirdPersonCameraAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.OBJECT_ID),
                        getFormulaWithBrickField(BrickField.DISTANCE),
                        getFormulaWithBrickField(BrickField.HEIGHT),
                        getFormulaWithBrickField(BrickField.PITCH)
                ));
    }
}