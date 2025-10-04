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
package org.catrobat.catroid.content;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.BrickValues;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ParameterizedData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.actions.*;
import org.catrobat.catroid.content.actions.conditional.GlideToAction;
import org.catrobat.catroid.content.actions.conditional.IfOnEdgeBounceAction;
import org.catrobat.catroid.content.bricks.HideText3Brick;
import org.catrobat.catroid.content.bricks.LegoEv3MotorMoveBrick;
import org.catrobat.catroid.content.bricks.LegoEv3MotorStopBrick;
import org.catrobat.catroid.content.bricks.LegoEv3MotorTurnAngleBrick;
import org.catrobat.catroid.content.bricks.LegoEv3SetLedBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorMoveBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorStopBrick;
import org.catrobat.catroid.content.bricks.LegoNxtMotorTurnAngleBrick;
import org.catrobat.catroid.content.bricks.PhiroMotorMoveBackwardBrick;
import org.catrobat.catroid.content.bricks.PhiroMotorMoveForwardBrick;
import org.catrobat.catroid.content.bricks.PhiroMotorStopBrick;
import org.catrobat.catroid.content.bricks.PhiroPlayToneBrick;
import org.catrobat.catroid.content.bricks.PhiroRGBLightBrick;
import org.catrobat.catroid.content.bricks.brickspinner.PickableDrum;
import org.catrobat.catroid.content.bricks.brickspinner.PickableMusicalInstrument;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserData;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.DeviceListAccessor;
import org.catrobat.catroid.io.DeviceUserDataAccessor;
import org.catrobat.catroid.io.DeviceVariableAccessor;
import org.catrobat.catroid.libraries.CustomBrickDefinition;
import org.catrobat.catroid.physics.PhysicsLook;
import org.catrobat.catroid.physics.PhysicsObject;
import org.catrobat.catroid.stage.SpeechSynthesizer;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.userbrick.UserDefinedBrickInput;
import org.catrobat.catroid.utils.MobileServiceAvailability;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import kotlin.Pair;

import static org.koin.java.KoinJavaComponent.get;

import android.util.Log;

@LunoClass
public class ActionFactory extends Actions {

	public EventAction createUserBrickAction(Sprite sprite, SequenceAction sequence,
			List<UserDefinedBrickInput> userDefinedBrickInputs, UUID userDefinedBrickID) {
		UserDefinedBrickAction action = action(UserDefinedBrickAction.class);

		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setInputs(userDefinedBrickInputs);
		action.setUserDefinedBrickID(userDefinedBrickID);
		action.setSprite(sprite);
		action.setWait(true);
		return action;
	}

	public EventAction createBroadcastAction(String broadcastMessage, boolean wait) {
		BroadcastAction action = action(BroadcastAction.class);
		action.setBroadcastMessage(broadcastMessage);
		action.setWait(wait);
		return action;
	}

