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
package org.catrobat.catroid.ui.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.danvexteam.lunoscript_annotations.LunoClass;
import com.google.common.io.Files;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.UserDefinedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrick;
import org.catrobat.catroid.content.strategy.ShowFormulaEditorStrategy;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaEditorEditText;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.FormulaSpannableStringBuilder;
import org.catrobat.catroid.formulaeditor.InternFormulaKeyboardAdapter;
import org.catrobat.catroid.formulaeditor.InternFormulaParser;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.formulaeditor.UndoState;
import org.catrobat.catroid.formulaeditor.UserData;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.ui.BottomBar;
import org.catrobat.catroid.ui.FormulaEditorClipboard;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.dialogs.FormulaEditorComputeDialog;
import org.catrobat.catroid.ui.dialogs.FormulaEditorIntroDialog;
import org.catrobat.catroid.ui.dialogs.regexassistant.RegularExpressionAssistantDialog;
import org.catrobat.catroid.ui.recyclerview.adapter.CategoryListRVAdapter;
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog;
import org.catrobat.catroid.ui.recyclerview.fragment.CategoryListFragment;
import org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment;
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment;
import org.catrobat.catroid.ui.runtimepermissions.BrickResourcesToRuntimePermissions;
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask;
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment;
import org.catrobat.catroid.ui.workspace.WorkspaceLayout;
import org.catrobat.catroid.userbrick.UserDefinedBrickInput;
import org.catrobat.catroid.utils.ProjectManagerExtensionsKt;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;
import org.catrobat.catroid.utils.SnackbarUtil;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.paintroid.colorpicker.ColorPickerDialog;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static org.catrobat.catroid.common.Constants.CODE_XML_FILE_NAME;
import static org.catrobat.catroid.common.Constants.UNDO_CODE_XML_FILE_NAME;
import static org.catrobat.catroid.ui.SpriteActivity.FRAGMENT_SCRIPTS;
import static org.catrobat.catroid.ui.SpriteActivityOnTabSelectedListenerKt.addTabLayout;
import static org.catrobat.catroid.ui.SpriteActivityOnTabSelectedListenerKt.removeTabLayout;
import static org.catrobat.catroid.utils.SnackbarUtil.wasHintAlreadyShown;

import org.catrobat.catroid.formulaeditor.InternFormulaKeyboardAdapter;
import org.catrobat.catroid.formulaeditor.InternToken;

import static androidx.fragment.app.DialogFragment.STYLE_NORMAL;

