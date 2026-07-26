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
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.BrickValues;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ParameterizedData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.actions.*;
import org.catrobat.catroid.content.actions.conditional.GlideToAction;
import org.catrobat.catroid.content.actions.conditional.IfOnEdgeBounceAction;
import org.catrobat.catroid.content.bricks.Brick;
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
import org.catrobat.catroid.formulaeditor.InterpretationException;
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

	public Action createRevoluteJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula spriteB, Formula anchorX, Formula anchorY) {
		CreateRevoluteJointAction action = action(CreateRevoluteJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setSpriteBName(spriteB);
		action.setAnchorX(anchorX);
		action.setAnchorY(anchorY);
		return action;
	}

	public Action createDestroyJointAction(Sprite sprite, SequenceAction sequence, Formula jointId) {
		DestroyJointAction action = action(DestroyJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		return action;
	}

	public Action createDistanceJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula spriteB, Formula length, Formula frequency, Formula damping) {
		CreateDistanceJointAction action = action(CreateDistanceJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setSpriteBName(spriteB);
		action.setLength(length);
		action.setFrequency(frequency);
		action.setDamping(damping);
		return action;
	}

	public Action createWeldJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula spriteB, Formula anchorX, Formula anchorY) {
		CreateWeldJointAction action = action(CreateWeldJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setSpriteBName(spriteB);
		action.setAnchorX(anchorX);
		action.setAnchorY(anchorY);
		return action;
	}

	public Action createPulleyJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula spriteA, Formula spriteB, Formula gndAnchorAx, Formula gndAnchorAy, Formula gndAnchorBx, Formula gndAnchorBy, Formula ratio) {
		CreatePulleyJointAction action = action(CreatePulleyJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setSpriteAName(spriteA);
		action.setSpriteBName(spriteB);
		action.setGroundAnchorAx(gndAnchorAx);
		action.setGroundAnchorAy(gndAnchorAy);
		action.setGroundAnchorBx(gndAnchorBx);
		action.setGroundAnchorBy(gndAnchorBy);
		action.setRatio(ratio);
		return action;
	}

	public Action createGearJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula jointAId, Formula jointBId, Formula ratio) {
		CreateGearJointAction action = action(CreateGearJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setJointAId(jointAId);
		action.setJointBId(jointBId);
		action.setRatio(ratio);
		return action;
	}

	public Action createSetThirdPersonCameraAction(Sprite sprite, SequenceAction sequence,
												   Formula objectId, Formula distance, Formula height, Formula pitch) {
		SetThirdPersonCameraAction action = action(SetThirdPersonCameraAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setDistance(distance);
		action.setHeight(height);
		action.setPitch(pitch);
		return action;
	}

	public Action createSetFreeCameraAction(Sprite sprite, SequenceAction sequence) {
		SetFreeCameraAction action = action(SetFreeCameraAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createSaveToInternalStorageAction(Sprite sprite, SequenceAction sequence,
													Formula projectFileName, Formula internalPath) {
		SaveToInternalStorageAction action = action(SaveToInternalStorageAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setProjectFileName(projectFileName);
		action.setInternalStoragePath(internalPath);
		return action;
	}

	public Action createLoadFromInternalStorageAction(Sprite sprite, SequenceAction sequence, Formula internalPath) {
		LoadFromInternalStorageAction action = action(LoadFromInternalStorageAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setInternalStoragePath(internalPath);
		return action;
	}


	public Action createPrismaticJointAction(Sprite sprite, SequenceAction sequence, Formula jointId, Formula spriteB, Formula anchorX, Formula anchorY, Formula axisX, Formula axisY) {
		CreatePrismaticJointAction action = action(CreatePrismaticJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointId(jointId);
		action.setSpriteBName(spriteB);
		action.setAnchorX(anchorX);
		action.setAnchorY(anchorY);
		action.setAxisX(axisX);
		action.setAxisY(axisY);
		return action;
	}

	public Action createApplyForceAction(Sprite sprite, SequenceAction sequence, Formula forceX, Formula forceY) {
		ApplyForceAction action = action(ApplyForceAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setForceX(forceX);
		action.setForceY(forceY);
		return action;
	}

    public Action createReplace3DModelAction(Sprite sprite, SequenceAction sequence,
                                             Formula objectId, Formula modelPath) {
        Replace3DModelAction action = action(Replace3DModelAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setModelPath(modelPath);
        return action;
    }

	public Action createApplyImpulseAction(Sprite sprite, SequenceAction sequence, Formula impulseX, Formula impulseY) {
		ApplyImpulseAction action = action(ApplyImpulseAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setImpulseX(impulseX);
		action.setImpulseY(impulseY);
		return action;
	}

	public Action createApplyTorqueAction(Sprite sprite, SequenceAction sequence, Formula torque) {
		ApplyTorqueAction action = action(ApplyTorqueAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setTorque(torque);
		return action;
	}

	public Action createApplyAngularImpulseAction(Sprite sprite, SequenceAction sequence, Formula impulse) {
		ApplyAngularImpulseAction action = action(ApplyAngularImpulseAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setImpulse(impulse);
		return action;
	}

	public Action createSetDampingAction(Sprite sprite, SequenceAction sequence, Formula linear, Formula angular) {
		SetDampingAction action = action(SetDampingAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setLinearDamping(linear);
		action.setAngularDamping(angular);
		return action;
	}

	public Action createPerformRayCastaction(Sprite sprite, SequenceAction sequence, Formula rayId, Formula startX, Formula startY, Formula endX, Formula endY) {
		PerformRayCastAction action = action(PerformRayCastAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setRayId(rayId);
		action.setStartX(startX);
		action.setStartY(startY);
		action.setEndX(endX);
		action.setEndY(endY);
		return action;
	}

	public Action createLaunchProjectAction(Sprite sprite, SequenceAction sequence, Formula projectName) {
		LaunchProjectAction action = action(LaunchProjectAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setProjectNameFormula(projectName);
		return action;
	}

	public Action createReturnToPreviousProjectAction() {
		return action(ReturnToPreviousProjectAction.class);
	}

	public Action createTryCatchFinallyAction(
			ScriptSequenceAction trySequence,
			ScriptSequenceAction catchSequence,
			ScriptSequenceAction finallySequence,
			UserVariable errorVariable
	) {
		TryCatchFinallyAction action = action(TryCatchFinallyAction.class);
		action.setTrySequence(trySequence);
		action.setCatchSequence(catchSequence);
		action.setFinallySequence(finallySequence);
		action.setErrorVariable(errorVariable);
		return action;
	}

	public Action createStartRecordingAction() {
		return action(StartRecordingAction.class);
	}

	public Action createStopRecordingAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		StopRecordingAction action = action(StopRecordingAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileNameFormula(fileName);
		return action;
	}

	public Action createSetCameraPositionAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y) {
		SetCameraPosition2Action action = action(SetCameraPosition2Action.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setXFormula(x);
		action.setYFormula(y);
		return action;
	}

	public Action createSetCameraZoomAction(Sprite sprite, SequenceAction sequence, Formula zoom) {
		SetCameraZoomAction action = action(SetCameraZoomAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setZoomFormula(zoom);
		return action;
	}

	public Action createSetCameraRotationAction(Sprite sprite, SequenceAction sequence, Formula rotation) {
		SetCameraRotation2Action action = action(SetCameraRotation2Action.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRotationFormula(rotation);
		return action;
	}

	public Action createPinToCameraAction(Sprite sprite, SequenceAction sequence) {
		PinToCameraAction action = action(PinToCameraAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		return action;
	}

	public Action createUnpinFromCameraAction(Sprite sprite, SequenceAction sequence) {
		UnpinFromCameraAction action = action(UnpinFromCameraAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
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

	public Action createSetSpotLightAction(Sprite sprite, ScriptSequenceAction sequence,
										   Formula lightId, Formula posX, Formula posY, Formula posZ,
										   Formula dirX, Formula dirY, Formula dirZ,
										   Formula colorR, Formula colorG, Formula colorB,
										   Formula intensity, Formula angle, Formula exponent, Formula range) {
		SetSpotLightAction action = Actions.action(SetSpotLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLightId(lightId);
		action.setPosX(posX);
		action.setPosY(posY);
		action.setPosZ(posZ);
		action.setDirX(dirX);
		action.setDirY(dirY);
		action.setDirZ(dirZ);
		action.setColorR(colorR);
		action.setColorG(colorG);
		action.setColorB(colorB);
		action.setIntensity(intensity);
		action.setAngle(angle);
		action.setExponent(exponent);
		action.setRange(range);
		return action;
	}

	public Action createRemovePbrLightAction(Sprite sprite, ScriptSequenceAction sequence, Formula lightId) {
		RemovePbrLightAction action = Actions.action(RemovePbrLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLightId(lightId);
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

	public Action createSetBackgroundLightAction(Sprite sprite, ScriptSequenceAction sequence, Formula intensity) {
		SetBackgroundLightAction action = Actions.action(SetBackgroundLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setIntensity(intensity);
		return action;
	}

	public Action createSetCameraRangeAction(Sprite sprite, ScriptSequenceAction sequence, Formula near, Formula far) {
		SetCameraRangeAction action = Actions.action(SetCameraRangeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setNear(near);
		action.setFar(far);
		return action;
	}

	public Action createSetShaderUniformAction(Sprite sprite, SequenceAction sequence, Formula name, Formula x, Formula y, Formula z) {
		SetShaderUniformAction action = Actions.action(SetShaderUniformAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setUniformName(name);
		action.setValueX(x);
		action.setValueY(y);
		action.setValueZ(z);
		return action;
	}

	public Action createSetSkyboxAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		SetSkyboxAction action = action(SetSkyboxAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createSetShaderCodeAction(Sprite sprite, SequenceAction sequence, Formula vertexCode, Formula fragmentCode) {
		SetShaderCodeAction action = Actions.action(SetShaderCodeAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setVertexCode(vertexCode);
		action.setFragmentCode(fragmentCode);
		return action;
	}


	public Action createSetCCDAction(Sprite sprite, SequenceAction sequence, Formula objectId, Formula enabled) {
		SetCCDAction action = Actions.action(SetCCDAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setEnabled(enabled);
		return action;
	}

	public Action createSetPhysicsStateAction(Sprite sprite, ScriptSequenceAction sequence,
											  Formula objectId, int stateSelection, int shapeSelection, Formula mass) {
		SetPhysicsStateAction action = Actions.action(SetPhysicsStateAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.scope = scope;
		action.objectId = objectId;
		action.stateSelection = stateSelection;
		action.shapeSelection = shapeSelection;
		action.mass = mass;
		return action;
	}

	public Action createEnablePbrRenderAction(Sprite sprite, ScriptSequenceAction sequence, int renderState) {
		EnablePbrRenderAction action = Actions.action(EnablePbrRenderAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRenderState(renderState);
		return action;
	}

	public Action createPlayAnimationAction(Sprite sprite, ScriptSequenceAction sequence,
											Formula objectId, Formula animationName, Formula loops,
											Formula speed, Formula transitionTime) {
		PlayAnimationAction action = Actions.action(PlayAnimationAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setAnimationName(animationName);
		action.setLoops(loops);
		action.setSpeed(speed);
		action.setTransitionTime(transitionTime);
		return action;
	}

	public Action createStopAnimationAction(Sprite sprite, ScriptSequenceAction sequence, Formula objectId) {
		StopAnimationAction action = Actions.action(StopAnimationAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		return action;
	}

	public Action createSetAnisotropicFilterAction(Sprite sprite, ScriptSequenceAction sequence,
												   Formula objectId, Formula level) {
		SetAnisotropicFilterAction action = Actions.action(SetAnisotropicFilterAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setObjectId(objectId);
		action.setLevel(level);
		return action;
	}

	public Action createSetPointLightAction(Sprite sprite, ScriptSequenceAction sequence,
											Formula lightId, Formula posX, Formula posY, Formula posZ,
											Formula colorR, Formula colorG, Formula colorB,
											Formula intensity, Formula range) {
		SetPointLightAction action = Actions.action(SetPointLightAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLightId(lightId);
		action.setPosX(posX);
		action.setPosY(posY);
		action.setPosZ(posZ);
		action.setColorR(colorR);
		action.setColorG(colorG);
		action.setColorB(colorB);
		action.setIntensity(intensity);
		action.setRange(range);
		return action;
	}

	public Action createSetDirectionalLight2Action(Sprite sprite, ScriptSequenceAction sequence,
												  Formula dirX, Formula dirY, Formula dirZ, Formula intensity) {
		SetDirectionalLigh2tAction action = Actions.action(SetDirectionalLigh2tAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDirX(dirX);
		action.setDirY(dirY);
		action.setDirZ(dirZ);
		action.setIntensity(intensity);
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

    public Action createObjectLookAtCorrectAction(Sprite sprite, ScriptSequenceAction sequence,
                                                  Formula objectId, Formula x, Formula y, Formula z) {
        ObjectLookAtCorrectAction action = Actions.action(ObjectLookAtCorrectAction.class);
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

	public Action createCloneAndNameAction(Sprite spriteToClone, Scope scope, Formula cloneName) {
		CloneAndNameAction action = action(CloneAndNameAction.class);
		action.setScope(scope);
		action.setSprite(spriteToClone);
		action.setCloneName(cloneName);
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

	public Action createBindVmOutputAction(UserVariable variable) {
		BindVmOutputAction action = action(BindVmOutputAction.class);
		action.setUserVariable(variable);
		return action;
	}

	public Action createSendVmInputAction(Sprite sprite, SequenceAction sequence, Formula inputText) {
		SendVmInputAction action = action(SendVmInputAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setInputText(inputText);
		return action;
	}

	public Action createRunVm2Action(Sprite sprite, SequenceAction sequence, Formula arguments) {
		RunVm2Action action = action(RunVm2Action.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setArguments(arguments);
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

		action.setScope(scope);
		action.setScript(script);
		action.setVariableName(variableName);
		return action;
	}

	public Action createLoadPythonLibraryAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadPythonLibraryAction action = action(LoadPythonLibraryAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createLoadNativeModuleAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadNativeModuleAction action = action(LoadNativeModuleAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createClearPythonEnvironmentAction(Sprite sprite, SequenceAction sequence) {
		ClearPythonEnvironmentAction action = action(ClearPythonEnvironmentAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

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

		String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random random = new Random();
		StringBuilder randomString = new StringBuilder(length);

		for (int i = 0; i < length; i++) {

			int index = random.nextInt(alphabet.length());

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

	public Action createAsyncRepeatAction(Sprite sprite, SequenceAction sequence, Formula count, Action repeatedAction,
									 boolean isLoopDelay) {
		AsyncRepeatAction action = Actions.action(AsyncRepeatAction.class);
		action.setRepeatCount(count);
		action.setAction(repeatedAction);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setLoopDelay(isLoopDelay);
		return action;
	}

	public Action createIntervalRepeatAction(Sprite sprite, SequenceAction sequence, Formula count, Formula interval, Action loopBodyAction) {
		IntervalRepeatAction action = action(IntervalRepeatAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setRepeatCount(count);
		action.setInterval(interval);
		action.setLoopBodyAction(loopBodyAction);
		return action;
	}

	public Action createPrepareSoundAction(Sprite sprite, SequenceAction sequence, Formula fileName, Formula cacheName) {
		PrepareSoundAction action = action(PrepareSoundAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileNameFormula(fileName);
		action.setCacheNameFormula(cacheName);
		return action;
	}

	public Action createSetViewPositionAction(Sprite sprite, SequenceAction sequence, Formula viewId, Formula x, Formula y) {
		SetViewPositionAction action = action(SetViewPositionAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setViewIdFormula(viewId);
		action.setXFormula(x);
		action.setYFormula(y);
		return action;
	}

	public Action createPlayPreparedSoundAction(Sprite sprite, SequenceAction sequence, Formula cacheName) {
		PlayPreparedSoundAction action = action(PlayPreparedSoundAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setCacheNameFormula(cacheName);
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

	public Action createLoadSceneAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadSceneAction action = action(LoadSceneAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createLockMouseAction() {
        return action(LockMouseAction.class);
	}

	public Action createUnlockMouseAction() {
		return action(UnlockMouseAction.class);
	}

	public Action createSetParentAction(Sprite sprite, SequenceAction sequence, Formula child, Formula parent) {
		SetParentAction action = action(SetParentAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setChildObject(child);
		action.setParentObject(parent);
		return action;
	}

	public Action createCreatePointJointAction(Sprite sprite, SequenceAction sequence,
											   Formula constraintName, Formula objectA, Formula objectB,
											   Formula pivotAX, Formula pivotAY, Formula pivotAZ,
											   Formula pivotBX, Formula pivotBY, Formula pivotBZ) {
		CreatePointJointAction action = action(CreatePointJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setConstraintName(constraintName);
		action.setObjectA(objectA);
		action.setObjectB(objectB);
		action.setPivotAX(pivotAX);
		action.setPivotAY(pivotAY);
		action.setPivotAZ(pivotAZ);
		action.setPivotBX(pivotBX);
		action.setPivotBY(pivotBY);
		action.setPivotBZ(pivotBZ);
		return action;
	}

	public Action createRemoveParentAction(Sprite sprite, SequenceAction sequence, Formula child) {
		RemoveParentAction action = action(RemoveParentAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setChildObject(child);
		return action;
	}

	public Action createRemoveJointAction(Sprite sprite, SequenceAction sequence, Formula jointName) {
		RemoveJointAction action = action(RemoveJointAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setJointName(jointName);
		return action;
	}

	public Action createSetActiveAction(Sprite sprite, SequenceAction sequence, Formula objectName, boolean activeState) {
		SetActiveAction action = action(SetActiveAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setObjectName(objectName);
		action.setActiveState(activeState);
		return action;
	}

	public Action createCloneObjectAction(Sprite sprite, SequenceAction sequence, Formula source, Formula newName) {
		CloneObjectAction action = action(CloneObjectAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setSourceObjectName(source);
		action.setCloneObjectName(newName);
		return action;
	}

	public Action createSetMaterialAction(Sprite sprite, SequenceAction sequence, Formula objectName,
										  Formula r, Formula g, Formula b, Formula a, Formula metallic, Formula roughness,
										  Formula colorTexture, Formula normalTexture, Formula mrTexture) {
		SetMaterialAction action = action(SetMaterialAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setObjectName(objectName);
		action.setColorR(r);
		action.setColorG(g);
		action.setColorB(b);
		action.setColorA(a);
		action.setMetallic(metallic);
		action.setRoughness(roughness);
		action.setBaseColorTexture(colorTexture);
		action.setNormalTexture(normalTexture);
		action.setMetallicRoughnessTexture(mrTexture);
		return action;
	}

	public Action createPlaySoundAtPositionAction(Sprite sprite, SequenceAction sequence, Formula soundName,
												  Formula instanceName, Formula x, Formula y, Formula z,
												  Formula volume, Formula pitch, boolean loop) {
		PlaySoundAtPositionAction action = action(PlaySoundAtPositionAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setSoundName(soundName);
		action.setInstanceName(instanceName);
		action.setPosX(x);
		action.setPosY(y);
		action.setPosZ(z);
		action.setVolume(volume);
		action.setPitch(pitch);
		action.setLoop(loop);
		return action;
	}

	public Action createPrepareSound2Action(Sprite sprite, SequenceAction sequence, Formula fileName, Formula soundName) {
		PrepareSoundAction2 action = action(PrepareSoundAction2.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setFileName(fileName);
		action.setSoundName(soundName);
		return action;
	}

	public Action createStopSoundAction2(Sprite sprite, SequenceAction sequence, Formula instanceName) {
		StopSoundAction2 action = action(StopSoundAction2.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setInstanceName(instanceName);
		return action;
	}

	public Action createSetGlobalSoundVolumeAction(Sprite sprite, SequenceAction sequence, Formula volume) {
		SetGlobalSoundVolumeAction action = action(SetGlobalSoundVolumeAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setVolume(volume);
		return action;
	}

	public Action createPrepareMusicAction(Sprite sprite, SequenceAction sequence, Formula fileName, Formula soundName) {
		PrepareMusicAction action = action(PrepareMusicAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setFileName(fileName);
		action.setSoundName(soundName);
		return action;
	}

	public Action createLoadSceneAdditiveAction(Sprite sprite, SequenceAction sequence, Formula fileName) {
		LoadSceneAdditiveAction action = action(LoadSceneAdditiveAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setFileName(fileName);
		return action;
	}

	public Action createSet3DSoundPositionAction(Sprite sprite, SequenceAction sequence,
												 Formula name, Formula x, Formula y, Formula z) {
		Set3DSoundPositionAction action = action(Set3DSoundPositionAction.class);
		action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
		action.setSoundName(name);
		action.setX(x);
		action.setY(y);
		action.setZ(z);
		return action;
	}

	public Action createVmRelativeMouseMoveAction(Sprite sprite, ScriptSequenceAction sequence, Formula dx, Formula dy, Formula mask) {
		VmRelativeMouseMoveAction action = action(VmRelativeMouseMoveAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setDxFormula(dx);
		action.setDyFormula(dy);
		action.setMaskFormula(mask);
		return action;
	}

	public Action createVmSetMonitorSizeAction(Sprite sprite, ScriptSequenceAction sequence, Formula width, Formula height) {
		VmSetMonitorSizeAction action = action(VmSetMonitorSizeAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setWidthFormula(width);
		action.setHeightFormula(height);
		return action;
	}

	public Action createSetPostProcessingAction(Sprite sprite, ScriptSequenceAction sequence, int effectIdx, int paramIdx, Formula val) {
		SetPostProcessingAction action = action(SetPostProcessingAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setEffectIndex(effectIdx);
		action.setParamIndex(paramIdx);
		action.setValueFormula(val);
		return action;
	}

	public Action createParticlesAction(Sprite sprite, ScriptSequenceAction sequence,
										Formula particleId, Formula looping, Formula duration,
										Formula startLifetime, Formula startSpeed, Formula startSize,
										Formula gravityModifier, Formula maxParticles, Formula emissionRate,
										Formula coneAngle, Formula coneRadius, Formula startColor,
										Formula endColor, Formula endSize, Formula texturePath,
										Formula isAdditive, Formula startRotation, Formula rotationOverLifetime,
										Formula positionX, Formula positionY, Formula positionZ,
										Formula rotationPitch, Formula rotationYaw, Formula rotationRoll) {
		CreateParticlesAction action = action(CreateParticlesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setParticleId(particleId);
		action.setLooping(looping);
		action.setDuration(duration);
		action.setStartLifetime(startLifetime);
		action.setStartSpeed(startSpeed);
		action.setStartSize(startSize);
		action.setGravityModifier(gravityModifier);
		action.setMaxParticles(maxParticles);
		action.setEmissionRate(emissionRate);
		action.setConeAngle(coneAngle);
		action.setConeRadius(coneRadius);
		action.setStartColor(startColor);
		action.setEndColor(endColor);
		action.setEndSize(endSize);
		action.setTexturePath(texturePath);
		action.setAdditive(isAdditive);
		action.setStartRotation(startRotation);
		action.setRotationOverLifetime(rotationOverLifetime);
		action.setPositionX(positionX);
		action.setPositionY(positionY);
		action.setPositionZ(positionZ);
		action.setRotationPitch(rotationPitch);
		action.setRotationYaw(rotationYaw);
		action.setRotationRoll(rotationRoll);
		return action;
	}

	public Action createDeleteParticlesAction(Sprite sprite, ScriptSequenceAction sequence, Formula particleId) {
		DeleteParticlesAction action = action(DeleteParticlesAction.class);
		Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
		action.setScope(scope);
		action.setParticleId(particleId);
		return action;
	}

    public Action createSetShadowsAction(Sprite sprite, SequenceAction sequence, Formula enabledFormula) {
        SetShadowsAction action = action(SetShadowsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setEnabledFormula(enabledFormula);
        return action;
    }

    public Action createSetVariableEasingAction(Sprite sprite, SequenceAction sequence,
                                                UserVariable variable,
                                                int typeIndex,
                                                Formula time, Formula duration,
                                                Formula start, Formula end) {
        SetVariableEasingAction action = action(SetVariableEasingAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

        action.setScope(scope);
        action.setUserVariable(variable);
        action.setTypeIndex(typeIndex);
        action.setTimeFormula(time);
        action.setDurationFormula(duration);
        action.setStartFormula(start);
        action.setEndFormula(end);

        return action;
    }

    public Action createSetRotationLockAction(Sprite sprite, SequenceAction sequence, Formula objectId) {
        SetRotationLockAction action = action(SetRotationLockAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        return action;
    }

    public Action createSetSpawnInvisibleAction(Sprite sprite, SequenceAction sequence, Formula objectId) {
        org.catrobat.catroid.content.actions.SetSpawnInvisibleAction action =
                action(org.catrobat.catroid.content.actions.SetSpawnInvisibleAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        return action;
    }

    public Action createKeyframeAnimationAction(Sprite sprite, SequenceAction sequence,
                                                Formula objectId, int actionType, Formula time) {
        KeyframeAnimationAction action = action(KeyframeAnimationAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setActionType(actionType);
        action.setTimeFormula(time);
        return action;
    }

    public Action createPtCreateTensorAction(Sprite sprite, SequenceAction sequence, Formula formulaWithBrickField, Formula formulaWithBrickField1, Formula formulaWithBrickField2, Formula formulaWithBrickField3) {
        PtCreateTensorAction action = action(PtCreateTensorAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNameFormula(formulaWithBrickField);
        action.setShapeFormula(formulaWithBrickField1);
        action.setValueFormula(formulaWithBrickField2);
        action.setTrainableFormula(formulaWithBrickField3);
        return action;
    }

    public Action createPtOpAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField, Formula formulaWithBrickField1, Formula formulaWithBrickField2, String op) {
        PtOpAction action = action(PtOpAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setResFormula(formulaWithBrickField);
        action.setAFormula(formulaWithBrickField1);
        action.setBFormula(formulaWithBrickField2);
        action.setOpType(op);
        return action;
    }

    public Action createPtBackwardAction(Sprite sprite, SequenceAction sequence, Formula lossName) {
        PtBackwardAction action = action(PtBackwardAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setLossNameFormula(lossName);
        return action;
    }

    public Action createPtStepAction(Sprite sprite, SequenceAction sequence, Formula lr) {
        PtStepAction action = action(PtStepAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setLrFormula(lr);
        return action;
    }

    public Action createPtSetTensorAction(Sprite sprite, SequenceAction sequence, Formula name, Formula data) {
        PtSetTensorAction action = action(PtSetTensorAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setNameFormula(name);
        action.setDataFormula(data);
        return action;
    }

    public Action createPtSetByIndexAction(Sprite sprite, SequenceAction sequence, Formula name, Formula index, Formula value) {
        PtSetByIndexAction action = action(PtSetByIndexAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setNameFormula(name);
        action.setIndexFormula(index);
        action.setValueFormula(value);
        return action;
    }

    public Action createPtReshapeAction(Sprite sprite, SequenceAction sequence, Formula name, Formula shape) {
        PtReshapeAction action = action(PtReshapeAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setNameFormula(name);
        action.setShapeFormula(shape);
        return action;
    }

    public Action createPtSetTrainingAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField) {
        PtSetTraining action = action(PtSetTraining.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setTrainingFormula(formulaWithBrickField);
        return action;
    }

    public Action createMLStepAdamAction(Sprite sprite, SequenceAction sequence, Formula lr) {
        MLStepAdamAction action = action(MLStepAdamAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setLr(lr);
        return action;
    }

    public Action createMLFileAction(Sprite sprite, SequenceAction sequence, Formula fileName, boolean isSave) {
        MLFileAction action = new MLFileAction(isSave);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setFileNameFormula(fileName);
        return action;
    }

    public Action createGenerateKeyAction(Sprite sprite, SequenceAction sequence,
                                          Formula filename, Formula pass, Formula alias, Formula commonName) {
        GenerateKeyAction action = action(GenerateKeyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFilenameFormula(filename);
        action.setPasswordFormula(pass);
        action.setAliasFormula(alias);
        action.setCommonNameFormula(commonName);
        return action;
    }

    public Action createSignApkAction(Sprite sprite, SequenceAction sequence,
                                      Formula input, Formula output, Formula key, Formula pass, Formula alias) {
        SignApkAction action = action(SignApkAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setInputApkFormula(input);
        action.setOutputApkFormula(output);
        action.setKeystoreFormula(key);
        action.setPasswordFormula(pass);
        action.setAliasFormula(alias);
        return action;
    }

    public Action createUpdateManifestAction(Sprite sprite, SequenceAction sequence,
                                             Formula apk, Formula pkg, Formula name,
                                             Formula vCode, Formula vName,
                                             Formula minSdk, Formula targetSdk,
                                             Formula debug, Formula pAdd, Formula pRem) {
        UpdateManifestAction action = action(UpdateManifestAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);

        action.setApkPathFormula(apk);
        action.setPackageNameFormula(pkg);
        action.setAppNameFormula(name);
        action.setVersionCodeFormula(vCode);
        action.setVersionNameFormula(vName);
        action.setMinSdkFormula(minSdk);
        action.setTargetSdkFormula(targetSdk);
        action.setDebuggableFormula(debug);
        action.setPermsAddFormula(pAdd);
        action.setPermsRemoveFormula(pRem);

        return action;
    }

    public Action createExtractFileAction(Sprite sprite, SequenceAction sequence,
                                          Formula apk, Formula inner, Formula dest) {
        ExtractFileAction action = action(ExtractFileAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);

        action.setApkPathFormula(apk);
        action.setInnerPathFormula(inner);
        action.setDestPathFormula(dest);

        return action;
    }

    public Action createAddFileToApkAction(Sprite sprite, SequenceAction sequence,
                                           Formula apk, Formula src, Formula dest) {
        AddFileToApkAction action = action(AddFileToApkAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);

        action.setApkPathFormula(apk);
        action.setSourcePathFormula(src);
        action.setDestPathFormula(dest);

        return action;
    }

    public Action createDeleteFromApkAction(Sprite sprite, SequenceAction sequence,
                                            Formula apk, Formula pattern) {
        DeleteFromApkAction action = action(DeleteFromApkAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);

        action.setApkPathFormula(apk);
        action.setDeletePatternFormula(pattern);

        return action;
    }

    public Action createThreedAlignNormalAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField, Formula formulaWithBrickField1, Formula formulaWithBrickField2, Formula formulaWithBrickField3) {
        ThreedAlignNormalAction action = action(ThreedAlignNormalAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);

        action.setObjId(formulaWithBrickField);
        action.setNx(formulaWithBrickField1);
        action.setNy(formulaWithBrickField2);
        action.setNz(formulaWithBrickField3);

        return action;
    }

    public ThreedCreateCylinderAction createThreedCreateCylinderAction(Sprite sprite, ScriptSequenceAction sequence, Formula id) {
        ThreedCreateCylinderAction action = action(ThreedCreateCylinderAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setObjectId(id);
        return action;
    }

    public ThreedAttachObjectToBoneAction createThreedAttachObjectToBoneAction(Sprite sprite, ScriptSequenceAction sequence,
                                                                               Formula childId, Formula parentId, Formula boneName, Formula ox, Formula oy, Formula oz) {
        ThreedAttachObjectToBoneAction action = action(ThreedAttachObjectToBoneAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.childId = childId;
        action.parentId = parentId;
        action.boneName = boneName;
        action.offsetX = ox;
        action.offsetY = oy;
        action.offsetZ = oz;
        return action;
    }

    public ThreedBindBoneToObjectAction createThreedBindBoneToObjectAction(Sprite sprite, ScriptSequenceAction sequence,
                                                                           Formula boneName, Formula modelId, Formula targetId) {
        ThreedBindBoneToObjectAction action = action(ThreedBindBoneToObjectAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.boneName = boneName;
        action.modelId = modelId;
        action.targetId = targetId;
        return action;
    }


    public Action createRunChip8Action(Sprite sprite, SequenceAction sequence, Formula filename) {
        RunChip8Action action = action(RunChip8Action.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFilenameFormula(filename);
        return action;
    }

    public Action createSetAIAction(Sprite sprite, ScriptSequenceAction sequence,
                                    Formula objId, Formula mode, Formula target, Formula speed, Formula stop, Formula range, Formula step, Formula avoid) {
        SetAIAction action = action(SetAIAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objId);
        action.setMode(mode);
        action.setTargetId(target);
        action.setSpeed(speed);
        action.setStopDist(stop);
        action.setRange(range);
        action.setStep(step);
        action.setAvoid(avoid);
        return action;
    }

    public Action createCameraTouchControlAction(Sprite sprite, ScriptSequenceAction sequence,
                                                 Formula enabled, Formula sensitivity, Formula x, Formula y, Formula w, Formula h) {

        CameraTouchControlAction action = action(CameraTouchControlAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);

        action.setScope(scope);
        action.setEnabled(enabled);
        action.setSensitivity(sensitivity);
        action.setBlockUI(false);
        action.setAreaX(x);
        action.setAreaY(y);
        action.setAreaW(w);
        action.setAreaH(h);

        return action;
    }

    public Action createSetTextureTilingAction(Sprite sprite, ScriptSequenceAction seq, Formula objId, Formula u, Formula v) {
        SetTextureTilingAction a = action(SetTextureTilingAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq));
        a.setObjectId(objId); a.setScaleU(u); a.setScaleV(v);
        return a;
    }

    public Action createSetSoundInstanceVolumeAction(Sprite sprite, ScriptSequenceAction seq, Formula name, Formula vol) {
        SetSoundInstanceVolumeAction a = action(SetSoundInstanceVolumeAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq));
        a.setInstanceName(name); a.setVolume(vol);
        return a;
    }

    public Action createSetSoundInstancePitchAction(Sprite sprite, ScriptSequenceAction seq, Formula name, Formula pitch) {
        SetSoundInstancePitchAction a = action(SetSoundInstancePitchAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq));
        a.setInstanceName(name); a.setPitch(pitch);
        return a;
    }

    public Action createSetParticleEmissionAction(Sprite sprite, ScriptSequenceAction seq, Formula objId, Formula rate) {
        SetParticleEmissionAction a = action(SetParticleEmissionAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq));
        a.setObjectId(objId); a.setRate(rate);
        return a;
    }

    public Action createSetAnimationSpeedAction(Sprite sprite, ScriptSequenceAction seq, Formula objId, Formula speed) {
        SetAnimationSpeedAction a = action(SetAnimationSpeedAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq));
        a.setObjectId(objId); a.setSpeed(speed);
        return a;
    }

    public Action createRotateCameraByAction(Sprite sprite, ScriptSequenceAction sequence,
                                             Formula pitch, Formula yaw, Formula roll) {
        RotateCameraByAction action = action(RotateCameraByAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setPitch(pitch);
        action.setYaw(yaw);
        action.setRoll(roll);
        return action;
    }

    public Action createCameraSettingsAction(Sprite sprite, ScriptSequenceAction sequence,
                                             Formula fov, Formula intensity, Formula duration) {
        CameraSettingsAction action = action(CameraSettingsAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setFov(fov);
        action.setShakeIntensity(intensity);
        action.setShakeDuration(duration);
        return action;
    }

    public ThreedCreateFixedConstraintAction createThreedCreateFixedConstraintAction(
            Sprite sprite, ScriptSequenceAction sequence,
            Formula id, Formula a, Formula b) {

        ThreedCreateFixedConstraintAction action = action(ThreedCreateFixedConstraintAction.class);
        action.scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.constraintId = id;
        action.objA = a;
        action.objB = b;
        return action;
    }

    public Action createAttachRaySensorAction(Sprite sprite, ScriptSequenceAction sequence,
                                              Formula rayName, Formula objectId,
                                              Formula offX, Formula offY, Formula offZ,
                                              Formula dirX, Formula dirY, Formula dirZ,
                                              Formula dist) {
        AttachRaySensorAction action = action(AttachRaySensorAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRayName(rayName);
        action.setObjectId(objectId);
        action.setOffX(offX);
        action.setOffY(offY);
        action.setOffZ(offZ);
        action.setDirX(dirX);
        action.setDirY(dirY);
        action.setDirZ(dirZ);
        action.setDist(dist);
        return action;
    }

    public Action createBakeByPrefixAction(Sprite sprite, ScriptSequenceAction sequence,
                                           Formula prefix, Formula resultName) {
        BakeByPrefixAction action = action(BakeByPrefixAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setPrefix(prefix);
        action.setResultName(resultName);
        return action;
    }

    public Action createSetPostProcessingNewAction(Sprite sprite, ScriptSequenceAction sequence,
                                                   int effectIndex, int paramIndex, Formula valueFormula) {
        SetPostProcessingNewAction action = new SetPostProcessingNewAction();
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setEffectIndex(effectIndex);
        action.setParamIndex(paramIndex);
        action.setValueFormula(valueFormula);
        return action;
    }

    public Action createSetShadowQualityAction(Sprite sprite, ScriptSequenceAction sequence, Formula size, Formula resolution) {
        SetShadowQualityAction action = new SetShadowQualityAction();
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setSizeFormula(size);
        action.setResolutionFormula(resolution);
        return action;
    }

    public Action createAddHingeAction(Sprite sprite, ScriptSequenceAction sequence,
                                       Formula id, Formula objA, Formula objB,
                                       Formula pax, Formula pay, Formula paz,
                                       Formula aax, Formula aay, Formula aaz,
                                       Formula pbx, Formula pby, Formula pbz,
                                       Formula abx, Formula aby, Formula abz) {
        AddHingeAction action = action(AddHingeAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setConstraintId(id);
        action.setObjectA(objA);
        action.setObjectB(objB);
        action.setPivotAX(pax); action.setPivotAY(pay); action.setPivotAZ(paz);
        action.setAxisAX(aax);  action.setAxisAY(aay);  action.setAxisAZ(aaz);
        action.setPivotBX(pbx); action.setPivotBY(pby); action.setPivotBZ(pbz);
        action.setAxisBX(abx);  action.setAxisBY(aby);  action.setAxisBZ(abz);
        return action;
    }

    public Action createSetHingeMotorAction(Sprite sprite, ScriptSequenceAction sequence,
                                            Formula id, Formula targetAngle, Formula maxForce) {
        SetHingeMotorAction action = action(SetHingeMotorAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setConstraintId(id);
        action.setTargetAngle(targetAngle);
        action.setMaxForce(maxForce);
        return action;
    }

    public Action createFast2DCreateAction(Sprite sprite, SequenceAction sequence, Formula entityId) {
        Fast2DCreateAction action = action(Fast2DCreateAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setEntityId(entityId);
        return action;
    }

    public Action createFast2DSetGravityAction(Sprite sprite, SequenceAction sequence,
                                               Formula gravityX, Formula gravityY) {
        Fast2DSetGravityAction action = action(Fast2DSetGravityAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setGravityX(gravityX);
        action.setGravityY(gravityY);
        return action;
    }

    public Action createFast2DSetPositionAction(Sprite sprite, SequenceAction sequence,
                                                Formula entityId, Formula posX, Formula posY) {
        Fast2DSetPositionAction action = action(Fast2DSetPositionAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setEntityId(entityId);
        action.setPosX(posX);
        action.setPosY(posY);
        return action;
    }

    public Action createFast2DSetRotationAction(Sprite s, SequenceAction seq, Formula id, Formula angle) {
        Fast2DSetRotationAction a = action(Fast2DSetRotationAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        a.setAngle(angle);
        return a;
    }

    public Action createFast2DSetScaleAction(Sprite s, SequenceAction seq, Formula id, Formula sx, Formula sy) {
        Fast2DSetScaleAction a = action(Fast2DSetScaleAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        a.setSx(sx); a.setSy(sy);
        return a;
    }

    public Action createFast2DSetTextureAction(Sprite s, SequenceAction seq, Formula id, Formula file) {
        Fast2DSetTextureAction a = action(Fast2DSetTextureAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        a.setFileName(file);
        return a;
    }

    public Action createFast2DSetZIndexAction(Sprite s, SequenceAction seq, Formula id, Formula z) {
        Fast2DSetZIndexAction a = action(Fast2DSetZIndexAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        a.setZIndex(z);
        return a;
    }

    public Action createFast2DDeleteAction(Sprite s, SequenceAction seq, Formula id) {
        Fast2DDeleteAction a = action(Fast2DDeleteAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        return a;
    }

    public Action createFast2DSetColorAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField, Formula formulaWithBrickField1, Formula formulaWithBrickField2, Formula formulaWithBrickField3, Formula formulaWithBrickField4) {
        Fast2DSetColorAction a = action(Fast2DSetColorAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        a.setEntityId(formulaWithBrickField);
        a.setR(formulaWithBrickField1);
        a.setG(formulaWithBrickField2);
        a.setB(formulaWithBrickField3);
        a.setA(formulaWithBrickField4);
        return a;
    }

    public Action createFast2DSetCameraAction(Sprite sprite, ScriptSequenceAction sequence, Formula formulaWithBrickField, Formula formulaWithBrickField1, Formula formulaWithBrickField2) {
        Fast2DSetCameraAction a = action(Fast2DSetCameraAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        a.setPosX(formulaWithBrickField);
        a.setPosY(formulaWithBrickField1);
        a.setZoom(formulaWithBrickField2);
        return a;
    }

    public Action createFast2DSetVelocityAction(Sprite s, SequenceAction seq, Formula id, Formula vx, Formula vy) {
        Fast2DSetVelocityAction a = action(Fast2DSetVelocityAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setVx(vx); a.setVy(vy); return a;
    }

    public Action createFast2DSetAngularVelocityAction(Sprite s, SequenceAction seq, Formula id, Formula v) {
        Fast2DSetAngularVelocityAction a = action(Fast2DSetAngularVelocityAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setAngVel(v); return a;
    }

    public Action createFast2DMakePhysicsAction(Sprite s, SequenceAction seq, Formula id, Formula dyn, Formula shape, Formula den, Formula fric, Formula bnc) {
        Fast2DMakePhysicsAction a = action(Fast2DMakePhysicsAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setDyn(dyn); a.setShape(shape); a.setDen(den); a.setFric(fric); a.setBnc(bnc); return a;
    }

    public Action createFast2DApplyForceAction(Sprite s, SequenceAction seq, Formula id, Formula fx, Formula fy) {
        Fast2DApplyForceAction a = action(Fast2DApplyForceAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setFx(fx); a.setFy(fy); return a;
    }

    public Action createFast2DApplyImpulseAction(Sprite s, SequenceAction seq, Formula id, Formula ix, Formula iy) {
        Fast2DApplyImpulseAction a = action(Fast2DApplyImpulseAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setFx(ix); a.setFy(iy); return a;
    }

    public Action createFast2DSetPhysicsVelocityAction(Sprite s, SequenceAction seq, Formula id, Formula vx, Formula vy) {
        Fast2DSetPhysicsVelocityAction a = action(Fast2DSetPhysicsVelocityAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id); a.setFx(vx); a.setFy(vy); return a;
    }

    public Action createFast2DSetCollisionFilterAction(Sprite s, SequenceAction seq, Formula id, Formula sensor, Formula group) {
        Fast2DSetCollisionFilterAction a = action(Fast2DSetCollisionFilterAction.class);
        a.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), s, seq));
        a.setEntityId(id);
        a.setSensor(sensor);
        a.setGroup(group);
        return a;
    }

    public Action createBufferAction(Sprite sprite, SequenceAction sequence, Formula name, Formula w, Formula h) {
        CreateBufferAction action = action(CreateBufferAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNameFormula(name); action.setWidthFormula(w); action.setHeightFormula(h);
        return action;
    }

    public Action removeBufferAction(Sprite sprite, SequenceAction sequence, Formula name) {
        RemoveFromBufferAction action = action(RemoveFromBufferAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNameFormula(name);
        return action;
    }

    public Action createAddToBufferAction(Sprite sprite, SequenceAction sequence, Formula name) {
        AddToBufferAction action = action(AddToBufferAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope); action.setNameFormula(name);
        return action;
    }

    public Action createSetBufferCameraAction(Sprite sprite, SequenceAction sequence, Formula name, Formula x, Formula y, Formula zoom, Formula rot) {
        SetBufferCameraAction action = action(SetBufferCameraAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope); action.setNameFormula(name);
        action.setXFormula(x); action.setYFormula(y); action.setZoomFormula(zoom); action.setRotFormula(rot);
        return action;
    }

    public Action createSaveBufferAction(Sprite sprite, SequenceAction sequence, Formula name, Formula file) {
        SaveBufferAction action = action(SaveBufferAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope); action.setNameFormula(name); action.setFileFormula(file);
        return action;
    }

    public Action createSetBufferAutoUpdateAction(Sprite sprite, SequenceAction sequence, Formula name, Formula state) {
        SetBufferAutoUpdateAction action = action(SetBufferAutoUpdateAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope); action.setNameFormula(name); action.setStateFormula(state);
        return action;
    }

    public Action createApplyBufferLookAction(Sprite sprite, SequenceAction sequence, Formula name) {
        ApplyBufferLookAction action = action(ApplyBufferLookAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope); action.setNameFormula(name);
        return action;
    }

    public Action createSetBufferOnlyAction(Sprite sprite, SequenceAction sequence, Formula state) {
        SetBufferOnlyAction action = action(SetBufferOnlyAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setStateFormula(state);
        return action;
    }

    public Action createSetBufferModeAction(Sprite sprite, SequenceAction sequence, Formula name, Formula r2d, Formula r3d) {
        SetBufferModeAction action = action(SetBufferModeAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setNameFormula(name); action.setRender2DFormula(r2d); action.setRender3DFormula(r3d);
        return action;
    }

    public Action createSetBufferCamera3DAction(Sprite sprite, SequenceAction sequence, Formula name, Formula x, Formula y, Formula z, Formula yaw, Formula pitch, Formula roll, Formula fov) {
        SetBufferCamera3DAction action = action(SetBufferCamera3DAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setNameFormula(name); action.setXForm(x); action.setYForm(y); action.setZForm(z);
        action.setYawForm(yaw); action.setPitchForm(pitch); action.setRollForm(roll); action.setFovForm(fov);
        return action;
    }

    public Action createVoxelBuildAction(Sprite sprite, ScriptSequenceAction sequence,
                                         Formula worldId, Formula texture, Formula width, Formula height) {
        VoxelBuildAction action = action(VoxelBuildAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setWorldId(worldId);
        action.setTexture(texture);
        action.setAtlasW(width);
        action.setAtlasH(height);
        return action;
    }

    public Action createVoxelLoadStringAction(Sprite sprite, ScriptSequenceAction sequence,
                                              Formula id, Formula data, Formula dx, Formula dy, Formula dz) {
        VoxelLoadStringAction action = action(VoxelLoadStringAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setWorldId(id);
        action.setDataStr(data);
        action.setDelimX(dx);
        action.setDelimY(dy);
        action.setDelimZ(dz);
        return action;
    }

    public Action createVoxelDeleteAction(Sprite sprite, ScriptSequenceAction sequence, Formula worldId) {
        VoxelDeleteAction action = action(VoxelDeleteAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setWorldId(worldId);
        return action;
    }

    public Action createVoxelSetTransparentAction(Sprite sprite, ScriptSequenceAction sequence,
                                                  Formula blockId, Formula isTransparent) {
        VoxelSetTransparentAction action = action(VoxelSetTransparentAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setBlockId(blockId);
        action.setTransparent(isTransparent);
        return action;
    }

    public Action createVoxelCreateWorldAction(Sprite sprite, ScriptSequenceAction sequence,
                                               Formula worldId, Formula sx, Formula sy, Formula sz, Formula wx, Formula wy, Formula wz) {
        VoxelCreateWorldAction action = action(VoxelCreateWorldAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setWorldId(worldId);
        action.setSizeX(sx);
        action.setSizeY(sy);
        action.setSizeZ(sz);
        action.setWorldX(wx);
        action.setWorldY(wy);
        action.setWorldZ(wz);
        return action;
    }

    public Action createVoxelConfigAction(Sprite sprite, ScriptSequenceAction sequence,
                                          Formula id, Formula shape, Formula tx, Formula ty) {
        VoxelConfigAction action = action(VoxelConfigAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setBlockId(id);
        action.setShape(shape);
        action.setAtlasX(tx);
        action.setAtlasY(ty);
        return action;
    }

    public Action createVoxelSetBlockAction(Sprite sprite, ScriptSequenceAction sequence,
                                            Formula id, Formula x, Formula y, Formula z, Formula type, Formula data) {
        VoxelSetBlockAction action = action(VoxelSetBlockAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setWorldId(id);
        action.setX(x);
        action.setY(y);
        action.setZ(z);
        action.setType(type);
        action.setData(data);
        return action;
    }

    public Action createAttachToCameraAction(Sprite sprite, SequenceAction sequence, Formula objectName) {
        AttachToCameraAction action = action(AttachToCameraAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectName(objectName);
        return action;
    }

    public Action createDetachFromCameraAction(Sprite sprite, SequenceAction sequence, Formula objectName) {
        DetachFromCameraAction action = action(DetachFromCameraAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectName(objectName);
        return action;
    }

    public Action createSetMaxPointLightsAction(Sprite sprite, SequenceAction sequence, Formula maxLights) {
        SetMaxPointLightsAction action = action(SetMaxPointLightsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setMaxLights(maxLights);
        return action;
    }

    public Action createPromoteLightAction(Sprite sprite, SequenceAction sequence, Formula lightId) {
        PromoteLightAction action = action(PromoteLightAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLightId(lightId);
        return action;
    }

    public Action createApplyShaderToImageAction(Sprite sprite, SequenceAction sequence, Formula filename, Formula vsh, Formula fsh) {
        ApplyShaderToImageAction action = action(ApplyShaderToImageAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFilename(filename);
        action.setVertexCode(vsh);
        action.setFragmentCode(fsh);
        return action;
    }

    public Action createCameraTrackingAction(Sprite sprite, SequenceAction sequence, Formula id, int mode, Formula px, Formula py, Formula pz, Formula yaw, Formula pitch, Formula roll) {
        CameraTrackingAction action = action(CameraTrackingAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(id);
        action.setMode(mode);
        action.setPx(px);
        action.setPy(py);
        action.setPz(pz);
        action.setRy(yaw);
        action.setRp(pitch);
        action.setRr(roll);
        return action;
    }

    public Action createSetScreenShaderAction(Sprite sprite, SequenceAction sequence, Formula vertex, Formula fragment) {
        SetScreenShaderAction action = action(SetScreenShaderAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setVertex(vertex);
        action.setFragment(fragment);
        return action;
    }

    public Action createSet3DSoundMaxDistanceAction(Sprite sprite, SequenceAction sequence, Formula instanceName, Formula distance) {
        Set3DSoundMaxDistanceAction action = action(Set3DSoundMaxDistanceAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setInstanceName(instanceName);
        action.setDistance(distance);
        return action;
    }

    public Action createSetEmissiveAction(Sprite sprite, SequenceAction sequence,
                                          Formula objectName, Formula colorR, Formula colorG, Formula colorB, Formula colorA,
                                          Formula intensity, Formula texturePath) {
        SetEmissiveAction action = action(SetEmissiveAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectName(objectName);
        action.setColorR(colorR);
        action.setColorG(colorG);
        action.setColorB(colorB);
        action.setColorA(colorA);
        action.setIntensity(intensity);
        action.setTexturePath(texturePath);
        return action;
    }

    public Action createSetFpsAction(Sprite sprite, SequenceAction sequence, Formula fps) {
        SetFpsAction action = action(SetFpsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFps(fps);
        return action;
    }

    public Action createSetRenderResolutionAction(Sprite sprite, SequenceAction sequence, Formula scale, Formula mode) {
        SetRenderResolutionAction action = action(SetRenderResolutionAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setScale(scale);
        action.setAspectMode(mode);
        return action;
    }

    public Action createSetObjectShaderAction(Sprite sprite, SequenceAction sequence,
                                              Formula objectId, Formula vertexCode, Formula fragmentCode) {
        SetObjectShaderAction action = action(SetObjectShaderAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectIdFormula(objectId);
        action.setVertexFormula(vertexCode);
        action.setFragmentFormula(fragmentCode);
        return action;
    }

    public Action createSetObjectShaderUniformAction(Sprite sprite, SequenceAction sequence,
                                                     Formula objectId, Formula name, Formula val1, Formula val2, Formula val3) {
        SetObjectShaderUniformAction action = action(SetObjectShaderUniformAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectIdFormula(objectId);
        action.setNameFormula(name);
        action.setVal1Formula(val1);
        action.setVal2Formula(val2);
        action.setVal3Formula(val3);
        return action;
    }

    public Action createRemoveObjectsByPrefixAction(Sprite sprite, SequenceAction sequence, Formula prefix) {
        RemoveObjectsByPrefixAction action = action(RemoveObjectsByPrefixAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPrefixFormula(prefix);
        return action;
    }

    public Action createRunAsSpriteAction(Sprite sprite, com.badlogic.gdx.scenes.scene2d.actions.SequenceAction sequence,
                                          org.catrobat.catroid.content.Script script,
                                          Formula spriteName, List<Brick> nestedBricks) {
        RunAsSpriteAction action = new RunAsSpriteAction(script, nestedBricks);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setSpriteName(spriteName);
        return action;
    }

    public Action createInstantAction(Sprite sprite, SequenceAction sequence, Action instantSequence) {
        InstantAction action = action(InstantAction.class);
        action.setAction(instantSequence);
        return action;
    }

    public Action createEasePropertyAction(Sprite sprite, SequenceAction sequence,
                                           int propertyIndex, int typeIndex, Formula duration, Formula start, Formula end) {
        EasePropertyAction action = action(EasePropertyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPropertyIndex(propertyIndex);
        action.setTypeIndex(typeIndex);
        action.setDurationFormula(duration);
        action.setStartFormula(start);
        action.setEndFormula(end);
        return action;
    }

    public Action createSpawnThreadAction(Sprite sprite, SequenceAction sequence, Formula id, List<Brick> threadBricks, Script script) {
        SpawnThreadAction action = action(SpawnThreadAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setThreadIdFormula(id);
        action.setThreadBricks(threadBricks);
        action.setScript(script);
        return action;
    }

    public Action createWaitThreadAction(Sprite sprite, SequenceAction sequence, Formula id) {
        WaitThreadAction action = action(WaitThreadAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setThreadIdFormula(id);
        return action;
    }

    public Action createStopThreadAction(Sprite sprite, SequenceAction sequence, Formula id) {
        StopThreadAction action = action(StopThreadAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setThreadIdFormula(id);
        return action;
    }

    public Action createAddTextToBufferAction(Sprite sprite, SequenceAction sequence, Formula buffer, Formula text) {
        AddTextToBufferAction action = action(AddTextToBufferAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setBufferName(buffer);
        action.setTextName(text);
        return action;
    }

    public Action createSetBufferShaderAction(Sprite sprite, SequenceAction sequence, Formula name, Formula vsh, Formula fsh) {
        SetBufferShaderAction action = action(SetBufferShaderAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setBufferName(name);
        action.setVertexCode(vsh);
        action.setFragmentCode(fsh);
        return action;
    }

    public Action createSetBufferShaderUniformAction(Sprite sprite, SequenceAction sequence, Formula bufName, Formula uniName, Formula v1, Formula v2, Formula v3, int count) {
        SetBufferShaderUniformAction action = action(SetBufferShaderUniformAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setBufferName(bufName);
        action.setUniformName(uniName);
        action.setVal1(v1);
        action.setVal2(v2);
        action.setVal3(v3);
        action.setParamCount(count);
        return action;
    }

    public Action createSetMainRenderLoopsAction(Sprite sprite, SequenceAction sequence, Formula r2d, Formula rf2d, Formula r3d) {
        SetMainRenderLoopsAction action = action(SetMainRenderLoopsAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setRender2D(r2d);
        action.setRenderFast2D(rf2d);
        action.setRender3D(r3d);
        return action;
    }

    public Action createSetBufferEffectsAction(Sprite sprite, SequenceAction sequence, Formula name, Formula vfx, Formula mip) {
        SetBufferEffectsAction action = action(SetBufferEffectsAction.class);
        action.setScope(new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence));
        action.setBufferName(name);
        action.setUseVfx(vfx);
        action.setUseMipmapping(mip);
        return action;
    }

    public Action createZipProjectFilesAction(Sprite sprite, SequenceAction sequence,
                                              Formula filesToZip, Formula zipFileName) {
        ZipProjectFilesAction action = action(ZipProjectFilesAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFilesToZip(filesToZip);
        action.setZipFileName(zipFileName);
        return action;
    }

    public Action createUnzipProjectFilesAction(Sprite sprite, SequenceAction sequence,
                                                Formula zipFileName) {
        UnzipProjectFilesAction action = action(UnzipProjectFilesAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setZipFileName(zipFileName);
        return action;
    }

    public Action createHttpCreateAction(Sprite sprite, SequenceAction sequence,
                                         Formula requestId, Formula method, Formula url) {
        HttpCreateAction action = action(HttpCreateAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        action.setMethod(method);
        action.setUrl(url);
        return action;
    }

    public Action createHttpConfigAction(Sprite sprite, SequenceAction sequence,
                                         Formula requestId, int configTypeSelection, Formula key, Formula value) {
        HttpConfigAction action = action(HttpConfigAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        action.setConfigTypeSelection(configTypeSelection);
        action.setKey(key);
        action.setValue(value);
        return action;
    }

    public Action createHttpBodyAction(Sprite sprite, SequenceAction sequence,
                                       Formula requestId, Formula bodyText, Formula contentType) {
        HttpBodyAction action = action(HttpBodyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        action.setBodyText(bodyText);
        action.setContentType(contentType);
        return action;
    }

    public Action createHttpAttachFileAction(Sprite sprite, SequenceAction sequence,
                                             Formula requestId, Formula fileName, Formula parameterName, Formula mimeType) {
        HttpAttachFileAction action = action(HttpAttachFileAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        action.setFileName(fileName);
        action.setParameterName(parameterName);
        action.setMimeType(mimeType);
        return action;
    }

    public Action createHttpSendAction(Sprite sprite, SequenceAction sequence,
                                       Formula requestId) {
        HttpSendAction action = action(HttpSendAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        return action;
    }

    public Action createHttpSaveFileAction(Sprite sprite, SequenceAction sequence,
                                           Formula requestId, Formula destinationFileName) {
        HttpSaveFileAction action = action(HttpSaveFileAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRequestId(requestId);
        action.setDestinationFileName(destinationFileName);
        return action;
    }

    public Action createBufferMaskAction(Sprite sprite, SequenceAction sequence,
                                         Formula bufferName, int modeSelection) {
        BufferMaskAction action = action(BufferMaskAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setBufferName(bufferName);
        action.setModeSelection(modeSelection);
        return action;
    }

    public Action createSetNativeParentAction(Sprite sprite, ScriptSequenceAction sequence,
                                              Formula viewId, Formula parentId) {
        SetNativeParentAction action = action(SetNativeParentAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setViewId(viewId);
        action.setParentId(parentId);
        return action;
    }

    public Action createCreateScrollViewAction(Sprite sprite, ScriptSequenceAction sequence,
                                               int scrollMode, int overScrollMode, Formula viewId, Formula bgColor, Formula showBars, Formula padding,
                                               Formula x, Formula y, Formula width, Formula height) {
        CreateScrollViewAction action = action(CreateScrollViewAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setScrollMode(scrollMode);
        action.setOverScrollMode(overScrollMode);
        action.setViewId(viewId);
        action.setBgColor(bgColor);
        action.setShowBars(showBars);
        action.setPadding(padding);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createCreateTextLabelAction(Sprite sprite, ScriptSequenceAction sequence,
                                              int formatMode, int alignMode, int scrollMode, Formula viewId, Formula text, Formula colorHex,
                                              Formula fontSize, Formula bgColor, Formula fontPath, Formula cornerRadius, Formula lineSpacing,
                                              Formula x, Formula y, Formula width, Formula height) {
        CreateTextLabelAction action = action(CreateTextLabelAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormatMode(formatMode);
        action.setAlignMode(alignMode);
        action.setScrollMode(scrollMode);
        action.setViewId(viewId);
        action.setText(text);
        action.setColorHex(colorHex);
        action.setFontSize(fontSize);
        action.setBgColor(bgColor);
        action.setFontPath(fontPath);
        action.setCornerRadius(cornerRadius);
        action.setLineSpacing(lineSpacing);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createNativeViewConfigAction(Sprite sprite, ScriptSequenceAction sequence,
                                               int propertySelection, Formula viewId, Formula value) {
        NativeViewConfigAction action = action(NativeViewConfigAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPropertySelection(propertySelection);
        action.setViewId(viewId);
        action.setValue(value);
        return action;
    }

    public Action createNativeViewControlAction(Sprite sprite, ScriptSequenceAction sequence,
                                                int commandSelection, Formula viewId, Formula parameter) {
        NativeViewControlAction action = action(NativeViewControlAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setCommandSelection(commandSelection);
        action.setViewId(viewId);
        action.setParameter(parameter);
        return action;
    }

    public Action createNativeViewListenerAction(Sprite sprite, ScriptSequenceAction sequence,
                                                 int eventSelection, Formula viewId, UserVariable userVariable) {
        NativeViewListenerAction action = action(NativeViewListenerAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setEventSelection(eventSelection);
        action.setViewId(viewId);
        action.setUserVariable(userVariable);
        return action;
    }

    public Action createCreateButtonAction(Sprite sprite, ScriptSequenceAction sequence,
                                           Formula viewId, Formula text, Formula colorHex, Formula fontSize, Formula bgColor, Formula cornerRadius,
                                           Formula x, Formula y, Formula width, Formula height) {
        CreateButtonAction action = action(CreateButtonAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setViewId(viewId);
        action.setText(text);
        action.setColorHex(colorHex);
        action.setFontSize(fontSize);
        action.setBgColor(bgColor);
        action.setCornerRadius(cornerRadius);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createCreateSwitchAction(Sprite sprite, ScriptSequenceAction sequence,
                                           Formula viewId, Formula text, Formula defaultState,
                                           Formula x, Formula y, Formula width, Formula height) {
        CreateSwitchAction action = action(CreateSwitchAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setViewId(viewId);
        action.setText(text);
        action.setDefaultState(defaultState);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createCreateImageViewAction(Sprite sprite, ScriptSequenceAction sequence,
                                              int scaleSelection, Formula viewId, Formula source, Formula cornerRadius,
                                              Formula x, Formula y, Formula width, Formula height) {
        CreateImageViewAction action = action(CreateImageViewAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setScaleSelection(scaleSelection);
        action.setViewId(viewId);
        action.setSource(source);
        action.setCornerRadius(cornerRadius);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createCreateSliderAction(Sprite sprite, ScriptSequenceAction sequence,
                                           Formula viewId, Formula minValue, Formula maxValue, Formula currentValue,
                                           Formula x, Formula y, Formula width, Formula height) {
        CreateSliderAction action = action(CreateSliderAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setViewId(viewId);
        action.setMinValue(minValue);
        action.setMaxValue(maxValue);
        action.setCurrentValue(currentValue);
        action.setX(x);
        action.setY(y);
        action.setWidth(width);
        action.setHeight(height);
        return action;
    }

    public Action createNativeViewAnimateAction(Sprite sprite, ScriptSequenceAction sequence,
                                                int propertySelection, int easingSelection, Formula viewId, Formula value, Formula durationMs) {
        NativeViewAnimateAction action = action(NativeViewAnimateAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPropertySelection(propertySelection);
        action.setEasingSelection(easingSelection);
        action.setViewId(viewId);
        action.setValue(value);
        action.setDurationMs(durationMs);
        return action;
    }

    public Action createNativeViewBindSpriteAction(Sprite sprite, ScriptSequenceAction sequence,
                                                   int alignMode, Formula viewId, Formula spriteName, Formula offsetX, Formula offsetY) {
        NativeViewBindSpriteAction action = action(NativeViewBindSpriteAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setAlignMode(alignMode);
        action.setViewId(viewId);
        action.setSpriteName(spriteName);
        action.setOffsetX(offsetX);
        action.setOffsetY(offsetY);
        return action;
    }

    public Action createShowNotificationAction(Sprite sprite, SequenceAction sequence,
                                               Formula notificationId, Formula channelName, Formula title, Formula text,
                                               int importanceSelection, Formula largeIconFile, boolean isOngoing) {
        ShowNotificationAction action = action(ShowNotificationAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNotificationId(notificationId);
        action.setChannelName(channelName);
        action.setTitle(title);
        action.setText(text);
        action.setImportanceSelection(importanceSelection);
        action.setLargeIconFile(largeIconFile);
        action.setOngoing(isOngoing);
        return action;
    }

    public Action createBroadcastWithParamsAction(Sprite sprite, SequenceAction sequence,
                                                  Formula signalName, Formula params) {
        BroadcastWithParamsAction action = action(BroadcastWithParamsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setSignalName(signalName);
        action.setParams(params);
        return action;
    }

    public Action createSaveBroadcastParamsAction(Sprite sprite, SequenceAction sequence,
                                                  String signalName, UserVariable userVariable) {
        SaveBroadcastParamsAction action = action(SaveBroadcastParamsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setSignalName(signalName);
        action.setUserVariable(userVariable);
        return action;
    }

    public Action createNotificationCreateAction(Sprite sprite, SequenceAction sequence,
                                                 Formula notifId, Formula channelName, Formula title, Formula text,
                                                 Formula largeIconFile, int importanceSelection, boolean isOngoing) {
        NotificationCreateAction action = action(NotificationCreateAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNotifId(notifId);
        action.setChannelName(channelName);
        action.setTitle(title);
        action.setText(text);
        action.setLargeIconFile(largeIconFile);
        action.setImportanceSelection(importanceSelection);
        action.setOngoing(isOngoing);
        return action;
    }

    public Action createNotificationShowAction(Sprite sprite, SequenceAction sequence,
                                               Formula notifId, Formula delaySeconds) {
        NotificationShowAction action = action(NotificationShowAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNotifId(notifId);
        action.setDelaySeconds(delaySeconds);
        return action;
    }

    public Action createEnableBackgroundModeAction(Sprite sprite, SequenceAction sequence,
                                                   Formula title, Formula text) {
        EnableBackgroundModeAction action = action(EnableBackgroundModeAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setTitle(title);
        action.setText(text);
        return action;
    }

    public Action createDisableBackgroundModeAction(Sprite sprite, SequenceAction sequence) {
        DisableBackgroundModeAction action = action(DisableBackgroundModeAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        return action;
    }

    public Action createNotificationAddButtonAction(Sprite sprite, SequenceAction sequence,
                                                    Formula notifId, Formula actionId, Formula text, Formula iconFile, Formula hint,
                                                    int behavior, boolean hasInput, boolean autoClose) {
        NotificationAddButtonAction action = action(NotificationAddButtonAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNotifId(notifId);
        action.setActionId(actionId);
        action.setText(text);
        action.setIconFile(iconFile);
        action.setHint(hint);
        action.setBehavior(behavior);
        action.setHasInput(hasInput);
        action.setAutoClose(autoClose);
        return action;
    }

    public Action createNotificationCancelAction(Sprite sprite, SequenceAction sequence, Formula notifId) {
        NotificationCancelAction action = action(NotificationCancelAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNotifId(notifId);
        return action;
    }

    public Action createConfigureLightAction(Sprite sprite, SequenceAction sequence,
                                             Formula lightId, int configTypeSelection, Formula value) {
        ConfigureLightAction action = action(ConfigureLightAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLightId(lightId);
        action.setConfigTypeSelection(configTypeSelection);
        action.setValue(value);
        return action;
    }

    public Action createConfigureMaterialAction(Sprite sprite, SequenceAction sequence,
                                                Formula objectId, int configTypeSelection, Formula value) {
        ConfigureMaterialAction action = action(ConfigureMaterialAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setConfigTypeSelection(configTypeSelection);
        action.setValue(value);
        return action;
    }

    public Action createConfigureParticlesAction(Sprite sprite, SequenceAction sequence,
                                                 Formula particleId, int configTypeSelection, Formula value) {
        ConfigureParticlesAction action = action(ConfigureParticlesAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setParticleId(particleId);
        action.setConfigTypeSelection(configTypeSelection);
        action.setValue(value);
        return action;
    }

    public Action createSetTextBufferOnlyAction(Sprite sprite, SequenceAction sequence,
                                                Formula textName, Formula stateFormula) {
        SetTextBufferOnlyAction action = action(SetTextBufferOnlyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setTextName(textName);
        action.setStateFormula(stateFormula);
        return action;
    }

    public Action createStartBufferRecordingAction(Sprite sprite, SequenceAction sequence,
                                                   Formula bufferName, Formula fileName, Formula fpsFormula, Formula bitrateFormula) {
        StartBufferRecordingAction action = action(StartBufferRecordingAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setBufferName(bufferName);
        action.setFileName(fileName);
        action.setFpsFormula(fpsFormula);
        action.setBitrateFormula(bitrateFormula);
        return action;
    }

    public Action createStopBufferRecordingAction(Sprite sprite, SequenceAction sequence) {
        StopBufferRecordingAction action = action(StopBufferRecordingAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        return action;
    }

    public Action createCompileJavaToDexAction(Sprite sprite, SequenceAction sequence,
                                               Formula srcFolder, Formula destDexFile, UserVariable errorVariable) {
        CompileJavaToDexAction action = action(CompileJavaToDexAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setSrcFolder(srcFolder);
        action.setDestDexFile(destDexFile);
        action.setErrorVariable(errorVariable);
        return action;
    }

    public Action createLoadAndRunDexAction(Sprite sprite, SequenceAction sequence,
                                            Formula dexPath, Formula className, Formula methodName, Formula methodArg) {
        LoadAndRunDexAction action = action(LoadAndRunDexAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setDexPath(dexPath);
        action.setClassName(className);
        action.setMethodName(methodName);
        action.setMethodArg(methodArg);
        return action;
    }

    public Action createOpenAppAction(Sprite sprite, SequenceAction sequence,
                                      Formula packageName) {
        OpenAppAction action = action(OpenAppAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPackageName(packageName);
        return action;
    }

    public Action createDownloadDependencyAction(Sprite sprite, SequenceAction sequence,
                                                 Formula libraryId, Formula repositories, UserVariable resultVariable, boolean recursive) {
        DownloadDependencyAction action = action(DownloadDependencyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLibraryId(libraryId);
        action.setRepositories(repositories);
        action.setResultVariable(resultVariable);
        action.setRecursive(recursive);
        return action;
    }

    public Action createExport3dObjectToGlbAction(Sprite sprite, SequenceAction sequence,
                                                  Formula objectId, Formula destFileName) {
        Export3dObjectToGlbAction action = action(Export3dObjectToGlbAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setDestFileName(destFileName);
        return action;
    }

    public Action createMqttConnectAction(Sprite sprite, SequenceAction sequence,
                                          Formula clientId, Formula host, Formula port) {
        MqttConnectAction action = action(MqttConnectAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setClientId(clientId);
        action.setHost(host);
        action.setPort(port);
        return action;
    }

    public Action createMqttJoinRoomAction(Sprite sprite, SequenceAction sequence,
                                           Formula clientId, Formula roomId, Formula salt) {
        MqttJoinRoomAction action = action(MqttJoinRoomAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setClientId(clientId);
        action.setRoomId(roomId);
        action.setSalt(salt);
        return action;
    }

    public Action createMqttPublishAction(Sprite sprite, SequenceAction sequence,
                                          Formula clientId, Formula roomId, Formula salt, Formula message) {
        MqttPublishAction action = action(MqttPublishAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setClientId(clientId);
        action.setRoomId(roomId);
        action.setSalt(salt);
        action.setMessage(message);
        return action;
    }

    public Action createMqttDisconnectAction(Sprite sprite, SequenceAction sequence,
                                             Formula clientId) {
        MqttDisconnectAction action = action(MqttDisconnectAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setClientId(clientId);
        return action;
    }

    public Action createEase3DPropertyAction(Sprite sprite, SequenceAction sequence,
                                             Formula objectId, int propertyIndex, int typeIndex, Formula duration, Formula start, Formula end) {
        Ease3DPropertyAction action = action(Ease3DPropertyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setPropertyIndex(propertyIndex);
        action.setTypeIndex(typeIndex);
        action.setDurationFormula(duration);
        action.setStartFormula(start);
        action.setEndFormula(end);
        return action;
    }

    public Action createBase64ToFileAction(Sprite sprite, SequenceAction sequence,
                                           Formula base64String, Formula destinationFileName) {
        Base64ToFileAction action = action(Base64ToFileAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setBase64String(base64String);
        action.setDestinationFileName(destinationFileName);
        return action;
    }

    public Action createShareAction(Sprite sprite, SequenceAction sequence,
                                    Formula contentFormula) {
        ShareAction action = action(ShareAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setContentFormula(contentFormula);
        return action;
    }

    public Action createSetCornerOffsetsAction(Sprite sprite, SequenceAction sequence,
                                               Formula tlx, Formula tly, Formula trx, Formula tryFormula,
                                               Formula brx, Formula bry, Formula blx, Formula bly) {
        SetCornerOffsetsAction action = action(SetCornerOffsetsAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setTlx(tlx);
        action.setTly(tly);
        action.setTrx(trx);
        action.setTryFormula(tryFormula);
        action.setBrx(brx);
        action.setBry(bry);
        action.setBlx(blx);
        action.setBly(bly);
        return action;
    }

    public Action createPlayGifAction(Sprite sprite, SequenceAction sequence,
                                      Formula gifFileName) {
        PlayGifAction action = action(PlayGifAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setGifFileName(gifFileName);
        return action;
    }

    public Action createStopGifAction(Sprite sprite, SequenceAction sequence) {
        StopGifAction action = action(StopGifAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        return action;
    }

    public Action createPlaySpritesheetAction(Sprite sprite, SequenceAction sequence,
                                              Formula rows, Formula cols, Formula selectedRow, Formula framesCount, Formula speed) {
        PlaySpritesheetAction action = action(PlaySpritesheetAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setRows(rows);
        action.setCols(cols);
        action.setSelectedRow(selectedRow);
        action.setFramesCount(framesCount);
        action.setSpeed(speed);
        return action;
    }

    public Action createStopSpritesheetAction(Sprite sprite, SequenceAction sequence) {
        StopSpritesheetAction action = action(StopSpritesheetAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        return action;
    }

    public Action createPtLayerLinearAction(Sprite sprite, SequenceAction sequence,
                                            Formula layerName, Formula input, Formula output, Formula inF, Formula outF) {
        PtLayerLinearAction action = action(PtLayerLinearAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setOutputFormula(output);
        action.setInFeaturesFormula(inF);
        action.setOutFeaturesFormula(outF);
        return action;
    }

    public Action createPtZeroGradAction(Sprite sprite, ScriptSequenceAction sequence) {
        PtZeroGradAction action = action(PtZeroGradAction.class);
        return action;
    }

    public Action createPtSliceAction(Sprite sprite, SequenceAction sequence,
                                      Formula res, Formula input, Formula start, Formula end) {
        PtSliceAction action = action(PtSliceAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setResFormula(res);
        action.setInputFormula(input);
        action.setStartColFormula(start);
        action.setEndColFormula(end);
        return action;
    }

    public Action createPtCreateNormalTensorAction(Sprite sprite, SequenceAction sequence,
                                                   Formula name, Formula shape, Formula mean, Formula std, Formula trainable) {
        PtCreateNormalTensorAction action = action(PtCreateNormalTensorAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNameFormula(name);
        action.setShapeFormula(shape);
        action.setMeanFormula(mean);
        action.setStdFormula(std);
        action.setTrainableFormula(trainable);
        return action;
    }

    public Action createPtEmbeddingAction(Sprite sprite, SequenceAction sequence,
                                          Formula layerName, Formula input, Formula output, Formula vocabSize, Formula embDim) {
        PtEmbeddingAction action = action(PtEmbeddingAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setOutputFormula(output);
        action.setVocabSizeFormula(vocabSize);
        action.setEmbDimFormula(embDim);
        return action;
    }

    public Action createPtAttentionAction(Sprite sprite, SequenceAction sequence,
                                          Formula layerName, Formula input, Formula output, Formula embedDim) {
        PtAttentionAction action = action(PtAttentionAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setOutputFormula(output);
        action.setEmbedDimFormula(embedDim);
        return action;
    }

    public Action createPtClipGradAction(Sprite sprite, SequenceAction sequence, Formula maxNorm) {
        PtClipGradAction action = action(PtClipGradAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setMaxNormFormula(maxNorm);
        return action;
    }

    public Action createPtConv2DAction(Sprite sprite, SequenceAction sequence,
                                       Formula layerName, Formula input, Formula output,
                                       Formula inC, Formula outC, Formula kSize, Formula stride) {
        PtConv2DAction action = action(PtConv2DAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setOutputFormula(output);
        action.setInChannelsFormula(inC);
        action.setOutChannelsFormula(outC);
        action.setKernelSizeFormula(kSize);
        action.setStrideFormula(stride);
        return action;
    }

    public Action createPtMaxPool2DAction(Sprite sprite, SequenceAction sequence,
                                          Formula res, Formula input, Formula poolSize, Formula stride) {
        PtMaxPool2DAction action = action(PtMaxPool2DAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setResFormula(res);
        action.setInputFormula(input);
        action.setPoolSizeFormula(poolSize);
        action.setStrideFormula(stride);
        return action;
    }

    public Action createPtDropoutAction(Sprite sprite, SequenceAction sequence,
                                        Formula res, Formula input, Formula prob) {
        PtDropoutAction action = action(PtDropoutAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setResFormula(res);
        action.setInputFormula(input);
        action.setProbFormula(prob);
        return action;
    }

    public Action createMLStepAdamWAction(Sprite sprite, SequenceAction sequence,
                                          Formula lr, Formula weightDecay) {
        MLStepAdamWAction action = action(MLStepAdamWAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLrFormula(lr);
        action.setWeightDecayFormula(weightDecay);
        return action;
    }

    public Action createPtLstmCellAction(Sprite sprite, SequenceAction sequence,
                                         Formula layerName, Formula input, Formula hIn, Formula cIn,
                                         Formula hOut, Formula cOut, Formula inDim, Formula hiddenDim) {
        PtLstmCellAction action = action(PtLstmCellAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setHInFormula(hIn);
        action.setCInFormula(cIn);
        action.setHOutFormula(hOut);
        action.setCOutFormula(cOut);
        action.setInDimFormula(inDim);
        action.setHiddenDimFormula(hiddenDim);
        return action;
    }

    public Action createPtGruCellAction(Sprite sprite, SequenceAction sequence,
                                        Formula layerName, Formula input, Formula hIn, Formula hOut,
                                        Formula inDim, Formula hiddenDim) {
        PtGruCellAction action = action(PtGruCellAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setLayerNameFormula(layerName);
        action.setInputFormula(input);
        action.setHInFormula(hIn);
        action.setHOutFormula(hOut);
        action.setInDimFormula(inDim);
        action.setHiddenDimFormula(hiddenDim);
        return action;
    }

    public Action createSetTextPropertyAction(Sprite sprite, SequenceAction sequence,
                                              Formula nameFormula, int propertySelection, Formula valueFormula) {
        SetTextPropertyAction action = action(SetTextPropertyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setNameFormula(nameFormula);
        action.setPropertySelection(propertySelection);
        action.setValueFormula(valueFormula);
        return action;
    }

    public Action createSetPenPropertyAction(Sprite sprite, SequenceAction sequence, int propertySelection, Formula valueFormula) {
        SetPenPropertyAction action = action(SetPenPropertyAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setPropertySelection(propertySelection);
        action.setValueFormula(valueFormula);
        return action;
    }

    public Action createPenDrawLineAction(Sprite sprite, SequenceAction sequence, Formula x1, Formula y1, Formula x2, Formula y2, Formula thick) {
        PenDrawLineAction action = action(PenDrawLineAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormulas(x1, y1, x2, y2, thick);
        return action;
    }

    public Action createPenDrawTriangleAction(Sprite sprite, SequenceAction sequence, Formula x1, Formula y1, Formula x2, Formula y2, Formula x3, Formula y3, Formula fill) {
        PenDrawTriangleAction action = action(PenDrawTriangleAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormulas(x1, y1, x2, y2, x3, y3, fill);
        return action;
    }

    public Action createPenClearColorAction(Sprite sprite, SequenceAction sequence, Formula color, Formula alpha) {
        PenClearColorAction action = action(PenClearColorAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormulas(color, alpha);
        return action;
    }

    public Action createPenFlushAction(Sprite sprite, SequenceAction sequence) {
        PenFlushAction action = action(PenFlushAction.class);
        return action;
    }

    public Action createPenDrawRectAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y, Formula width, Formula height, Formula fill) {
        PenDrawRectAction action = action(PenDrawRectAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormulas(x, y, width, height, fill);
        return action;
    }

    public Action createPenDrawCircleAction(Sprite sprite, SequenceAction sequence, Formula x, Formula y, Formula radius, Formula startAngle, Formula degrees, Formula fill) {
        PenDrawCircleAction action = action(PenDrawCircleAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setFormulas(x, y, radius, startAngle, degrees, fill);
        return action;
    }

    public Action createApply3dTorqueAction(Sprite sprite, SequenceAction sequence,
                                            Formula objectId, Formula x, Formula y, Formula z) {
        Apply3dTorqueAction action = action(Apply3dTorqueAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setObjectId(objectId);
        action.setX(x);
        action.setY(y);
        action.setZ(z);
        return action;
    }

    public Action createSpringConstraintAction(Sprite sprite, SequenceAction sequence,
                                               Formula constraintId, Formula objectIdA, Formula objectIdB,
                                               Formula pivotAx, Formula pivotAy, Formula pivotAz,
                                               Formula pivotBx, Formula pivotBy, Formula pivotBz) {
        CreateSpringConstraintAction action = action(CreateSpringConstraintAction.class);
        Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, sequence);
        action.setScope(scope);
        action.setConstraintId(constraintId);
        action.setObjectIdA(objectIdA);
        action.setObjectIdB(objectIdB);
        action.setPivotAx(pivotAx);
        action.setPivotAy(pivotAy);
        action.setPivotAz(pivotAz);
        action.setPivotBx(pivotBx);
        action.setPivotBy(pivotBy);
        action.setPivotBz(pivotBz);
        return action;
    }
}
