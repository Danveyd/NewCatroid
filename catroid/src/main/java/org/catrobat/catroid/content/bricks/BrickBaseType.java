/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BrickBaseType implements Brick {

	private static final long serialVersionUID = 1L;

	public transient View view;
	private transient CheckBox checkbox;
	private transient Map<Integer, BrickSpinner<?>> attachedSpinners;

	private transient boolean buildingDetachedView;

	protected transient Brick parent;

	protected boolean commentedOut;

	protected UUID brickId = UUID.randomUUID();

    private transient boolean isPhantom = false;

    @Override
    public boolean isPhantom() {
        return isPhantom;
    }

    @Override
    public void setPhantom(boolean isPhantom) {
        this.isPhantom = isPhantom;
    }

	@Override
	public boolean isCommentedOut() {
		return commentedOut;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		this.commentedOut = commentedOut;
	}

	@Nullable
	@Override
	public CheckBox getCheckBox() {
		return checkbox;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		BrickBaseType clone = (BrickBaseType) super.clone();
		clone.view = null;
		clone.checkbox = null;
		clone.parent = null;
		clone.commentedOut = commentedOut;
		clone.brickId = UUID.randomUUID();
		return clone;
	}

	@Override
	public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
	}

	@LayoutRes
	public abstract int getViewResource();

	@CallSuper
	@Override
	public View getView(Context context) {
		View cachedView = view;
		if (cachedView != null && cachedView.getContext() == context) {
			return cachedView;
		}
		view = LayoutInflater.from(context).inflate(getViewResource(), null, false);
		checkbox = view.findViewById(R.id.brick_checkbox);
		return view;
	}

	@Override
	public View getPrototypeView(Context context) {
		DetachedViewState state = beginDetachedView();
		try {
			View freshView = getView(context);
			disableSpinners(freshView);
			removeSpinnerListeners(freshView);
			return freshView;
		} finally {
			endDetachedView(state);
		}
	}

	public static class DetachedViewState {
		private View view;
		private CheckBox checkbox;
		protected Object extra;
	}

	@CallSuper
	protected DetachedViewState beginDetachedView() {
		DetachedViewState state = new DetachedViewState();
		state.view = this.view;
		state.checkbox = this.checkbox;
		this.view = null;
		this.checkbox = null;
		this.buildingDetachedView = true;
		return state;
	}

	@CallSuper
	protected void endDetachedView(DetachedViewState state) {
		this.view = state.view;
		this.checkbox = state.checkbox;
		this.buildingDetachedView = false;
	}

	public void attachSpinner(Integer spinnerId, BrickSpinner<?> spinner) {
		if (buildingDetachedView) {
			return;
		}
		if (attachedSpinners == null) {
			attachedSpinners = new HashMap<>();
		}
		attachedSpinners.put(spinnerId, spinner);
	}

	@Override
	public void invalidateCachedView() {
		this.view = null;
		this.checkbox = null;
		if (attachedSpinners != null) {
			for (BrickSpinner<?> attachedSpinner : attachedSpinners.values()) {
				attachedSpinner.release();
			}
			attachedSpinners.clear();
		}
	}

	public void disableSpinners() {
		disableSpinners(view);
	}

	private void disableSpinners(View view) {
		if (view instanceof Spinner) {
			view.setEnabled(false);
			view.setClickable(false);
			view.setFocusable(false);
		}
		if (view instanceof ViewGroup) {
			ViewGroup parent = (ViewGroup) view;
			for (int i = 0; i < parent.getChildCount(); i++) {
				disableSpinners(parent.getChildAt(i));
			}
		}
	}

	private void removeSpinnerListeners(View view) {
		if (view instanceof Spinner) {
			((Spinner) view).setOnItemSelectedListener(null);
		}
		if (view instanceof ViewGroup) {
			ViewGroup parent = (ViewGroup) view;
			for (int i = 0; i < parent.getChildCount(); i++) {
				removeSpinnerListeners(parent.getChildAt(i));
			}
		}
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return false;
	}

	@Override
	public List<Brick> getAllParts() {
		return Collections.singletonList(this);
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		bricks.add(this);
	}

	@Override
	public Script getScript() {
		return getParent().getScript();
	}

	@Override
	public int getPositionInScript() {
		if (getParent() instanceof ScriptBrick) {
			return getScript().getBrickList().indexOf(this);
		}
		return getParent().getPositionInScript();
	}

	@Override
	public Brick getParent() {
		return parent;
	}

	@Override
	public void setParent(Brick parent) {
		this.parent = parent;
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return getParent().getDragAndDropTargetList();
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return getDragAndDropTargetList().indexOf(this);
	}

	@Override
	public boolean removeChild(Brick brick) {
		return false;
	}

	public boolean hasHelpPage() {
		return true;
	}

	void notifyDataSetChanged(AppCompatActivity activity) {
		ScriptFragment parentFragment = (ScriptFragment) activity
				.getSupportFragmentManager().findFragmentByTag(ScriptFragment.TAG);
		if (parentFragment != null) {
			parentFragment.notifyDataSetChanged();
		}
	}

	public String getHelpUrl(String category) {
		return "https://wiki.catrobat.org/bin/view/Documentation/BrickDocumentation/" + this.getClass().getSimpleName();
	}

	protected String getPositionInformation() {
		int position = -1;
		String scriptName = "unknown";
		if (getParent() != null) {
			position = getPositionInScript();
			scriptName = getScript().getClass().getSimpleName();
		}
		position += 2;
		return "Brick at position " + position + "\nin \"" + scriptName + "\"";
	}

	@Override
	public UUID getBrickID() {
		return brickId;
	}

	@Override
	public List<Brick> findBricksInNestedBricks(List<UUID> brickIds) {
		if (!(this instanceof CompositeBrick)) {
			return null;
		}

		List<Brick> foundBricks = new ArrayList<>();
		CompositeBrick compositeBrick = (CompositeBrick) this;

		for (Brick brick : compositeBrick.getNestedBricks()) {
			if (brickIds.contains(brick.getBrickID())) {
				foundBricks.add(brick);
			} else if (brick instanceof CompositeBrick) {
				List<Brick> tmpBricks = brick.findBricksInNestedBricks(brickIds);
				if (tmpBricks != null) {
					return tmpBricks;
				}
			}

			if (brickIds.size() == foundBricks.size()) {
				break;
			}
		}

		if (foundBricks.size() == 0 && compositeBrick.hasSecondaryList()) {
			for (Brick brick : compositeBrick.getSecondaryNestedBricks()) {
				if (brickIds.contains(brick.getBrickID())) {
					foundBricks.add(brick);
				} else if (brick instanceof CompositeBrick) {
					List<Brick> tmpBricks = brick.findBricksInNestedBricks(brickIds);
					if (tmpBricks != null) {
						return tmpBricks;
					}
				}

				if (brickIds.size() == foundBricks.size()) {
					break;
				}
			}
		}

		if (foundBricks.size() > 0) {
			return foundBricks;
		}
		return null;
	}

	@Override
	public boolean addBrickInNestedBrick(UUID parentBrickId, int subStackIndex, List<Brick> bricksToAdd) {
		if (!(this instanceof CompositeBrick)) {
			return false;
		}

		CompositeBrick compositeBrick = (CompositeBrick) this;

		if (getBrickID().equals(parentBrickId)) {
			if (subStackIndex == 0) {
				compositeBrick.getNestedBricks().addAll(0, bricksToAdd);
				return true;
			} else if (subStackIndex == 1 && compositeBrick.hasSecondaryList()) {
				compositeBrick.getSecondaryNestedBricks().addAll(0, bricksToAdd);
				return true;
			}
		}

		int index = 0;

		for (Brick brick : compositeBrick.getNestedBricks()) {
			++index;
			if (subStackIndex == -1
					&& brick.getBrickID().equals(parentBrickId)) {
				compositeBrick.getNestedBricks().addAll(index, bricksToAdd);
			} else if (brick instanceof CompositeBrick
					&& brick.addBrickInNestedBrick(parentBrickId, subStackIndex, bricksToAdd)) {
				return true;
			}
		}

		if (!compositeBrick.hasSecondaryList()) {
			return false;
		}

		index = 0;
		for (Brick brick : compositeBrick.getSecondaryNestedBricks()) {
			++index;
			if (subStackIndex == -1
					&& brick.getBrickID().equals(parentBrickId)) {
				compositeBrick.getSecondaryNestedBricks().addAll(index, bricksToAdd);
				return true;
			} else if (brick instanceof CompositeBrick
					&& brick.addBrickInNestedBrick(parentBrickId, subStackIndex, bricksToAdd)) {
				return true;
			}
		}
		return false;
	}
}