	public Action RunShellAction(Sprite sprite, SequenceAction sequence, Formula command, UserVariable variable) {
		RunShellAction action = action(RunShellAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCommand(command);
		action.setUserVariable(variable);
		return action;
	}

	public Action createWaitAction(Sprite sprite, SequenceAction sequence, Formula delay) {
		WaitAction action = action(WaitAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDelay(delay);
		return action;
	}

	public Action createCopyProjectFileAction(Sprite sprite, SequenceAction sequence,
											  Formula sourceFileName, Formula newFileName) {
		CopyProjectFileAction action = action(CopyProjectFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSourceFileName(sourceFileName);
		action.setNewFileName(newFileName);
		return action;
	}

	public Action createSetSoundVolumeAction(Sprite sprite, SequenceAction sequence,
											 SoundInfo sound, Formula volume) {
		SetSoundVolumeAction action = action(SetSoundVolumeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSound(sound);
		action.setVolume(volume);
		return action;
	}

	public Action createWaitForSoundAction(Sprite sprite, SequenceAction sequence, Formula delay,
			String soundFilePath) {
		WaitForSoundAction action = action(WaitForSoundAction.class);
		action.setSoundFilePath(soundFilePath);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDelay(delay);
		return action;
	}

	public Action createCreate3dObjectAction(Sprite sprite, ScriptSequenceAction sequence,
											 Formula objectId, Formula modelPath) {
		Create3dObjectAction action = Actions.action(Create3dObjectAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setModelPath(modelPath);
		return action;
	}

	public Action createSetFogAction(Sprite sprite, SequenceAction sequence, Formula r, Formula g, Formula b, Formula density) {
		SetFogAction action = Actions.action(SetFogAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.red = r;
		action.green = g;
		action.blue = b;
		action.density = density;
		return action;
	}

	public Action createCastRayAction(Sprite sprite, ScriptSequenceAction sequence,
									  Formula rayName,
									  Formula fromX, Formula fromY, Formula fromZ,
									  Formula dirX, Formula dirY, Formula dirZ) {
		CastRayAction action = Actions.action(CastRayAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.rayName = rayName;
		action.fromX = fromX; action.fromY = fromY; action.fromZ = fromZ;
		action.dirX = dirX; action.dirY = dirY; action.dirZ = dirZ;
		return action;
	}

	public Action createSetSkyColorAction(Sprite sprite, ScriptSequenceAction sequence,
										  Formula red, Formula green, Formula blue) {
		SetSkyColorAction action = Actions.action(SetSkyColorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.red = red;
		action.green = green;
		action.blue = blue;
		return action;
	}

	public Action createRemove3dObjectAction(Sprite sprite, ScriptSequenceAction sequence, Formula objectId) {
		Remove3dObjectAction action = Actions.action(Remove3dObjectAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.objectId = objectId;
		return action;
	}

	public Action createSet3dPositionAction(Sprite sprite, ScriptSequenceAction sequence,
											Formula objectId, Formula x, Formula y, Formula z) {
		Set3dPositionAction action = Actions.action(Set3dPositionAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action set3dVelocity(Sprite sprite, ScriptSequenceAction sequence,
											Formula objectId, Formula x, Formula y, Formula z) {
		Set3dVelocityAction action = Actions.action(Set3dVelocityAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action apply3dForce(Sprite sprite, ScriptSequenceAction sequence,
								Formula objectId, Formula x, Formula y, Formula z) {
		Apply3dForceAction action = Actions.action(Apply3dForceAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action createSetObjectTextureAction(Sprite sprite, ScriptSequenceAction sequence,
											   Formula objectId, Formula textureName) {
		SetObjectTextureAction action = Actions.action(SetObjectTextureAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.textureName = textureName;
		return action;
	}

	public Action createSetCameraRotationAction(Sprite sprite, ScriptSequenceAction sequence,
												Formula yaw, Formula pitch, Formula roll) {
		SetCameraRotationAction action = Actions.action(SetCameraRotationAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.yaw = yaw;
		action.pitch = pitch;
		action.roll = roll;
		return action;
	}

	public Action createCube(Sprite sprite, ScriptSequenceAction sequence,
												Formula name) {
		CreateCubeAction action = Actions.action(CreateCubeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = name;
		return action;
	}

	public Action set3dGravity(Sprite sprite, ScriptSequenceAction sequence, Formula x, Formula y, Formula z) {
		Set3dGravityAction action = Actions.action(Set3dGravityAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action createSetObjectColorAction(Sprite sprite, ScriptSequenceAction sequence,
											 Formula id, Formula r, Formula g, Formula b) {
		SetObjectColorAction action = Actions.action(SetObjectColorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = id;
		action.red = r;
		action.green = g;
		action.blue = b;
		return action;
	}

	public Action createSetAmbientLightAction(Sprite sprite, ScriptSequenceAction sequence,
											  Formula r, Formula g, Formula b) {
		SetAmbientLightAction action = Actions.action(SetAmbientLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.red = r;
		action.green = g;
		action.blue = b;
		return action;
	}

	public Action createSetDirectionalLightAction(Sprite sprite, ScriptSequenceAction sequence,
												  Formula id, Formula r, Formula g, Formula b, Formula dx, Formula dy, Formula dz) {
		SetDirectionalLightAction action = Actions.action(SetDirectionalLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.lightId = id;
		action.red = r;
		action.green = g;
		action.blue = b;
		action.dirX = dx;
		action.dirY = dy;
		action.dirZ = dz;
		return action;
	}

	public Action createUploadFileAction(Sprite sprite, SequenceAction sequence, Formula url, Formula filePath, int fileTypeSelection, Formula mimeType, int storageTypeSelection) {
		UploadFileAction action = Actions.action(UploadFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.url = url;
		action.filePath = filePath;
		action.fileTypeSelection = fileTypeSelection;
		action.mimeType = mimeType;
		action.storageTypeSelection = storageTypeSelection;
		return action;
	}

	public Action createSetShaderUniformAction(Sprite sprite, SequenceAction sequence, Formula name, Formula x, Formula y, Formula z) {
		SetShaderUniformAction action = Actions.action(SetShaderUniformAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setUniformName(name);
		action.setValueX(x);
		action.setValueY(y); // Будет null для float
		action.setValueZ(z); // Будет null для float
		return action;
	}

	public Action createSetShaderCodeAction(Sprite sprite, SequenceAction sequence, Formula vertexCode, Formula fragmentCode) {
		SetShaderCodeAction action = Actions.action(SetShaderCodeAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setVertexCode(vertexCode);
		action.setFragmentCode(fragmentCode);
		return action;
	}

	// в org.catrobat.catroid.content
	public Action createSetCCDAction(Sprite sprite, SequenceAction sequence, Formula objectId, Formula enabled) {
		SetCCDAction action = Actions.action(SetCCDAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setEnabled(enabled);
		return action;
	}

	public Action createSetPhysicsStateAction(Sprite sprite, ScriptSequenceAction sequence,
											  Formula objectId, int stateSelection, Formula mass) {
		SetPhysicsStateAction action = Actions.action(SetPhysicsStateAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.stateSelection = stateSelection;
		action.mass = mass;
		return action;
	}

	public Action createSetCameraPositionAction(Sprite sprite, ScriptSequenceAction sequence,
												Formula x, Formula y, Formula z) {
		SetCameraPositionAction action = Actions.action(SetCameraPositionAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action createCameraLookAtAction(Sprite sprite, ScriptSequenceAction sequence,
										   Formula x, Formula y, Formula z) {
		CameraLookAtAction action = Actions.action(CameraLookAtAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action createSet3dRotationAction(Sprite sprite, ScriptSequenceAction sequence,
											Formula objectId, Formula yaw, Formula pitch, Formula roll) {
		Set3dRotationAction action = Actions.action(Set3dRotationAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.yaw = yaw;
		action.pitch = pitch;
		action.roll = roll;
		return action;
	}

	public Action createSet3dScaleAction(Sprite sprite, ScriptSequenceAction sequence,
										 Formula objectId, Formula scaleX, Formula scaleY, Formula scaleZ) {
		Set3dScaleAction action = Actions.action(Set3dScaleAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.scaleX = scaleX;
		action.scaleY = scaleY;
		action.scaleZ = scaleZ;
		return action;
	}

	public Action createObjectLookAtAction(Sprite sprite, ScriptSequenceAction sequence,
										   Formula objectId, Formula x, Formula y, Formula z) {
		ObjectLookAtAction action = Actions.action(ObjectLookAtAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.xValue = x;
		action.yValue = y;
		action.zValue = z;
		return action;
	}

	public Action createPlaySoundAtAction(Sprite sprite, SequenceAction sequence, Formula delay,
			SoundInfo sound) {
		PlaySoundAtAction action = action(PlaySoundAtAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setSprite(sprite);
		action.setSound(sound);
		action.setScope(scope);
		action.setOffset(delay);
		return action;
	}

	public Action createWaitForBubbleBrickAction(Sprite sprite, SequenceAction sequence, Formula delay) {
		WaitForBubbleBrickAction action = Actions.action(WaitForBubbleBrickAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDelay(delay);
		return action;
	}

	public Action createChangeBrightnessByNAction(Sprite sprite, SequenceAction sequence,
			Formula changeBrightness) {
		ChangeBrightnessByNAction action = Actions.action(ChangeBrightnessByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBrightness(changeBrightness);
		return action;
	}

	public Action createChangeColorByNAction(Sprite sprite, SequenceAction sequence,
			Formula changeColor) {
		ChangeColorByNAction action = Actions.action(ChangeColorByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setColor(changeColor);
		return action;
	}

	public Action createChangeTransparencyByNAction(Sprite sprite, SequenceAction sequence,
			Formula transparency) {
		ChangeTransparencyByNAction action = Actions.action(ChangeTransparencyByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setTransparency(transparency);
		return action;
	}

	public Action createChangeSizeByNAction(Sprite sprite, SequenceAction sequence, Formula size) {
		ChangeSizeByNAction action = Actions.action(ChangeSizeByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createChangeVolumeByNAction(Sprite sprite, SequenceAction sequence, Formula volume) {
		ChangeVolumeByNAction action = Actions.action(ChangeVolumeByNAction.class);
		action.setVolume(volume);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createChangeXByNAction(Sprite sprite, SequenceAction sequence, Formula xMovement) {
		ChangeXByNAction action = Actions.action(ChangeXByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setxMovement(xMovement);
		return action;
	}

	public Action createChangeYByNAction(Sprite sprite, SequenceAction sequence, Formula yMovement) {
		ChangeYByNAction action = Actions.action(ChangeYByNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setyMovement(yMovement);
		return action;
	}

	public Action createSetRotationStyleAction(Sprite sprite, @Look.RotationStyle int mode) {
		SetRotationStyleAction action = Actions.action(SetRotationStyleAction.class);
		action.setRotationStyle(mode);
		action.setSprite(sprite);
		return action;
	}

	public Action createClearGraphicEffectAction(Sprite sprite) {
		ClearGraphicEffectAction action = Actions.action(ClearGraphicEffectAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createComeToFrontAction(Sprite sprite) {
		ComeToFrontAction action = Actions.action(ComeToFrontAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createGlideToAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y,
			Formula duration) {
		GlideToAction action = Actions.action(GlideToAction.class);
		action.setPosition(x, y);
		action.setDuration(duration);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createPlaceAtAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y) {
		GlideToAction action = Actions.action(GlideToAction.class);
		action.setPosition(x, y);
		action.setDuration(0);
		action.setInterpolation(null);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSquareAction(Sprite sprite, SequenceAction sequence,
									 Formula name, Formula color,
									 Formula x, Formula y,
									 Formula w, Formula h,
									 Formula trans, Formula rot,
									 Formula borders) {
		SquareAction action = Actions.action(SquareAction.class);
		action.setX(x);
		action.setY(y);
		action.setNam(name);
		action.setColor(color);
		action.setW(w);
		action.setH(h);
		action.setTrans(trans);
		action.setRot(rot);
		action.setBord(borders);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createDelSquareAction(Sprite sprite, SequenceAction sequence,
									 Formula name) {
		DelSquareAction action = Actions.action(DelSquareAction.class);
		action.setNam(name);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createGoToAction(Sprite sprite, Sprite destinationSprite, int spinnerSelection) {
		switch (spinnerSelection) {
			case BrickValues.GO_TO_TOUCH_POSITION:
				GoToTouchPositionAction touchPositionAction = Actions.action(GoToTouchPositionAction.class);
				touchPositionAction.setSprite(sprite);
				return touchPositionAction;
			case BrickValues.GO_TO_RANDOM_POSITION:
				GoToRandomPositionAction randomPositionAction = Actions.action(GoToRandomPositionAction.class);
				randomPositionAction.setSprite(sprite);
				return randomPositionAction;
			case BrickValues.GO_TO_OTHER_SPRITE_POSITION:
				GoToOtherSpritePositionAction otherSpritePositionAction = Actions
						.action(GoToOtherSpritePositionAction.class);
				otherSpritePositionAction.setSprite(sprite);
				otherSpritePositionAction.setDestinationSprite(destinationSprite);
				return otherSpritePositionAction;
			default:
				return null;
		}
	}

	public Action createGoNStepsBackAction(Sprite sprite, SequenceAction sequence, Formula steps) {
		GoNStepsBackAction action = Actions.action(GoNStepsBackAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSteps(steps);
		return action;
	}

	public Action createHideAction(Sprite sprite) {
		SetVisibleAction action = Actions.action(SetVisibleAction.class);
		action.setSprite(sprite);
		action.setVisible(false);
		return action;
	}

	public Action createIfOnEdgeBounceAction(Sprite sprite) {
		IfOnEdgeBounceAction action = Actions.action(IfOnEdgeBounceAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createLegoNxtMotorMoveAction(Sprite sprite, SequenceAction sequence,
			LegoNxtMotorMoveBrick.Motor motorEnum, Formula speed) {
		LegoNxtMotorMoveAction action = Actions.action(LegoNxtMotorMoveAction.class);
		action.setMotorEnum(motorEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSpeed(speed);
		return action;
	}

	public Action createLegoNxtMotorStopAction(LegoNxtMotorStopBrick.Motor motorEnum) {
		LegoNxtMotorStopAction action = Actions.action(LegoNxtMotorStopAction.class);
		action.setMotorEnum(motorEnum);
		return action;
	}

	public Action createLegoNxtMotorTurnAngleAction(Sprite sprite, SequenceAction sequence,
			LegoNxtMotorTurnAngleBrick.Motor motorEnum, Formula degrees) {
		LegoNxtMotorTurnAngleAction action = Actions.action(LegoNxtMotorTurnAngleAction.class);
		action.setMotorEnum(motorEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDegrees(degrees);
		return action;
	}

	public Action createLegoNxtPlayToneAction(Sprite sprite, SequenceAction sequence, Formula hertz,
			Formula durationInSeconds) {
		LegoNxtPlayToneAction action = Actions.action(LegoNxtPlayToneAction.class);
		action.setHertz(hertz);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDurationInSeconds(durationInSeconds);
		return action;
	}

	public Action createLegoEv3SingleMotorMoveAction(Sprite sprite, SequenceAction sequence,
			LegoEv3MotorMoveBrick.Motor motorEnum, Formula speed) {
		LegoEv3MotorMoveAction action = action(LegoEv3MotorMoveAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setMotorEnum(motorEnum);
		action.setSpeed(speed);
		return action;
	}

	public Action createLegoEv3MotorStopAction(LegoEv3MotorStopBrick.Motor motorEnum) {
		LegoEv3MotorStopAction action = action(LegoEv3MotorStopAction.class);
		action.setMotorEnum(motorEnum);
		return action;
	}

	public Action createLegoEv3SetLedAction(LegoEv3SetLedBrick.LedStatus ledStatusEnum) {
		LegoEv3SetLedAction action = action(LegoEv3SetLedAction.class);
		action.setLedStatusEnum(ledStatusEnum);
		return action;
	}

	public Action createLegoEv3PlayToneAction(Sprite sprite, SequenceAction sequence,
			Formula hertz, Formula durationInSeconds, Formula volumeInPercent) {
		LegoEv3PlayToneAction action = action(LegoEv3PlayToneAction.class);
		action.setHertz(hertz);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDurationInSeconds(durationInSeconds);
		action.setVolumeInPercent(volumeInPercent);
		return action;
	}

	public Action createLegoEv3MotorTurnAngleAction(Sprite sprite, SequenceAction sequence,
			LegoEv3MotorTurnAngleBrick.Motor motorEnum, Formula degrees) {
		LegoEv3MotorTurnAngleAction action = action(LegoEv3MotorTurnAngleAction.class);
		action.setMotorEnum(motorEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDegrees(degrees);
		return action;
	}

	public Action createPhiroPlayToneActionAction(Sprite sprite, SequenceAction sequence,
			PhiroPlayToneBrick.Tone toneEnum, Formula duration) {
		PhiroPlayToneAction action = action(PhiroPlayToneAction.class);
		action.setSelectedTone(toneEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDurationInSeconds(duration);
		return action;
	}

	public Action createPhiroMotorMoveForwardActionAction(Sprite sprite, SequenceAction sequence,
			PhiroMotorMoveForwardBrick.Motor motorEnum, Formula speed) {
		PhiroMotorMoveForwardAction action = action(PhiroMotorMoveForwardAction.class);
		action.setMotorEnum(motorEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSpeed(speed);
		return action;
	}

	public Action createPhiroMotorMoveBackwardActionAction(Sprite sprite, SequenceAction sequence,
			PhiroMotorMoveBackwardBrick.Motor motorEnum, Formula speed) {
		PhiroMotorMoveBackwardAction action = action(PhiroMotorMoveBackwardAction.class);
		action.setMotorEnum(motorEnum);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSpeed(speed);
		return action;
	}

	public Action createPhiroRgbLedEyeActionAction(Sprite sprite, SequenceAction sequence,
			PhiroRGBLightBrick.Eye eye, Formula red, Formula green, Formula blue) {
		PhiroRGBLightAction action = action(PhiroRGBLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setEyeEnum(eye);
		action.setRed(red);
		action.setGreen(green);
		action.setBlue(blue);
		return action;
	}

	public Action createPhiroSendSelectedSensorAction(Sprite sprite, SequenceAction sequence,
			int sensorNumber, Action ifAction, Action elseAction) {
		PhiroSensorAction action = action(PhiroSensorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSensor(sensorNumber);
		action.setIfAction(ifAction);
		action.setElseAction(elseAction);
		return action;
	}

	public Action createPhiroMotorStopActionAction(PhiroMotorStopBrick.Motor motorEnum) {
		PhiroMotorStopAction action = action(PhiroMotorStopAction.class);
		action.setMotorEnum(motorEnum);
		return action;
	}

	public Action createMoveNStepsAction(Sprite sprite, SequenceAction sequence, Formula steps) {
		MoveNStepsAction action = Actions.action(MoveNStepsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSteps(steps);
		return action;
	}

	public Action createPenDownAction(Sprite sprite) {
		PenDownAction action = Actions.action(PenDownAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createPenUpAction(Sprite sprite) {
		PenUpAction action = Actions.action(PenUpAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createStartPlotAction(Sprite sprite) {
		StartPlotAction action = Actions.action(StartPlotAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createStopPlotAction(Sprite sprite) {
		StopPlotAction action = Actions.action(StopPlotAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createSetPenSizeAction(Sprite sprite, SequenceAction sequence, Formula penSize) {
		SetPenSizeAction action = Actions.action(SetPenSizeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPenSize(penSize);
		return action;
	}

	public Action createSetPenColorAction(Sprite sprite, SequenceAction sequence, Formula red,
			Formula green, Formula blue) {
		SetPenColorAction action = Actions.action(SetPenColorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRed(red);
		action.setGreen(green);
		action.setBlue(blue);
		return action;
	}

	public Action createClearBackgroundAction() {
		return Actions.action(ClearBackgroundAction.class);
	}

	public Action createSetCameraFocusPointAction(Sprite sprite, SequenceAction sequence,
			Formula horizontal, Formula vertical) {
		SetCameraFocusPointAction action = action(SetCameraFocusPointAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSprite(sprite);
		action.setHorizontal(horizontal);
		action.setVertical(vertical);
		return action;
	}

	public Action createStampAction(Sprite sprite) {
		StampAction action = Actions.action(StampAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createPlaySoundAction(Sprite sprite, SoundInfo sound) {
		PlaySoundAction action = Actions.action(PlaySoundAction.class);
		action.setSprite(sprite);
		action.setSound(sound);
		return action;
	}

	public Action createStopSoundAction(Sprite sprite, SoundInfo sound) {
		StopSoundAction action = Actions.action(StopSoundAction.class);
		action.setSprite(sprite);
		action.setSound(sound);
		return action;
	}

	public Action createPointInDirectionAction(Sprite sprite, SequenceAction sequence, Formula degrees) {
		PointInDirectionAction action = Actions.action(PointInDirectionAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDegreesInUserInterfaceDimensionUnit(degrees);
		return action;
	}

	public Action createPointToAction(Sprite sprite, Sprite pointedSprite) {
		PointToAction action = Actions.action(PointToAction.class);
		action.setSprite(sprite);
		action.setPointedSprite(pointedSprite);
		return action;
	}

	public Action createCloneAction(Sprite sprite) {
		CloneAction action = Actions.action(CloneAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createDeleteThisCloneAction(Sprite sprite) {
		DeleteThisCloneAction action = Actions.action(DeleteThisCloneAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createSetBrightnessAction(Sprite sprite, SequenceAction sequence, Formula brightness) {
		SetBrightnessAction action = Actions.action(SetBrightnessAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBrightness(brightness);
		return action;
	}

	public Action createSetColorAction(Sprite sprite, SequenceAction sequence, Formula color) {
		SetColorAction action = Actions.action(SetColorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setColor(color);
		return action;
	}

	public Action playVideoAction(Sprite sprite, SequenceAction sequence, Formula name) {
		PlayVideoAction action = Actions.action(PlayVideoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action pauseVideoAction(Sprite sprite, SequenceAction sequence, Formula name) {
		PauseVideoAction action = Actions.action(PauseVideoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action seekVideoAction(Sprite sprite, SequenceAction sequence, Formula name, Formula time ) {
		SeekVideoAction action = Actions.action(SeekVideoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setTime(time);
		return action;
	}

	public Action createSetTransparencyAction(Sprite sprite, SequenceAction sequence,
			Formula transparency) {
		SetTransparencyAction action = Actions.action(SetTransparencyAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setTransparency(transparency);
		return action;
	}

	public Action createSetLookAction(Sprite sprite, LookData lookData) {
		return createSetLookAction(sprite, lookData, false);
	}

	public Action createSetHitboxAction(Sprite sprite, SequenceAction sequence, LookData lookData) {
		SetHitboxAction action = Actions.action(SetHitboxAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLookData(lookData);
		return action;
	}

	public Action createSetLookAction(Sprite sprite, LookData lookData, boolean wait) {
		SetLookAction action = Actions.action(SetLookAction.class);
		action.setSprite(sprite);
		action.setLookData(lookData);
		action.setWait(wait);
		return action;
	}

	public Action createSetLookByIndexAction(Sprite sprite, SequenceAction sequence, Formula formula) {
		SetLookByIndexAction action = Actions.action(SetLookByIndexAction.class);
		action.setSprite(sprite);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(formula);
		return action;
	}

	public Action createSetBackgroundAction(LookData lookData, boolean wait) {
		SetLookAction action = Actions.action(SetLookAction.class);
		action.setSprite(ProjectManager.getInstance().getCurrentlyPlayingScene().getBackgroundSprite());
		action.setLookData(lookData);
		action.setWait(wait);
		return action;
	}

	public Action createSetBackgroundByIndexAction(Sprite sprite, SequenceAction sequence,
			Formula formula, boolean wait) {
		SetLookByIndexAction action = Actions.action(SetLookByIndexAction.class);
		action.setSprite(ProjectManager.getInstance().getCurrentlyPlayingScene().getBackgroundSprite());
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(formula);
		action.setWait(wait);
		return action;
	}

	public Action createSetNextLookAction(Sprite sprite, SequenceAction sequence) {
		SetNextLookAction action = Actions.action(SetNextLookAction.class);
		action.setSprite(sprite);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSetPreviousLookAction(Sprite sprite, SequenceAction sequence) {
		SetPreviousLookAction action = action(SetPreviousLookAction.class);
		action.setSprite(sprite);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createDeleteLookAction(Sprite sprite) {
		DeleteLookAction action = Actions.action(DeleteLookAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createSetSizeToAction(Sprite sprite, SequenceAction sequence, Formula size) {
		SetSizeToAction action = Actions.action(SetSizeToAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createSetWidthAction(Sprite sprite, SequenceAction sequence, Formula size) {
		SetWidthAction action = Actions.action(SetWidthAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createChangeWidthAction(Sprite sprite, SequenceAction sequence, Formula size) {
		ChangeWidthAction action = Actions.action(ChangeWidthAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createSetHeightAction(Sprite sprite, SequenceAction sequence, Formula size) {
		SetHeightAction action = Actions.action(SetHeightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createChangeHeightAction(Sprite sprite, SequenceAction sequence, Formula size) {
		ChangeHeightAction action = Actions.action(ChangeHeightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSize(size);
		return action;
	}

	public Action createGlideToPhysicsAction(Sprite sprite, PhysicsLook physicsLook,
			SequenceAction sequence, Formula x,
			Formula y, float duration, float delta) {

		GlideToPhysicsAction action = Actions.action(GlideToPhysicsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPhysicsLook(physicsLook);
		action.setPosition(x, y);
		action.setDuration(duration);
		action.act(delta);
		return action;
	}

	public Action createSetVolumeToAction(Sprite sprite, SequenceAction sequence, Formula volume) {
		SetVolumeToAction action = Actions.action(SetVolumeToAction.class);
		action.setVolume(volume);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSetXAction(Sprite sprite, SequenceAction sequence, Formula x) {
		SetXAction action = Actions.action(SetXAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setX(x);
		return action;
	}

	public Action createSetYAction(Sprite sprite, SequenceAction sequence, Formula y) {
		SetYAction action = Actions.action(SetYAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setY(y);
		return action;
	}

	public Action createShowAction(Sprite sprite) {
		SetVisibleAction action = Actions.action(SetVisibleAction.class);
		action.setSprite(sprite);
		action.setVisible(true);
		return action;
	}

	public Action createSpeakAction(Sprite sprite, SequenceAction sequence, Formula text) {
		SpeakAction action = action(SpeakAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setSpeechSynthesizer(new SpeechSynthesizer(scope, text));
		action.setMobileServiceAvailability(get(MobileServiceAvailability.class));
		action.setContext(StageActivity.activeStageActivity.get());

		return action;
	}

	public Action createSpeakAndWaitAction(Sprite sprite, SequenceAction sequence, Formula text) {
		SpeakAndWaitAction action = action(SpeakAndWaitAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setSpeechSynthesizer(new SpeechSynthesizer(scope, text));
		action.setMobileServiceAvailability(get(MobileServiceAvailability.class));
		action.setContext(StageActivity.activeStageActivity.get());
		return action;
	}

	public Action createStopAllSoundsAction() {
		return Actions.action(StopAllSoundsAction.class);
	}

	public Action createPauseForBeatsAction(Sprite sprite, SequenceAction sequence, Formula beats) {
		PauseForBeatsAction action = action(PauseForBeatsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBeats(beats);
		return action;
	}

	public Action createPlayNoteForBeatsAction(Sprite sprite, SequenceAction sequence, Formula note,
			Formula beats) {
		PlayNoteForBeatsAction action = action(PlayNoteForBeatsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setMidiValue(note);
		action.setBeats(beats);
		return action;
	}

	public Action createSetInstrumentAction(PickableMusicalInstrument instrument) {
		SetInstrumentAction action = action(SetInstrumentAction.class);
		action.setInstrument(instrument);
		return action;
	}

	public Action createSetTempoAction(Sprite sprite, SequenceAction sequence, Formula tempo) {
		SetTempoAction action = action(SetTempoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setTempo(tempo);
		return action;
	}

	public Action createChangeTempoAction(Sprite sprite, SequenceAction sequence, Formula tempo) {
		ChangeTempoByAction action = action(ChangeTempoByAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setTempo(tempo);
		return action;
	}

	public Action createPlayDrumForBeatsAction(Sprite sprite, SequenceAction sequence, Formula beats,
			PickableDrum drum) {
		PlayDrumForBeatsAction action = action(PlayDrumForBeatsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBeats(beats);
		action.setDrum(drum);
		return action;
	}

	public Action createTurnLeftAction(Sprite sprite, SequenceAction sequence, Formula degrees) {
		TurnLeftAction action = Actions.action(TurnLeftAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDegrees(degrees);
		return action;
	}

	public Action createTurnRightAction(Sprite sprite, SequenceAction sequence, Formula degrees) {
		TurnRightAction action = Actions.action(TurnRightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDegrees(degrees);
		return action;
	}

	public Action createChangeVariableAction(Sprite sprite, SequenceAction sequence, Formula variableFormula, UserVariable userVariable) {
		ChangeVariableAction action = Actions.action(ChangeVariableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setChangeVariable(variableFormula);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createSetVariableAction(Sprite sprite, SequenceAction sequence, Formula variableFormula,
			UserVariable userVariable) {
		SetVariableAction action = Actions.action(SetVariableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setChangeVariable(variableFormula);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createGetZipFileNamesAction(Sprite sprite, SequenceAction sequence,
											  Formula zipFileName, UserVariable userVariable) {
		GetZipFileNamesAction action = action(GetZipFileNamesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setZipFileName(zipFileName);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createTextfield(Sprite sprite, SequenceAction sequence,
										  Formula name, Formula def, Formula x, Formula y, Formula w, Formula h, Formula ts, Formula tc, Formula bc, Formula ht, Formula hc, Formula align,
                                  Formula pass, Formula corner, Formula len, Formula type, Formula font, UserVariable userVariable) {
		CreateTextFieldAction action = Actions.action(CreateTextFieldAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setInitialText(def);
		action.setPosX(x);
		action.setPosY(y);
		action.setWidth(w);
		action.setHeight(h);
		action.setText_size(ts);
		action.setText_color(tc);
		action.setBg_color(bc);
		action.setHint_text(ht);
		action.setHint_color(hc);
		action.setAlignment_f(align);
		action.set_password(pass);
		action.setCorner_radius(corner);
		action.setMax_length(len);
		action.setInput_type(type);
		action.setFont_path(font);
		action.setVariable(userVariable);
		return action;
	}

	public Action createReadFromFilesAction(Sprite sprite, SequenceAction sequence, Formula value,
										   UserVariable userVariable) {
		ReadFromFilesAction action = Actions.action(ReadFromFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(value);
		action.setVariable(userVariable);
		return action;
	}

	public Action createDeleteFilesAction(Sprite sprite, SequenceAction sequence, Formula value) {
		DeleteFilesAction action = Actions.action(DeleteFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(value);
		return action;
	}

	public Action createWriteToFilesAction(Sprite sprite, SequenceAction sequence, Formula value,
										  UserVariable userVariable) {
		WriteToFilesAction action = Actions.action(WriteToFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(value);
		action.setVariable(userVariable);
		return action;
	}

	public Action createPostWebRequestAction(Sprite sprite, SequenceAction sequence,
			Formula url, Formula header, Formula body, UserVariable userVariable) {
		PostWebRequestAction action = Actions.action(PostWebRequestAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRurl(url);
		action.setHeader(header);
		action.setBody(body);
		action.setVariable(userVariable);
		return action;
	}

	public Action createRunJS(Sprite sprite, SequenceAction sequence, Formula code, UserVariable userVariable) {
		RunJSAction action = Actions.action(RunJSAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRunScript(code);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createRunLua(Sprite sprite, SequenceAction sequence, Formula code, UserVariable userVariable) {
		RunLuaAction action = Actions.action(RunLuaAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCode(code);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createResizeImg(Sprite sprite, SequenceAction sequence, Formula file, Formula x, Formula y) {
		ResizeImgAction action = Actions.action(ResizeImgAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		action.setW(x);
		action.setH(y);
		return action;
	}

	public Action createGrayscaleImg(Sprite sprite, SequenceAction sequence, Formula file) {
		GrayscaleImgAction action = Actions.action(GrayscaleImgAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		return action;
	}

	public Action createNormalizeImg(Sprite sprite, SequenceAction sequence, Formula file, Formula r, Formula g, Formula b) {
		NormalizeImgAction action = Actions.action(NormalizeImgAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		action.setR(r);
		action.setG(g);
		action.setB(b);
		return action;
	}

	public Action createAskGeminiAction(Sprite sprite, SequenceAction sequence,
											 Formula question, UserVariable userVariable) {
		AskGeminiAction action = Actions.action(AskGeminiAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAsk(question);
		action.setVariable(userVariable);
		return action;
	}

	public Action createAskGemini2Action(Sprite sprite, SequenceAction sequence,
										Formula question, Formula model, UserVariable userVariable) {
		AskGemini2Action action = Actions.action(AskGemini2Action.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAsk(question);
		action.setModel(model);
		action.setVariable(userVariable);
		return action;
	}

	public Action createCustomAction(Sprite sprite, SequenceAction sequence,
									 CustomBrickDefinition definition, List<Formula> parameterFormulas) {
		CustomAction action = Actions.action(CustomAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

		// Используем сеттеры для настройки
		action.setScope(scope);
		action.setDefinition(definition);
		action.setParameterFormulas(parameterFormulas);

		return action;
	}

	public Action createAskAction(Sprite sprite, SequenceAction sequence, Formula questionFormula,
			UserVariable answerVariable) {
		AskAction action = Actions.action(AskAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setQuestionFormula(questionFormula);
		action.setAnswerVariable(answerVariable);
		return action;
	}

	public Action createBigAskAction(Sprite sprite, SequenceAction sequence, Formula questionFormula,
									 Formula msg, Formula ok, Formula canel, Formula def, UserVariable answerVariable) {
		BigAskAction action = Actions.action(BigAskAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setQuestionFormula(questionFormula);
		action.setMsg(msg);
		action.setSumb(ok);
		action.setCanel(canel);
		action.setStandardText(def);
		action.setAnswerVariable(answerVariable);
		return action;
	}

	public Action createAskSpeechAction(Sprite sprite, SequenceAction sequence, Formula questionFormula,
			UserVariable answerVariable) {
		AskSpeechAction action = Actions.action(AskSpeechAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setQuestionFormula(questionFormula);
		action.setAnswerVariable(answerVariable);
		return action;
	}

	public Action createDeleteItemOfUserListAction(Sprite sprite, SequenceAction sequence,
			Formula userListFormula, UserList userList) {
		DeleteItemOfUserListAction action = action(DeleteItemOfUserListAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormulaIndexToDelete(userListFormula);
		action.setUserList(userList);
		return action;
	}

	public Action createClearUserListAction(UserList userList) {
		ClearUserListAction action = action(ClearUserListAction.class);
		action.setUserList(userList);
		return action;
	}

	public Action createAddItemToUserListAction(Sprite sprite, SequenceAction sequence,
			Formula userListFormula, UserList userList) {
		AddItemToUserListAction action = action(AddItemToUserListAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormulaItemToAdd(userListFormula);
		action.setUserList(userList);
		return action;
	}

	public Action createRegexAction(Sprite sprite, SequenceAction sequence,
												Formula text, Formula regex, UserList userList) {
		RegexAction action = action(RegexAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setText_f(text);
		action.setRegex_f(regex);
		action.setUserlist(userList);
		return action;
	}

	public Action createShowToastAction(Sprite sprite, SequenceAction sequence,
			Formula toastFormula) {
		ShowToastAction action = action(ShowToastAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setToast(toastFormula);
		return action;
	}

	public Action createPhoto(Sprite sprite, SequenceAction sequence) {
		PhotoAction action = action(PhotoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createShader(Sprite sprite, SequenceAction sequence,
										Formula code, Formula code2) {
		ShaderAction action = action(ShaderAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(code);
		action.setVertex(code2);
		return action;
	}

	public Action createCutLook(Sprite sprite, SequenceAction sequence,
							   Formula x1, Formula y1, Formula x2, Formula y2) {
		CutLookAction action = action(CutLookAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setX1(x1);
		action.setY1(y1);
		action.setX2(x2);
		action.setY2(y2);
		return action;
	}

	public Action createLoadNNAction(Sprite sprite, SequenceAction sequence,
										Formula file) {
		LoadNNAction action = action(LoadNNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		return action;
	}

	public Action createUnoadNNAction(Sprite sprite, SequenceAction sequence) {
		UnloadNNAction action = action(UnloadNNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createPredictNNAction(Sprite sprite, SequenceAction sequence,
										Formula input, UserVariable variable) {
		PredictNNAction action = action(PredictNNAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setInput(input);
		action.setVariable(variable);
		return action;
	}

	public Action createLunoScriptAction(Sprite sprite, SequenceAction sequence,
										Formula code) {
		LunoscriptAction action = action(LunoscriptAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCode(code);
		return action;
	}

	public Action createHideStatusBarAction(Sprite sprite, SequenceAction sequence
										) {
		hideStatusBarAction action = action(hideStatusBarAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSetStopSoundsAction(Sprite sprite, SequenceAction sequence,
										Formula formula) {
		SetStopSoundsAction action = action(SetStopSoundsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(formula);
		return action;
	}

	public Action createSetSaveScenesAction(Sprite sprite, SequenceAction sequence,
											Formula formula) {
		SetSaveScenesAction action = action(SetSaveScenesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(formula);
		return action;
	}

	public Action createClearSceneAction(String sceneName, Sprite sprite) {
		ClearSceneAction action = action(ClearSceneAction.class);
		action.setScene(sceneName);
		action.setSprite(sprite);
		return action;
	}

	public Action createTestAction(Sprite sprite, SequenceAction sequence) {
		TestAction action = action(TestAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createStringToTableAction(Sprite sprite, SequenceAction sequence,
										Formula str, Formula x, Formula y, Formula name) {
		StringToTableAction action = action(StringToTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setStr(str);
		action.setXd(x);
		action.setYd(y);
		action.setName(name);
		return action;
	}

	public Action createSetDnsAction(Sprite sprite, SequenceAction sequence,
										Formula toastFormula) {
		SetDnsAction action = action(SetDnsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDns(toastFormula);
		return action;
	}

	public Action createMoveFilesAction(Sprite sprite, SequenceAction sequence,
										Formula toastFormula) {
		MoveFilesAction action = action(MoveFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(toastFormula);
		return action;
	}

	public Action createMoveDownloadsAction(Sprite sprite, SequenceAction sequence,
										Formula toastFormula) {
		MoveDownloadsAction action = action(MoveDownloadsAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(toastFormula);
		return action;
	}

	public Action createOrientationAction(Sprite sprite, SequenceAction sequence) {
		OrientationAction action = action(OrientationAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createDialogAction(Sprite sprite, SequenceAction sequence,
										Formula name, Formula title, Formula message) {
		CreateDIalogAction action = action(CreateDIalogAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setTitle(title);
		action.setMessage(message);
		return action;
	}

	public Action createPositiveAction(Sprite sprite, SequenceAction sequence,
									 Formula name, Formula text) {
		SetPositiveAction action = action(SetPositiveAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setText(text);
		return action;
	}

	public Action createNegativeAction(Sprite sprite, SequenceAction sequence,
									   Formula name, Formula text) {
		SetNegativeAction action = action(SetNegativeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setText(text);
		return action;
	}

	public Action createNeutralAction(Sprite sprite, SequenceAction sequence,
									   Formula name, Formula text) {
		SetNeutralButton action = action(SetNeutralButton.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setText(text);
		return action;
	}

	public Action createRunPythonScriptAction(Sprite sprite, SequenceAction sequence, Formula script, Formula variableName) {
		RunPythonScriptAction action = action(RunPythonScriptAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		//scope.setContext(sprite.getContext());
		action.setScope(scope);
		action.setScript(script);
		action.setVariableName(variableName); // Устанавливаем новое поле
		return action;
	}

	public Action createLoadPythonLibraryAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadPythonLibraryAction action = action(LoadPythonLibraryAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		//scope.setContext(sprite.getContext());
		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createLoadNativeModuleAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadNativeModuleAction action = action(LoadNativeModuleAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		//scope.setContext(sprite.getContext());
		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createClearPythonEnvironmentAction(Sprite sprite, SequenceAction sequence) {
		ClearPythonEnvironmentAction action = action(ClearPythonEnvironmentAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		//scope.setContext(sprite.getContext());
		action.setScope(scope);
		return action;
	}

	public Action createEditAction(Sprite sprite, SequenceAction sequence,
									  Formula name, Formula text) {
		AddEditAction action = action(AddEditAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setText(text);
		return action;
	}

	public Action createRadioAction(Sprite sprite, SequenceAction sequence,
								   Formula name, Formula text) {
		AddRadioAction action = action(AddRadioAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setText(text);
		return action;
	}

	public Action createShowDialogAction(Sprite sprite, SequenceAction sequence,
									Formula name) {
		ShowDialogAction action = action(ShowDialogAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action createSetCallbackAction(Sprite sprite, SequenceAction sequence,
									Formula name, UserVariable button) {
		SetCallbackAction action = action(SetCallbackAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setVariable1(button);
		return action;
	}

	public Action createLookToTableAction(Sprite sprite, SequenceAction sequence,
										Formula r, Formula g, Formula b, Formula a) {
		LookToTableAction action = action(LookToTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setR_table(r);
		action.setG_table(g);
		action.setB_table(b);
		action.setA_table(a);
		return action;
	}

	public Action createLookFromTableAction(Sprite sprite, SequenceAction sequence,
										  Formula r, Formula g, Formula b, Formula a) {
		LookFromTableAction action = action(LookFromTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setR_table(r);
		action.setG_table(g);
		action.setB_table(b);
		action.setA_table(a);
		return action;
	}

	public Action createOpenFileAction(Sprite sprite, SequenceAction sequence,
										Formula file) {
		OpenFileAction action = action(OpenFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		return action;
	}

	public Action createOpenFilesAction(Sprite sprite, SequenceAction sequence,
									   Formula file) {
		OpenFilesAction action = action(OpenFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		return action;
	}

	public Action createReadBaseAction(Sprite sprite, SequenceAction sequence,
										Formula base, Formula key, UserVariable variable) {
		ReadBaseAction action = action(ReadBaseAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBase(base);
		action.setKey(key);
		action.setVariable(variable);
		return action;
	}

	public Action createWriteBaseAction(Sprite sprite, SequenceAction sequence,
									   Formula base, Formula key, Formula value) {
		WriteBaseAction action = action(WriteBaseAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBase(base);
		action.setKey(key);
		action.setValue(value);
		return action;
	}

	public Action createDeleteBaseAction(Sprite sprite, SequenceAction sequence,
										Formula base, Formula key) {
		DeleteBaseAction action = action(DeleteBaseAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setBase(base);
		action.setKey(key);
		return action;
	}

	public Action createUnzipAction(Sprite sprite, SequenceAction sequence,
										Formula name) {
		UnzipAction action = action(UnzipAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action createZipAction(Sprite sprite, SequenceAction sequence,
									Formula name, Formula files) {
		ZipAction action = action(ZipAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setFiles(files);
		return action;
	}

	public Action createHideText3Action(Sprite sprite, SequenceAction sequence,
										Formula name,
										AndroidStringProvider androidStringProvider) {
		HideText3Action action = action(HideText3Action.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		if (scope == null) {
			Log.e("HideText", "Scope is null (Factory)");
		}
		action.setScope(scope);
		action.setName(name);
		action.setSprite(sprite);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createSplitAction(Sprite sprite, SequenceAction sequence,
										Formula string, Formula simbol, UserList userlist) {
		SplitAction action = action(SplitAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setString(string);
		action.setSimbol(simbol);
		action.setUserList(userlist);
		return action;
	}

	public Action createCreateTableAction(Sprite sprite, SequenceAction sequence,
										Formula name, Formula x, Formula y) {
		CreateTableAction action = action(CreateTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setX(x);
		action.setY(y);
		return action;
	}

	public Action createFloat(Sprite sprite, SequenceAction sequence,
										  Formula name) {
		CreateFloatAction action = action(CreateFloatAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action createTableToFloat(Sprite sprite, SequenceAction sequence,
							  Formula tname, Formula fname) {
		TableToFloatAction action = action(TableToFloatAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setT_name(tname);
		action.setF_name(fname);
		return action;
	}

	public Action createPutFloat(Sprite sprite, SequenceAction sequence,
									 Formula name, Formula value, Formula index) {
		PutFloatAction action = action(PutFloatAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setF_name(name);
		action.setValue(value);
		action.setIndex(index);
		return action;
	}

	public Action createDeleteFloat(Sprite sprite, SequenceAction sequence,
									 Formula name, Formula index) {
		DeleteFloatAction action = action(DeleteFloatAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setIndex(index);
		action.setF_name(name);
		return action;
	}

	public Action createInsertTableAction(Sprite sprite, SequenceAction sequence,
										  Formula name, Formula value, Formula x, Formula y) {
		InsertTableAction action = action(InsertTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setValue(value);
		action.setX(x);
		action.setY(y);
		return action;
	}

	public Action createDeleteTableAction(Sprite sprite, SequenceAction sequence,
										  Formula name) {
		DeleteTableAction action = action(DeleteTableAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action createDeleteAllTablesAction(Sprite sprite, SequenceAction sequence) {
		DeleteAllTablesAction action = action(DeleteAllTablesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSoundFileAction(Sprite sprite, SequenceAction sequence,
										Formula fileFormula) {
		SoundFileAction action = action(SoundFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(fileFormula);
		return action;
	}

	public Action createSoundFilesAction(Sprite sprite, SequenceAction sequence,
										Formula fileFormula) {
		SoundFilesAction action = action(SoundFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(fileFormula);
		return action;
	}

	public Action createLookFileAction(Sprite sprite, SequenceAction sequence,
										Formula fileFormula) {
		LookFileAction action = action(LookFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(fileFormula);
		return action;
	}

	public Action createSaveLookFilesAction(Sprite sprite, SequenceAction sequence,
									   Formula fileFormula) {
		SaveLookFilesAction action = action(SaveLookFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(fileFormula);
		return action;
	}

	public Action createSetLookFilesAction(Sprite sprite, SequenceAction sequence,
											Formula fileFormula) {
		SetLookFilesAction action = action(SetLookFilesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(fileFormula);
		return action;
	}

	public Action createSaveLookAction(Sprite sprite, SequenceAction sequence,
										Formula nameFormula) {
		SaveLookAction action = action(SaveLookAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(nameFormula);
		return action;
	}

	public Action createFileUrlAction(Sprite sprite, SequenceAction sequence,
										Formula toastFormula, Formula nameFormula) {
		FileUrlAction action = action(FileUrlAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setUrl(toastFormula);
		action.setName(nameFormula);
		return action;
	}

	public Action createFilesUrlAction(Sprite sprite, SequenceAction sequence,
									  Formula toastFormula, Formula nameFormula) {
		FilesUrlAction action = action(FilesUrlAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setUrl(toastFormula);
		action.setName(nameFormula);
		return action;
	}

	public Action evalWebAction(Sprite sprite, SequenceAction sequence,
									   Formula js, Formula name) {
		EvalWebAction action = action(EvalWebAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCode(js);
		action.setName(name);
		return action;
	}

	public Action setWebAction(Sprite sprite, SequenceAction sequence,
								Formula name, UserVariable variable) {
		SetWebAction action = action(SetWebAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setUserVariable(variable);
		action.setName(name);
		return action;
	}

	public Action createStartServerAction(Sprite sprite, SequenceAction sequence,
										Formula port) {
		StartServerAction action = action(StartServerAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPort(port);
		return action;
	}

	public Action createSendServerAction(Sprite sprite, SequenceAction sequence,
										  Formula value) {
		SendServerAction action = action(SendServerAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setValue(value);
		return action;
	}

	public Action createStopServerAction(Sprite sprite, SequenceAction sequence) {
		StopServerAction action = action(StopServerAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createListenServerAction(Sprite sprite, SequenceAction sequence,
										  UserVariable variable) {
		ListenServerAction action = action(ListenServerAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setVariable(variable);
		return action;
	}

	public Action createConnectServerAction(Sprite sprite, SequenceAction sequence,
										  Formula ip, Formula port) {
		ConnectServerAction action = action(ConnectServerAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setIp(ip);
		action.setPort(port);
		return action;
	}

	public static String generateRandomString(int length) {
		// Алфавит, из которого будут выбраны буквы
		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random random = new Random();
		StringBuilder randomString = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			// Генерируем случайный индекс в пределах длины алфавита
			int index = random.nextInt(alphabet.length());
			// Добавляем случайно выбранную букву к результату
			randomString.append(alphabet.charAt(index));
		}

		return randomString.toString();
	}

	public Action createScreenShotAction(Sprite sprite, SequenceAction sequence) {
		ScreenShotAction action = action(ScreenShotAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(generateRandomString(10));
		return action;
	}

	public Action createWebUrlAction(Sprite sprite, SequenceAction sequence,
										Formula name, Formula url, Formula x, Formula y, Formula width, Formula height) {
		CreateWebUrlAction action = action(CreateWebUrlAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setUrl(url);
		action.setPosX(x);
		action.setPosY(y);
		action.setWidth(width);
		action.setHeight(height);
		return action;
	}

	public Action createWebFileAction(Sprite sprite, SequenceAction sequence,
									 Formula name, Formula url, Formula x, Formula y, Formula width, Formula height) {
		CreateWebFileAction action = action(CreateWebFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setFile(url);
		action.setPosX(x);
		action.setPosY(y);
		action.setWidth(width);
		action.setHeight(height);
		return action;
	}

	public Action videoAction(Sprite sprite, SequenceAction sequence,
									  Formula name, Formula url, Formula x, Formula y, Formula width, Formula height,
									  Formula loop, Formula control) {
		CreateVideoAction action = action(CreateVideoAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		action.setFile(url);
		action.setPosX(x);
		action.setPosY(y);
		action.setWidth(width);
		action.setHeight(height);
		action.setLoop(loop);
		action.setControls(control);
		return action;
	}

	public Action createDeleteWebAction(Sprite sprite, SequenceAction sequence,
									  Formula name) {
		DeleteWebAction action = action(DeleteWebAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setName(name);
		return action;
	}

	public Action createSetGeminiKey(Sprite sprite, SequenceAction sequence, Formula keyFormula) {
		SetGeminiKeyAction action = action(SetGeminiKeyAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setKey(keyFormula);
		return action;
	}

	public Action createVarAction(Sprite sprite, SequenceAction sequence,
										Formula varname, Formula value) {
		CreateVarAction action = action(CreateVarAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setVarname(varname);
		action.setValue(value);
		return action;
	}

	public Action createDeleteVarAction(Sprite sprite, SequenceAction sequence,
								  Formula varname) {
		DeleteVarAction action = action(DeleteVarAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setVarname(varname);
		return action;
	}

	public Action createDeleteVarsAction(Sprite sprite, SequenceAction sequence) {
		DeleteVarAction action = action(DeleteVarAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createListenMicroAction(Sprite sprite, SequenceAction sequence,
			Formula time) {
		ListenMicroAction action = action(ListenMicroAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setTime(time);
		return action;
	}

	public Action createCopyTextAction(Sprite sprite, SequenceAction sequence,
			Formula toastFormula) {
		CopyTextAction action = action(CopyTextAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setText(toastFormula);
		return action;
	}

	public Action createInsertItemIntoUserListAction(Sprite sprite, SequenceAction sequence,
			Formula userListFormulaIndexToInsert,
			Formula userListFormulaItemToInsert, UserList userList) {
		InsertItemIntoUserListAction action = action(InsertItemIntoUserListAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormulaIndexToInsert(userListFormulaIndexToInsert);
		action.setFormulaItemToInsert(userListFormulaItemToInsert);

		action.setUserList(userList);
		return action;
	}

	public Action createStoreCSVIntoUserListAction(Sprite sprite, SequenceAction sequence,
			Formula userListFormulaColumn, Formula userListFormulaCSV, UserList userList) {
		StoreCSVIntoUserListAction action = action(StoreCSVIntoUserListAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormulaColumnToExtract(userListFormulaColumn);
		action.setFormulaCSVData(userListFormulaCSV);
		action.setUserList(userList);
		return action;
	}

	public Action createReplaceItemInUserListAction(Sprite sprite, SequenceAction sequence,
			Formula userListFormulaIndexToReplace,
			Formula userListFormulaItemToInsert, UserList userList) {
		ReplaceItemInUserListAction action = action(ReplaceItemInUserListAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormulaIndexToReplace(userListFormulaIndexToReplace);
		action.setFormulaItemToInsert(userListFormulaItemToInsert);
		action.setUserList(userList);
		return action;
	}

	public Action createResetTimerAction() {
		return Actions.action(ResetTimerAction.class);
	}

	public Action createThinkSayBubbleAction(Sprite sprite, SequenceAction sequence,
			AndroidStringProvider androidStringProvider, Formula text, int type) {
		ThinkSayBubbleAction action = action(ThinkSayBubbleAction.class);
		action.setText(text);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAndroidStringProvider(androidStringProvider);
		action.setType(type);
		return action;
	}

	public Action createThinkSayForBubbleAction(Sprite sprite, SequenceAction sequence,
			AndroidStringProvider androidStringProvider, Formula text, int type) {
		ThinkSayBubbleAction action = action(ThinkSayBubbleAction.class);
		action.setText(text);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAndroidStringProvider(androidStringProvider);
		action.setType(type);
		return action;
	}

	public Action createSceneTransitionAction(String sceneName, Sprite sprite) {
		SceneTransitionAction action = action(SceneTransitionAction.class);
		action.setScene(sceneName);
		action.setSprite(sprite);
		return action;
	}

	public Action createSceneStartAction(String sceneName, Sprite sprite) {
		SceneStartAction action = action(SceneStartAction.class);
		action.setScene(sceneName);
		action.setSprite(sprite);
		return action;
	}

	public Action createSceneIdAction(Integer sceneName, Sprite sprite) {
		SceneIdAction action = action(SceneIdAction.class);
		action.setScene(sceneName);
		action.setSprite(sprite);
		return action;
	}

	public Action createIfLogicAction(Sprite sprite, SequenceAction sequence, Formula condition,
			Action ifAction, Action elseAction) {
		IfLogicAction action = Actions.action(IfLogicAction.class);
		action.setIfAction(ifAction);
		action.setIfCondition(condition);
		action.setElseAction(elseAction);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createRepeatAction(Sprite sprite, SequenceAction sequence, Formula count, Action repeatedAction,
			boolean isLoopDelay) {
		RepeatAction action = Actions.action(RepeatAction.class);
		action.setRepeatCount(count);
		action.setAction(repeatedAction);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public Action createForVariableFromToAction(Sprite sprite,
			SequenceAction sequence, UserVariable controlVariable,
			Formula from, Formula to, Action repeatedAction, boolean isLoopDelay) {
		ForVariableFromToAction action = Actions.action(ForVariableFromToAction.class);
		action.setRange(from, to);
		action.setControlVariable(controlVariable);
		action.setAction(repeatedAction);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public Action createForItemInUserListAction(UserList userList,
			UserVariable userVariable, Action repeatedAction, boolean isLoopDelay) {
		ForItemInUserListAction action = Actions.action(ForItemInUserListAction.class);
		action.setAction(repeatedAction);
		action.setUserList(userList);
		action.setCurrentItemVariable(userVariable);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public Action createWaitUntilAction(Sprite sprite, SequenceAction sequence, Formula condition) {
		WaitUntilAction action = Actions.action(WaitUntilAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCondition(condition);
		return action;
	}

	public Action createRepeatUntilAction(Sprite sprite, SequenceAction sequence, Formula condition, Action repeatedAction,
			boolean isLoopDelay) {
		RepeatUntilAction action = action(RepeatUntilAction.class);
		action.setRepeatCondition(condition);
		action.setAction(repeatedAction);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public Action createDelayAction(Sprite sprite, SequenceAction sequence, Formula delay) {
		WaitAction action = Actions.action(WaitAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDelay(delay);
		return action;
	}

	public Action createForeverAction(Sprite sprite, SequenceAction sequence, ScriptSequenceAction foreverSequence,
			boolean isLoopDelay) {
		RepeatAction action = Actions.action(RepeatAction.class);
		action.setForeverRepeat(true);
		action.setAction(foreverSequence);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public static Action createStitchAction(Sprite sprite) {
		StitchAction action = Actions.action(StitchAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createRunningStitchAction(Sprite sprite, SequenceAction sequence, Formula length) {
		RunningStitchAction action = Actions.action(RunningStitchAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLength(length);
		return action;
	}

	public Action createTripleStitchAction(Sprite sprite, SequenceAction sequence, Formula steps) {
		TripleStitchAction action = Actions.action(TripleStitchAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setSteps(steps);
		return action;
	}

	public Action createZigZagStitchAction(Sprite sprite, SequenceAction sequence, Formula length,
			Formula width) {
		ZigZagStitchAction action = Actions.action(ZigZagStitchAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLength(length);
		action.setWidth(width);
		return action;
	}

	public static Action createStopRunningStitchAction(Sprite sprite) {
		StopRunningStitchAction action = Actions.action(StopRunningStitchAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createWriteEmbroideryToFileAction(Sprite sprite, SequenceAction sequence,
			Formula fileName) {
		WriteEmbroideryToFileAction action = Actions.action(WriteEmbroideryToFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(fileName);

		return action;
	}

	public Action createSavePlotAction(Sprite sprite, SequenceAction sequence, Formula fileName){
		SavePlotAction action = Actions.action(SavePlotAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(fileName);
		return action;
	}

	public Action createSewUpAction(Sprite sprite) {
		SewUpAction action = Actions.action(SewUpAction.class);
		action.setSprite(sprite);
		return action;
	}

	public Action createSetThreadColorAction(Sprite sprite, SequenceAction sequence, Formula color) {
		SetThreadColorAction action = Actions.action(SetThreadColorAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setSprite(sprite);
		action.setScope(scope);
		action.setColor(color);
		return action;
	}

	public static Action createScriptSequenceAction(Script script) {
		return new ScriptSequenceAction(script);
	}

	public Action createSetBounceFactorAction(Sprite sprite, SequenceAction sequence,
			Formula bounceFactor) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createTurnRightSpeedAction(Sprite sprite, SequenceAction sequence,
			Formula degreesPerSecond) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createTurnLeftSpeedAction(Sprite sprite, SequenceAction sequence,
			Formula degreesPerSecond) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetVelocityAction(Sprite sprite, SequenceAction sequence, Formula velocityX,
			Formula velocityY) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetPhysicsObjectTypeAction(Sprite sprite, PhysicsObject.Type type) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetMassAction(Sprite sprite, SequenceAction sequence, Formula mass) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetGravityAction(Sprite sprite, SequenceAction sequence, Formula gravityX,
			Formula gravityY) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetFrictionAction(Sprite sprite, SequenceAction sequence,
			Formula friction) {
		throw new RuntimeException("No physics action available in non-physics sprite!");
	}

	public Action createSetTextAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y, Formula text) {
		SetTextAction action = action(SetTextAction.class);

		action.setPosition(x, y);
		action.setText(text);
		action.setDuration(5);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSet3dFrictionAction(Sprite sprite, SequenceAction sequence,Formula id, Formula fric) {
		Set3dFrictionAction action = action(Set3dFrictionAction.class);

		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.friction = fric;
		action.objectId = id;
		return action;
	}

	public Action createCreateSphereAction(Sprite sprite, ScriptSequenceAction sequence, Formula objectId) {
		CreateSphereAction action = Actions.action(CreateSphereAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		return action;
	}

	public Action createSetRestitutionAction(Sprite sprite, ScriptSequenceAction sequence,
											 Formula objectId, Formula restitution) {
		SetRestitutionAction action = Actions.action(SetRestitutionAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.restitution = restitution;
		return action;
	}

	public Action createShowVariableAction(Sprite sprite, SequenceAction sequence, Formula xPosition,
			Formula yPosition, UserVariable userVariable, AndroidStringProvider androidStringProvider) {
		ShowTextAction action = action(ShowTextAction.class);
		action.setPosition(xPosition, yPosition);
		action.setVariableToShow(userVariable);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowVariableColorAndSizeAction(Sprite sprite, SequenceAction sequence,
			Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color,
			UserVariable userVariable, int alignment, AndroidStringProvider androidStringProvider) {
		ShowTextColorSizeAlignmentAction action = action(ShowTextColorSizeAlignmentAction.class);
		action.setPosition(xPosition, yPosition);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setVariableToShow(userVariable);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowVarFontAction(Sprite sprite, SequenceAction sequence,
													   Formula file,
													   Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color,
													   UserVariable userVariable, int alignment, AndroidStringProvider androidStringProvider) {
		ShowVarFontAction action = action(ShowVarFontAction.class);
		action.setPosition(xPosition, yPosition);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setVariableToShow(userVariable);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFile(file);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowtext3Action(Sprite sprite, SequenceAction sequence,
													   Formula name, Formula text, Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color, int alignment, AndroidStringProvider androidStringProvider) {
		ShowText3Action action = action(ShowText3Action.class);
		action.setPosition(xPosition, yPosition);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setName(name);
		action.setText(text);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowTextFontAction(Sprite sprite, SequenceAction sequence,
										Formula name, Formula text, Formula font, Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color, int alignment, AndroidStringProvider androidStringProvider) {
		ShowTextFontAction action = action(ShowTextFontAction.class);
		action.setPosition(xPosition, yPosition);
		action.setFile(font);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setName(name);
		action.setText(text);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowTextRotationAction(Sprite sprite, SequenceAction sequence,
										   Formula name, Formula text, Formula rotation, Formula font, Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color, int alignment, AndroidStringProvider androidStringProvider) {
		ShowTextRotationAction action = action(ShowTextRotationAction.class);
		action.setPosition(xPosition, yPosition);
		action.setFile(font);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setName(name);
		action.setText(text);
		action.setRotation(rotation);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createShowTextVar(Sprite sprite, SequenceAction sequence,
													   Formula xPosition, Formula yPosition, Formula relativeTextSize, Formula color,
													   Formula name, Formula text, int alignment, AndroidStringProvider androidStringProvider) {
		ShowText2Action action = action(ShowText2Action.class);
		action.setPosition(xPosition, yPosition);
		action.setRelativeTextSize(relativeTextSize);
		action.setColor(color);
		action.setName(name);
		action.setText(text);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setAlignment(alignment);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createHideVariableAction(Sprite sprite, UserVariable userVariable,
			AndroidStringProvider androidStringProvider) {
		HideTextAction action = action(HideTextAction.class);
		action.setVariableToHide(userVariable);
		action.setSprite(sprite);
		action.setAndroidStringProvider(androidStringProvider);
		return action;
	}

	public Action createFlashAction(boolean flashOn) {
		FlashAction action = action(FlashAction.class);
		action.setFlashOn(flashOn);
		return action;
	}

	public Action createVibrateAction(Sprite sprite, SequenceAction sequence, Formula duration) {
		VibrateAction action = action(VibrateAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDuration(duration);
		return action;
	}

	public Action createUpdateCameraPreviewAction(boolean turnOn) {
		CameraBrickAction action = action(CameraBrickAction.class);
		action.setActive(turnOn);
		return action;
	}

	public Action nativeViewLayer(int layer) {
		NativeLayerAction action = action(NativeLayerAction.class);
		action.setLayer(layer);
		return action;
	}

	public Action createFadeParticleEffectsAction(Sprite sprite, boolean turnOn) {
		FadeParticleEffectAction action = action(FadeParticleEffectAction.class);
		action.setFadeIn(turnOn);
		action.setSprite(sprite);
		action.setBackgroundSprite(ProjectManager.getInstance().getCurrentlyPlayingScene().getBackgroundSprite());
		return action;
	}

	public Action createAdditiveParticleEffectsAction(Sprite sprite, boolean turnOn) {
		AdditiveParticleEffectAction action = action(AdditiveParticleEffectAction.class);
		action.setFadeIn(turnOn);
		action.setSprite(sprite);
		return action;
	}

	public Action createSetParticleColorAction(Sprite sprite, Formula color, SequenceAction sequence) {
		SetParticleColorAction action = action(SetParticleColorAction.class);
		action.setColor(color);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSetFrontCameraAction() {
		ChooseCameraAction action = action(ChooseCameraAction.class);
		action.setFrontCamera();
		return action;
	}

	public Action createChooseFileAction(Sprite sprite, SequenceAction sequence,
										 Integer type, UserVariable variable) {
		ChooseFileAction action = action(ChooseFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileType(type);
		action.setVariable(variable);

		return action;
	}

	public Action createSetBackCameraAction() {
		ChooseCameraAction action = action(ChooseCameraAction.class);
		action.setBackCamera();
		return action;
	}

	public Action createSendDigitalArduinoValueAction(Sprite sprite, SequenceAction sequence,
			Formula pinNumber, Formula pinValue) {
		ArduinoSendDigitalValueAction action = action(ArduinoSendDigitalValueAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPinNumber(pinNumber);
		action.setPinValue(pinValue);
		return action;
	}

	public Action createSendPWMArduinoValueAction(Sprite sprite, SequenceAction sequence,
			Formula pinNumber, Formula pinValue) {
		ArduinoSendPWMValueAction action = action(ArduinoSendPWMValueAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPinNumber(pinNumber);
		action.setPinValue(pinValue);
		return action;
	}

	public Action createSendDigitalRaspiValueAction(Sprite sprite, SequenceAction sequence,
			Formula pinNumber, Formula pinValue) {
		RaspiSendDigitalValueAction action = action(RaspiSendDigitalValueAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPinNumber(pinNumber);
		action.setPinValue(pinValue);
		return action;
	}

	public Action createSendRaspiPwmValueAction(Sprite sprite, SequenceAction sequence,
			Formula pinNumber, Formula pwmFrequency, Formula pwmPercentage) {
		RaspiPwmAction action = action(RaspiPwmAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPinNumberFormula(pinNumber);
		action.setPwmFrequencyFormula(pwmFrequency);
		action.setPwmPercentageFormula(pwmPercentage);
		return action;
	}

	public Action createRaspiIfLogicActionAction(Sprite sprite, SequenceAction sequence,
			Formula pinNumber, Action ifAction, Action elseAction) {
		RaspiIfLogicAction action = action(RaspiIfLogicAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPinNumber(pinNumber);
		action.setIfAction(ifAction);
		action.setElseAction(elseAction);
		return action;
	}

	public Action createStopScriptAction(int spinnerSelection, Script currentScript, Sprite sprite) {
		switch (spinnerSelection) {
			case BrickValues.STOP_THIS_SCRIPT:
				StopThisScriptAction stopThisScriptAction = Actions.action(StopThisScriptAction.class);
				stopThisScriptAction.setCurrentScript(currentScript);
				return stopThisScriptAction;
			case BrickValues.STOP_OTHER_SCRIPTS:
				StopOtherScriptsAction stopOtherScriptsAction = Actions.action(StopOtherScriptsAction.class);
				stopOtherScriptsAction.setCurrentScript(currentScript);
				stopOtherScriptsAction.setSprite(sprite);
				return stopOtherScriptsAction;
			default:
				return Actions.action(StopAllScriptsAction.class);
		}
	}

	public Action createReportAction(Sprite sprite, SequenceAction sequence, Script currentScript, Formula reportFormula) {
		if (currentScript instanceof UserDefinedScript) {
			ReportAction reportAction = Actions.action(ReportAction.class);
			Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
			reportAction.setScope(scope);
			reportAction.setCurrentScript(currentScript);
			reportAction.setReportFormula(reportFormula);
			return reportAction;
		} else {
			StopThisScriptAction stopThisScriptAction = Actions.action(StopThisScriptAction.class);
			stopThisScriptAction.setCurrentScript(currentScript);
			return stopThisScriptAction;
		}
	}

	public Action createSetNfcTagAction(Sprite sprite, SequenceAction sequence, Formula nfcNdefMessage, int nfcNdefSpinnerSelection) {
		SetNfcTagAction setNfcTagAction = Actions.action(SetNfcTagAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		setNfcTagAction.setScope(scope);
		setNfcTagAction.setNfcTagNdefSpinnerSelection(nfcNdefSpinnerSelection);
		setNfcTagAction.setNfcNdefMessage(nfcNdefMessage);
		return setNfcTagAction;
	}

	public Action createAssertEqualsAction(Sprite sprite, SequenceAction sequence, Formula actual,
			Formula expected,
			String position) {
		AssertEqualsAction action = action(AssertEqualsAction.class);
		action.setActualFormula(actual);
		action.setExpectedFormula(expected);

		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPosition(position);

		return action;
	}

	public Action createAssertUserListsAction(Sprite sprite, SequenceAction sequence, UserList actual, UserList expected,
			String position) {
		AssertUserListAction action = action(AssertUserListAction.class);
		action.setActualUserList(actual);
		action.setExpectedUserList(expected);

		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPosition(position);

		return action;
	}

	public Action createRepeatParameterizedAction(Sprite sprite, ParameterizedData data,
			List<? extends Pair<UserList, UserVariable>> parameters,
			String position, Action repeatedAction, boolean isLoopDelay) {
		RepeatParameterizedAction action = action(RepeatParameterizedAction.class);
		action.setParameterizedData(data);
		action.setParameters(parameters);
		action.setAction(repeatedAction);
		action.setLoopDelay(isLoopDelay);

		action.setSprite(sprite);
		action.setPosition(position);

		return action;
	}

	public Action createParameterizedAssertAction(Sprite sprite, SequenceAction sequence, Formula actual, UserList expected,
			ParameterizedData data, String position) {
		ParameterizedAssertAction action = action(ParameterizedAssertAction.class);
		action.setActualFormula(actual);
		action.setExpectedList(expected);
		action.setParameterizedData(data);

		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPosition(position);

		return action;
	}

	public Action createFinishStageAction(boolean silent) {
		FinishStageAction action = action(FinishStageAction.class);
		action.setSilent(silent);
		return action;
	}

	public Action createTapAtAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y) {
		TapAtAction action = Actions.action(TapAtAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setStartX(x);
		action.setStartY(y);
		return action;
	}

	public Action createTapForAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y,
			Formula duration) {
		TapAtAction action = Actions.action(TapAtAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDurationFormula(duration);
		action.setStartX(x);
		action.setStartY(y);
		return action;
	}

	public Action createTouchAndSlideAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y,
			Formula xChange, Formula yChange, Formula duration) {
		TapAtAction action = Actions.action(TapAtAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDurationFormula(duration);
		action.setStartX(x);
		action.setStartY(y);
		action.setChangeX(xChange);
		action.setChangeY(yChange);
		return action;
	}

	public Action createWriteVariableOnDeviceAction(UserVariable userVariable) {
		WriteUserDataOnDeviceAction action = Actions.action(WriteUserDataOnDeviceAction.class);
		File projectDirectory = ProjectManager.getInstance().getCurrentProject().getDirectory();
		DeviceVariableAccessor accessor = new DeviceVariableAccessor(projectDirectory);
		action.setUserData(userVariable);
		action.setAccessor(accessor);

		return action;
	}

	public Action createWriteVariableToFileAction(Sprite sprite, SequenceAction sequence,
			Formula variableFormula, UserVariable userVariable) {
		WriteVarToFileAction action = action(WriteVarToFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setUserVariable(userVariable);
		action.setFormula(variableFormula);

		return action;
	}

	public Action createReadVariableFromFileAction(Sprite sprite, SequenceAction sequence, Formula variableFormula,
			UserVariable userVariable, boolean deleteFile) {
		ReadVariableFromFileAction action = Actions.action(ReadVariableFromFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setUserVariable(userVariable);
		action.setFormula(variableFormula);
		action.setDeleteFile(deleteFile);

		return action;
	}

	public Action createWriteListOnDeviceAction(UserList userList) {
		WriteUserDataOnDeviceAction action = Actions.action(WriteUserDataOnDeviceAction.class);
		File projectDirectory = ProjectManager.getInstance().getCurrentProject().getDirectory();
		DeviceUserDataAccessor accessor = new DeviceListAccessor(projectDirectory);
		UserData data = userList;
		action.setUserData(data);
		action.setAccessor(accessor);

		return action;
	}

	public Action createWaitTillIdleAction() {
		return action(WaitTillIdleAction.class);
	}

	public Action createReadVariableFromDeviceAction(UserVariable userVariable) {
		ReadVariableFromDeviceAction action = Actions.action(ReadVariableFromDeviceAction.class);
		action.setUserVariable(userVariable);

		return action;
	}

	public Action createReadListFromDeviceAction(UserList userList) {
		ReadListFromDeviceAction action = Actions.action(ReadListFromDeviceAction.class);
		action.setUserList(userList);

		return action;
	}

	public Action createWebRequestAction(Sprite sprite, SequenceAction sequence, Formula variableFormula,
			UserVariable userVariable) {
		WebRequestAction action = action(WebRequestAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(variableFormula);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createAskGPTAction(Sprite sprite, SequenceAction sequence, Formula prompt,
									 Formula system, UserVariable userVariable) {
		AskGPTAction action = action(AskGPTAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setPformula(prompt);
		action.setSformula(system);
		action.setUserVariable(userVariable);
		return action;
	}

	public Action createLookRequestAction(Sprite sprite, SequenceAction sequence,
			Formula variableFormula) {
		LookRequestAction action = action(LookRequestAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(variableFormula);
		return action;
	}

	public Action createOpenUrlAction(Sprite sprite, SequenceAction sequence, Formula variableFormula) {
		OpenUrlAction action = action(OpenUrlAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(variableFormula);
		return action;
	}

	public Action createStartListeningAction(UserVariable userVariable) {
		// This is a fix to get the StartListeningBrick to work on Huawei Phones,
		// can be changed once HMS is fully implemented and working
		// As soon as this is the case, remove the if-statement and only use the else-branch
		if (get(MobileServiceAvailability.class).isHmsAvailable(ProjectManager.getInstance().getApplicationContext())) {
			AskSpeechAction action = Actions.action(AskSpeechAction.class);
			action.setAnswerVariable(userVariable);
			return action;
		} else {
			StartListeningAction action = Actions.action(StartListeningAction.class);
			action.setUserVariable(userVariable);
			return action;
		}
	}

	public Action createSetListeningLanguageAction(String listeningLanguageTag) {
		SetListeningLanguageAction action = action(SetListeningLanguageAction.class);
		action.listeningLanguageTag = listeningLanguageTag;
		return action;
	}

	public Action createPaintNewLookAction(Sprite sprite, SequenceAction sequence,
			Formula variableFormula, SetNextLookAction nextLookAction) {
		PaintNewLookAction action = action(PaintNewLookAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(variableFormula);
		action.nextLookAction(nextLookAction);
		return action;
	}

	public Action createCopyLookAction(Sprite sprite, SequenceAction sequence, Formula variableFormula,
			SetNextLookAction nextLookAction) {
		CopyLookAction action = action(CopyLookAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFormula(variableFormula);
		action.nextLookAction(nextLookAction);
		return action;
	}

	public Action createEditLookAction(Sprite sprite, SequenceAction sequence,
			SetNextLookAction nextLookAction) {
		EditLookAction action = action(EditLookAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.nextLookAction(nextLookAction);
		return action;
	}

    public Action createExportProjectFileAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField) {
		ExportProjectFileAction action = action(ExportProjectFileAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setProjectFileName(formulaWithBrickField);
		return action;
    }

	public Action createGLViewAction(Sprite sprite, SequenceAction sequence, Formula name, Formula x, Formula y, Formula width, Formula height) {
		CreateGLViewAction action = action(CreateGLViewAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setViewName(name);
		action.setX(x);
		action.setY(y);
		action.setWidth(width);
		action.setHeight(height);
		return action;
	}

	public Action createAttachSOAction(Sprite sprite, SequenceAction sequence, Formula name, Formula file) {
		AttachSOAction action = action(AttachSOAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setViewName(name);
		action.setSoFileName(file);
		return action;
	}

	public Action createDiskAction(Sprite sprite, SequenceAction sequence, Formula name, Formula size) {
		CreateDiskAction action = action(CreateDiskAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDiskName(name);
		action.setDiskSize(size);
		return action;
	}

	public Action createRunVMAction(Sprite sprite, SequenceAction sequence, Formula memory, Formula cpu, Formula hda, Formula cdrom) {
		RunVMAction action = action(RunVMAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setMemory(memory);
		action.setCpuCores(cpu);
		action.setHdaPath(hda);
		action.setCdromPath(cdrom);
		return action;
	}

	public Action createStopVMAction(Sprite sprite, SequenceAction sequence) {
		StopVMAction action = action(StopVMAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createToggleDisplayAction(Sprite sprite, SequenceAction sequence, Formula visible) {
		ToggleDisplayAction action = action(ToggleDisplayAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setVisible(visible);
		return action;
	}

	public Action createMouseEventAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y, Formula state) {
		MouseEventAction action = action(MouseEventAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setX(x);
		action.setY(y);
		action.setButtonState(state);
		return action;
	}

	public Action createKeyEventAction(Sprite sprite, SequenceAction sequence, Formula character, Formula isDown) {
		KeyEventAction action = action(KeyEventAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCharacter(character);
		action.setDown(isDown);
		return action;
	}
}
