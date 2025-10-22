package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.recyclerview.dialog.dialoginterface.NewItemInterface;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SetSoundVolumeBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<SoundInfo>,
        NewItemInterface<SoundInfo> {

    private static final long serialVersionUID = 1L;

    private SoundInfo sound;
    private transient BrickSpinner<SoundInfo> spinner;

    public SetSoundVolumeBrick() {
        // Связываем поле громкости с ID из layout
        addAllowedBrickField(BrickField.VOLUME, R.id.brick_set_sound_volume_edit_text);
    }

    // Конструктор для строк
    public SetSoundVolumeBrick(Double volume) {
        this();
        setFormulaWithBrickField(BrickField.VOLUME, new Formula(volume));
    }

    // Основной конструктор
    public SetSoundVolumeBrick(SoundInfo sound, Formula volume) {
        this();
        this.sound = sound;
        setFormulaWithBrickField(BrickField.VOLUME, volume);
    }

    public SoundInfo getSound() {
        return sound;
    }

    public void setSound(SoundInfo sound) {
        this.sound = sound;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        SetSoundVolumeBrick clone = (SetSoundVolumeBrick) super.clone();
        clone.spinner = null;
        return clone;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_sound_volume;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        List<Nameable> items = new ArrayList<>();
        items.add(new NewOption(context.getString(R.string.new_option)));
        items.addAll(ProjectManager.getInstance().getCurrentSprite().getSoundList());

        spinner = new BrickSpinner<>(R.id.brick_set_sound_volume_spinner, view, items);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(sound);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetSoundVolumeAction(
                sprite,
                sequence,
                sound,
                getFormulaWithBrickField(BrickField.VOLUME)
        ));
    }

    @Override
    public void onItemSelected(Integer spinnerId, @Nullable SoundInfo item) {
        sound = item;
    }

    @Override
    public void onNewOptionSelected(Integer spinnerId) {
        AppCompatActivity activity = UiUtils.getActivityFromView(view);
        if (!(activity instanceof SpriteActivity)) {
            return;
        }
        ((SpriteActivity) activity).registerOnNewSoundListener(this);
        ((SpriteActivity) activity).handleAddSoundButton();
    }

    @Override
    public void addItem(SoundInfo item) {
        spinner.add(item);
        spinner.setSelection(item);
    }

    @Override
    public void onEditOptionSelected(Integer spinnerId) { }

    @Override
    public void onStringOptionSelected(Integer spinnerId, String string) { }
}