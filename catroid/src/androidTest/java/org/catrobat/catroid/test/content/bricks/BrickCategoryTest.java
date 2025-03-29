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

package org.catrobat.catroid.test.content.bricks;

import android.content.Context;
import android.preference.PreferenceManager;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.*;
import org.catrobat.catroid.ui.fragment.CategoryBricksFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static android.content.SharedPreferences.Editor;

import static junit.framework.Assert.assertEquals;

import static org.catrobat.catroid.ui.settingsfragments.SettingsFragment.SETTINGS_SHOW_AI_SPEECH_RECOGNITION_SENSORS;
import static org.catrobat.catroid.ui.settingsfragments.SettingsFragment.SETTINGS_SHOW_AI_SPEECH_SYNTHETIZATION_SENSORS;

@RunWith(Parameterized.class)
public class BrickCategoryTest {

	private final List<String> speechAISettings = new ArrayList<>(Arrays.asList(
			SETTINGS_SHOW_AI_SPEECH_RECOGNITION_SENSORS,
			SETTINGS_SHOW_AI_SPEECH_SYNTHETIZATION_SENSORS));

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"Motion", Arrays.asList(PlaceAtBrick.class,
						SetXBrick.class,
						SetYBrick.class,
						ChangeXByNBrick.class,
						ChangeYByNBrick.class,
						GoToBrick.class,
						IfOnEdgeBounceBrick.class,
						MoveNStepsBrick.class,
						TurnLeftBrick.class,
						TurnRightBrick.class,
						PointInDirectionBrick.class,
						PointToBrick.class,
						SetRotationStyleBrick.class,
						GlideToBrick.class,
						GoNStepsBackBrick.class,
						ComeToFrontBrick.class,
						SetCameraFocusPointBrick.class,
						VibrationBrick.class,
						SetPhysicsObjectTypeBrick.class,
						WhenBounceOffBrick.class,
						SetVelocityBrick.class,
						TurnLeftSpeedBrick.class,
						TurnRightSpeedBrick.class,
						SetGravityBrick.class,
						SetMassBrick.class,
						SetBounceBrick.class,
						SetFrictionBrick.class,
						FadeParticleEffectBrick.class)},
				{"Embroidery", Arrays.asList(StitchBrick.class,
						SetThreadColorBrick.class,
						RunningStitchBrick.class,
						ZigZagStitchBrick.class,
						TripleStitchBrick.class,
						SewUpBrick.class,
						StopRunningStitchBrick.class,
						WriteEmbroideryToFileBrick.class)},
				{"Event", Arrays.asList(WhenStartedBrick.class,
						WhenBrick.class,
						WhenTouchDownBrick.class,
						BroadcastReceiverBrick.class,
						BroadcastBrick.class,
						BroadcastWaitBrick.class,
						WhenConditionBrick.class,
						WhenBounceOffBrick.class,
						WhenBackgroundChangesBrick.class,
						WhenClonedBrick.class,
						CloneBrick.class,
						DeleteThisCloneBrick.class)},
				{"Looks", Arrays.asList(SetLookBrick.class,
						SetLookByIndexBrick.class,
						NextLookBrick.class,
						PreviousLookBrick.class,
						SetSizeToBrick.class,
						SetWidthBrick.class,
						ChangeWidthBrick.class,
						ChangeHeightBrick.class,
						SetHeightBrick.class,
						ChangeSizeByNBrick.class,
						HideBrick.class,
						ShowBrick.class,
						AskBrick.class,
						SayBubbleBrick.class,
						SayForBubbleBrick.class,
						ThinkBubbleBrick.class,
						ThinkForBubbleBrick.class,
						ShowTextBrick.class,
						ShowTextColorSizeAlignmentBrick.class,
						SetTransparencyBrick.class,
						ChangeTransparencyByNBrick.class,
						SetBrightnessBrick.class,
						ChangeBrightnessByNBrick.class,
						SetColorBrick.class,
						ChangeColorByNBrick.class,
						FadeParticleEffectBrick.class,
						ParticleEffectAdditivityBrick.class,
						SetParticleColorBrick.class,
						ClearGraphicEffectBrick.class,
						SetCameraFocusPointBrick.class,
						WhenBackgroundChangesBrick.class,
						SetBackgroundBrick.class,
						SetBackgroundByIndexBrick.class,
						SetBackgroundAndWaitBrick.class,
						SetBackgroundByIndexAndWaitBrick.class,
						CameraBrick.class,
						ChooseCameraBrick.class,
						FlashBrick.class,
						LookRequestBrick.class,
						ScreenShotBrick.class,
						SaveLookBrick.class,
						LookFileBrick.class,
						PaintNewLookBrick.class,
						EditLookBrick.class,
						CopyLookBrick.class,
						DeleteLookBrick.class,
						OpenUrlBrick.class)},
				{"Pen", Arrays.asList(PenDownBrick.class,
						PenUpBrick.class,
						SetPenSizeBrick.class,
						SetPenColorBrick.class,
						StampBrick.class,
						ClearBackgroundBrick.class)},
				{"Sound", Arrays.asList(PlaySoundBrick.class,
						PlaySoundAndWaitBrick.class,
						PlaySoundAtBrick.class,
						SoundFileBrick.class,
						StopSoundBrick.class,
						StopAllSoundsBrick.class,
						SetVolumeToBrick.class,
						ChangeVolumeByNBrick.class,
						SpeakBrick.class,
						SpeakAndWaitBrick.class,
						AskSpeechBrick.class,
						StartListeningBrick.class,
						SetListeningLanguageBrick.class,
						SetInstrumentBrick.class,
						PlayNoteForBeatsBrick.class,
						PlayDrumForBeatsBrick.class,
						SetTempoBrick.class,
						ChangeTempoByNBrick.class,
						PauseForBeatsBrick.class)},
				{"Control", Arrays.asList(WaitBrick.class,
						NoteBrick.class,
						ForeverBrick.class,
						IfLogicBeginBrick.class,
						IfThenLogicBeginBrick.class,
						WaitUntilBrick.class,
						RepeatBrick.class,
						RepeatUntilBrick.class,
						ForVariableFromToBrick.class,
						ForItemInUserListBrick.class,
						SceneTransitionBrick.class,
						SceneStartBrick.class,
						SceneIdBrick.class,
						ExitStageBrick.class,
						StopScriptBrick.class,
						WaitTillIdleBrick.class,
						WhenClonedBrick.class,
						CloneBrick.class,
						DeleteThisCloneBrick.class,
						BroadcastReceiverBrick.class,
						BroadcastBrick.class,
						BroadcastWaitBrick.class,
						TapAtBrick.class,
						TapForBrick.class,
						TouchAndSlideBrick.class,
						OpenUrlBrick.class)},
				{"Data", Arrays.asList(SetVariableBrick.class,
						ChangeVariableBrick.class,
						ShowTextBrick.class,
						ShowTextColorSizeAlignmentBrick.class,
						HideTextBrick.class,
						WriteVariableOnDeviceBrick.class,
						ReadVariableFromDeviceBrick.class,
						WriteVariableToFileBrick.class,
						ReadVariableFromFileBrick.class,
						FileUrlBrick.class,
						AddItemToUserListBrick.class,
						DeleteItemOfUserListBrick.class,
						ClearUserListBrick.class,
						InsertItemIntoUserListBrick.class,
						ReplaceItemInUserListBrick.class,
						WriteListOnDeviceBrick.class,
						ReadListFromDeviceBrick.class,
						StoreCSVIntoUserListBrick.class,
						WebRequestBrick.class,
						PostWebRequestBrick.class,
						CreateVarBrick.class,
						DeleteVarBrick.class,
						DeleteVarsBrick.class,
						CreateTableBrick.class,
						InsertTableBrick.class,
						DeleteTableBrick.class,
						DeleteAllTablesBrick.class,
						LookRequestBrick.class,
						AskBrick.class,
						AskSpeechBrick.class,
						StartListeningBrick.class)},
				{"Neural", Arrays.asList(SetGeminiKeyBrick.class,
						AskGeminiBrick.class)},
				{"Internet", Arrays.asList(StartServerBrick.class,
						StopServerBrick.class,
						ConnectServerBrick.class,
						SendServerBrick.class,
						ListenServerBrick.class)},
				{"Device", Arrays.asList(ResetTimerBrick.class,
						ShowToastBlock.class,
						DividePolandBrick.class,
						CopyTextBrick.class,
						ListenMicroBrick.class,
						RunJSBrick.class,
						CreateWebUrlBrick.class,
						CreateWebFileBrick.class,
						DeleteWebBrick.class,
						WhenBrick.class,
						WhenTouchDownBrick.class,
						WebRequestBrick.class,
						LookRequestBrick.class,
						OpenUrlBrick.class,
						VibrationBrick.class,
						SpeakBrick.class,
						SpeakAndWaitBrick.class,
						AskSpeechBrick.class,
						StartListeningBrick.class,
						CameraBrick.class,
						ChooseCameraBrick.class,
						FlashBrick.class,
						WriteVariableOnDeviceBrick.class,
						ReadVariableFromDeviceBrick.class,
						WriteVariableToFileBrick.class,
						ReadVariableFromFileBrick.class,
						WriteListOnDeviceBrick.class,
						ReadListFromDeviceBrick.class,
						TapAtBrick.class,
						TapForBrick.class,
						TouchAndSlideBrick.class)
				},
				{"Lego NXT", Arrays.asList(LegoNxtMotorTurnAngleBrick.class,
						LegoNxtMotorStopBrick.class,
						LegoNxtMotorMoveBrick.class,
						LegoNxtPlayToneBrick.class)},
				{"Lego EV3", Arrays.asList(LegoEv3MotorTurnAngleBrick.class,
						LegoEv3MotorMoveBrick.class,
						LegoEv3MotorStopBrick.class,
						LegoEv3PlayToneBrick.class,
						LegoEv3SetLedBrick.class)},
				{"AR.Drone 2.0", Arrays.asList(DroneTakeOffLandBrick.class,
						DroneEmergencyBrick.class,
						DroneMoveUpBrick.class,
						DroneMoveDownBrick.class,
						DroneMoveLeftBrick.class,
						DroneMoveRightBrick.class,
						DroneMoveForwardBrick.class,
						DroneMoveBackwardBrick.class,
						DroneTurnLeftBrick.class,
						DroneTurnRightBrick.class,
						DroneFlipBrick.class,
						DronePlayLedAnimationBrick.class,
						DroneSwitchCameraBrick.class)},
				{"Jumping Sumo", Arrays.asList(JumpingSumoMoveForwardBrick.class,
						JumpingSumoMoveBackwardBrick.class,
						JumpingSumoAnimationsBrick.class,
						JumpingSumoSoundBrick.class,
						JumpingSumoNoSoundBrick.class,
						JumpingSumoJumpLongBrick.class,
						JumpingSumoJumpHighBrick.class,
						JumpingSumoRotateLeftBrick.class,
						JumpingSumoRotateRightBrick.class,
						JumpingSumoTurnBrick.class,
						JumpingSumoTakingPictureBrick.class)},
				{"Phiro", Arrays.asList(PhiroMotorMoveForwardBrick.class,
						PhiroMotorMoveBackwardBrick.class,
						PhiroMotorStopBrick.class,
						PhiroPlayToneBrick.class,
						PhiroRGBLightBrick.class,
						PhiroIfLogicBeginBrick.class,
						SetVariableBrick.class,
						SetVariableBrick.class,
						SetVariableBrick.class,
						SetVariableBrick.class,
						SetVariableBrick.class,
						SetVariableBrick.class)},
				{"Arduino", Arrays.asList(ArduinoSendDigitalValueBrick.class,
						ArduinoSendPWMValueBrick.class)},
				{"Chromecast", Arrays.asList(WhenGamepadButtonBrick.class)},
				{"Raspberry Pi", Arrays.asList(WhenRaspiPinChangedBrick.class,
						RaspiIfLogicBeginBrick.class,
						RaspiSendDigitalValueBrick.class,
						RaspiPwmBrick.class)},
				{"Testing", Arrays.asList(AssertEqualsBrick.class,
						AssertUserListsBrick.class,
						ParameterizedBrick.class,
						WaitTillIdleBrick.class,
						TapAtBrick.class,
						TapForBrick.class,
						TouchAndSlideBrick.class,
						FinishStageBrick.class,
						StoreCSVIntoUserListBrick.class,
						WebRequestBrick.class)},
		});
	}

	@Parameterized.Parameter
	public String category;

	@Parameterized.Parameter(1)
	public List<Class> expectedClasses;

	private CategoryBricksFactory categoryBricksFactory;

	@Before
	public void setUp() throws Exception {
		Editor sharedPreferencesEditor = PreferenceManager
				.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext()).edit();

		sharedPreferencesEditor.clear();
		// The speech AI settings have to be activated here, because these bricks have no own
		// brick category.
		for (String setting : speechAISettings) {
			sharedPreferencesEditor.putBoolean(setting, true);
		}
		sharedPreferencesEditor.commit();

		createProject(ApplicationProvider.getApplicationContext());
		categoryBricksFactory = new CategoryBricksFactory();
	}

	@After
	public void tearDown() {
		Editor sharedPreferencesEditor = PreferenceManager
				.getDefaultSharedPreferences(ApplicationProvider.getApplicationContext()).edit();
		sharedPreferencesEditor.clear().commit();
	}

	public void createProject(Context context) {
		Project project = new Project(context, getClass().getSimpleName());
		Sprite sprite = new Sprite("testSprite");
		Script script = new StartScript();
		script.addBrick(new SetXBrick());
		sprite.addScript(script);
		project.getDefaultScene().addSprite(sprite);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentSprite(sprite);
		ProjectManager.getInstance().setCurrentlyEditedScene(project.getDefaultScene());
	}

	@Test
	public void testBrickCategory() {
		List<Brick> categoryBricks = categoryBricksFactory.getBricks(category, false,
				ApplicationProvider.getApplicationContext());

		List<Class> brickClasses = new ArrayList<>();
		for (Brick brick : categoryBricks) {
			brickClasses.add(brick.getClass());
		}

		assertEquals(expectedClasses, brickClasses);
	}
}