@LunoClass
public class FormulaEditorFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener,
		DataListFragment.FormulaEditorDataInterface {

	public static final String TAG = FormulaEditorFragment.class.getSimpleName();

	private static final int SET_FORMULA_ON_CREATE_VIEW = 0;
	private static final int SET_FORMULA_ON_SWITCH_EDIT_TEXT = 1;
	private static final int SET_FORMULA_ON_RETURN_FROM_VISUAL_PLACEMENT = 2;
	private static final int SET_FORMULA_ON_RETURN_FROM_COLOR_PICKER = 3;
	private static final int TIME_WINDOW = 2000;
	public static final int REQUEST_GPS = 1;
	public static final int REQUEST_PERMISSIONS_COMPUTE_DIALOG = 701;

	public static final String FORMULA_EDITOR_FRAGMENT_TAG = FormulaEditorFragment.class.getSimpleName();
	public static final String FORMULA_BRICK_BUNDLE_ARGUMENT = "formula_brick";
	public static final String FORMULA_FIELD_BUNDLE_ARGUMENT = "formula_field";
	public static final String DO_NOT_SHOW_WARNING = "DO_NOT_SHOW_WARNING";

	private FormulaEditorEditText formulaEditorEditText;
    private ViewGroup formulaEditorKeyboard;
	private LinearLayout formulaEditorBrick;

	private FormulaBrick formulaBrick;

	private static Brick.FormulaField currentFormulaField;
	private static Formula currentFormula;
	private Menu currentMenu;

	private long[] confirmSwitchEditTextTimeStamp = {0, 0};
	private int confirmSwitchEditTextCounter = 0;
	private boolean hasFormulaBeenChanged = false;

	private String actionBarTitleBuffer = "";

	private CategoryListRVAdapter.CategoryListItem chosenCategoryItem = null;
	private UserData<?> chosenUserDataItem = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		boolean isRestoringPreviouslyDestroyedActivity = savedInstanceState != null;
		if (isRestoringPreviouslyDestroyedActivity) {
			getFragmentManager().popBackStack(FORMULA_EDITOR_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			return;
		}

		formulaBrick = (FormulaBrick) getArguments().getSerializable(FORMULA_BRICK_BUNDLE_ARGUMENT);
		currentFormulaField = (Brick.FormulaField) getArguments().getSerializable(FORMULA_FIELD_BUNDLE_ARGUMENT);
		currentFormula = formulaBrick.getFormulaWithBrickField(currentFormulaField);
	}

	public void addCustomFunctionToActiveFormula(String customFunctionName) {
		if (formulaEditorEditText == null || formulaEditorEditText.getInternFormula() == null) {
			Log.w(TAG, "FormulaEditorEditText или его InternFormula не инициализированы в addCustomFunctionToActiveFormula");
			return;
		}


		InternFormulaKeyboardAdapter adapter = new InternFormulaKeyboardAdapter();
		List<InternToken> tokens = adapter.buildCustomJsFunction(customFunctionName);
		if (tokens != null) {




			addTokensToActiveFormula(tokens);
		} else {
			Log.e(TAG, "Не удалось создать токены для кастомной функции: " + customFunctionName);
		}
		updateButtonsOnKeyboardAndInvalidateOptionsMenu();
	}

    public void setShowCustomView(boolean showCustomView) {
        this.showCustomView = showCustomView;
    }

	private void addTokensToActiveFormula(List<InternToken> tokens) {
		if (formulaEditorEditText != null) {
			formulaEditorEditText.addTokensToActiveFormula(tokens);
		}
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            CharSequence title = actionBar.getTitle();
            actionBarTitleBuffer = title != null ? title.toString() : "";
            actionBar.setTitle(R.string.formula_editor_title);
        } else {
            actionBarTitleBuffer = "";
        }

        setHasOptionsMenu(true);
        SettingsFragment.setToChosenLanguage(getActivity());
    }

	private static void showFragment(Context context, FormulaBrick formulaBrick, Brick.FormulaField formulaField, boolean showCustomView) {
		AppCompatActivity activity = UiUtils.getActivityFromContextWrapper(context);
		if (activity == null) {
			return;
		}

        WorkspaceLayout workspaceLayout = activity.findViewById(R.id.workspace_layout);
        if (workspaceLayout != null && workspaceLayout.getVisibility() == View.VISIBLE) {
            workspaceLayout.openFormulaEditorWindow(formulaBrick, formulaField, showCustomView);
            return;
        }

		FormulaEditorFragment formulaEditorFragment = (FormulaEditorFragment) activity.getSupportFragmentManager()
				.findFragmentByTag(FORMULA_EDITOR_FRAGMENT_TAG);

		if (formulaEditorFragment == null) {
			formulaEditorFragment = new FormulaEditorFragment();
			formulaEditorFragment.showCustomView = showCustomView;
			Bundle bundle = new Bundle();
			bundle.putSerializable(FORMULA_BRICK_BUNDLE_ARGUMENT, formulaBrick);
			bundle.putSerializable(FORMULA_FIELD_BUNDLE_ARGUMENT, formulaField);
			formulaEditorFragment.setArguments(bundle);

			activity.getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, formulaEditorFragment, FORMULA_EDITOR_FRAGMENT_TAG)
					.addToBackStack(FORMULA_EDITOR_FRAGMENT_TAG)
					.commit();

			BottomBar.hideBottomBar(activity);
		} else {
			formulaEditorFragment.showCustomView = false;
			formulaEditorFragment.updateBrickView();
			formulaEditorFragment.setInputFormula(formulaField, SET_FORMULA_ON_SWITCH_EDIT_TEXT);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (SnackbarUtil.areHintsEnabled(this.getActivity())) {
			SnackbarUtil.dismissAllHints();
			if (!wasHintAlreadyShown(getActivity(), getActivity().getResources()
					.getResourceName(R.string.formula_editor_intro_title_formula_editor))) {
				new FormulaEditorIntroDialog(this, R.style.StageDialog).show();
				SnackbarUtil.setHintShown(getActivity(),
						getActivity().getResources().getResourceName(R.string.formula_editor_intro_title_formula_editor));
			}
		}
	}

	private boolean showCustomView = false;

	public static void showFragment(Context context, FormulaBrick formulaBrick, Brick.FormulaField formulaField) {
		showFragment(context, formulaBrick, formulaField, false);
	}

	public static void showCustomFragment(Context context, FormulaBrick formulaBrick, Brick.FormulaField formulaField) {
		showFragment(context, formulaBrick, formulaField, true);
	}

	public void updateBrickView() {
		formulaEditorBrick.removeAllViews();

		if (showCustomView) {
			formulaEditorEditText.setVisibility(View.GONE);
			formulaEditorKeyboard.setVisibility(View.GONE);
			formulaEditorBrick.addView(formulaBrick.getCustomView(getActivity()));
		} else {
			formulaEditorEditText.setVisibility(View.VISIBLE);
			formulaEditorKeyboard.setVisibility(View.VISIBLE);

			formulaBrick.invalidateCachedView();
			View brickView = formulaBrick.getView(getActivity());

			formulaBrick.setClickListeners();
			formulaBrick.disableSpinners();
			formulaBrick.highlightTextView(currentFormulaField);

			formulaEditorBrick.addView(brickView);
		}
	}

	public void updateFragmentAfterVisualPlacement() {
		updateBrickView();
		setInputFormula(currentFormulaField, SET_FORMULA_ON_RETURN_FROM_VISUAL_PLACEMENT);
	}

	public void updateFragmentAfterColorPicker() {
		updateBrickView();
		setInputFormula(currentFormulaField, SET_FORMULA_ON_RETURN_FROM_COLOR_PICKER);
	}

    private void onUserDismiss() {
        refreshFormulaPreviewString(currentFormula.getTrimmedFormulaString(getActivity()));
        formulaEditorEditText.endEdit();
        View workspaceLayout = getActivity().findViewById(R.id.workspace_layout);
        if (workspaceLayout != null && workspaceLayout.getVisibility() == View.VISIBLE) {
        } else {
            getFragmentManager().popBackStack();
        }
        if (getActivity() != null) {
            if (!(getActivity() instanceof org.catrobat.catroid.ui.dialogs.RuntimeConsoleActivity)) {
                BottomBar.showBottomBar(getActivity());
                BottomBar.showPlayButton(getActivity());
            }
        }
    }

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		setHasOptionsMenu(true);
        boolean isPcMode = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_pc_mode_enabled", false);

        int layoutId = isPcMode ? R.layout.fragment_formula_editor_pc : R.layout.fragment_formula_editor;
        View fragmentView = inflater.inflate(layoutId, container, false);

		fragmentView.setFocusableInTouchMode(true);
		fragmentView.requestFocus();

		formulaEditorBrick = fragmentView.findViewById(R.id.formula_editor_brick_space);
		formulaEditorEditText = fragmentView.findViewById(R.id.formula_editor_edit_field);
		formulaEditorKeyboard = fragmentView.findViewById(R.id.formula_editor_keyboardview);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            formulaEditorEditText.setShowSoftInputOnFocus(false);
        } else {
            formulaEditorEditText.setInputType(android.text.InputType.TYPE_NULL);
        }

		updateBrickView();

		fragmentView.getViewTreeObserver().addOnGlobalLayoutListener(this);
		setInputFormula(currentFormulaField, SET_FORMULA_ON_CREATE_VIEW);

		formulaEditorEditText.init(this);
		AppCompatActivity activity = (AppCompatActivity) getActivity();

		if (activity != null && activity.getSupportActionBar() != null) {
			activity.getSupportActionBar().setTitle(R.string.formula_editor_title);
		}

		return fragmentView;
	}

	@Override
	public void onStart() {
		formulaEditorKeyboard.setClickable(true);

		getView().requestFocus();
		View.OnTouchListener touchListener = new View.OnTouchListener() {
			private Handler handler;
			private Runnable deleteAction;
            private Handler clipboardHandler;
            private Runnable clipboardLongClickAction;
            private boolean isPasteLongClick = false;

			private boolean handleLongClick(final View view, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (handler == null) {
						return true;
					}
					handler.removeCallbacks(deleteAction);
					handler = null;
				}
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					deleteAction = new Runnable() {
						@Override
						public void run() {
							handler.postDelayed(this, 100);
							if (formulaEditorEditText.isThereSomethingToDelete()) {
								formulaEditorEditText.handleKeyEvent(view.getId(), "");
							}
						}
					};

					if (handler != null) {
						return true;
					}
					handler = new Handler();
					handler.postDelayed(deleteAction, 400);
				}
				return true;
			}

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    updateButtonsOnKeyboardAndInvalidateOptionsMenu();
                    view.setPressed(false);

                    if (view.getId() == R.id.formula_editor_keyboard_paste) {
                        if (clipboardHandler != null && clipboardLongClickAction != null) {
                            clipboardHandler.removeCallbacks(clipboardLongClickAction);
                        }
                        if (!isPasteLongClick) {
                            FormulaEditorClipboard.INSTANCE.paste(formulaEditorEditText);
                        }
                        isPasteLongClick = false;
                        return true;
                    }

                    handleLongClick(view, event);
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setPressed(true);
                    switch (view.getId()) {
						case R.id.formula_editor_keyboard_compute:
							showComputeDialog();
							return true;
						case R.id.formula_editor_keyboard_function:
							showCategoryListFragment(CategoryListFragment.FUNCTION_TAG,
									R.string.formula_editor_functions);
							return true;
						case R.id.formula_editor_keyboard_logic:
							showCategoryListFragment(CategoryListFragment.LOGIC_TAG,
									R.string.formula_editor_logic);
							return true;
						case R.id.formula_editor_keyboard_object:
							showCategoryListFragment(CategoryListFragment.OBJECT_TAG,
									R.string.formula_editor_choose_object_variable);
							return true;
						case R.id.formula_editor_keyboard_sensors:
							showCategoryListFragment(CategoryListFragment.SENSOR_TAG,
									R.string.formula_editor_device);
							return true;
						case R.id.formula_editor_keyboard_data:
							showDataFragment();
							return true;
						case R.id.formula_editor_keyboard_functional_button_toggle:
							toggleFunctionalButtons();
							return true;
						case R.id.formula_editor_keyboard_string:
							if (isSelectedTextFirstParamOfRegularExpression()) {
								showNewRegexAssistantDialog();
							} else {
								showNewStringDialog();
							}
							return true;
						case R.id.formula_editor_keyboard_delete:
							formulaEditorEditText.handleKeyEvent(view.getId(), "");
							return handleLongClick(view, event);
						case R.id.formula_editor_keyboard_color_picker:
							showColorPickerDialog(view);
							return true;
                        case R.id.formula_editor_keyboard_paste:
                            isPasteLongClick = false;
                            if (clipboardHandler == null) {
                                clipboardHandler = new Handler();
                            }
                            clipboardLongClickAction = new Runnable() {
                                @Override
                                public void run() {
                                    Context context = getContext();
                                    if (context != null && isAdded()) {
                                        isPasteLongClick = true;
                                        FormulaEditorClipboard.INSTANCE.showClipboardHistoryDialog(context, formulaEditorEditText);
                                    }
                                }
                            };
                            clipboardHandler.postDelayed(clipboardLongClickAction, 500);
                            return true;
						case R.id.formula_editor_keyboard_copy:
							FormulaEditorClipboard.INSTANCE.checkIfSelectedAndCopy(formulaEditorEditText);
							return true;
						default:
							formulaEditorEditText.handleKeyEvent(view.getId(), "");
							return true;
					}
				}
				return false;
			}
		};

        attachTouchListenerToButtons(formulaEditorKeyboard, touchListener);
        updateButtonsOnKeyboardAndInvalidateOptionsMenu();


        formulaEditorEditText.requestFocus();

        formulaEditorEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
                if (formulaEditorEditText.getInternFormula() == null) return false;


                if (event.isCtrlPressed() && event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    if (keyCode == android.view.KeyEvent.KEYCODE_C) {
                        org.catrobat.catroid.ui.FormulaEditorClipboard.INSTANCE.checkIfSelectedAndCopy(formulaEditorEditText);
                        return true;
                    } else if (keyCode == android.view.KeyEvent.KEYCODE_V) {
                        org.catrobat.catroid.ui.FormulaEditorClipboard.INSTANCE.paste(formulaEditorEditText);
                        return true;
                    } else if (keyCode == android.view.KeyEvent.KEYCODE_X) {
                        org.catrobat.catroid.ui.FormulaEditorClipboard.INSTANCE.checkIfSelectedAndCopy(formulaEditorEditText);
                        if (formulaEditorEditText.isThereSomethingToDelete()) {
                            formulaEditorEditText.handleKeyEvent(R.id.formula_editor_keyboard_delete, "");
                        }
                        return true;
                    } else if (keyCode == android.view.KeyEvent.KEYCODE_Z) {
                        formulaEditorEditText.undo();
                        updateButtonsOnKeyboardAndInvalidateOptionsMenu();
                        return true;
                    }
                }

                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT ||
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT ||
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP ||
                        keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {

                    if (event.getAction() == android.view.KeyEvent.ACTION_UP) {
                        formulaEditorEditText.syncCursorPosition();
                    }
                    return false;
                }


                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    int buttonId = -1;
                    char unicodeChar = (char) event.getUnicodeChar();


                    if (keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                        if (saveFormulaIfPossible()) {
                            exitFormulaEditorFragment();
                        }
                        return true;
                    }


                    if (keyCode == android.view.KeyEvent.KEYCODE_DEL) {
                        if (formulaEditorEditText.isThereSomethingToDelete()) {
                            formulaEditorEditText.handleKeyEvent(R.id.formula_editor_keyboard_delete, "");
                        }
                        return true;
                    }


                    if (unicodeChar >= '0' && unicodeChar <= '9') {
                        int[] numIds = {R.id.formula_editor_keyboard_0, R.id.formula_editor_keyboard_1,
                                R.id.formula_editor_keyboard_2, R.id.formula_editor_keyboard_3,
                                R.id.formula_editor_keyboard_4, R.id.formula_editor_keyboard_5,
                                R.id.formula_editor_keyboard_6, R.id.formula_editor_keyboard_7,
                                R.id.formula_editor_keyboard_8, R.id.formula_editor_keyboard_9};
                        buttonId = numIds[unicodeChar - '0'];
                    }

                    else if (unicodeChar == '+') buttonId = R.id.formula_editor_keyboard_plus;
                    else if (unicodeChar == '-') buttonId = R.id.formula_editor_keyboard_minus;
                    else if (unicodeChar == '*' || unicodeChar == 'x' || unicodeChar == 'X') buttonId = R.id.formula_editor_keyboard_mult;
                    else if (unicodeChar == '/') buttonId = R.id.formula_editor_keyboard_divide;
                    else if (unicodeChar == '(') buttonId = R.id.formula_editor_keyboard_bracket_open;
                    else if (unicodeChar == ')') buttonId = R.id.formula_editor_keyboard_bracket_close;
                    else if (unicodeChar == '.' || unicodeChar == ',') buttonId = R.id.formula_editor_keyboard_decimal_mark;


                    if (buttonId != -1) {
                        formulaEditorEditText.handleKeyEvent(buttonId, "");
                        updateButtonsOnKeyboardAndInvalidateOptionsMenu();
                        return true;
                    }


                    if (event.isPrintingKey() || keyCode == android.view.KeyEvent.KEYCODE_SPACE) {
                        return true;
                    }
                }
                return false;
            }
        });

		super.onStart();
	}

    private void attachTouchListenerToButtons(ViewGroup parent, View.OnTouchListener listener) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                attachTouchListenerToButtons((ViewGroup) child, listener);
            } else if (child.getId() != View.NO_ID && child.getVisibility() != View.GONE) {
                child.setOnTouchListener(listener);
            }
        }
    }

	private void showColorPicker(ShowFormulaEditorStrategy.Callback callback,
			FragmentManager fragmentManager) {
		int currentColor = callback.getValue();
		ColorPickerDialog dialog = ColorPickerDialog.Companion.newInstance(currentColor, true,
				true);
		Bitmap projectBitmap = ProjectManagerExtensionsKt
				.getProjectBitmap(ProjectManager.getInstance());
		dialog.setBitmap(projectBitmap);
		dialog.addOnColorPickedListener(callback::setValue);
		dialog.setStyle(STYLE_NORMAL, R.style.AlertDialogWithTitle);
		dialog.show(fragmentManager, null);
	}

    private void showColorPickerDialog(View view) {
        AppCompatActivity activity = UiUtils.getActivityFromView(view);
        if (activity == null) {
            return;
        }
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) {
            return;
        }
        showColorPicker(new ShowFormulaEditorStrategy.Callback() {
            @Override
            public void showFormulaEditor(View view) {
            }

            @Override
            public void setValue(int value) {
                if ((value & 0xFF000000) == 0xFF000000) {
                    addString(String.format("#%06X", (0xFFFFFF & value)));
                } else {
                    addString(String.format("#%08X", value));
                }
            }

            @Override
            public int getValue() {
                String currentValue = getSelectedFormulaText();
                if (currentValue != null) {
                    currentValue = currentValue.trim().replace("'", "").replace("\"", "");
                    if (currentValue.matches("^#[0-9A-Fa-f]{6}$")) {
                        return Color.parseColor(currentValue);
                    } else if (currentValue.matches("^#[0-9A-Fa-f]{8}$")) {
                        try {
                            return Color.parseColor(currentValue);
                        } catch (IllegalArgumentException e) {
                            try {
                                String hex = currentValue.substring(1);
                                String argb = "#" + hex.substring(6, 8) + hex.substring(0, 6);
                                return Color.parseColor(argb);
                            } catch (Exception ex) {
                                return 0;
                            }
                        }
                    }
                }
                return 0;
            }
        }, fragmentManager);
    }

    public void toggleFunctionalButtons() {
        View row1 = getActivity().findViewById(R.id.tableRow11);
        View row2 = getActivity().findViewById(R.id.tableRow12);
        ImageButton toggleButton = getActivity().findViewById(R.id.formula_editor_keyboard_functional_button_toggle);

        if (row1 == null || row2 == null || toggleButton == null) {
            return;
        }

        boolean isVisible = row1.getVisibility() == View.VISIBLE;
        row1.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        row2.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        toggleButton.setImageDrawable(ContextCompat.getDrawable(getContext(), isVisible ? R.drawable.ic_keyboard_toggle_caret_up : R.drawable.ic_keyboard_toggle_caret_down));
        toggleFormulaEditorSpace(isVisible);
    }

    private void showBeautifulFilePickerDialog() {
        Context context = getContext();
        if (context == null) return;

        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        if (currentProject == null) return;

        File filesDir = currentProject.getFilesDir();
        if (filesDir == null || !filesDir.exists()) {
            ToastUtil.showError(context, "Files directory not found");
            return;
        }

        File[] allFiles = filesDir.listFiles(new java.io.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile();
            }
        });

        if (allFiles == null || allFiles.length == 0) {
            ToastUtil.showInfoLong(context, getString(R.string.no_files_found));
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_project_file_picker, null);
        EditText searchInput = dialogView.findViewById(R.id.search_file_input);
        ListView listView = dialogView.findViewById(R.id.project_files_list);

        List<File> fileList = new ArrayList<>();
        for (File f : allFiles) {
            fileList.add(f);
        }

        java.util.Collections.sort(fileList, new java.util.Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        FileAdapter adapter = new FileAdapter(context, fileList);
        listView.setAdapter(adapter);

        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.dialog_project_files_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = adapter.getItem(position);
            addString(selectedFile.getName());
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private class FileAdapter extends android.widget.ArrayAdapter<File> {
        private final List<File> originalList;
        private final List<File> filteredList;

        public FileAdapter(Context context, List<File> files) {
            super(context, 0, files);
            this.originalList = new ArrayList<>(files);
            this.filteredList = new ArrayList<>(files);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public File getItem(int position) {
            return filteredList.get(position);
        }

        public void filter(String text) {
            filteredList.clear();
            if (text.isEmpty()) {
                filteredList.addAll(originalList);
            } else {
                String query = text.toLowerCase();
                for (File file : originalList) {
                    if (file.getName().toLowerCase().contains(query)) {
                        filteredList.add(file);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_project_file_picker, parent, false);
            }

            ImageView iconView = convertView.findViewById(R.id.file_icon);
            TextView nameView = convertView.findViewById(R.id.file_name);
            TextView infoView = convertView.findViewById(R.id.file_info);

            File file = getItem(position);
            nameView.setText(file.getName());

            long sizeInBytes = file.length();
            String sizeStr;
            if (sizeInBytes < 1024) {
                sizeStr = sizeInBytes + " B";
            } else if (sizeInBytes < 1024 * 1024) {
                sizeStr = String.format("%.1f KB", sizeInBytes / 1024.0);
            } else {
                sizeStr = String.format("%.1f MB", sizeInBytes / (1024.0 * 1024.0));
            }

            String ext = getFileExtension(file.getName()).toLowerCase();
            int iconResId = R.drawable.file_present_24px;
            String typeStr = "File";

            if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("webp") || ext.equals("gif")) {
                iconResId = R.drawable.ic_draw_image;
                typeStr = "Image";
            } else if (ext.equals("mp3") || ext.equals("wav") || ext.equals("ogg") || ext.equals("m4a")) {
                iconResId = R.drawable.ic_music_library;
                typeStr = "Audio";
            } else if (ext.equals("xml") || ext.equals("json") || ext.equals("txt") || ext.equals("py") || ext.equals("lua") || ext.equals("js") || ext.equals("java") || ext.equals("kt") || ext.equals("md")) {
                iconResId = R.drawable.code_24px;
                typeStr = "Text";
            } else if(ext.equals("rscene") || ext.equals("glb") || ext.equals("obj")) {
                iconResId =  R.drawable.deployed_code_24px;
                typeStr = "Model";
            }

            iconView.setImageResource(iconResId);
            iconView.setColorFilter(ContextCompat.getColor(getContext(), R.color.accent));

            infoView.setText(typeStr + " • " + sizeStr);

            return convertView;
        }

        private String getFileExtension(String fileName) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot == -1) {
                return "";
            }
            return fileName.substring(lastDot + 1);
        }
    }

	private void toggleFormulaEditorSpace(boolean isVisible) {
		View keyboard = getActivity().findViewById(R.id.formula_editor_keyboardview);
		View brickAndFormula = getActivity().findViewById(R.id.formula_editor_brick_and_formula);

		LinearLayout.LayoutParams keyboardLayoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT,
				1
		);

		LinearLayout.LayoutParams formulaLayoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT,
				1
		);

		if (isVisible) {
			View row1 = getActivity().findViewById(R.id.tableRow11);
			View row2 = getActivity().findViewById(R.id.tableRow12);
			int rowsHeight = row1.getHeight() + row2.getHeight();
			keyboardLayoutParams.topMargin = rowsHeight;
			formulaLayoutParams.bottomMargin = -rowsHeight;
		} else {
			keyboardLayoutParams.topMargin = 0;
			formulaLayoutParams.bottomMargin = 0;
		}
		brickAndFormula.setLayoutParams(formulaLayoutParams);
		keyboard.setLayoutParams(keyboardLayoutParams);
	}

	@VisibleForTesting
	public FormulaEditorEditText getFormulaEditorEditText() {
		return formulaEditorEditText;
	}

    @Override
    public void onStop() {
        super.onStop();
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) {
            return;
        }
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(actionBarTitleBuffer);
        }
    }

	private boolean isSelectedTextFirstParamOfRegularExpression() {
		return getFormulaEditorEditText().isSelectedTokenFirstParamOfRegularExpression();
	}

	private void showNewRegexAssistantDialog() {
		String selectedFormulaText = getSelectedFormulaText();

		TextInputDialog.Builder builder = new TextInputDialog.Builder(getContext());

		builder.setHint(getString(R.string.string_label))
				.setText(selectedFormulaText)
				.setPositiveButton(getString(R.string.ok), (TextInputDialog.OnClickListener) (dialog, textInput) -> addString(textInput));

		int titleId = R.string.formula_editor_dialog_change_regular_expression;

		builder.setNeutralButton(R.string.assistant,
				(DialogInterface.OnClickListener) (dialog, textInput) -> openAssistantDialog());

		builder.setTitle(titleId)
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void openAssistantDialog() {
		new RegularExpressionAssistantDialog(getContext(), getFragmentManager()).createAssistant();
	}

	private void showNewStringDialog() {
		String selectedFormulaText = getSelectedFormulaText();

		TextInputDialog.Builder builder = new TextInputDialog.Builder(getContext());

		builder.setHint(getString(R.string.string_label))
				.setText(selectedFormulaText)
				.setPositiveButton(getString(R.string.ok), (TextInputDialog.OnClickListener) (dialog, textInput) -> addString(textInput));

		int titleId = selectedFormulaText == null
				? R.string.formula_editor_new_string_name : R.string.formula_editor_dialog_change_text;

		builder.setTitle(titleId)
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	public void addString(String string) {
		String previousString = getSelectedFormulaText();
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		Sprite currentSprite = ProjectManager.getInstance().getCurrentSprite();
		Context context = getContext();
		if (context != null) {
			boolean doNotShowWarning = false;
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			if (preferences.contains(DO_NOT_SHOW_WARNING)) {
				doNotShowWarning = preferences.getBoolean(DO_NOT_SHOW_WARNING, false);
			}
			if (!doNotShowWarning && recognizedFormulaInText(string, context, currentProject, currentSprite)) {
				showFormulaInTextWarning();
			}
		}
		if (previousString != null && !previousString.matches("\\s*")) {
			overrideSelectedText(string);
		} else {
			addStringToActiveFormula(string);
		}
		updateButtonsOnKeyboardAndInvalidateOptionsMenu();
	}

	@VisibleForTesting
	public boolean recognizedFormulaInText(String string, Context context, Project project, Sprite sprite) {
		boolean recognizedFormula = false;
		String[] formulasWithParams = context.getResources().getStringArray(R.array.formulas_with_params);
		String[] formulasWithoutParams = context.getResources().getStringArray(R.array.formulas_without_params);

		for (String formulaWithParams : formulasWithParams) {
			if (string.matches(".*" + formulaWithParams + "\\(.+\\)" + ".*")) {
				recognizedFormula = true;
				break;
			}
		}

		for (String formulaWithoutParams : formulasWithoutParams) {
			if (string.contains(formulaWithoutParams)) {
				recognizedFormula = true;
				break;
			}
		}

		recognizedFormula |= stringContainsUserVariable(string, project.getMultiplayerVariables())
				|| stringContainsUserVariable(string, project.getUserVariables())
				|| stringContainsUserVariable(string, sprite.getUserVariables())
				|| stringContainsUserList(string, project.getUserLists())
				|| stringContainsUserList(string, sprite.getUserLists())
				|| stringContainsUserDefinedBrickInput(string);

		return recognizedFormula;
	}

	private boolean stringContainsUserVariable(String string, List<UserVariable> variableList) {
		for (UserVariable variable : variableList) {
			if (string.contains(variable.getName())) {
				return true;
			}
		}
		return false;
	}

	private boolean stringContainsUserList(String string, List<UserList> userList) {
		for (UserList list : userList) {
			if (string.contains(list.getName())) {
				return true;
			}
		}
		return false;
	}

	private boolean stringContainsUserDefinedBrickInput(String string) {
		if (getFormulaBrick().getScript().getScriptBrick() instanceof UserDefinedReceiverBrick) {
			List<UserDefinedBrickInput> userDefinedBrickInputs =
					((UserDefinedReceiverBrick) getFormulaBrick().getScript().getScriptBrick()).getUserDefinedBrick().getUserDefinedBrickInputs();
			for (UserDefinedBrickInput variable : userDefinedBrickInputs) {
				if (string.contains(variable.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private void showFormulaInTextWarning() {
		new AlertDialog.Builder(getActivity())
				.setTitle(R.string.warning)
				.setMessage(R.string.warning_formula_recognized)
				.setPositiveButton(R.string.ok, null)
				.setNegativeButton(R.string.do_not_show_again, (dialogInterface, i) -> {
					PreferenceManager.getDefaultSharedPreferences(getContext())
							.edit()
							.putBoolean(DO_NOT_SHOW_WARNING, true)
							.apply();
				})
				.create()
				.show();
	}

	private void showComputeDialog() {
		InternFormulaParser internFormulaParser = formulaEditorEditText.getFormulaParser();
		final FormulaElement formulaElement = internFormulaParser.parseFormula(generateScope());
		if (formulaElement == null) {
			if (internFormulaParser.getErrorTokenIndex() >= 0) {
				formulaEditorEditText.setParseErrorCursorAndSelection();
			}
			return;
		}
		final Brick.ResourcesSet resourcesSet = new Brick.ResourcesSet();
		formulaElement.addRequiredResources(resourcesSet);
		List<String> requiredRuntimePermissions = BrickResourcesToRuntimePermissions.translate(resourcesSet);

		new RequiresPermissionTask(REQUEST_PERMISSIONS_COMPUTE_DIALOG, requiredRuntimePermissions, R.string.runtime_permission_general) {
			public void task() {
				if (resourcesSet.contains(Brick.SENSOR_GPS)) {
					SensorHandler sensorHandler = SensorHandler.getInstance(getActivity());
					sensorHandler.setLocationManager((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE));
					if (!SensorHandler.gpsAvailable()) {
						Intent checkIntent = new Intent();
						checkIntent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivityForResult(checkIntent, REQUEST_GPS);
						return;
					}
				}
				Formula formulaToCompute = new Formula(formulaElement);
				FormulaEditorComputeDialog computeDialog =
						new FormulaEditorComputeDialog(getActivity(), generateScope());
				computeDialog.setFormula(formulaToCompute);
				computeDialog.show();
			}
		}.execute(getActivity());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_GPS && resultCode == AppCompatActivity.RESULT_CANCELED && SensorHandler.gpsAvailable()) {
			showComputeDialog();
		} else {
			ToastUtil.showError(getActivity(), R.string.error_gps_not_available);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		currentMenu = menu;

		for (int index = 0; index < menu.size(); index++) {
			menu.getItem(index).setVisible(false);
		}

		MenuItem undo = menu.findItem(R.id.menu_undo);
		if (formulaEditorEditText == null || !formulaEditorEditText.getHistory().undoIsPossible()) {
			undo.setIcon(R.drawable.icon_undo_disabled);
			undo.setEnabled(false);
		} else {
			undo.setIcon(R.drawable.icon_undo);
			undo.setEnabled(true);
		}

		MenuItem redo = menu.findItem(R.id.menu_redo);
		if (formulaEditorEditText == null || !formulaEditorEditText.getHistory().redoIsPossible()) {
			redo.setIcon(R.drawable.icon_redo_disabled);
			redo.setEnabled(false);
		} else {
			redo.setIcon(R.drawable.icon_redo);
			redo.setEnabled(true);
		}

		menu.findItem(R.id.menu_undo).setVisible(true);
		menu.findItem(R.id.menu_redo).setVisible(true);
        menu.findItem(R.id.menu_files).setVisible(true);

		super.onPrepareOptionsMenu(menu);
		updateButtonsOnKeyboard();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_formulaeditor, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_undo:
				formulaEditorEditText.undo();
				break;
			case R.id.menu_redo:
				formulaEditorEditText.redo();
				break;
            case R.id.menu_files:
                showBeautifulFilePickerDialog();
                break;
		}
		updateButtonsOnKeyboardAndInvalidateOptionsMenu();
		return super.onOptionsItemSelected(item);
	}

	public void setInputFormula(Brick.FormulaField formulaField, int mode) {

		switch (mode) {
			case SET_FORMULA_ON_CREATE_VIEW:
				formulaEditorEditText.enterNewFormula(new UndoState(currentFormula.getInternFormulaState(),
						formulaField));
				refreshFormulaPreviewString(formulaEditorEditText.getStringFromInternFormula());
                //formulaEditorEditText.getInternFormula().setCursorAndSelection(0, false);
				break;
			case SET_FORMULA_ON_RETURN_FROM_VISUAL_PLACEMENT:
			case SET_FORMULA_ON_RETURN_FROM_COLOR_PICKER:
			case SET_FORMULA_ON_SWITCH_EDIT_TEXT:
                try {
                    Formula newFormula = formulaBrick.getFormulaWithBrickField(formulaField);
                    if (currentFormula == newFormula && formulaEditorEditText.hasChanges()) {
                        formulaEditorEditText.quickSelect();
                        break;
                    }
                    if (formulaEditorEditText.hasChanges()) {
                        confirmSwitchEditTextTimeStamp[0] = confirmSwitchEditTextTimeStamp[1];
                        confirmSwitchEditTextTimeStamp[1] = System.currentTimeMillis();
                        confirmSwitchEditTextCounter++;
                        if (!saveFormulaIfPossible()) {
                            return;
                        }
                    }
                    if (currentMenu != null) {
                        MenuItem undo = currentMenu.findItem(R.id.menu_undo);
                        if (undo != null) {
                            undo.setIcon(R.drawable.icon_undo_disabled);
                            undo.setEnabled(false);
                        }

                        MenuItem redo = currentMenu.findItem(R.id.menu_redo);
                        if (redo != null) {
                            redo.setIcon(R.drawable.icon_redo_disabled);
                            redo.setEnabled(false);
                        }
                    }

                    formulaEditorEditText.endEdit();
                    currentFormulaField = formulaField;
                    currentFormula = newFormula;
                    formulaEditorEditText.enterNewFormula(new UndoState(currentFormula.getInternFormulaState(),
                            currentFormulaField));
                    refreshFormulaPreviewString(formulaEditorEditText.getStringFromInternFormula());
                    //formulaEditorEditText.getInternFormula().setCursorAndSelection(0, false);
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
			default:
				break;
		}
	}

	private Scope generateScope() {
		Project project = ProjectManager.getInstance().getCurrentProject();
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		ScriptSequenceAction sequence = null;
		Script script = formulaBrick.getScript();
		if (script instanceof UserDefinedScript) {
			UserDefinedReceiverBrick brick = (UserDefinedReceiverBrick) script.getScriptBrick();
			List<UserDefinedBrickInput> inputs =
					brick.getUserDefinedBrick().getUserDefinedBrickInputs();
			List<Object> inputNames = new ArrayList<>();
			for (UserDefinedBrickInput input : inputs) {
				inputNames.add(convertUserDefinedBrickInputToUserVariable(input));
			}

			sequence = new ScriptSequenceAction(script);
			((UserDefinedScript) (sequence.getScript())).setUserDefinedBrickInputs(inputNames);
		}
		return new Scope(project, sprite, sequence);
	}

	private UserVariable convertUserDefinedBrickInputToUserVariable(UserDefinedBrickInput input) {
		return new UserVariable(
				input.getName(),
				input.getValue().getUserFriendlyString(
						new AndroidStringProvider(getContext()),
						null
				)
		);
	}

	public boolean saveFormulaIfPossible() {
		InternFormulaParser formulaToParse = formulaEditorEditText.getFormulaParser();
		FormulaElement formulaParseTree = formulaToParse.parseFormula(generateScope());

		switch (formulaToParse.getErrorTokenIndex()) {
			case InternFormulaParser.PARSER_OK:
				return saveValidFormula(formulaParseTree);
			case InternFormulaParser.PARSER_STACK_OVERFLOW:
				return checkReturnWithoutSaving(InternFormulaParser.PARSER_STACK_OVERFLOW);
			case InternFormulaParser.PARSER_NO_INPUT:
				if (currentFormulaField instanceof Brick.BrickField && Brick.BrickField.isExpectingStringValue((Brick.BrickField) currentFormulaField)) {
					return saveValidFormula(new FormulaElement(FormulaElement.ElementType.STRING, "", null));
				}

			default:
				formulaEditorEditText.setParseErrorCursorAndSelection();
				return checkReturnWithoutSaving(InternFormulaParser.PARSER_INPUT_SYNTAX_ERROR);
		}
	}

	private boolean saveValidFormula(FormulaElement formulaElement) {
		currentFormula.setRoot(formulaElement);
		formulaEditorEditText.formulaSaved();
		hasFormulaBeenChanged = true;
		return true;
	}

	private boolean checkReturnWithoutSaving(int errorType) {
		if ((System.currentTimeMillis() <= confirmSwitchEditTextTimeStamp[0] + TIME_WINDOW)
				&& (confirmSwitchEditTextCounter > 1)) {
			confirmSwitchEditTextTimeStamp[0] = 0;
			confirmSwitchEditTextTimeStamp[1] = 0;
			confirmSwitchEditTextCounter = 0;
			ToastUtil.showSuccess(getActivity(), R.string.formula_editor_changes_discarded);
			return true;
		} else {
			switch (errorType) {
				case InternFormulaParser.PARSER_INPUT_SYNTAX_ERROR:
					ToastUtil.showError(getActivity(), R.string.formula_editor_parse_fail);
					break;
				case InternFormulaParser.PARSER_STACK_OVERFLOW:
					ToastUtil.showError(getActivity(), R.string.formula_editor_parse_fail_formula_too_long);
					break;
			}
			return false;
		}
	}

	private boolean hasFileChanged() {
		File currentCodeFile = new File(ProjectManager.getInstance().getCurrentProject().getDirectory(), CODE_XML_FILE_NAME);
		File undoCodeFile = new File(ProjectManager.getInstance().getCurrentProject().getDirectory(), UNDO_CODE_XML_FILE_NAME);

		if (currentCodeFile.exists() && undoCodeFile.exists()) {
			try {
				List<String> currentFile = Files.readLines(currentCodeFile, StandardCharsets.UTF_8);
				List<String> undoFile = Files.readLines(undoCodeFile, StandardCharsets.UTF_8);

				return !currentFile.equals(undoFile);
			} catch (IOException exception) {
				Log.e(TAG, "Comparing project files failed.", exception);
			}
		}

		return false;
	}

    public void exitFormulaEditorFragment() {
        if (getActivity() instanceof SpriteActivity) {
            ((SpriteActivity) getActivity()).setUndoMenuItemVisibility(false);
        }

        if (hasFormulaBeenChanged || formulaEditorEditText.hasChanges()) {
            if (saveFormulaIfPossible()) {
                hasFormulaBeenChanged = false;
            } else {
                return;
            }
        }

        onUserDismiss();

        clearFormulaFieldHighlight();

        ScriptFragment scriptFragment = (ScriptFragment) getActivity().getSupportFragmentManager().findFragmentByTag(ScriptFragment.TAG);
        if (scriptFragment != null) {
            scriptFragment.notifyDataSetChanged();
        }

        if (!(getActivity() instanceof org.catrobat.catroid.ui.dialogs.RuntimeConsoleActivity)) {
            XstreamSerializer.getInstance().saveProject(ProjectManager.getInstance().getCurrentProject());
        }

        if (getActivity() != null) {
            ScriptFragment fragment = (ScriptFragment) getActivity().getSupportFragmentManager().findFragmentByTag(ScriptFragment.TAG);

            if (fragment != null && (hasFileChanged() || fragment.checkVariables())) {
                if (getActivity() instanceof SpriteActivity) {
                    ((SpriteActivity) getActivity()).setUndoMenuItemVisibility(true);
                }
            }
        }
    }

	private void clearFormulaFieldHighlight() {
		if (formulaBrick != null) {
			formulaBrick.invalidateCachedView();
		}
	}

	@VisibleForTesting
	public void endFormulaEditor() {
		if (formulaEditorEditText.hasChanges()) {
			if (saveFormulaIfPossible()) {
				hasFormulaBeenChanged = false;
				onUserDismiss();
			}
		} else {
			onUserDismiss();
		}
	}

	public void refreshFormulaPreviewString(String newString) {
		updateBrickView();
		TextView formulaFieldView = formulaBrick.getTextView(currentFormulaField);
		formulaFieldView.setText(
				FormulaSpannableStringBuilder.buildSpannableFormulaString(this.getContext(),
						newString,
						formulaFieldView.getTextSize()));
	}

	private void showCategoryListFragment(String tag, int actionbarResId) {
        View workspaceLayoutView = getActivity().findViewById(R.id.workspace_layout);
        if (workspaceLayoutView != null && workspaceLayoutView.getVisibility() == View.VISIBLE) {
            org.catrobat.catroid.ui.workspace.WorkspaceLayout workspaceLayout =
                    (org.catrobat.catroid.ui.workspace.WorkspaceLayout) workspaceLayoutView;
            workspaceLayout.openCategoryListWindow(tag, getActivity().getString(actionbarResId));
            return;
        }

		CategoryListFragment fragment = new CategoryListFragment();
		Bundle bundle = new Bundle();
		bundle.putString(CategoryListFragment.ACTION_BAR_TITLE_BUNDLE_ARGUMENT,
				getActivity().getString(actionbarResId));
		bundle.putString(CategoryListFragment.FRAGMENT_TAG_BUNDLE_ARGUMENT, tag);
		fragment.setArguments(bundle);
		fragment.onPrepareOptionsMenu(currentMenu);

		getFragmentManager().beginTransaction()
				.hide(getFragmentManager().findFragmentByTag(FORMULA_EDITOR_FRAGMENT_TAG))
				.add(R.id.fragment_container, fragment, tag)
				.addToBackStack(tag)
				.commit();
	}

	private void showDataFragment() {
        View workspaceLayoutView = getActivity().findViewById(R.id.workspace_layout);
        if (workspaceLayoutView != null && workspaceLayoutView.getVisibility() == View.VISIBLE) {
            org.catrobat.catroid.ui.workspace.WorkspaceLayout workspaceLayout =
                    (org.catrobat.catroid.ui.workspace.WorkspaceLayout) workspaceLayoutView;
            workspaceLayout.openDataListWindow(formulaBrick.getScript().getScriptBrick());
            return;
        }

		DataListFragment fragment = new DataListFragment();
		fragment.setFormulaEditorDataInterface(this);

		Bundle bundle = new Bundle();
		bundle.putSerializable(DataListFragment.PARENT_SCRIPT_BRICK_BUNDLE_ARGUMENT,
				formulaBrick.getScript().getScriptBrick());
		fragment.setArguments(bundle);

		getFragmentManager().beginTransaction()
				.hide(getFragmentManager().findFragmentByTag(FORMULA_EDITOR_FRAGMENT_TAG))
				.add(R.id.fragment_container, fragment, DataListFragment.TAG)
				.addToBackStack(DataListFragment.TAG)
				.commit();
	}

	@Override
	public void onVariableRenamed(String previousName, String newName) {
		formulaEditorEditText.updateVariableReferences(previousName, newName);
	}

	@Override
	public void onListRenamed(String previousName, String newName) {
		formulaEditorEditText.updateListReferences(previousName, newName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onGlobalLayout() {
		getView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
		Rect brickRect = new Rect();
		Rect keyboardRec = new Rect();
		formulaEditorBrick.getGlobalVisibleRect(brickRect);
		formulaEditorKeyboard.getGlobalVisibleRect(keyboardRec);
	}

	public void addResourceToActiveFormula(int resource) {
		formulaEditorEditText.handleKeyEvent(resource, "");
		boolean requiresCollisionPolygons = resource == R.string.formula_editor_function_collides_with_edge
				|| resource == R.string.formula_editor_function_touched;
		if (requiresCollisionPolygons) {
			ProjectManager.getInstance().getCurrentSprite().createCollisionPolygons();
		}
	}

	public void addUserListToActiveFormula(String userListName) {
		formulaEditorEditText.handleKeyEvent(InternFormulaKeyboardAdapter.FORMULA_EDITOR_USER_LIST_RESOURCE_ID,
				userListName);
	}

	public void addUserVariableToActiveFormula(String userVariableName) {
		formulaEditorEditText.handleKeyEvent(InternFormulaKeyboardAdapter.FORMULA_EDITOR_USER_VARIABLE_RESOURCE_ID,
				userVariableName);
	}

	public void addUserDefinedBrickInputToActiveFormula(String userDefinedBrickInput) {
		formulaEditorEditText.handleKeyEvent(InternFormulaKeyboardAdapter.FORMULA_EDITOR_USER_DEFINED_BRICK_INPUT_RESOURCE_ID,
				userDefinedBrickInput);
	}

	public void addCollideFormulaToActiveFormula(String spriteName) {
		formulaEditorEditText.handleKeyEvent(InternFormulaKeyboardAdapter.FORMULA_EDITOR_COLLIDE_RESOURCE_ID,
				spriteName);
	}

	public void addStringToActiveFormula(String string) {
		formulaEditorEditText.handleKeyEvent(R.id.formula_editor_keyboard_string, string);
	}

	public String getSelectedFormulaText() {
		return formulaEditorEditText.getSelectedTextFromInternFormula();
	}

	public FormulaBrick getFormulaBrick() {
		return formulaBrick;
	}

	public Brick.FormulaField getCurrentBrickField() {
		return currentFormulaField;
	}

	public void overrideSelectedText(String string) {
		formulaEditorEditText.overrideSelectedText(string);
	}

	public void setChosenCategoryItem(CategoryListRVAdapter.CategoryListItem chosenCategoryItem) {
		this.chosenCategoryItem = chosenCategoryItem;

        View workspaceView = getActivity() != null ? getActivity().findViewById(R.id.workspace_layout) : null;
        if (workspaceView != null && workspaceView.getVisibility() == View.VISIBLE) {
            WorkspaceLayout workspace = (WorkspaceLayout) workspaceView;
            workspace.setProgrammaticBackPress(true);
            onHiddenChanged(false);
        }
	}

    public void setChosenUserDataItem(UserData<?> chosenUserDataItem) {
        this.chosenUserDataItem = chosenUserDataItem;

        View workspaceView = getActivity() != null ? getActivity().findViewById(R.id.workspace_layout) : null;
        if (workspaceView != null && workspaceView.getVisibility() == View.VISIBLE) {
            WorkspaceLayout workspace = (WorkspaceLayout) workspaceView;
            workspace.setProgrammaticBackPress(true);
            onHiddenChanged(false);
            if (getActivity() instanceof SpriteActivity) {
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (getActivity() == null) {
            return;
        }

        if (!hidden) {
            ActionBar actionBar = null;
            if (getActivity() instanceof AppCompatActivity) {
                actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            }

            if (actionBar != null) {
                actionBar.setTitle(R.string.formula_editor_title);
            }

            BottomBar.hideBottomBar(getActivity());
            updateButtonsOnKeyboardAndInvalidateOptionsMenu();
            updateBrickView();

            if (chosenCategoryItem != null) {
                if (chosenCategoryItem.isCustomFunction && chosenCategoryItem.customFunctionName != null) {
                    addCustomFunctionToActiveFormula(chosenCategoryItem.customFunctionName);
                } else {
                    addResourceToActiveFormula(chosenCategoryItem.nameResId);
                }
                chosenCategoryItem = null;
            }
            if (chosenUserDataItem != null) {
                if (chosenUserDataItem instanceof UserVariable) {
                    addUserVariableToActiveFormula(chosenUserDataItem.getName());
                } else if (chosenUserDataItem instanceof UserList) {
                    addUserListToActiveFormula(chosenUserDataItem.getName());
                } else if (chosenUserDataItem instanceof UserDefinedBrickInput) {
                    addUserDefinedBrickInputToActiveFormula(chosenUserDataItem.getName());
                }
                chosenUserDataItem = null;
            }

            if (formulaEditorEditText != null) {
                formulaEditorEditText.requestFocus();
            }
        }
    }

    public void updateButtonsOnKeyboardAndInvalidateOptionsMenu() {
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
        updateButtonsOnKeyboard();
    }

    public void updateButtonsOnKeyboard() {
        androidx.fragment.app.FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        ImageButton backspaceOnKeyboard = activity.findViewById(R.id.formula_editor_keyboard_delete);
        if (backspaceOnKeyboard == null) {
            return;
        }
        if (!formulaEditorEditText.isThereSomethingToDelete()) {
            backspaceOnKeyboard.setAlpha(255 / 3);
            backspaceOnKeyboard.setEnabled(false);
        } else {
            backspaceOnKeyboard.setAlpha(255);
            backspaceOnKeyboard.setEnabled(true);
        }
    }

	public int getIndexOfCorrespondingRegularExpression() {
		return formulaEditorEditText.getIndexOfCorrespondingRegularExpression();
	}

	public void setSelectionToFirstParamOfRegularExpressionAtInternalIndex(int indexOfRegularExpression) {
		formulaEditorEditText.setSelectionToFirstParamOfRegularExpressionAtInternalIndex(indexOfRegularExpression);
	}

	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		removeTabLayout(getActivity());
	}

	@Override
	public void onDetach() {
		clearFormulaFieldHighlight();
		addTabLayout(getActivity(), FRAGMENT_SCRIPTS);
		super.onDetach();
	}
}
