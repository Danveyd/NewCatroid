/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class UploadFileBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    private int fileTypeSelection = 0;
    private int storageTypeSelection = 0;

    public UploadFileBrick() {
        addAllowedBrickField(BrickField.URL, R.id.brick_upload_url_edit);
        addAllowedBrickField(BrickField.FILE, R.id.brick_upload_file_path_edit);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_upload_mime_type_edit);
    }

    public UploadFileBrick(Formula url, Formula filePath, int fileType, Formula mimeType, int storageType) {
        this();
        setFormulaWithBrickField(BrickField.URL, url);
        setFormulaWithBrickField(BrickField.FILE, filePath);
        setFormulaWithBrickField(BrickField.TEXT, mimeType);
        this.fileTypeSelection = fileType;
        this.storageTypeSelection = storageType;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_upload_file;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner fileTypeSpinner = view.findViewById(R.id.brick_upload_file_type_spinner);
        ArrayAdapter<CharSequence> fileTypeAdapter = ArrayAdapter.createFromResource(
                context, R.array.upload_file_types, android.R.layout.simple_spinner_item);
        fileTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileTypeSpinner.setAdapter(fileTypeAdapter);
        fileTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fileTypeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        fileTypeSpinner.setSelection(fileTypeSelection);

        Spinner storageTypeSpinner = view.findViewById(R.id.brick_upload_storage_type_spinner);
        ArrayAdapter<CharSequence> storageTypeAdapter = ArrayAdapter.createFromResource(
                context, R.array.upload_storage_types, android.R.layout.simple_spinner_item);
        storageTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storageTypeSpinner.setAdapter(storageTypeAdapter);
        storageTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                storageTypeSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        storageTypeSpinner.setSelection(storageTypeSelection);

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createUploadFileAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.URL),
                        getFormulaWithBrickField(BrickField.FILE),
                        fileTypeSelection,
                        getFormulaWithBrickField(BrickField.TEXT),
                        storageTypeSelection
                ));
    }
}