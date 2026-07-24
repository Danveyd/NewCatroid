/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
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
package org.catrobat.catroid.ui.recyclerview.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import org.catrobat.catroid.BuildConfig;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.ai.KoveAutocompleteController;
import org.catrobat.catroid.codeanalysis.AnalysisManager;
import org.catrobat.catroid.codeanalysis.AnalysisResult;
import org.catrobat.catroid.codeanalysis.CodeAnalyzer;
import org.catrobat.catroid.codeanalysis.Severity;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.BrickInfo;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.EmptyEventBrick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.UserDefinedBrick;
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrick;
import org.catrobat.catroid.content.bricks.VisualPlacementBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InternToExternGenerator;
import org.catrobat.catroid.formulaeditor.Sensors;
import org.catrobat.catroid.formulaeditor.UserData;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.io.asynctask.ProjectLoader;
import org.catrobat.catroid.io.asynctask.ProjectSaver;
import org.catrobat.catroid.ui.BottomBar;
import org.catrobat.catroid.ui.ScriptFinder;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.controller.BackpackListManager;
import org.catrobat.catroid.ui.controller.RecentBrickListManager;
import org.catrobat.catroid.ui.dragndrop.BrickListView;
import org.catrobat.catroid.ui.fragment.AddBrickFragment;
import org.catrobat.catroid.ui.fragment.BrickCategoryFragment;
import org.catrobat.catroid.ui.fragment.BrickCategoryFragment.OnCategorySelectedListener;
import org.catrobat.catroid.ui.fragment.BrickSearchFragment;
import org.catrobat.catroid.ui.fragment.CategoryBricksFactory;
import org.catrobat.catroid.ui.fragment.UserDefinedBrickListFragment;
import org.catrobat.catroid.ui.recyclerview.adapter.BrickAdapter;
import org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity;
import org.catrobat.catroid.ui.recyclerview.controller.BrickController;
import org.catrobat.catroid.ui.recyclerview.controller.ScriptController;
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog;
import org.catrobat.catroid.ui.recyclerview.dialog.textwatcher.DuplicateInputTextWatcher;
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider;
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment;
import org.catrobat.catroid.utils.SnackbarUtil;
import org.catrobat.catroid.utils.ToastUtil;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleOwnerKt;

import static org.catrobat.catroid.common.Constants.CODE_XML_FILE_NAME;
import static org.catrobat.catroid.common.Constants.UNDO_CODE_XML_FILE_NAME;
import static org.catrobat.catroid.common.ScreenValues.DEFAULT_SCREEN_RESOLUTION;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;

