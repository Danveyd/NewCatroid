package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateParticleEffectBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateParticleEffectBrick() {
        addAllowedBrickField(BrickField.FILE, R.id.brick_particle_file_edit_text);
    }

    public CreateParticleEffectBrick(String fileName) {
        this(new Formula(fileName));
    }

    public CreateParticleEffectBrick(Formula fileFormula) {
        this();
        setFormulaWithBrickField(BrickField.FILE, fileFormula);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_particle_effect;
    }

    @Override
    public View getView(Context context) {
        View v = super.getView(context);

        View editBtn = v.findViewById(R.id.brick_open_particle_editor_button);
        if (editBtn != null) {
            editBtn.setOnClickListener(click -> {
                Formula f = getFormulaWithBrickField(BrickField.FILE);
                String fileName = f != null ? f.getTrimmedFormulaString(context).replace("'", "").replace("\"", "") : "fire.particle";

                try {
                    Intent intent = new Intent(context, Class.forName("org.catrobat.catroid.particles.ui.ParticleEditorActivity"));
                    intent.putExtra("PARTICLE_FILE_NAME", fileName);
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return v;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCreateParticleEffectAction(
                sprite, sequence, getFormulaWithBrickField(BrickField.FILE)
        ));
    }
}
