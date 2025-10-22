package org.catrobat.catroid.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import org.catrobat.catroid.raptor.SceneManager;

public class EditorFragment extends AndroidFragmentApplication {

    private EditorListener editorListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        editorListener = new EditorListener((EditorActivity) getActivity());
        return initializeForView(editorListener);
    }

    public EditorListener getListener() {
        return editorListener;
    }

    public SceneManager getSceneManager() {
        if (editorListener != null) {
            return editorListener.getSceneManager();
        }
        return null;
    }
}