public class ScriptFragment extends ListFragment implements
		ActionMode.Callback,
		BrickAdapter.OnBrickClickListener,
		BrickAdapter.SelectionListener, OnCategorySelectedListener,
		AddBrickFragment.OnAddBrickListener,
		ProjectLoader.ProjectLoadListener,
		BrickAdapter.OnScriptChangedListener {

	public static final String TAG = ScriptFragment.class.getSimpleName();
	private static final String BRICK_TAG = "brickToFocus";
	private static final String SCRIPT_TAG = "scriptToFocus";

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({NONE, BACKPACK, COPY, DELETE, COMMENT, CATBLOCKS})
	@interface ActionModeType {
	}

	private static final int NONE = 0;
	private static final int BACKPACK = 1;
	private static final int COPY = 2;
	private static final int DELETE = 3;
	private static final int COMMENT = 4;
	private static final int CATBLOCKS = 5;

	@ActionModeType
	private int actionModeType = NONE;

	private ActionMode actionMode;
	private BrickAdapter adapter;
	private BrickListView listView;
	private ScriptFinder scriptFinder;
	private String currentSceneName;
	private String currentSpriteName;
	private int undoBrickPosition;

	private ScriptController scriptController = new ScriptController();
	private BrickController brickController = new BrickController();

	private Parcelable savedListViewState;
	private Brick brickToFocus;
	private Script scriptToFocus;

	private CodeAnalyzer codeAnalyzer;
    private kotlinx.coroutines.Job codeAnalysisJob = null;

	private SpriteActivity activity;

    private View analysisStatusIndicator;
    private TextView errorCountText;
    private TextView warningCountText;

    private Sprite currentSprite;

    private static final boolean DEBUG_SPRITE_PRINTER = false;

    public static ScriptFragment newInstance(Brick brickToFocus) {
		ScriptFragment scriptFragment = new ScriptFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BRICK_TAG, brickToFocus);
		scriptFragment.setArguments(bundle);
		return scriptFragment;
	}

	public static ScriptFragment newInstance(Script scriptToFocus) {
		ScriptFragment scriptFragment = new ScriptFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(SCRIPT_TAG, scriptToFocus);
		scriptFragment.setArguments(bundle);
		return scriptFragment;
	}

	public ScriptFragment() {
		// required empty constructor
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle bundle = getArguments();
		if (bundle != null) {
			this.brickToFocus = (Brick) bundle.get(BRICK_TAG);
			this.scriptToFocus = (Script) bundle.get(SCRIPT_TAG);
		}
		codeAnalyzer = new CodeAnalyzer(requireContext());
	}

	private List<UserVariable> savedUserVariables;
	private List<UserVariable> savedMultiplayerVariables;
	private List<UserList> savedUserLists;
	private transient List<UserVariable> savedLocalUserVariables;
	private transient List<UserList> savedLocalLists;

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater inflater = mode.getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);

		switch (actionModeType) {
			case BACKPACK:
				adapter.setCheckBoxMode(BrickAdapter.SCRIPTS_ONLY);
				mode.setTitle(getString(R.string.am_backpack));
				break;
			case COPY:
				adapter.setCheckBoxMode(BrickAdapter.CONNECTED_ONLY);
				mode.setTitle(getString(R.string.am_copy));
				break;
			case DELETE:
				adapter.setCheckBoxMode(BrickAdapter.ALL);
				mode.setTitle(getString(R.string.am_delete));
				break;
			case COMMENT:
				adapter.selectAllCommentedOutBricks();
				adapter.setCheckBoxMode(BrickAdapter.ALL);
				mode.setTitle(getString(R.string.comment_in_out));
				break;
			case NONE:
				adapter.setCheckBoxMode(NONE);
				actionMode.finish();
				return false;
			case CATBLOCKS:
				break;
		}
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            handleContextualAction();
        } else {
            return false;
        }
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		resetActionModeParameters();
		adapter.clearSelection();
		BottomBar.showBottomBar(getActivity());
	}

	private void handleContextualAction() {
		if (adapter.isEmpty()) {
			actionMode.finish();
		}

		switch (actionModeType) {
			case BACKPACK:
				showNewScriptGroupAlert(adapter.getSelectedItems());
				break;
			case COPY:
				copy(adapter.getSelectedItems());
				break;
			case DELETE:
				showDeleteAlert(adapter.getSelectedItems());
				break;
			case COMMENT:
				toggleComments(adapter.getSelectedItems());
				break;
			case NONE:
				throw new IllegalStateException("ActionModeType not set correctly");
			case CATBLOCKS:
				break;
		}
	}

	private void resetActionModeParameters() {
		actionModeType = NONE;
		actionMode = null;
		adapter.setCheckBoxMode(BrickAdapter.NONE);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = View.inflate(getActivity(), R.layout.fragment_script, null);
		listView = view.findViewById(android.R.id.list);int bottomListPadding;
        if (BuildConfig.FEATURE_AI_ASSIST_ENABLED) {
            bottomListPadding = (ScreenValues.currentScreenResolution != null)
                    ? (int) (ScreenValues.currentScreenResolution.getHeight() / 2.5)
                    : DEFAULT_SCREEN_RESOLUTION.getHeight();
        } else {
            bottomListPadding = (ScreenValues.currentScreenResolution != null)
                    ? ScreenValues.currentScreenResolution.getHeight() / 3
                    : DEFAULT_SCREEN_RESOLUTION.getHeight() / 3;
        }

        View footerView = new View(getActivity());
        footerView.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                bottomListPadding
        ));
        listView.addFooterView(footerView, null, false);

        listView.setPadding(0, 0, 0, 0);
        listView.setClipToPadding(false);

        listView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        listView.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(android.widget.AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
                    KoveAutocompleteController.getInstance().dismissSuggestions();
                }
            }

            @Override
            public void onScroll(android.widget.AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        if (getActivity() instanceof SpriteActivity) {
            activity = (SpriteActivity) getActivity();
        }
        SettingsFragment.setToChosenLanguage(getActivity());

		scriptFinder = view.findViewById(R.id.findview);
		scriptFinder.setOnResultFoundListener((sceneIndex, spriteIndex, brickIndex, totalResults,
											   textView
		) -> {
			Project currentProject = ProjectManager.getInstance().getCurrentProject();
			Scene currentScene = currentProject.getSceneList().get(sceneIndex);
			Sprite currentSprite = currentScene.getSpriteList().get(spriteIndex);

			textView.setText(createActionBarTitle(currentProject,
					currentScene,
					currentSprite));

			ProjectManager.getInstance().setCurrentSceneAndSprite(currentScene.getName(),
					currentSprite.getName());

            this.currentSprite = currentSprite;

			adapter.updateItems(currentSprite);
			adapter.notifyDataSetChanged();
			listView.smoothScrollToPosition(brickIndex);
			highlightBrickAtIndex(brickIndex);
			hideKeyboard();

            refreshFastScroll();

            if (DEBUG_SPRITE_PRINTER) {
                printSpriteScriptsToLog(currentSprite);
            }
		});

		scriptFinder.setOnCloseListener(() -> {
			listView.cancelHighlighting();
			finishActionMode();
			if (activity != null && !activity.isFinishing()) {
				activity.setCurrentSceneAndSprite(ProjectManager.getInstance().getCurrentlyEditedScene(),
						ProjectManager.getInstance().getCurrentSprite());
				activity.getSupportActionBar().setTitle(activity.createActionBarTitle());
				activity.addTabs();
                activity.findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
			}
		});

        scriptFinder.setOnOpenListener(() -> {
            if (activity != null) {
                activity.removeTabs();
                activity.findViewById(R.id.toolbar).setVisibility(View.GONE);
            }
        });

        analysisStatusIndicator = view.findViewById(R.id.analysis_status_indicator);
        errorCountText = view.findViewById(R.id.error_count_text);
        warningCountText = view.findViewById(R.id.warning_count_text);

        if (analysisStatusIndicator != null) {
            analysisStatusIndicator.setOnClickListener(v -> showAnalysisSummaryDialog());
        }

		setHasOptionsMenu(true);
		return view;
	}

	public String createActionBarTitle(Project currentProject, Scene currentScene, Sprite currentSprite) {
		if (currentProject.getSceneList().size() == 1) {
			return currentSprite.getName();
		} else {
			return currentScene.getName() + ": " + currentSprite.getName();
		}
	}

	private void highlightBrickAtIndex(int index) {
		listView.getBrickPositionsToHighlight().clear();
		listView.getBrickPositionsToHighlight().add(index);
	}

	private void hideKeyboard() {
		InputMethodManager imm =
				(InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
	}

    private static void clearBrickViewsReflection(Brick brick) {
        if (brick == null) return;

        Class<?> clazz = brick.getClass();
        while (clazz != null && clazz != Object.class) {
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    if (android.view.View.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        field.set(brick, null);
                    }
                } catch (Exception ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (codeAnalysisJob != null) {
            codeAnalysisJob.cancel(null);
            codeAnalysisJob = null;
        }

        if (this.currentSprite != null && this.currentSprite.getScriptList() != null) {
            for (Script script : this.currentSprite.getScriptList()) {
                List<Brick> bricks = getBricksFromScript(script);
                if (bricks != null) {
                    for (Brick brick : bricks) {
                        clearBrickViewsReflection(brick);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scriptFinder.isOpen() && activity != null) {
            activity.findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        }

        if (adapter != null) {
            adapter.setSelectionListener(null);
            adapter.setOnItemClickListener(null);
            adapter.setOnScriptChangedListener(null);
            adapter = null;
        }
        listView = null;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		Scene currentScene = ProjectManager.getInstance().getCurrentlyEditedScene();
        this.currentSprite = ProjectManager.getInstance().getCurrentSprite();
		currentProject.getBroadcastMessageContainer().update();

        adapter = new BrickAdapter(this.currentSprite);
        adapter.setSelectionListener(this);
        adapter.setOnItemClickListener(this);

		adapter.setOnScriptChangedListener(this);

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(adapter);
		listView.setOnItemLongClickListener(adapter);

		if (currentSprite.equals(currentScene.getBackgroundSprite())) {
			InternToExternGenerator.setInternExternLanguageConverterMap(Sensors.OBJECT_NUMBER_OF_LOOKS,
					R.string.formula_editor_object_number_of_backgrounds);
		} else {
			InternToExternGenerator.setInternExternLanguageConverterMap(Sensors.OBJECT_NUMBER_OF_LOOKS,
					R.string.formula_editor_object_number_of_looks);
		}
	}

	@Override
	public void onScriptChanged() {
		runCodeAnalysis();
	}

    @Override
    public void onResume() {
        super.onResume();

        Project project = ProjectManager.getInstance().getCurrentProject();
        Scene scene = ProjectManager.getInstance().getCurrentlyEditedScene();
        Sprite sprite = ProjectManager.getInstance().getCurrentSprite();

        if (sprite != null) {
            this.currentSprite = sprite;
        }

        ActionBar actionBar = null;
        if (getActivity() instanceof AppCompatActivity) {
            actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        }

        if (actionBar != null && project != null && scene != null && sprite != null) {
            if (project.getSceneList().size() > 1) {
                actionBar.setTitle(scene.getName() + ": " + sprite.getName());
            } else {
                actionBar.setTitle(sprite.getName());
            }
        }

        if (BackpackListManager.getInstance().isBackpackEmpty()) {
            BackpackListManager.getInstance().loadBackpack();
        }

        BottomBar.showBottomBar(getActivity());
        BottomBar.showPlayButton(getActivity());
        BottomBar.showAddButton(getActivity());

        if (BuildConfig.FEATURE_AI_ASSIST_ENABLED) {
            BottomBar.showAiAssistButton(getActivity());
        }

        if (sprite != null && adapter != null) {
            adapter.updateItems(sprite);
            if (DEBUG_SPRITE_PRINTER) {
                printSpriteScriptsToLog(sprite);
            }
        }

        if (savedListViewState != null && listView != null) {
            listView.onRestoreInstanceState(savedListViewState);
        }

        scrollToFocusItem();
        SnackbarUtil.showHintSnackbar(getActivity(), R.string.hint_scripts);

        runCodeAnalysis();

        refreshFastScroll();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (codeAnalysisJob != null) {
            codeAnalysisJob.cancel(null);
            codeAnalysisJob = null;
        }

        KoveAutocompleteController.getInstance().cancelActiveJob();
        KoveAutocompleteController.getInstance().dismissSuggestions();

        try {
            if (!(getActivity() instanceof org.catrobat.catroid.ui.dialogs.RuntimeConsoleActivity)) {
                Project currentProject = ProjectManager.getInstance().getCurrentProject();
                new ProjectSaver(currentProject, getContext()).saveProjectAsync();
            }
        } catch (Exception e) {
            Log.e(TAG, "'onPause' Critical Error");
            e.printStackTrace();
        }

        savedListViewState = listView.onSaveInstanceState();

        if (getActivity() instanceof SpriteActivity) {
            ((SpriteActivity) getActivity()).setUndoMenuItemVisibility(false);
        }

        AnalysisManager.INSTANCE.clearResults();
    }

    public BrickAdapter getAdapter() {
        return adapter;
    }

    private void runCodeAnalysis() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean isAnalysisEnabled = prefs.getBoolean("pref_code_analysis_enabled", true);

        if (codeAnalysisJob != null) {
            codeAnalysisJob.cancel(null);
            codeAnalysisJob = null;
        }

        AnalysisManager.INSTANCE.clearResults();

        if (!isAnalysisEnabled) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }

        Sprite currentSprite = ProjectManager.getInstance().getCurrentSprite();
        if (currentSprite == null) return;

        final List<Script> scriptsCopy;
        synchronized (currentSprite) {
            scriptsCopy = new ArrayList<>(currentSprite.getScriptList());
        }

        codeAnalysisJob = BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(this), Dispatchers.getIO(), CoroutineStart.DEFAULT, (scope, continuation) -> {
            final Map<Brick, AnalysisResult> allResults = new HashMap<>();

            try {
                final org.catrobat.catroid.codeanalysis.GlobalAnalysisContext globalContext =
                        org.catrobat.catroid.codeanalysis.GlobalAnalysisContext.Companion.build();

                int size = scriptsCopy.size();
                for (int i = 0; i < size; i++) {
                    if (i >= scriptsCopy.size()) {
                        break;
                    }
                    Script script = scriptsCopy.get(i);
                    if (script == null) {
                        continue;
                    }
                    Map<Brick, AnalysisResult> scriptResults = codeAnalyzer.analyzeScript(script, globalContext);
                    if (scriptResults != null) {
                        allResults.putAll(scriptResults);
                    }
                }

                AnalysisManager.INSTANCE.updateResults(allResults);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (ProjectManager.getInstance().getCurrentProject() == null) {
                            Log.d(TAG, "Project already closed, skipping UI update");
                            return;
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        updateAnalysisIndicator();
                    });
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error in analysis background job", t);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem detailsItem = menu.findItem(R.id.show_details);
        if (detailsItem != null) detailsItem.setVisible(false);

        MenuItem renameItem = menu.findItem(R.id.rename);
        if (renameItem != null) renameItem.setVisible(false);

        MenuItem catblocksItem = menu.findItem(R.id.catblocks_reorder_scripts);
        if (catblocksItem != null) catblocksItem.setVisible(false);

        MenuItem findItem = menu.findItem(R.id.find);
        if (findItem != null) findItem.setVisible(true);

        MenuItem indentItem = menu.findItem(R.id.menu_toggle_indentation);
        if (indentItem != null) {
            indentItem.setVisible(true);
            boolean enabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("pref_enable_brick_indentation", false);

            indentItem.setTitle(enabled ? R.string.menu_disable_indentation : R.string.menu_enable_indentation);
        }

        super.onPrepareOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (listView.isCurrentlyMoving()) {
			listView.highlightMovingItem();
			return true;
		}
		if (listView.isCurrentlyHighlighted()) {
			listView.cancelHighlighting();
		}
		switch (item.getItemId()) {
			case R.id.menu_undo:
				loadProjectAfterUndoOption();
				break;
			case R.id.backpack:
				prepareBackpackActionMode();
				break;
			case R.id.copy:
				prepareActionMode(COPY);
				break;
			case R.id.delete:
				prepareActionMode(DELETE);
				break;
			case R.id.comment_in_out:
				startActionMode(COMMENT);
				break;
			/*case R.id.catblocks:
				switchToCatblocks();
				break;*/
			case R.id.find:
				scriptFinder.open();
				break;
            case R.id.menu_toggle_indentation:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                boolean enabled = prefs.getBoolean("pref_enable_brick_indentation", true);
                prefs.edit().putBoolean("pref_enable_brick_indentation", !enabled).apply();

                adapter.notifyDataSetChanged();

                getActivity().invalidateOptionsMenu();
                break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void notifyDataSetChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
	}

	public boolean isCurrentlyMoving() {
		if (listView != null) {
			return listView.isCurrentlyMoving();
		}
		return false;
	}

	public void highlightMovingItem() {
		listView.highlightMovingItem();
	}

	public void cancelMove() {
		listView.cancelMove();
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		adapter.updateItems(sprite);
	}

	public boolean isCurrentlyHighlighted() {
		return listView.isCurrentlyHighlighted();
	}

	public void cancelHighlighting() {
		listView.cancelHighlighting();
	}

	private void showCategoryFragment() {
		BrickCategoryFragment brickCategoryFragment = new BrickCategoryFragment();
		brickCategoryFragment.setOnCategorySelectedListener(this);

		getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, brickCategoryFragment, BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG)
				.addToBackStack(BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG)
				.commit();
	}

	@Override
	public void onCategorySelected(String category) {
		ListFragment fragment = null;
		String tag = "";
		Fragment currentFragment = getParentFragmentManager().findFragmentById(R.id.fragment_container);
		if (category.equals(getContext().getString(R.string.category_user_bricks))) {
			fragment = UserDefinedBrickListFragment.newInstance(this);
			tag = UserDefinedBrickListFragment.USER_DEFINED_BRICK_LIST_FRAGMENT_TAG;
		} else if (currentFragment instanceof AddBrickFragment || category.equals(getContext().getString(R.string.category_search_bricks))) {
			fragment = BrickSearchFragment.newInstance(this, category);
			tag = BrickSearchFragment.BRICK_SEARCH_FRAGMENT_TAG;
		} else {
			fragment = AddBrickFragment.newInstance(category, this);
			tag = AddBrickFragment.ADD_BRICK_FRAGMENT_TAG;
		}

		getFragmentManager().beginTransaction()
				.add(R.id.fragment_container, fragment, tag)
				.addToBackStack(null)
				.commit();
	}

	protected void prepareActionMode(int type) {
		if (adapter.getCount() == 1) {
			switch (type) {
				case COPY:
					copy(adapter.getItems());
					break;
				case DELETE:
					delete(adapter.getItems());
					break;
				default:
					startActionMode(type);
					break;
			}
		} else {
			startActionMode(type);
		}
	}

	private void prepareBackpackActionMode() {
		if (adapter.getItems().size() == 1) {
			showNewScriptGroupAlert(adapter.getItems());
			return;
		}

		if (BackpackListManager.getInstance().getBackpackedScriptGroups().isEmpty()) {
			startActionMode(BACKPACK);
		} else if (adapter.isEmpty()) {
			switchToBackpack();
		} else {
			showBackpackModeChooser();
		}
	}

	private void startActionMode(@ActionModeType int type) {
		if (adapter.isEmpty()) {
			ToastUtil.showError(getActivity(), R.string.am_empty_list);
		} else {
			actionModeType = type;
			actionMode = getActivity().startActionMode(this);
			BottomBar.hideBottomBar(getActivity());
		}
	}

	@Override
	public void onSelectionChanged(int selectedItemCnt) {
		switch (actionModeType) {
			case BACKPACK:
				actionMode.setTitle(getString(R.string.am_backpack) + " " + selectedItemCnt);
				break;
			case COPY:
				actionMode.setTitle(getString(R.string.am_copy) + " " + selectedItemCnt);
				break;
			case DELETE:
				actionMode.setTitle(getString(R.string.am_delete) + " " + selectedItemCnt);
				break;
			case COMMENT:
				actionMode.setTitle(getString(R.string.comment_in_out) + " " + selectedItemCnt);
				break;
			case NONE:
				throw new IllegalStateException("ActionModeType not set Correctly");
			case CATBLOCKS:
				break;
		}
	}

	public void finishActionMode() {
		adapter.clearSelection();
		if (actionModeType != NONE) {
			actionMode.finish();
		}
	}

	public Brick findBrickByHash(int hashCode) {
		return adapter.findByHash(hashCode);
	}

	public void handleAddButton() {
		if (listView.isCurrentlyHighlighted()) {
			listView.cancelHighlighting();
		}
		if (listView.isCurrentlyMoving()) {
			listView.highlightMovingItem();
		} else {
            if (getActivity() instanceof SpriteActivity) {
                ((SpriteActivity) getActivity()).setUndoMenuItemVisibility(false);
            }
			showCategoryFragment();
		}
	}

	public void addBrick(Brick brick) {
        KoveAutocompleteController.getInstance().cancelActiveJob();
        KoveAutocompleteController.getInstance().dismissSuggestions();

		try {
			if (!brick.getClass().equals(UserDefinedReceiverBrick.class) && !brick.getClass().equals(UserDefinedBrick.class)) {
				RecentBrickListManager.getInstance().addBrick(brick.clone());
			}
		} catch (CloneNotSupportedException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		addBrick(brick, sprite, adapter, listView);
	}

	@VisibleForTesting
	public void addBrick(Brick brick, Sprite sprite, BrickAdapter brickAdapter, BrickListView brickListView) {
		if (brickAdapter.getCount() == 0) {
			if (brick instanceof ScriptBrick) {
				sprite.addScript(brick.getScript());
			} else {
				Script script = new StartScript();
				script.addBrick(brick);
				sprite.addScript(script);
			}
			brickAdapter.updateItems(sprite);
		} else if (brickAdapter.getCount() == 1 && !(brick instanceof ScriptBrick)) {
			sprite.getScriptList().get(0).addBrick(brick);
			brickAdapter.updateItems(sprite);
		} else {
			int firstVisibleBrick = brickListView.getFirstVisiblePosition();
			int lastVisibleBrick = brickListView.getLastVisiblePosition();
			int position = (1 + lastVisibleBrick - firstVisibleBrick) / 2;
			position += firstVisibleBrick;
			brickAdapter.addItem(position, brick);
			brickListView.startMoving(brick);
		}
	}

    @Override
    public void onBrickClick(Brick brick, int position) {
        if (listView.isCurrentlyHighlighted()) {
            listView.cancelHighlighting();
            return;
        }

        KoveAutocompleteController.getInstance().onUserActivity(
                ProjectManager.getInstance().getCurrentSprite(),
                brick.getScript(),
                brick.getPositionInScript(),
                adapter
        );

        List<Integer> options = getContextMenuItems(brick);
        List<String> names = new ArrayList<>();
        for (Integer option : options) {
            names.add(getString(option));
        }
        ListAdapter arrayAdapter = UiUtils.getAlertDialogAdapterForMenuIcons(options, names,
                requireContext(), requireActivity());

        View dialogTitleView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_brick_context, null);

        View brickView = ((org.catrobat.catroid.content.bricks.BrickBaseType) brick)
                .getFreshDetachedView(getContext());
        ViewGroup brickContainer = dialogTitleView.findViewById(R.id.brick_view_container);

        final int maxBrickHeight = (int) (200 * getContext().getResources().getDisplayMetrics().density);
        android.widget.ScrollView wrapperScrollView = new android.widget.ScrollView(getContext()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int heightMode = android.view.View.MeasureSpec.getMode(heightMeasureSpec);
                int heightSize = android.view.View.MeasureSpec.getSize(heightMeasureSpec);
                if (heightSize > maxBrickHeight || heightMode == android.view.View.MeasureSpec.UNSPECIFIED) {
                    heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(maxBrickHeight, android.view.View.MeasureSpec.AT_MOST);
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        wrapperScrollView.addView(brickView);
        brickContainer.addView(wrapperScrollView);

        TextView descriptionView = dialogTitleView.findViewById(R.id.brick_description);
        String description = BrickInfo.getDescription(brick);
        descriptionView.setText(description);

        AnalysisResult result = AnalysisManager.INSTANCE.getResultFor(brick);
        LinearLayout warningLayout = dialogTitleView.findViewById(R.id.warning_layout);

        if (result != null) {
            TextView warningTitle = dialogTitleView.findViewById(R.id.warning_title);
            TextView warningMessage = dialogTitleView.findViewById(R.id.warning_message);

            String title = (result.getSeverity() == Severity.ERROR) ? "Ошибка:" : "Предупреждение:";
            int color = (result.getSeverity() == Severity.ERROR) ? 0xFFFF4444 : 0xFFFFBB33;

            warningTitle.setText(title);
            warningTitle.setTextColor(color);
            warningMessage.setText(result.getMessage());

            warningLayout.setVisibility(View.VISIBLE);
        } else {
            warningLayout.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(getContext())
                .setCustomTitle(dialogTitleView)
                .setAdapter(arrayAdapter, (dialog, which) ->
                        handleContextMenuItemClick(options.get(which), brick, position)
                ).show();
    }

	@VisibleForTesting
	public static List<Integer> getContextMenuItems(Brick brick) {
		List<Integer> items = new ArrayList<>();

        if (brick instanceof org.catrobat.catroid.content.bricks.CompositeBrick) {
            if (org.catrobat.catroid.utils.BrickCollapseManager.INSTANCE.isCollapsed(brick)) {
                items.add(R.string.brick_context_dialog_expand);
            } else {
                items.add(R.string.brick_context_dialog_collapse);
            }
        }

		if (brick instanceof UserDefinedReceiverBrick) {
			items.add(R.string.backpack_add);
			items.add(R.string.brick_context_dialog_delete_definition);
			items.add(R.string.brick_context_dialog_move_definition);
			items.add(R.string.brick_context_dialog_help);
			return items;
		}

        items.add(R.string.brick_context_dialog_cut);

        if (org.catrobat.catroid.utils.BlockClipboard.getInstance().getLatest() != null) {
            items.add(R.string.brick_context_dialog_paste_below);
            items.add(R.string.brick_context_dialog_clipboard_history);
        }

		if (brick instanceof ScriptBrick) {
			items.add(R.string.backpack_add);

			if (!(brick instanceof EmptyEventBrick)) {
				items.add(brick.isCommentedOut()
						? R.string.brick_context_dialog_comment_in_script
						: R.string.brick_context_dialog_comment_out_script);
			}

			items.add(R.string.brick_context_dialog_copy_script);

			items.add(R.string.brick_context_dialog_delete_script);

			if (brick instanceof FormulaBrick && ((FormulaBrick) brick).hasEditableFormulaField()) {
				items.add(R.string.brick_context_dialog_formula_edit_brick);
			}
			items.add(R.string.brick_context_dialog_move_script);

			items.add(R.string.brick_context_dialog_help);
		} else {
			items.add(R.string.brick_context_dialog_copy_brick);
			if (brick.consistsOfMultipleParts()) {
				items.add(R.string.brick_context_dialog_highlight_brick_parts);
			}
			items.add(R.string.brick_context_dialog_delete_brick);

			items.add(brick.isCommentedOut()
					? R.string.brick_context_dialog_comment_in
					: R.string.brick_context_dialog_comment_out);
			if (brick instanceof VisualPlacementBrick && ((VisualPlacementBrick) brick).areAllBrickFieldsNumbers()) {
				items.add(R.string.brick_option_place_visually);
			}
			if (brick instanceof FormulaBrick && ((FormulaBrick) brick).hasEditableFormulaField()) {
				items.add(R.string.brick_context_dialog_formula_edit_brick);
			}
			if (brick.equals(brick.getAllParts().get(0))) {
				items.add(R.string.brick_context_dialog_move_brick);
			}

			if (brick.hasHelpPage()) {
				items.add(R.string.brick_context_dialog_help);
			}
		}

        items.add(R.string.brick_context_dialog_system_info);

		return items;
	}

	private void handleContextMenuItemClick(int itemId, Brick brick, int position) {
		showUndo(false);
		switch (itemId) {
			case R.string.backpack_add:
				List<Brick> bricksToPack = new ArrayList<>();
				brick.addToFlatList(bricksToPack);
				showNewScriptGroupAlert(bricksToPack);
				break;
			case R.string.brick_context_dialog_copy_brick:
			case R.string.brick_context_dialog_copy_script:
				try {
					Brick clonedBrick = brick.getAllParts().get(0).clone();
					adapter.addItem(position, clonedBrick);
					listView.startMoving(clonedBrick);
				} catch (CloneNotSupportedException e) {
					ToastUtil.showError(getContext(), R.string.error_copying_brick);
					Log.e(TAG, Log.getStackTraceString(e));
				}
				break;
			case R.string.brick_context_dialog_delete_brick:
			case R.string.brick_context_dialog_delete_script:
				showDeleteAlert(brick.getAllParts());
				break;
			case R.string.brick_context_dialog_delete_definition:
				showDeleteAlert(brick.getAllParts());
				break;
			case R.string.brick_context_dialog_comment_in:
			case R.string.brick_context_dialog_comment_in_script:
				for (Brick brickPart : brick.getAllParts()) {
					brickPart.setCommentedOut(false);
				}
				adapter.notifyDataSetChanged();
				break;
			case R.string.brick_context_dialog_comment_out:
			case R.string.brick_context_dialog_comment_out_script:
				for (Brick brickPart : brick.getAllParts()) {
					brickPart.setCommentedOut(true);
				}
				adapter.notifyDataSetChanged();
				break;
			case R.string.brick_option_place_visually:
				VisualPlacementBrick visualPlacementBrick = (VisualPlacementBrick) brick;
				visualPlacementBrick.placeVisually(visualPlacementBrick.getXBrickField(),
						visualPlacementBrick.getYBrickField());
				break;
			case R.string.brick_context_dialog_formula_edit_brick:
				((FormulaBrick) brick).onClick(listView);
				break;
			case R.string.brick_context_dialog_move_brick:
			case R.string.brick_context_dialog_move_script:
			case R.string.brick_context_dialog_move_definition:
				onBrickLongClick(brick, position);
				break;
			case R.string.brick_context_dialog_help:
				openWebViewWithHelpPage(brick);
				break;
			case R.string.brick_context_dialog_highlight_brick_parts:
				List<Brick> bricksOfControlStructure = brick.getAllParts();
				List<Integer> positions = new ArrayList<>();
				for (Brick brickInControlStructure : bricksOfControlStructure) {
					positions.add(adapter.getPosition(brickInControlStructure));
				}
				listView.highlightControlStructureBricks(positions);
				break;
            case R.string.brick_context_dialog_collapse:
            case R.string.brick_context_dialog_expand:
                org.catrobat.catroid.utils.BrickCollapseManager.INSTANCE.toggleCollapsed(brick);
                adapter.updateItems(ProjectManager.getInstance().getCurrentSprite());
                break;
            case R.string.brick_context_dialog_system_info:
                showDetailedSystemInfoDialog(brick);
                break;
            case R.string.brick_context_dialog_cut:
                if (copyProjectForUndoOption()) {
                    showUndo(true);
                    setUndoBrickPosition(brick);
                }
                org.catrobat.catroid.utils.BlockClipboard.getInstance().copy(brick.getAllParts());
                Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
                brickController.delete(brick.getAllParts(), sprite);
                adapter.updateItems(sprite);
                break;

            case R.string.brick_context_dialog_paste_below:
                List<Brick> latestBricks = org.catrobat.catroid.utils.BlockClipboard.getInstance().getLatest();
                if (latestBricks != null) {
                    pasteBricksBelow(latestBricks, brick);
                }
                break;

            case R.string.brick_context_dialog_clipboard_history:
                showClipboardHistoryDialog(brick);
                break;
		}
	}

    private void pasteBricksBelow(List<Brick> originalBricks, Brick targetBrick) {
        if (originalBricks == null || originalBricks.isEmpty()) return;

        Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
        List<Brick> clonedBricks = new ArrayList<>();

        for (Brick b : originalBricks) {
            try {
                clonedBricks.add(b.clone());
            } catch (CloneNotSupportedException e) {
                Log.e(TAG, "Ошибка клонирования при вставке", e);
            }
        }

        if (clonedBricks.isEmpty()) return;

        if (copyProjectForUndoOption()) {
            showUndo(true);
        }

        if (clonedBricks.get(0) instanceof ScriptBrick) {
            for (Brick b : clonedBricks) {
                if (b instanceof ScriptBrick) {
                    sprite.addScript(((ScriptBrick) b).getScript());
                }
            }
        } else {
            List<Brick> targetList = targetBrick.getDragAndDropTargetList();
            int startIndex = targetBrick.getPositionInDragAndDropTargetList() + 1;

            for (int i = 0; i < clonedBricks.size(); i++) {
                Brick b = clonedBricks.get(i);
                b.setParent(targetBrick.getParent());
                if (startIndex + i <= targetList.size()) {
                    targetList.add(startIndex + i, b);
                } else {
                    targetList.add(b);
                }
            }
        }

        adapter.updateItems(sprite);
    }

    private void showClipboardHistoryDialog(Brick targetBrick) {
        List<List<Brick>> history = org.catrobat.catroid.utils.BlockClipboard.getInstance().getHistory();
        if (history.isEmpty()) return;

        List<String> displayNames = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            List<Brick> group = history.get(i);
            StringBuilder sb = new StringBuilder();
            sb.append(i + 1).append(". ");
            if (group.size() == 1) {
                sb.append(group.get(0).getClass().getSimpleName());
            } else {
                sb.append("Group (").append(group.size()).append(" bricks): ");
                for (int j = 0; j < Math.min(3, group.size()); j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(group.get(j).getClass().getSimpleName());
                }
                if (group.size() > 3) sb.append("...");
            }
            displayNames.add(sb.toString());
        }

        new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.brick_context_dialog_clipboard_history))
                .setItems(displayNames.toArray(new CharSequence[0]), (dialog, which) -> {
                    List<Brick> selectedGroup = history.get(which);
                    pasteBricksBelow(selectedGroup, targetBrick);
                })
                .setNegativeButton(getContext().getString(R.string.cancel), null)
                .show();
    }

	private void openWebViewWithHelpPage(Brick brick) {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		Sprite backgroundSprite = ProjectManager.getInstance().getCurrentlyEditedScene().getBackgroundSprite();
		String category = new CategoryBricksFactory().getBrickCategory(brick, sprite == backgroundSprite, getContext());

		String brickHelpUrl = brick.getHelpUrl(category);
		Intent intent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(brickHelpUrl));
		startActivity(intent);
	}

	@Override
	public boolean onBrickLongClick(Brick brick, int position) {
		showUndo(false);
		if (listView.isCurrentlyHighlighted()) {
			listView.cancelHighlighting();
		} else {
			listView.startMoving(brick);
		}
		return true;
	}

	private void showBackpackModeChooser() {
		CharSequence[] items = new CharSequence[] {getString(R.string.pack), getString(R.string.unpack)};
		new AlertDialog.Builder(getContext())
				.setTitle(R.string.backpack_title)
				.setItems(items, (dialog, which) -> {
					switch (which) {
						case 0:
							startActionMode(BACKPACK);
							break;
						case 1:
							switchToBackpack();
					}
				})
				.show();
	}

	public void showNewScriptGroupAlert(List<Brick> selectedBricks) {
		TextInputDialog.Builder builder = new TextInputDialog.Builder(getContext());
		DuplicateInputTextWatcher duplicateInputTextwatcher = new DuplicateInputTextWatcher(null);
		duplicateInputTextwatcher.setScope(BackpackListManager.getInstance().getBackpackedScriptGroups());
		builder.setText(new UniqueNameProvider().getUniqueName(getString(R.string.default_script_group_name), BackpackListManager.getInstance().getBackpackedScriptGroups()));

		builder.setHint(getString(R.string.script_group_label))
				.setTextWatcher(duplicateInputTextwatcher)
				.setPositiveButton(getString(R.string.ok), (TextInputDialog.OnClickListener) (dialog, textInput) -> pack(textInput, selectedBricks));

		builder.setTitle(R.string.new_group)
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	public void pack(String name, List<Brick> selectedBricks) {
		try {
			scriptController.pack(name, selectedBricks);
			ToastUtil.showSuccess(getActivity(), getString(R.string.packed_script_group));
			switchToBackpack();
		} catch (CloneNotSupportedException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}

		finishActionMode();
	}

	private void switchToBackpack() {
        View workspace = getActivity() != null ? getActivity().findViewById(R.id.workspace_layout) : null;
        if (workspace != null && workspace.getVisibility() == View.VISIBLE) {
            org.catrobat.catroid.ui.workspace.WorkspaceLayout workspaceLayout =
                    (org.catrobat.catroid.ui.workspace.WorkspaceLayout) workspace;

            workspaceLayout.openWindow("Backpack_Scripts", "Рюкзак: Скрипты",
                    org.catrobat.catroid.ui.recyclerview.backpack.BackpackScriptFragment::new);
            return;
        }

		Intent intent = new Intent(getActivity(), BackpackActivity.class);
		intent.putExtra(BackpackActivity.EXTRA_FRAGMENT_POSITION, BackpackActivity.FRAGMENT_SCRIPTS);
		startActivity(intent);
	}

	private void switchToCatblocks() {
		if (!BuildConfig.FEATURE_CATBLOCKS_ENABLED) {
			return;
		}

		int firstVisible = listView.getFirstVisiblePosition();
		UUID firstVisibleBrickID = null;
		if (listView.getCount() > 0 && firstVisible >= 0) {
			Object firstVisibleObject = listView.getItemAtPosition(firstVisible);
			if (firstVisibleObject instanceof Brick) {
				Brick firstVisibleBrick = (Brick) firstVisibleObject;
				if (firstVisibleBrick instanceof ScriptBrick) {
					firstVisibleBrickID = firstVisibleBrick.getScript().getScriptId();
				} else {
					firstVisibleBrickID = firstVisibleBrick.getBrickID();
				}
			}
		}

		SettingsFragment.setUseCatBlocks(getContext(), true);

		CatblocksScriptFragment catblocksFragment = new CatblocksScriptFragment(firstVisibleBrickID);

		FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.fragment_container, catblocksFragment,
				CatblocksScriptFragment.Companion.getTAG());
		fragmentTransaction.commit();
	}

	private void copy(List<Brick> selectedBricks) {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		brickController.copy(selectedBricks, sprite);
		adapter.updateItems(sprite);
		finishActionMode();
	}

	private void showDeleteAlert(List<Brick> selectedBricks) {
		if (selectedBricks.size() > 0 && copyProjectForUndoOption()) {
			showUndo(true);
			undoBrickPosition = adapter.getPosition(selectedBricks.get(0));
		}
		delete(selectedBricks);
	}

	private void delete(List<Brick> selectedItems) {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		brickController.delete(selectedItems, sprite);
		adapter.updateItems(sprite);
		finishActionMode();
	}

	private void toggleComments(List<Brick> selectedBricks) {
		for (Brick brick : adapter.getItems()) {
			brick.setCommentedOut(selectedBricks.contains(brick));
		}
		finishActionMode();
	}

	public void setUndoBrickPosition(Brick brick) {
		undoBrickPosition = adapter.getPosition(brick);
	}

	public boolean copyProjectForUndoOption() {
		ProjectManager projectManager = ProjectManager.getInstance();
		Sprite currentSprite = projectManager.getCurrentSprite();
		currentSpriteName = currentSprite.getName();
		currentSceneName = projectManager.getCurrentlyEditedScene().getName();
		Project project = projectManager.getCurrentProject();
		XstreamSerializer.getInstance().saveProject(project);
		File currentCodeFile = new File(project.getDirectory(), CODE_XML_FILE_NAME);
		File undoCodeFile = new File(project.getDirectory(), UNDO_CODE_XML_FILE_NAME);

		if (currentCodeFile.exists()) {
			try {
				StorageOperations.transferData(currentCodeFile, undoCodeFile);
				saveVariables();
				return true;
			} catch (IOException exception) {
				Log.e(TAG, "Copying project " + project.getName() + " failed.", exception);
			}
		}
		return false;
	}

	public void loadProjectAfterUndoOption() {
		Project project = ProjectManager.getInstance().getCurrentProject();
		File currentCodeFile = new File(project.getDirectory(), CODE_XML_FILE_NAME);
		File undoCodeFile = new File(project.getDirectory(), UNDO_CODE_XML_FILE_NAME);

		if (currentCodeFile.exists()) {
			try {
				StorageOperations.transferData(undoCodeFile, currentCodeFile);
				new ProjectLoader(project.getDirectory(), getContext()).setListener(this).loadProjectAsync();
			} catch (IOException exception) {
				Log.e(TAG, "Replaceing project " + project.getName() + " failed.", exception);
			}
		}
	}

    @Override
    public void onLoadFinished(boolean success) {
        if (!success || !isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }

        ProjectManager.getInstance().setCurrentSceneAndSprite(currentSceneName, currentSpriteName);
        if (checkVariables()) {
            loadVariables();
        }
        refreshFragmentAfterUndo();

        refreshFastScroll();
    }

	private void saveVariables() {
		ProjectManager projectManager = ProjectManager.getInstance();
		Sprite currentSprite = projectManager.getCurrentSprite();
		Project project = projectManager.getCurrentProject();

		savedUserVariables = project.getUserVariablesCopy();
		savedMultiplayerVariables = project.getMultiplayerVariablesCopy();
		savedUserLists = project.getUserListsCopy();
		savedLocalUserVariables = currentSprite.getUserVariablesCopy();
		savedLocalLists = currentSprite.getUserListsCopy();
	}

	public boolean checkVariables() {
		ProjectManager projectManager = ProjectManager.getInstance();
		Sprite currentSprite = projectManager.getCurrentSprite();
		Project project = projectManager.getCurrentProject();

		return (project.hasUserDataChanged(project.getUserVariables(), savedUserVariables)
				|| project.hasUserDataChanged(project.getMultiplayerVariables(), savedMultiplayerVariables)
				|| project.hasUserDataChanged(project.getUserLists(), savedUserLists)
				|| currentSprite.hasUserDataChanged(currentSprite.getUserVariables(), savedLocalUserVariables)
				|| currentSprite.hasUserDataChanged(currentSprite.getUserLists(), savedLocalLists));
	}

	private void loadVariables() {
		ProjectManager projectManager = ProjectManager.getInstance();
		Sprite currentSprite = projectManager.getCurrentSprite();
		Project project = projectManager.getCurrentProject();

		project.restoreUserDataValues(project.getUserVariables(), savedUserVariables);
		project.restoreUserDataValues(project.getMultiplayerVariables(), savedMultiplayerVariables);
		project.restoreUserDataValues(project.getUserLists(), savedUserLists);
		currentSprite.restoreUserDataValues(currentSprite.getUserVariables(), savedLocalUserVariables);
		currentSprite.restoreUserDataValues(currentSprite.getUserLists(), savedLocalLists);
	}

    private void refreshFragmentAfterUndo() {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }

        this.currentSprite = ProjectManager.getInstance().getCurrentSprite();

        if (adapter != null && this.currentSprite != null) {
            adapter.updateItems(this.currentSprite);
            adapter.notifyDataSetChanged();
        }

        runCodeAnalysis();

        if (listView != null && (undoBrickPosition < listView.getFirstVisiblePosition() || undoBrickPosition > listView.getLastVisiblePosition())) {
            listView.post(() -> {
                if (listView != null) {
                    listView.setSelection(undoBrickPosition);
                }
            });
        }
    }

    public void showUndo(boolean visible) {
        if (getActivity() instanceof SpriteActivity) {
            ((SpriteActivity) getActivity()).showUndo(visible);
        }
    }

	private void scrollToFocusItem() {
		if (scriptToFocus == null && brickToFocus == null) {
			return;
		}

		int scrollToIndex = -1;
		for (int i = 0; i < listView.getAdapter().getCount(); ++i) {
			Object item = listView.getItemAtPosition(i);
			if (!(item instanceof Brick)) {
				continue;
			}
			Brick brick = (Brick) item;
			if ((brickToFocus != null && brick == brickToFocus)
					|| (scriptToFocus != null && brick.getScript() == scriptToFocus)) {
				scrollToIndex = i;
				break;
			}
		}
		if (scrollToIndex == -1) {
			return;
		}
		if (getActivity() != null) {
			int finalScrollToIndex = scrollToIndex;
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					listView.setSelection(finalScrollToIndex);
				}
			});
		}
		scriptToFocus = null;
		brickToFocus = null;
	}

	public int getActionModeType() {
		return actionModeType;
	}

	public boolean isFinderOpen() {
		return scriptFinder.isOpen();
	}

	public void closeFinder() {
		if (!scriptFinder.isClosed()) {
			scriptFinder.close();
		}
	}

    private void updateAnalysisIndicator() {
        if (analysisStatusIndicator == null || errorCountText == null || warningCountText == null) {
            return;
        }

        int errorCount = org.catrobat.catroid.codeanalysis.AnalysisManager.INSTANCE.getErrorCount();
        int warningCount = org.catrobat.catroid.codeanalysis.AnalysisManager.INSTANCE.getWarningCount();

        if (errorCount == 0 && warningCount == 0) {
            analysisStatusIndicator.setVisibility(View.GONE);
        } else {
            analysisStatusIndicator.setVisibility(View.VISIBLE);
            errorCountText.setText(String.valueOf(errorCount));
            warningCountText.setText(String.valueOf(warningCount));

            View errorDot = analysisStatusIndicator.findViewById(R.id.error_dot);
            if (errorDot != null) errorDot.setVisibility(errorCount > 0 ? View.VISIBLE : View.GONE);
            errorCountText.setVisibility(errorCount > 0 ? View.VISIBLE : View.GONE);

            View warningDot = analysisStatusIndicator.findViewById(R.id.warning_dot);
            if (warningDot != null) warningDot.setVisibility(warningCount > 0 ? View.VISIBLE : View.GONE);
            warningCountText.setVisibility(warningCount > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void showAnalysisSummaryDialog() {
        java.util.Map<Brick, org.catrobat.catroid.codeanalysis.AnalysisResult> results =
                org.catrobat.catroid.codeanalysis.AnalysisManager.INSTANCE.getResults();

        if (results.isEmpty() || getContext() == null) {
            return;
        }

        Context context = getContext();
        float density = context.getResources().getDisplayMetrics().density;

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        for (java.util.Map.Entry<Brick, org.catrobat.catroid.codeanalysis.AnalysisResult> entry : results.entrySet()) {
            final Brick brick = entry.getKey();
            org.catrobat.catroid.codeanalysis.AnalysisResult result = entry.getValue();

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding((int) (12 * density), (int) (12 * density), (int) (12 * density), (int) (12 * density));

            android.graphics.drawable.StateListDrawable stateListDrawable = new android.graphics.drawable.StateListDrawable();

            android.graphics.drawable.GradientDrawable pressedDrawable = new android.graphics.drawable.GradientDrawable();
            pressedDrawable.setColor(context.getResources().getColor(R.color.surface_card_pressed));
            pressedDrawable.setCornerRadius(8 * density);

            android.graphics.drawable.GradientDrawable normalDrawable = new android.graphics.drawable.GradientDrawable();
            normalDrawable.setColor(context.getResources().getColor(R.color.surface_card));
            normalDrawable.setCornerRadius(8 * density);

            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, pressedDrawable);
            stateListDrawable.addState(new int[]{}, normalDrawable);

            row.setBackground(stateListDrawable);
            row.setClickable(true);
            row.setFocusable(true);

            row.setOnClickListener(v -> {
                org.catrobat.catroid.content.bricks.Brick parentBrick = brick.getParent();
                boolean anyExpanded = false;

                while (parentBrick != null) {
                    if (org.catrobat.catroid.utils.BrickCollapseManager.INSTANCE.isCollapsed(parentBrick)) {
                        org.catrobat.catroid.utils.BrickCollapseManager.INSTANCE.setCollapsed(parentBrick, false);
                        anyExpanded = true;
                    }
                    parentBrick = parentBrick.getParent();
                }

                if (anyExpanded) {
                    adapter.updateItems(org.catrobat.catroid.ProjectManager.getInstance().getCurrentSprite());
                }

                int position = adapter.getPosition(brick);
                if (position != -1) {
                    listView.smoothScrollToPosition(position);
                    highlightBrickAtIndex(position);
                    listView.invalidate();
                }
                if (dialogHolder[0] != null) {
                    dialogHolder[0].dismiss();
                }
            });

            View marker = new View(context);
            int markerColor = (result.getSeverity() == org.catrobat.catroid.codeanalysis.Severity.ERROR)
                    ? context.getResources().getColor(R.color.red)
                    : context.getResources().getColor(R.color.orange);
            marker.setBackgroundColor(markerColor);

            LinearLayout.LayoutParams markerParams = new LinearLayout.LayoutParams((int) (4 * density), ViewGroup.LayoutParams.MATCH_PARENT);
            markerParams.setMargins(0, 0, (int) (12 * density), 0);
            marker.setLayoutParams(markerParams);
            row.addView(marker);

            LinearLayout textContainer = new LinearLayout(context);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textContainer.setLayoutParams(textParams);

            TextView msgView = new TextView(context);
            msgView.setText(result.getMessage());
            msgView.setTextColor(context.getResources().getColor(R.color.solid_white));
            msgView.setTextSize(14);
            msgView.setPadding(0, 0, 0, (int) (4 * density));
            textContainer.addView(msgView);

            TextView subView = new TextView(context);
            subView.setText(context.getString(R.string.analysis_click_to_jump));
            subView.setTextColor(context.getResources().getColor(R.color.text_secondary));
            subView.setTextSize(11);
            textContainer.addView(subView);

            row.addView(textContainer);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, (int) (8 * density));
            row.setLayoutParams(rowParams);

            container.addView(row);
        }

        scrollView.addView(container);

        dialogHolder[0] = new androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.analysis_dialog_title)
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .create();

        dialogHolder[0].show();
    }

    private void showDetailedSystemInfoDialog(Brick brick) {
        Context context = getContext();
        if (context == null) return;
        float density = context.getResources().getDisplayMetrics().density;

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        scrollView.setPadding((int) (16 * density), (int) (12 * density), (int) (16 * density), (int) (12 * density));

        TextView infoTextView = new TextView(context);
        infoTextView.setText(getBrickSystemInfo(brick));
        infoTextView.setTextColor(context.getResources().getColor(R.color.solid_white));
        infoTextView.setTextSize(13);
        infoTextView.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.NORMAL));

        scrollView.addView(infoTextView);

        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_add_brick_system_info_title)
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();
    }

    private String getBrickSystemInfo(Brick brick) {
        StringBuilder sb = new StringBuilder();
        sb.append("Class Name:\n");
        sb.append(brick.getClass().getName()).append("\n");
        sb.append("---------------------------------------\n\n");

        Class<?> superclass = brick.getClass().getSuperclass();
        String superclassName = (superclass != null) ? superclass.getSimpleName() : "None";
        sb.append("Superclass:\n");
        sb.append(superclassName).append("\n");
        sb.append("---------------------------------------\n\n");

        if (brick instanceof org.catrobat.catroid.content.bricks.FormulaBrick) {
            org.catrobat.catroid.content.bricks.FormulaBrick formulaBrick =
                    (org.catrobat.catroid.content.bricks.FormulaBrick) brick;
            java.util.Map<org.catrobat.catroid.content.bricks.Brick.BrickField, org.catrobat.catroid.formulaeditor.Formula> formulas =
                    formulaBrick.getAllFormulaFieldsWithFormulas();
            if (formulas != null && !formulas.isEmpty()) {
                sb.append("Formula Fields (BrickFields):\n");;
                for (java.util.Map.Entry<org.catrobat.catroid.content.bricks.Brick.BrickField, org.catrobat.catroid.formulaeditor.Formula> entry : formulas.entrySet()) {
                    sb.append("  * ").append(entry.getKey()).append(" = ")
                            .append(entry.getValue().getTrimmedFormulaString(getContext())).append("\n");
                }
                sb.append("---------------------------------------\n\n");
            }
        }

        if (brick instanceof org.catrobat.catroid.content.bricks.UserDataBrick) {
            org.catrobat.catroid.content.bricks.UserDataBrick userDataBrick =
                    (org.catrobat.catroid.content.bricks.UserDataBrick) brick;
            Map<Brick.BrickData, UserData> data =
                    userDataBrick.getAllBrickDataWithValues();
            if (data != null && !data.isEmpty()) {
                sb.append("Data Fields (UserData):\n");
                for (Map.Entry<Brick.BrickData, UserData> entry : data.entrySet()) {
                    sb.append("  * ").append(entry.getKey()).append(" = ")
                            .append(entry.getValue()).append("\n");
                }
                sb.append("---------------------------------------\n\n");
            }
        }

        sb.append("Declared Fields:\n");
        java.lang.reflect.Field[] fields = brick.getClass().getDeclaredFields();
        if (fields.length > 0) {
            for (java.lang.reflect.Field field : fields) {
                try {
                    field.setAccessible(true);
                    String nameStr = field.getName();
                    if (!nameStr.startsWith("$") && !nameStr.equals("serialVersionUID") && !nameStr.equals("Companion")) {
                        String type = field.getType().getSimpleName();
                        Object value = field.get(brick);
                        sb.append("  * ").append(nameStr).append(" (").append(type).append(") = ")
                                .append(value != null ? value.toString() : "null").append("\n");
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            sb.append("  * No declared fields found\n");
        }
        sb.append("---------------------------------------\n\n");

        return sb.toString();
    }

    private void printSpriteScriptsToLog(Sprite sprite) {
        if (sprite == null) return;
        Context context = getContext();
        if (context == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("\n==================================================\n");
        sb.append(" SCRIPTS FOR SPRITE: ").append(sprite.getName().toUpperCase()).append("\n");
        sb.append("==================================================\n");

        List<Script> scriptList = sprite.getScriptList();
        if (scriptList == null || scriptList.isEmpty()) {
            sb.append(" (Скрипты отсутствуют)\n");
        } else {
            for (Script script : scriptList) {
                sb.append("\n").append(formatScriptHeader(script)).append("\n");
                List<Brick> bricks = getBricksFromScript(script);
                printBricks(bricks, 1, sb, context);
            }
        }
        sb.append("==================================================\n");

        String fullLog = sb.toString();
        int maxLogSize = 2000;
        for (int i = 0; i <= fullLog.length() / maxLogSize; i++) {
            int start = i * maxLogSize;
            int end = Math.min((i + 1) * maxLogSize, fullLog.length());
            if (start < end) {
                Log.d("SpritePrinter", fullLog.substring(start, end));
            }
        }
    }

    private void printBricks(List<Brick> bricks, int indentLevel, StringBuilder sb, Context context) {
        if (bricks == null || bricks.isEmpty()) return;

        for (Brick brick : bricks) {
            boolean isComposite = brick instanceof org.catrobat.catroid.content.bricks.CompositeBrick;

            String indent = getIndentString(indentLevel);
            String formatted = formatBrick(brick, context);

            if (brick.isCommentedOut()) {
                sb.append(indent).append("// [ОТКЛЮЧЕН] ").append(formatted).append("\n");
            } else {
                sb.append(indent).append(formatted).append("\n");
            }

            if (isComposite) {
                org.catrobat.catroid.content.bricks.CompositeBrick composite =
                        (org.catrobat.catroid.content.bricks.CompositeBrick) brick;

                List<Brick> nested = composite.getNestedBricks();
                if (nested != null && !nested.isEmpty()) {
                    printBricks(nested, indentLevel + 1, sb, context);
                }

                if (composite.hasSecondaryList()) {
                    List<Brick> secondary = composite.getSecondaryNestedBricks();
                    if (secondary != null && !secondary.isEmpty()) {
                        sb.append(getIndentString(indentLevel)).append("IfLogicElseBrick\n");
                        printBricks(secondary, indentLevel + 1, sb, context);
                    }
                }

                String endName = getEndBlockName(brick);
                sb.append(indent).append(endName).append("\n");
            }
        }
    }

    private String getEndBlockName(Brick brick) {
        String name = brick.getClass().getSimpleName();
        if (name.contains("Forever") || name.contains("LoopEndless")) {
            return "LoopEndBrick";
        }
        if (name.contains("If") || name.contains("IfThen") || name.contains("IfLogic")) {
            return "IfThenLogicEndBrick";
        }
        if (name.contains("Repeat")) {
            return "LoopEndBrick";
        }
        if (name.contains("ForVariable") || name.contains("ForItem")) {
            return "LoopEndBrick";
        }
        if (name.contains("TryCatchFinally")) {
            return "TryCatchFinallyEndBrick";
        }
        return "EndBlock";
    }

    private String formatScriptHeader(Script script) {
        String name = script.getClass().getSimpleName();
        switch (name) {
            case "StartScript": return "При запуске сцены:";
            case "WhenScript": return "При нажатии на спрайт:";
            case "WhenTouchDownScript": return "При касании экрана:";
            case "BroadcastScript": return "Когда я получу вещание:";
            case "WhenConditionScript": return "Когда условие становится истинным:";
            case "WhenClonedScript": return "Когда я начинаю как клон:";
            case "WhenBackgroundChangesScript": return "Когда фон меняется:";
            default: return "Скрипт [" + name + "]:";
        }
    }

    private String formatBrick(Brick brick, Context context) {
        String className = brick.getClass().getSimpleName();
        StringBuilder sb = new StringBuilder(className);

        Map<Brick.BrickField, String> printedFormulas = new java.util.LinkedHashMap<>();
        if (brick instanceof FormulaBrick) {
            FormulaBrick formulaBrick = (FormulaBrick) brick;
            Map<Brick.BrickField, Formula> formulas = formulaBrick.getAllFormulaFieldsWithFormulas();
            if (formulas != null) {
                for (Map.Entry<Brick.BrickField, Formula> entry : formulas.entrySet()) {
                    String valStr = "null";
                    if (entry.getValue() != null) {
                        try {
                            valStr = entry.getValue().getTrimmedFormulaString(context);
                        } catch (Exception e) {
                            valStr = entry.getValue().toString();
                        }
                    }
                    printedFormulas.put(entry.getKey(), valStr);
                }
            }
        }

        List<String> parameters = new ArrayList<>();

        for (Map.Entry<Brick.BrickField, String> entry : printedFormulas.entrySet()) {
            parameters.add(entry.getKey().name() + ": '" + entry.getValue() + "'");
        }

        Class<?> clazz = brick.getClass();
        while (clazz != null && clazz != Object.class) {
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                try {
                    field.setAccessible(true);
                    String name = field.getName();

                    if (name.startsWith("$") ||
                            name.equals("serialVersionUID") ||
                            name.equals("Companion") ||
                            name.equals("parent") ||
                            name.equals("view") ||
                            name.equals("checkbox") ||
                            name.equals("formulaMap") ||
                            name.equals("brickFieldToTextViewIdMap") ||
                            name.contains("cached") ||
                            name.contains("spinnerValues") ||
                            java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    Class<?> type = field.getType();
                    if (type.isPrimitive() ||
                            type.isEnum() ||
                            type == String.class ||
                            Number.class.isAssignableFrom(type) ||
                            type == Boolean.class) {

                        Object val = field.get(brick);
                        if (val != null) {
                            boolean alreadyPrinted = false;
                            for (Brick.BrickField bf : printedFormulas.keySet()) {
                                if (bf.name().equalsIgnoreCase(name)) {
                                    alreadyPrinted = true;
                                    break;
                                }
                            }
                            if (!alreadyPrinted) {
                                parameters.add(name + ": " + val.toString());
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (!parameters.isEmpty()) {
            sb.append(" [").append(android.text.TextUtils.join(", ", parameters)).append("]");
        }

        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Brick> getBricksFromScript(Script script) {
        try {
            java.lang.reflect.Method getBrickListMethod = script.getClass().getMethod("getBrickList");
            return (List<Brick>) getBrickListMethod.invoke(script);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getBricksMethod = script.getClass().getMethod("getBricks");
                return (List<Brick>) getBricksMethod.invoke(script);
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }
    }

    private String getIndentString(int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }
        return indent.toString();
    }

    private void refreshFastScroll() {
        if (listView != null) {
            listView.setFastScrollEnabled(false);
            listView.setFastScrollEnabled(true);
            listView.setFastScrollAlwaysVisible(true);
        }
    }
}
