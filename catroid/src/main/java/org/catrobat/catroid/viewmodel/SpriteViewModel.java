package org.catrobat.catroid.viewmodel;

import androidx.lifecycle.ViewModel;

public class SpriteViewModel extends ViewModel {
    private String currentSpriteName;

    public String getCurrentSpriteName() {
        return currentSpriteName;
    }

    public void setCurrentSpriteName(String name) {
        this.currentSpriteName = name;
    }
}