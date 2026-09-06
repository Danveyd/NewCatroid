package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CameraTrackingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int trackMode = 0; // 0: Detach, 1: Pos, 2: Rot, 3: Both

    public CameraTrackingBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_camera_track_id);

        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_camera_track_px);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_camera_track_py);
        addAllowedBrickField(BrickField.Z_POSITION, R.id.brick_camera_track_pz);

        addAllowedBrickField(BrickField.YAW, R.id.brick_camera_track_ry);
        addAllowedBrickField(BrickField.PITCH, R.id.brick_camera_track_rp);
        addAllowedBrickField(BrickField.ROLL, R.id.brick_camera_track_rr);
    }

    public CameraTrackingBrick(String objectId, int mode, float px, float py, float pz, float ry, float rp, float rr) {
        this();
        setFormulaWithBrickField(BrickField.NAME, new Formula(objectId));
        this.trackMode = mode;
        setFormulaWithBrickField(BrickField.X_POSITION, new Formula(px));
        setFormulaWithBrickField(BrickField.Y_POSITION, new Formula(py));
        setFormulaWithBrickField(BrickField.Z_POSITION, new Formula(pz));
        setFormulaWithBrickField(BrickField.YAW, new Formula(ry));
        setFormulaWithBrickField(BrickField.PITCH, new Formula(rp));
        setFormulaWithBrickField(BrickField.ROLL, new Formula(rr));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_camera_tracking;
    }

    @Override
    public View getView(Context context) {
        final View brickView = super.getView(context);

        Spinner modeSpinner = brickView.findViewById(R.id.brick_camera_track_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.brick_camera_track_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                trackMode = position;
                updateVisibility(brickView);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        modeSpinner.setSelection(trackMode);
        updateVisibility(brickView);

        return brickView;
    }

    private void updateVisibility(View root) {
        if (root == null) {
            return;
        }
        LinearLayout posLayout = root.findViewById(R.id.brick_camera_track_pos_layout);
        LinearLayout rotLayout = root.findViewById(R.id.brick_camera_track_rot_layout);

        if (posLayout == null || rotLayout == null) {
            return;
        }
        posLayout.setVisibility((trackMode == 1 || trackMode == 3) ? View.VISIBLE : View.GONE);
        rotLayout.setVisibility((trackMode == 2 || trackMode == 3) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCameraTrackingAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                trackMode,
                getFormulaWithBrickField(BrickField.X_POSITION),
                getFormulaWithBrickField(BrickField.Y_POSITION),
                getFormulaWithBrickField(BrickField.Z_POSITION),
                getFormulaWithBrickField(BrickField.YAW),
                getFormulaWithBrickField(BrickField.PITCH),
                getFormulaWithBrickField(BrickField.ROLL)
        ));
    }
}
