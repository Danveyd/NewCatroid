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
package org.catrobat.catroid.ui.fragment

import android.content.Context
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.BrickValues
import org.catrobat.catroid.content.BroadcastScript
import org.catrobat.catroid.content.BroadcastWithParamsReceiverScript
import org.catrobat.catroid.content.RaspiInterruptScript
import org.catrobat.catroid.content.WhenBounceOffScript
import org.catrobat.catroid.content.WhenClonedWithNameScript
import org.catrobat.catroid.content.WhenConditionScript
import org.catrobat.catroid.content.WhenGamepadButtonScript
import org.catrobat.catroid.content.WhenMqttMessageScript
import org.catrobat.catroid.content.WhenNotificationActionTriggeredScript
import org.catrobat.catroid.content.WhenNotificationClickedScript
import org.catrobat.catroid.content.actions.NativeViewBindSpriteAction
import org.catrobat.catroid.content.actions.SetAnimationSpeedAction
import org.catrobat.catroid.content.actions.hideStatusBarAction
import org.catrobat.catroid.content.bricks.AddEditBrick
import org.catrobat.catroid.content.bricks.AddFileToApkBrick
import org.catrobat.catroid.content.bricks.AddHingeBrick
import org.catrobat.catroid.content.bricks.AddItemToUserListBrick
import org.catrobat.catroid.content.bricks.AddRadioBrick
import org.catrobat.catroid.content.bricks.AddTextToBufferBrick
import org.catrobat.catroid.content.bricks.AddToBufferBrick
import org.catrobat.catroid.content.bricks.Apply3dForceBrick
import org.catrobat.catroid.content.bricks.Apply3dTorqueBrick
import org.catrobat.catroid.content.bricks.ApplyAngularImpulseBrick
import org.catrobat.catroid.content.bricks.ApplyBufferLookBrick
import org.catrobat.catroid.content.bricks.ApplyForceBrick
import org.catrobat.catroid.content.bricks.ApplyImpulseBrick
import org.catrobat.catroid.content.bricks.ApplyShaderToImageBrick
import org.catrobat.catroid.content.bricks.ApplyTorqueBrick
import org.catrobat.catroid.content.bricks.ArduinoSendDigitalValueBrick
import org.catrobat.catroid.content.bricks.ArduinoSendPWMValueBrick
import org.catrobat.catroid.content.bricks.AskBrick
import org.catrobat.catroid.content.bricks.AskGPTBrick
import org.catrobat.catroid.content.bricks.AskGemini2Brick
import org.catrobat.catroid.content.bricks.AskGeminiBrick
import org.catrobat.catroid.content.bricks.AskSpeechBrick
import org.catrobat.catroid.content.bricks.AssertEqualsBrick
import org.catrobat.catroid.content.bricks.AssertUserListsBrick
import org.catrobat.catroid.content.bricks.AsyncRepeatBrick
import org.catrobat.catroid.content.bricks.AttachRaySensorBrick
import org.catrobat.catroid.content.bricks.AttachSOBrick
import org.catrobat.catroid.content.bricks.AttachToCameraBrick
import org.catrobat.catroid.content.bricks.BackgroundRequestBrick
import org.catrobat.catroid.content.bricks.BakeByPrefixBrick
import org.catrobat.catroid.content.bricks.Base64ToFileBrick
import org.catrobat.catroid.content.bricks.BigAskBrick
import org.catrobat.catroid.content.bricks.BindVmOutputBrick
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.BroadcastBrick
import org.catrobat.catroid.content.bricks.BroadcastReceiverBrick
import org.catrobat.catroid.content.bricks.BroadcastWaitBrick
import org.catrobat.catroid.content.bricks.BroadcastWithParamsBrick
import org.catrobat.catroid.content.bricks.BroadcastWithParamsReceiverBrick
import org.catrobat.catroid.content.bricks.BufferMaskBrick
import org.catrobat.catroid.content.bricks.CameraBrick
import org.catrobat.catroid.content.bricks.CameraLookAtBrick
import org.catrobat.catroid.content.bricks.CameraSettingsBrick
import org.catrobat.catroid.content.bricks.CameraTouchControlBrick
import org.catrobat.catroid.content.bricks.CameraTrackingBrick
import org.catrobat.catroid.content.bricks.CastRayBrick
import org.catrobat.catroid.content.bricks.ChangeBrightnessByNBrick
import org.catrobat.catroid.content.bricks.ChangeColorByNBrick
import org.catrobat.catroid.content.bricks.ChangeHeightBrick
import org.catrobat.catroid.content.bricks.ChangeSizeByNBrick
import org.catrobat.catroid.content.bricks.ChangeTempoByNBrick
import org.catrobat.catroid.content.bricks.ChangeTransparencyByNBrick
import org.catrobat.catroid.content.bricks.ChangeVariableBrick
import org.catrobat.catroid.content.bricks.ChangeVolumeByNBrick
import org.catrobat.catroid.content.bricks.ChangeWidthBrick
import org.catrobat.catroid.content.bricks.ChangeXByNBrick
import org.catrobat.catroid.content.bricks.ChangeYByNBrick
import org.catrobat.catroid.content.bricks.ChooseCameraBrick
import org.catrobat.catroid.content.bricks.ChooseFileBrick
import org.catrobat.catroid.content.bricks.ClearBackgroundBrick
import org.catrobat.catroid.content.bricks.ClearGraphicEffectBrick
import org.catrobat.catroid.content.bricks.ClearPythonEnvironmentBrick
import org.catrobat.catroid.content.bricks.ClearSceneBrick
import org.catrobat.catroid.content.bricks.ClearUserListBrick
import org.catrobat.catroid.content.bricks.CloneAndNameBrick
import org.catrobat.catroid.content.bricks.CloneBrick
import org.catrobat.catroid.content.bricks.CloneObjectBrick
import org.catrobat.catroid.content.bricks.ComeToFrontBrick
import org.catrobat.catroid.content.bricks.ConfigureLightBrick
import org.catrobat.catroid.content.bricks.ConfigureMaterialBrick
import org.catrobat.catroid.content.bricks.ConfigureParticlesBrick
import org.catrobat.catroid.content.bricks.ConnectServerBrick
import org.catrobat.catroid.content.bricks.CopyLookBrick
import org.catrobat.catroid.content.bricks.CopyProjectFileBrick
import org.catrobat.catroid.content.bricks.CopyTextBrick
import org.catrobat.catroid.content.bricks.Create3dObjectBrick
import org.catrobat.catroid.content.bricks.CreateBufferBrick
import org.catrobat.catroid.content.bricks.CreateButtonBrick
import org.catrobat.catroid.content.bricks.CreateCubeBrick
import org.catrobat.catroid.content.bricks.CreateDialogBrick
import org.catrobat.catroid.content.bricks.CreateDiskBrick
import org.catrobat.catroid.content.bricks.CreateDistanceJointBrick
import org.catrobat.catroid.content.bricks.CreateFloatBrick
import org.catrobat.catroid.content.bricks.CreateGLViewBrick
import org.catrobat.catroid.content.bricks.CreateGearJointBrick
import org.catrobat.catroid.content.bricks.CreateImageViewBrick
import org.catrobat.catroid.content.bricks.CreateParticlesBrick
import org.catrobat.catroid.content.bricks.CreatePointJointBrick
import org.catrobat.catroid.content.bricks.CreatePrismaticJointBrick
import org.catrobat.catroid.content.bricks.CreatePulleyJointBrick
import org.catrobat.catroid.content.bricks.CreateRevoluteJointBrick
import org.catrobat.catroid.content.bricks.CreateScrollViewBrick
import org.catrobat.catroid.content.bricks.CreateSliderBrick
import org.catrobat.catroid.content.bricks.CreateSphereBrick
import org.catrobat.catroid.content.bricks.CreateSpringConstraintBrick
import org.catrobat.catroid.content.bricks.CreateSwitchBrick
import org.catrobat.catroid.content.bricks.CreateTableBrick
import org.catrobat.catroid.content.bricks.CreateTextFieldBrick
import org.catrobat.catroid.content.bricks.CreateTextLabelBrick
import org.catrobat.catroid.content.bricks.CreateVarBrick
import org.catrobat.catroid.content.bricks.CreateVideoBrick
import org.catrobat.catroid.content.bricks.CreateWebFileBrick
import org.catrobat.catroid.content.bricks.CreateWebUrlBrick
import org.catrobat.catroid.content.bricks.CreateWeldJointBrick
import org.catrobat.catroid.content.bricks.CustomBrick
import org.catrobat.catroid.content.bricks.CutLookBrick
import org.catrobat.catroid.content.bricks.DelSquareBrick
import org.catrobat.catroid.content.bricks.DeleteAllTablesBrick
import org.catrobat.catroid.content.bricks.DeleteBaseBrick
import org.catrobat.catroid.content.bricks.DeleteFilesBrick
import org.catrobat.catroid.content.bricks.DeleteFloatBrick
import org.catrobat.catroid.content.bricks.DeleteFromApkBrick
import org.catrobat.catroid.content.bricks.DeleteItemOfUserListBrick
import org.catrobat.catroid.content.bricks.DeleteLookBrick
import org.catrobat.catroid.content.bricks.DeleteParticlesBrick
import org.catrobat.catroid.content.bricks.DeleteTableBrick
import org.catrobat.catroid.content.bricks.DeleteThisCloneBrick
import org.catrobat.catroid.content.bricks.DeleteVarBrick
import org.catrobat.catroid.content.bricks.DeleteVarsBrick
import org.catrobat.catroid.content.bricks.DeleteWebBrick
import org.catrobat.catroid.content.bricks.DestroyJointBrick
import org.catrobat.catroid.content.bricks.DetachFromCameraBrick
import org.catrobat.catroid.content.bricks.DisableBackgroundModeBrick
import org.catrobat.catroid.content.bricks.DroneEmergencyBrick
import org.catrobat.catroid.content.bricks.DroneFlipBrick
import org.catrobat.catroid.content.bricks.DroneMoveBackwardBrick
import org.catrobat.catroid.content.bricks.DroneMoveDownBrick
import org.catrobat.catroid.content.bricks.DroneMoveForwardBrick
import org.catrobat.catroid.content.bricks.DroneMoveLeftBrick
import org.catrobat.catroid.content.bricks.DroneMoveRightBrick
import org.catrobat.catroid.content.bricks.DroneMoveUpBrick
import org.catrobat.catroid.content.bricks.DronePlayLedAnimationBrick
import org.catrobat.catroid.content.bricks.DroneSwitchCameraBrick
import org.catrobat.catroid.content.bricks.DroneTakeOffLandBrick
import org.catrobat.catroid.content.bricks.DroneTurnLeftBrick
import org.catrobat.catroid.content.bricks.DroneTurnRightBrick
import org.catrobat.catroid.content.bricks.Ease3DPropertyBrick
import org.catrobat.catroid.content.bricks.EasePropertyBrick
import org.catrobat.catroid.content.bricks.EditLookBrick
import org.catrobat.catroid.content.bricks.EnableBackgroundModeBrick
import org.catrobat.catroid.content.bricks.EnablePbrRenderBrick
import org.catrobat.catroid.content.bricks.EvalWebBrick
import org.catrobat.catroid.content.bricks.ExitStageBrick
import org.catrobat.catroid.content.bricks.Export3dObjectToGlbBrick
import org.catrobat.catroid.content.bricks.ExportProjectFileBrick
import org.catrobat.catroid.content.bricks.ExtractFileBrick
import org.catrobat.catroid.content.bricks.FadeParticleEffectBrick
import org.catrobat.catroid.content.bricks.Fast2DApplyForceBrick
import org.catrobat.catroid.content.bricks.Fast2DApplyImpulseBrick
import org.catrobat.catroid.content.bricks.Fast2DCreateBrick
import org.catrobat.catroid.content.bricks.Fast2DDeleteBrick
import org.catrobat.catroid.content.bricks.Fast2DMakePhysicsBrick
import org.catrobat.catroid.content.bricks.Fast2DSetAngularVelocityBrick
import org.catrobat.catroid.content.bricks.Fast2DSetCameraBrick
import org.catrobat.catroid.content.bricks.Fast2DSetCollisionFilterBrick
import org.catrobat.catroid.content.bricks.Fast2DSetColorBrick
import org.catrobat.catroid.content.bricks.Fast2DSetGravityBrick
import org.catrobat.catroid.content.bricks.Fast2DSetPhysicsVelocityBrick
import org.catrobat.catroid.content.bricks.Fast2DSetPositionBrick
import org.catrobat.catroid.content.bricks.Fast2DSetRotationBrick
import org.catrobat.catroid.content.bricks.Fast2DSetScaleBrick
import org.catrobat.catroid.content.bricks.Fast2DSetTextureBrick
import org.catrobat.catroid.content.bricks.Fast2DSetVelocityBrick
import org.catrobat.catroid.content.bricks.FileUrlBrick
import org.catrobat.catroid.content.bricks.FilesUrlBrick
import org.catrobat.catroid.content.bricks.FinishStageBrick
import org.catrobat.catroid.content.bricks.FlashBrick
import org.catrobat.catroid.content.bricks.ForItemInUserListBrick
import org.catrobat.catroid.content.bricks.ForVariableFromToBrick
import org.catrobat.catroid.content.bricks.ForeverBrick
import org.catrobat.catroid.content.bricks.GenerateKeyBrick
import org.catrobat.catroid.content.bricks.GetZipFileNamesBrick
import org.catrobat.catroid.content.bricks.GlideToBrick
import org.catrobat.catroid.content.bricks.GoNStepsBackBrick
import org.catrobat.catroid.content.bricks.GoToBrick
import org.catrobat.catroid.content.bricks.GrayscaleImgBrick
import org.catrobat.catroid.content.bricks.HideBrick
import org.catrobat.catroid.content.bricks.HideStatusBarBrick
import org.catrobat.catroid.content.bricks.HideText3Brick
import org.catrobat.catroid.content.bricks.HideTextBrick
import org.catrobat.catroid.content.bricks.HttpAttachFileBrick
import org.catrobat.catroid.content.bricks.HttpBodyBrick
import org.catrobat.catroid.content.bricks.HttpConfigBrick
import org.catrobat.catroid.content.bricks.HttpCreateBrick
import org.catrobat.catroid.content.bricks.HttpSaveFileBrick
import org.catrobat.catroid.content.bricks.HttpSendBrick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.IfOnEdgeBounceBrick
import org.catrobat.catroid.content.bricks.IfThenLogicBeginBrick
import org.catrobat.catroid.content.bricks.InsertItemIntoUserListBrick
import org.catrobat.catroid.content.bricks.InsertTableBrick
import org.catrobat.catroid.content.bricks.InstantBrick
import org.catrobat.catroid.content.bricks.IntervalRepeatBrick
import org.catrobat.catroid.content.bricks.JavaCompileBrick
import org.catrobat.catroid.content.bricks.JavaDownloadDepBrick
import org.catrobat.catroid.content.bricks.JavaLoadAndRunBrick
import org.catrobat.catroid.content.bricks.JumpingSumoAnimationsBrick
import org.catrobat.catroid.content.bricks.JumpingSumoJumpHighBrick
import org.catrobat.catroid.content.bricks.JumpingSumoJumpLongBrick
import org.catrobat.catroid.content.bricks.JumpingSumoMoveBackwardBrick
import org.catrobat.catroid.content.bricks.JumpingSumoMoveForwardBrick
import org.catrobat.catroid.content.bricks.JumpingSumoNoSoundBrick
import org.catrobat.catroid.content.bricks.JumpingSumoRotateLeftBrick
import org.catrobat.catroid.content.bricks.JumpingSumoRotateRightBrick
import org.catrobat.catroid.content.bricks.JumpingSumoSoundBrick
import org.catrobat.catroid.content.bricks.JumpingSumoTakingPictureBrick
import org.catrobat.catroid.content.bricks.JumpingSumoTurnBrick
import org.catrobat.catroid.content.bricks.KeyEventBrick
import org.catrobat.catroid.content.bricks.KeyframeAnimationBrick
import org.catrobat.catroid.content.bricks.LaunchProjectBrick
import org.catrobat.catroid.content.bricks.LegoEv3MotorMoveBrick
import org.catrobat.catroid.content.bricks.LegoEv3MotorStopBrick
import org.catrobat.catroid.content.bricks.LegoEv3MotorTurnAngleBrick
import org.catrobat.catroid.content.bricks.LegoEv3PlayToneBrick
import org.catrobat.catroid.content.bricks.LegoEv3SetLedBrick
import org.catrobat.catroid.content.bricks.LegoNxtMotorMoveBrick
import org.catrobat.catroid.content.bricks.LegoNxtMotorStopBrick
import org.catrobat.catroid.content.bricks.LegoNxtMotorTurnAngleBrick
import org.catrobat.catroid.content.bricks.LegoNxtPlayToneBrick
import org.catrobat.catroid.content.bricks.ListenMicroBrick
import org.catrobat.catroid.content.bricks.ListenServerBrick
import org.catrobat.catroid.content.bricks.LoadFromInternalStorageBrick
import org.catrobat.catroid.content.bricks.LoadNNBrick
import org.catrobat.catroid.content.bricks.LoadNativeModuleBrick
import org.catrobat.catroid.content.bricks.LoadPythonLibraryBrick
import org.catrobat.catroid.content.bricks.LoadSceneAdditiveBrick
import org.catrobat.catroid.content.bricks.LoadSceneBrick
import org.catrobat.catroid.content.bricks.LockMouseBrick
import org.catrobat.catroid.content.bricks.LookFileBrick
import org.catrobat.catroid.content.bricks.LookFromTableBrick
import org.catrobat.catroid.content.bricks.LookRequestBrick
import org.catrobat.catroid.content.bricks.LookToTableBrick
import org.catrobat.catroid.content.bricks.LunoScriptBrick
import org.catrobat.catroid.content.bricks.MLLoadBrick
import org.catrobat.catroid.content.bricks.MLSaveBrick
import org.catrobat.catroid.content.bricks.MLStepAdamBrick
import org.catrobat.catroid.content.bricks.MLStepAdamWBrick
import org.catrobat.catroid.content.bricks.MouseEventBrick
import org.catrobat.catroid.content.bricks.MoveDownloadsBrick
import org.catrobat.catroid.content.bricks.MoveFilesBrick
import org.catrobat.catroid.content.bricks.MoveNStepsBrick
import org.catrobat.catroid.content.bricks.MqttConnectBrick
import org.catrobat.catroid.content.bricks.MqttDisconnectBrick
import org.catrobat.catroid.content.bricks.MqttJoinRoomBrick
import org.catrobat.catroid.content.bricks.MqttPublishBrick
import org.catrobat.catroid.content.bricks.NativeLayerBrick
import org.catrobat.catroid.content.bricks.NativeViewAnimateBrick
import org.catrobat.catroid.content.bricks.NativeViewBindSpriteBrick
import org.catrobat.catroid.content.bricks.NativeViewConfigBrick
import org.catrobat.catroid.content.bricks.NativeViewControlBrick
import org.catrobat.catroid.content.bricks.NativeViewListenerBrick
import org.catrobat.catroid.content.bricks.NextLookBrick
import org.catrobat.catroid.content.bricks.NormalizeImgBrick
import org.catrobat.catroid.content.bricks.NoteBrick
import org.catrobat.catroid.content.bricks.NotificationAddButtonBrick
import org.catrobat.catroid.content.bricks.NotificationCancelBrick
import org.catrobat.catroid.content.bricks.NotificationCreateBrick
import org.catrobat.catroid.content.bricks.NotificationShowBrick
import org.catrobat.catroid.content.bricks.ObjectLookAtBrick
import org.catrobat.catroid.content.bricks.ObjectLookAtCorrectBrick
import org.catrobat.catroid.content.bricks.OpenAppBrick
import org.catrobat.catroid.content.bricks.OpenFileBrick
import org.catrobat.catroid.content.bricks.OpenFilesBrick
import org.catrobat.catroid.content.bricks.OpenUrlBrick
import org.catrobat.catroid.content.bricks.OrientationBrick
import org.catrobat.catroid.content.bricks.PaintNewLookBrick
import org.catrobat.catroid.content.bricks.ParameterizedBrick
import org.catrobat.catroid.content.bricks.ParameterizedEndBrick
import org.catrobat.catroid.content.bricks.ParticleEffectAdditivityBrick
import org.catrobat.catroid.content.bricks.PauseForBeatsBrick
import org.catrobat.catroid.content.bricks.PauseVideoBrick
import org.catrobat.catroid.content.bricks.PenClearColorBrick
import org.catrobat.catroid.content.bricks.PenDownBrick
import org.catrobat.catroid.content.bricks.PenDrawCircleBrick
import org.catrobat.catroid.content.bricks.PenDrawLineBrick
import org.catrobat.catroid.content.bricks.PenDrawRectBrick
import org.catrobat.catroid.content.bricks.PenDrawTriangleBrick
import org.catrobat.catroid.content.bricks.PenFlushBrick
import org.catrobat.catroid.content.bricks.PenUpBrick
import org.catrobat.catroid.content.bricks.PerformRayCastBrick
import org.catrobat.catroid.content.bricks.PhiroIfLogicBeginBrick
import org.catrobat.catroid.content.bricks.PhiroMotorMoveBackwardBrick
import org.catrobat.catroid.content.bricks.PhiroMotorMoveForwardBrick
import org.catrobat.catroid.content.bricks.PhiroMotorStopBrick
import org.catrobat.catroid.content.bricks.PhiroPlayToneBrick
import org.catrobat.catroid.content.bricks.PhiroRGBLightBrick
import org.catrobat.catroid.content.bricks.PhotoBrick
import org.catrobat.catroid.content.bricks.PinToCameraBrick
import org.catrobat.catroid.content.bricks.PlaceAtBrick
import org.catrobat.catroid.content.bricks.PlayAnimationBrick
import org.catrobat.catroid.content.bricks.PlayDrumForBeatsBrick
import org.catrobat.catroid.content.bricks.PlayGifBrick
import org.catrobat.catroid.content.bricks.PlayNoteForBeatsBrick
import org.catrobat.catroid.content.bricks.PlayPreparedSoundBrick
import org.catrobat.catroid.content.bricks.PlaySoundAndWaitBrick
import org.catrobat.catroid.content.bricks.PlaySoundAtBrick
import org.catrobat.catroid.content.bricks.PlaySoundAtPositionBrick
import org.catrobat.catroid.content.bricks.PlaySoundBrick
import org.catrobat.catroid.content.bricks.PlaySpritesheetBrick
import org.catrobat.catroid.content.bricks.PlayVideoBrick
import org.catrobat.catroid.content.bricks.PointInDirectionBrick
import org.catrobat.catroid.content.bricks.PointToBrick
import org.catrobat.catroid.content.bricks.PostWebRequestBrick
import org.catrobat.catroid.content.bricks.PredictNNBrick
import org.catrobat.catroid.content.bricks.PrepareMusicAs3DSoundBrick
import org.catrobat.catroid.content.bricks.PrepareSoundBrick
import org.catrobat.catroid.content.bricks.PrepareSoundBrick2
import org.catrobat.catroid.content.bricks.PreviousLookBrick
import org.catrobat.catroid.content.bricks.PromoteLightBrick
import org.catrobat.catroid.content.bricks.PtAttentionBrick
import org.catrobat.catroid.content.bricks.PtBackwardBrick
import org.catrobat.catroid.content.bricks.PtClipGradBrick
import org.catrobat.catroid.content.bricks.PtConv2DBrick
import org.catrobat.catroid.content.bricks.PtCreateNormalTensorBrick
import org.catrobat.catroid.content.bricks.PtCreateTensorBrick
import org.catrobat.catroid.content.bricks.PtDropoutBrick
import org.catrobat.catroid.content.bricks.PtEmbeddingBrick
import org.catrobat.catroid.content.bricks.PtGruCellBrick
import org.catrobat.catroid.content.bricks.PtLayerLinearBrick
import org.catrobat.catroid.content.bricks.PtLstmCellBrick
import org.catrobat.catroid.content.bricks.PtMaxPool2DBrick
import org.catrobat.catroid.content.bricks.PtOpBrick
import org.catrobat.catroid.content.bricks.PtReshapeBrick
import org.catrobat.catroid.content.bricks.PtSetByIndexBrick
import org.catrobat.catroid.content.bricks.PtSetTensorBrick
import org.catrobat.catroid.content.bricks.PtSetTrainingBrick
import org.catrobat.catroid.content.bricks.PtSliceBrick
import org.catrobat.catroid.content.bricks.PtStepBrick
import org.catrobat.catroid.content.bricks.PtZeroGradBrick
import org.catrobat.catroid.content.bricks.PutFloatBrick
import org.catrobat.catroid.content.bricks.RaspiIfLogicBeginBrick
import org.catrobat.catroid.content.bricks.RaspiPwmBrick
import org.catrobat.catroid.content.bricks.RaspiSendDigitalValueBrick
import org.catrobat.catroid.content.bricks.ReadBaseBrick
import org.catrobat.catroid.content.bricks.ReadFromFilesBrick
import org.catrobat.catroid.content.bricks.ReadListFromDeviceBrick
import org.catrobat.catroid.content.bricks.ReadVariableFromDeviceBrick
import org.catrobat.catroid.content.bricks.ReadVariableFromFileBrick
import org.catrobat.catroid.content.bricks.RegexBrick
import org.catrobat.catroid.content.bricks.Remove3dObjectBrick
import org.catrobat.catroid.content.bricks.RemoveFromBufferBrick
import org.catrobat.catroid.content.bricks.RemoveJointBrick
import org.catrobat.catroid.content.bricks.RemoveObjectsByPrefixBrick
import org.catrobat.catroid.content.bricks.RemoveParentBrick
import org.catrobat.catroid.content.bricks.RemovePbrLightBrick
import org.catrobat.catroid.content.bricks.RepeatBrick
import org.catrobat.catroid.content.bricks.RepeatUntilBrick
import org.catrobat.catroid.content.bricks.Replace3DModelBrick
import org.catrobat.catroid.content.bricks.ReplaceItemInUserListBrick
import org.catrobat.catroid.content.bricks.ReportBrick
import org.catrobat.catroid.content.bricks.ResetTimerBrick
import org.catrobat.catroid.content.bricks.ResizeImgBrick
import org.catrobat.catroid.content.bricks.ReturnToPreviousProjectBrick
import org.catrobat.catroid.content.bricks.RotateCameraByBrick
import org.catrobat.catroid.content.bricks.RunAsSpriteBrick
import org.catrobat.catroid.content.bricks.RunChip8Brick
import org.catrobat.catroid.content.bricks.RunJSBrick
import org.catrobat.catroid.content.bricks.RunLuaBrick
import org.catrobat.catroid.content.bricks.RunPythonScriptBrick
import org.catrobat.catroid.content.bricks.RunShellBrick
import org.catrobat.catroid.content.bricks.RunVMBrick
import org.catrobat.catroid.content.bricks.RunVm2Brick
import org.catrobat.catroid.content.bricks.RunningStitchBrick
import org.catrobat.catroid.content.bricks.SaveBufferBrick
import org.catrobat.catroid.content.bricks.SaveLookBrick
import org.catrobat.catroid.content.bricks.SaveLookFilesBrick
import org.catrobat.catroid.content.bricks.SavePlotBrick
import org.catrobat.catroid.content.bricks.SaveToInternalStorageBrick
import org.catrobat.catroid.content.bricks.SayBubbleBrick
import org.catrobat.catroid.content.bricks.SayForBubbleBrick
import org.catrobat.catroid.content.bricks.SceneIdBrick
import org.catrobat.catroid.content.bricks.SceneStartBrick
import org.catrobat.catroid.content.bricks.SceneTransitionBrick
import org.catrobat.catroid.content.bricks.ScreenShotBrick
import org.catrobat.catroid.content.bricks.SeekVideoBrick
import org.catrobat.catroid.content.bricks.SendServerBrick
import org.catrobat.catroid.content.bricks.SendVmInputBrick
import org.catrobat.catroid.content.bricks.Set3DSoundMaxDistanceBrick
import org.catrobat.catroid.content.bricks.Set3DSoundPositionBrick
import org.catrobat.catroid.content.bricks.Set3dFrictionBrick
import org.catrobat.catroid.content.bricks.Set3dGravityBrick
import org.catrobat.catroid.content.bricks.Set3dPositionBrick
import org.catrobat.catroid.content.bricks.Set3dRotationBrick
import org.catrobat.catroid.content.bricks.Set3dScaleBrick
import org.catrobat.catroid.content.bricks.Set3dVelocityBrick
import org.catrobat.catroid.content.bricks.SetAIBrick
import org.catrobat.catroid.content.bricks.SetActiveBrick
import org.catrobat.catroid.content.bricks.SetAmbientLightBrick
import org.catrobat.catroid.content.bricks.SetAnimationSpeedBrick
import org.catrobat.catroid.content.bricks.SetAnisotropicFilterBrick
import org.catrobat.catroid.content.bricks.SetBackgroundAndWaitBrick
import org.catrobat.catroid.content.bricks.SetBackgroundBrick
import org.catrobat.catroid.content.bricks.SetBackgroundByIndexAndWaitBrick
import org.catrobat.catroid.content.bricks.SetBackgroundByIndexBrick
import org.catrobat.catroid.content.bricks.SetBackgroundLightBrick
import org.catrobat.catroid.content.bricks.SetBounceBrick
import org.catrobat.catroid.content.bricks.SetBrightnessBrick
import org.catrobat.catroid.content.bricks.SetBufferAutoUpdateBrick
import org.catrobat.catroid.content.bricks.SetBufferCamera3DBrick
import org.catrobat.catroid.content.bricks.SetBufferCameraBrick
import org.catrobat.catroid.content.bricks.SetBufferEffectsBrick
import org.catrobat.catroid.content.bricks.SetBufferModeBrick
import org.catrobat.catroid.content.bricks.SetBufferOnlyBrick
import org.catrobat.catroid.content.bricks.SetBufferShaderBrick
import org.catrobat.catroid.content.bricks.SetBufferShaderUniformBrick
import org.catrobat.catroid.content.bricks.SetCCDBrick
import org.catrobat.catroid.content.bricks.SetCallbackBrick
import org.catrobat.catroid.content.bricks.SetCameraFocusPointBrick
import org.catrobat.catroid.content.bricks.SetCameraPosition2Brick
import org.catrobat.catroid.content.bricks.SetCameraPositionBrick
import org.catrobat.catroid.content.bricks.SetCameraRangeBrick
import org.catrobat.catroid.content.bricks.SetCameraRotation2Brick
import org.catrobat.catroid.content.bricks.SetCameraRotationBrick
import org.catrobat.catroid.content.bricks.SetCameraZoomBrick
import org.catrobat.catroid.content.bricks.SetColorBrick
import org.catrobat.catroid.content.bricks.SetCornerOffsetsBrick
import org.catrobat.catroid.content.bricks.SetDampingBrick
import org.catrobat.catroid.content.bricks.SetDirectionalLight2Brick
import org.catrobat.catroid.content.bricks.SetDirectionalLightBrick
import org.catrobat.catroid.content.bricks.SetDnsBrick
import org.catrobat.catroid.content.bricks.SetEmissiveBrick
import org.catrobat.catroid.content.bricks.SetFogBrick
import org.catrobat.catroid.content.bricks.SetFpsBrick
import org.catrobat.catroid.content.bricks.SetFreeCameraBrick
import org.catrobat.catroid.content.bricks.SetFrictionBrick
import org.catrobat.catroid.content.bricks.SetGeminiKeyBrick
import org.catrobat.catroid.content.bricks.SetGlobalSoundVolumeBrick
import org.catrobat.catroid.content.bricks.SetGravityBrick
import org.catrobat.catroid.content.bricks.SetHeightBrick
import org.catrobat.catroid.content.bricks.SetHingeMotorBrick
import org.catrobat.catroid.content.bricks.SetHitboxBrick
import org.catrobat.catroid.content.bricks.SetInstrumentBrick
import org.catrobat.catroid.content.bricks.SetListeningLanguageBrick
import org.catrobat.catroid.content.bricks.SetLookBrick
import org.catrobat.catroid.content.bricks.SetLookByIndexBrick
import org.catrobat.catroid.content.bricks.SetLookFilesBrick
import org.catrobat.catroid.content.bricks.SetMainRenderLoopsBrick
import org.catrobat.catroid.content.bricks.SetMassBrick
import org.catrobat.catroid.content.bricks.SetMaterialBrick
import org.catrobat.catroid.content.bricks.SetMaxPointLightsBrick
import org.catrobat.catroid.content.bricks.SetNativeParentBrick
import org.catrobat.catroid.content.bricks.SetNegativeBrick
import org.catrobat.catroid.content.bricks.SetNeutralBrick
import org.catrobat.catroid.content.bricks.SetNfcTagBrick
import org.catrobat.catroid.content.bricks.SetObjectColorBrick
import org.catrobat.catroid.content.bricks.SetObjectShaderBrick
import org.catrobat.catroid.content.bricks.SetObjectShaderUniformBrick
import org.catrobat.catroid.content.bricks.SetObjectTextureBrick
import org.catrobat.catroid.content.bricks.SetParentBrick
import org.catrobat.catroid.content.bricks.SetParticleColorBrick
import org.catrobat.catroid.content.bricks.SetParticleEmissionBrick
import org.catrobat.catroid.content.bricks.SetPenColorBrick
import org.catrobat.catroid.content.bricks.SetPenPropertyBrick
import org.catrobat.catroid.content.bricks.SetPenSizeBrick
import org.catrobat.catroid.content.bricks.SetPhysicsObjectTypeBrick
import org.catrobat.catroid.content.bricks.SetPhysicsStateBrick
import org.catrobat.catroid.content.bricks.SetPointLightBrick
import org.catrobat.catroid.content.bricks.SetPositiveBrick
import org.catrobat.catroid.content.bricks.SetPostProcessingBrick
import org.catrobat.catroid.content.bricks.SetPostProcessingNewBrick
import org.catrobat.catroid.content.bricks.SetRenderResolutionBrick
import org.catrobat.catroid.content.bricks.SetRestitutionBrick
import org.catrobat.catroid.content.bricks.SetRotationLockBrick
import org.catrobat.catroid.content.bricks.SetRotationStyleBrick
import org.catrobat.catroid.content.bricks.SetSaveScenesBrick
import org.catrobat.catroid.content.bricks.SetScreenShaderBrick
import org.catrobat.catroid.content.bricks.SetShaderCodeBrick
import org.catrobat.catroid.content.bricks.SetShaderUniformFloatBrick
import org.catrobat.catroid.content.bricks.SetShaderUniformVec3Brick
import org.catrobat.catroid.content.bricks.SetShadowQualityBrick
import org.catrobat.catroid.content.bricks.SetShadowsBrick
import org.catrobat.catroid.content.bricks.SetSizeToBrick
import org.catrobat.catroid.content.bricks.SetSkyColorBrick
import org.catrobat.catroid.content.bricks.SetSkyboxBrick
import org.catrobat.catroid.content.bricks.SetSoundInstancePitchBrick
import org.catrobat.catroid.content.bricks.SetSoundInstanceVolumeBrick
import org.catrobat.catroid.content.bricks.SetSoundVolumeBrick
import org.catrobat.catroid.content.bricks.SetSpawnInvisibleBrick
import org.catrobat.catroid.content.bricks.SetSpotLightBrick
import org.catrobat.catroid.content.bricks.SetStopSoundsBrick
import org.catrobat.catroid.content.bricks.SetTempoBrick
import org.catrobat.catroid.content.bricks.SetTextBufferOnlyBrick
import org.catrobat.catroid.content.bricks.SetTextPropertyBrick
import org.catrobat.catroid.content.bricks.SetTextureTilingBrick
import org.catrobat.catroid.content.bricks.SetThirdPersonCameraBrick
import org.catrobat.catroid.content.bricks.SetThreadColorBrick
import org.catrobat.catroid.content.bricks.SetTransparencyBrick
import org.catrobat.catroid.content.bricks.SetVariableBrick
import org.catrobat.catroid.content.bricks.SetVariableEasingBrick
import org.catrobat.catroid.content.bricks.SetVelocityBrick
import org.catrobat.catroid.content.bricks.SetViewPositionBrick
import org.catrobat.catroid.content.bricks.SetVolumeToBrick
import org.catrobat.catroid.content.bricks.SetWebBrick
import org.catrobat.catroid.content.bricks.SetWidthBrick
import org.catrobat.catroid.content.bricks.SetXBrick
import org.catrobat.catroid.content.bricks.SetYBrick
import org.catrobat.catroid.content.bricks.SewUpBrick
import org.catrobat.catroid.content.bricks.ShaderBrick
import org.catrobat.catroid.content.bricks.ShareBrick
import org.catrobat.catroid.content.bricks.ShowBrick
import org.catrobat.catroid.content.bricks.ShowDialogBrick
import org.catrobat.catroid.content.bricks.ShowNotificationBrick
import org.catrobat.catroid.content.bricks.ShowText3Brick
import org.catrobat.catroid.content.bricks.ShowTextBrick
import org.catrobat.catroid.content.bricks.ShowTextColorSizeAlignmentBrick
import org.catrobat.catroid.content.bricks.ShowTextFontBrick
import org.catrobat.catroid.content.bricks.ShowTextRotationBrick
import org.catrobat.catroid.content.bricks.ShowToastBlock
import org.catrobat.catroid.content.bricks.ShowVarFontBrick
import org.catrobat.catroid.content.bricks.SignApkBrick
import org.catrobat.catroid.content.bricks.SoundFileBrick
import org.catrobat.catroid.content.bricks.SoundFilesBrick
import org.catrobat.catroid.content.bricks.SpawnThreadBrick
import org.catrobat.catroid.content.bricks.SpeakAndWaitBrick
import org.catrobat.catroid.content.bricks.SpeakBrick
import org.catrobat.catroid.content.bricks.SplitBrick
import org.catrobat.catroid.content.bricks.SquareBrick
import org.catrobat.catroid.content.bricks.StampBrick
import org.catrobat.catroid.content.bricks.StartBufferRecordingBrick
import org.catrobat.catroid.content.bricks.StartListeningBrick
import org.catrobat.catroid.content.bricks.StartPlotBrick
import org.catrobat.catroid.content.bricks.StartRecordingBrick
import org.catrobat.catroid.content.bricks.StartServerBrick
import org.catrobat.catroid.content.bricks.StitchBrick
import org.catrobat.catroid.content.bricks.StopAllSoundsBrick
import org.catrobat.catroid.content.bricks.StopAnimationBrick
import org.catrobat.catroid.content.bricks.StopBufferRecordingBrick
import org.catrobat.catroid.content.bricks.StopGifBrick
import org.catrobat.catroid.content.bricks.StopPlotBrick
import org.catrobat.catroid.content.bricks.StopRecordingBrick
import org.catrobat.catroid.content.bricks.StopRunningStitchBrick
import org.catrobat.catroid.content.bricks.StopScriptBrick
import org.catrobat.catroid.content.bricks.StopServerBrick
import org.catrobat.catroid.content.bricks.StopSoundBrick
import org.catrobat.catroid.content.bricks.StopSoundBrick2
import org.catrobat.catroid.content.bricks.StopSpritesheetBrick
import org.catrobat.catroid.content.bricks.StopThreadBrick
import org.catrobat.catroid.content.bricks.StopVMBrick
import org.catrobat.catroid.content.bricks.StoreCSVIntoUserListBrick
import org.catrobat.catroid.content.bricks.StringToTableBrick
import org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick
import org.catrobat.catroid.content.bricks.TableToFloatBrick
import org.catrobat.catroid.content.bricks.TapAtBrick
import org.catrobat.catroid.content.bricks.TapForBrick
import org.catrobat.catroid.content.bricks.TestBrick
import org.catrobat.catroid.content.bricks.ThinkBubbleBrick
import org.catrobat.catroid.content.bricks.ThinkForBubbleBrick
import org.catrobat.catroid.content.bricks.ThreedAlignNormalBrick
import org.catrobat.catroid.content.bricks.ThreedAttachObjectToBoneBrick
import org.catrobat.catroid.content.bricks.ThreedBindBoneToObjectBrick
import org.catrobat.catroid.content.bricks.ThreedCreateCylinderBrick
import org.catrobat.catroid.content.bricks.ThreedCreateFixedConstraintBrick
import org.catrobat.catroid.content.bricks.ToggleDisplayBrick
import org.catrobat.catroid.content.bricks.TouchAndSlideBrick
import org.catrobat.catroid.content.bricks.TripleStitchBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick
import org.catrobat.catroid.content.bricks.TurnLeftBrick
import org.catrobat.catroid.content.bricks.TurnLeftSpeedBrick
import org.catrobat.catroid.content.bricks.TurnRightBrick
import org.catrobat.catroid.content.bricks.TurnRightSpeedBrick
import org.catrobat.catroid.content.bricks.UnloadNNBrick
import org.catrobat.catroid.content.bricks.UnlockMouseBrick
import org.catrobat.catroid.content.bricks.UnpinFromCameraBrick
import org.catrobat.catroid.content.bricks.UnzipBrick
import org.catrobat.catroid.content.bricks.UnzipProjectFilesBrick
import org.catrobat.catroid.content.bricks.UpdateManifestBrick
import org.catrobat.catroid.content.bricks.UploadFileBrick
import org.catrobat.catroid.content.bricks.UserDefinedBrick
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrick
import org.catrobat.catroid.content.bricks.VibrationBrick
import org.catrobat.catroid.content.bricks.VmRelativeMouseMoveBrick
import org.catrobat.catroid.content.bricks.VmSetMonitorSizeBrick
import org.catrobat.catroid.content.bricks.VoxelBuildBrick
import org.catrobat.catroid.content.bricks.VoxelConfigBrick
import org.catrobat.catroid.content.bricks.VoxelCreateWorldBrick
import org.catrobat.catroid.content.bricks.VoxelDeleteBrick
import org.catrobat.catroid.content.bricks.VoxelLoadStringBrick
import org.catrobat.catroid.content.bricks.VoxelSetBlockBrick
import org.catrobat.catroid.content.bricks.VoxelSetTransparentBrick
import org.catrobat.catroid.content.bricks.WaitBrick
import org.catrobat.catroid.content.bricks.WaitThreadBrick
import org.catrobat.catroid.content.bricks.WaitTillIdleBrick
import org.catrobat.catroid.content.bricks.WaitUntilBrick
import org.catrobat.catroid.content.bricks.WebRequestBrick
import org.catrobat.catroid.content.bricks.WhenAfterUpdateBrick
import org.catrobat.catroid.content.bricks.WhenAppMinimizedBrick
import org.catrobat.catroid.content.bricks.WhenAppRestoredBrick
import org.catrobat.catroid.content.bricks.WhenBackPressedBrick
import org.catrobat.catroid.content.bricks.WhenBackgroundChangesBrick
import org.catrobat.catroid.content.bricks.WhenBeforeUpdateBrick
import org.catrobat.catroid.content.bricks.WhenBounceOffBrick
import org.catrobat.catroid.content.bricks.WhenBrick
import org.catrobat.catroid.content.bricks.WhenClonedBrick
import org.catrobat.catroid.content.bricks.WhenClonedWithNameBrick
import org.catrobat.catroid.content.bricks.WhenConditionBrick
import org.catrobat.catroid.content.bricks.WhenFingerMovedOnScreenBrick
import org.catrobat.catroid.content.bricks.WhenFingerMovedOverSpriteBrick
import org.catrobat.catroid.content.bricks.WhenGamepadButtonBrick
import org.catrobat.catroid.content.bricks.WhenMouseButtonClickedBrick
import org.catrobat.catroid.content.bricks.WhenMouseWheelScrolledBrick
import org.catrobat.catroid.content.bricks.WhenMqttMessageBrick
import org.catrobat.catroid.content.bricks.WhenNfcBrick
import org.catrobat.catroid.content.bricks.WhenNotificationActionTriggeredBrick
import org.catrobat.catroid.content.bricks.WhenNotificationClickedBrick
import org.catrobat.catroid.content.bricks.WhenProjectExitsBrick
import org.catrobat.catroid.content.bricks.WhenRaspiPinChangedBrick
import org.catrobat.catroid.content.bricks.WhenSpriteReleasedBrick
import org.catrobat.catroid.content.bricks.WhenStartedBrick
import org.catrobat.catroid.content.bricks.WhenTouchDownBrick
import org.catrobat.catroid.content.bricks.WhenWindowResizedBrick
import org.catrobat.catroid.content.bricks.WriteBaseBrick
import org.catrobat.catroid.content.bricks.WriteEmbroideryToFileBrick
import org.catrobat.catroid.content.bricks.WriteListOnDeviceBrick
import org.catrobat.catroid.content.bricks.WriteToFilesBrick
import org.catrobat.catroid.content.bricks.WriteVariableOnDeviceBrick
import org.catrobat.catroid.content.bricks.WriteVariableToFileBrick
import org.catrobat.catroid.content.bricks.ZigZagStitchBrick
import org.catrobat.catroid.content.bricks.ZipBrick
import org.catrobat.catroid.content.bricks.ZipProjectFilesBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.Operators
import org.catrobat.catroid.formulaeditor.Sensors
import org.catrobat.catroid.libraries.CustomBrickManager
import org.catrobat.catroid.ui.controller.RecentBrickListManager
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import java.util.ArrayList
import java.util.Locale
import kotlin.contracts.contract

open class CategoryBricksFactory {

    open fun getBricks(category: String, isBackgroundSprite: Boolean, context: Context): List<Brick> {
        if (category == context.getString(R.string.category_favorites)) {
            return setupFavoriteBricksCategoryList(context, isBackgroundSprite)
        }
        if (category == context.getString(R.string.category_recently_used)) {
            return setupRecentBricksCategoryList(isBackgroundSprite)
        }
        if (category == context.getString(R.string.category_pen)) {
            return setupPenCategoryList(isBackgroundSprite)
        }

        val bricks = when (category) {
            context.getString(R.string.category_event) -> setupEventCategoryList(context, isBackgroundSprite)
            context.getString(R.string.category_control) -> setupControlCategoryList(context)
            context.getString(R.string.category_motion) -> setupMotionCategoryList(context, isBackgroundSprite)
            context.getString(R.string.category_sound) -> setupSoundCategoryList(context)
            context.getString(R.string.category_looks) -> setupLooksCategoryList(context, isBackgroundSprite)
            context.getString(R.string.category_user_bricks) -> setupUserBricksCategoryList()
            context.getString(R.string.category_data) -> setupDataCategoryList(context, isBackgroundSprite)
            context.getString(R.string.category_device) -> setupDeviceCategoryList(context, isBackgroundSprite)
            context.getString(R.string.category_lego_nxt) -> setupLegoNxtCategoryList()
            context.getString(R.string.category_lego_ev3) -> setupLegoEv3CategoryList()
            context.getString(R.string.category_arduino) -> setupArduinoCategoryList()
            context.getString(R.string.category_drone) -> setupDroneCategoryList()
            context.getString(R.string.category_jumping_sumo) -> setupJumpingSumoCategoryList()
            context.getString(R.string.category_phiro) -> setupPhiroProCategoryList()
            context.getString(R.string.category_cast) -> setupChromecastCategoryList(context)
            context.getString(R.string.category_raspi) -> setupRaspiCategoryList()
            context.getString(R.string.category_embroidery) -> setupEmbroideryCategoryList(context)
            context.getString(R.string.category_plot) -> setupPlotCategoryList(context)
            context.getString(R.string.category_neural) -> setupNeuralCategoryList(context)
            context.getString(R.string.pocketensor) -> setupPocketensorCategoryList(context)
            context.getString(R.string.fast2d) -> setupFast2dCategoryList(context)
            context.getString(R.string.category_file) -> setupFileCategoryList(context)
            context.getString(R.string.category_threed) -> setupThreedCategoryList(context)
            context.getString(R.string.category_internet) -> setupInternetCategoryList(context)
            context.getString(R.string.category_assertions) -> setupAssertionsCategoryList(context)
            context.getString(R.string.category_libraries) -> setupLibrariesCategoryList()
            else -> emptyList()
        }

        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                return bricks.filter { it !is org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick }
            }
        }

        return bricks
    }

    private fun getPreferenceKeyForCategory(category: String, context: Context): String? {
        return when (category) {
            context.getString(R.string.category_event) -> "pref_grouping_event"
            context.getString(R.string.category_control) -> "pref_grouping_control"
            context.getString(R.string.category_motion) -> "pref_grouping_motion"
            context.getString(R.string.category_sound) -> "pref_grouping_sound"
            context.getString(R.string.category_looks) -> "pref_grouping_looks"
            context.getString(R.string.category_data) -> "pref_grouping_data"
            context.getString(R.string.category_device) -> "pref_grouping_device"
            context.getString(R.string.category_threed) -> "pref_grouping_threed"
            else -> null
        }
    }

    private fun setupLibrariesCategoryList(): List<Brick> {
        val libraryBricks = mutableListOf<Brick>()

        val definitions = CustomBrickManager.getAllDefinitions()

        for (def in definitions) {
            val brick = CustomBrick(def.id)
            libraryBricks.add(brick)
        }

        return libraryBricks
    }

    fun setupRecentBricksCategoryList(isBackgroundSprite: Boolean): List<Brick> = RecentBrickListManager.getInstance().getRecentBricks(isBackgroundSprite)

    protected open fun setupEventCategoryList(
        context: Context,
        isBackgroundSprite: Boolean
    ): List<Brick> {
        val category = context.getString(R.string.category_event)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val defaultIf = FormulaElement(
                    FormulaElement.ElementType.OPERATOR,
                    Operators.SMALLER_THAN.toString(),
                    null
                )
                defaultIf.setLeftChild(FormulaElement(FormulaElement.ElementType.NUMBER, "1", null))
                defaultIf.setRightChild(FormulaElement(FormulaElement.ElementType.NUMBER, "2", null))
                val eventBrickList: MutableList<Brick> = ArrayList()
                eventBrickList.add(WhenStartedBrick())
                eventBrickList.add(WhenBrick())
                eventBrickList.add(WhenTouchDownBrick())
                eventBrickList.add(WhenSpriteReleasedBrick())
                eventBrickList.add(WhenFingerMovedOverSpriteBrick())
                eventBrickList.add(WhenFingerMovedOnScreenBrick())
                val broadcastMessages =
                    ProjectManager.getInstance().currentProject?.broadcastMessageContainer?.broadcastMessages
                var broadcastMessage: String? = context.getString(R.string.brick_broadcast_default_value)
                if (broadcastMessages != null && broadcastMessages.size > 0) {
                    broadcastMessage = broadcastMessages[0]
                }
                eventBrickList.add(BroadcastReceiverBrick(BroadcastScript(broadcastMessage)))
                eventBrickList.add(BroadcastBrick(broadcastMessage))
                eventBrickList.add(BroadcastWaitBrick(broadcastMessage))
                eventBrickList.add(BroadcastWithParamsReceiverBrick(broadcastMessage, null))
                eventBrickList.add(BroadcastWithParamsBrick(broadcastMessage, "any data..."))
                eventBrickList.add(WhenNotificationClickedBrick(WhenNotificationClickedScript(Formula(123456))))
                eventBrickList.add(WhenNotificationActionTriggeredBrick(
                    WhenNotificationActionTriggeredScript(Formula("123"))))
                eventBrickList.add(WhenProjectExitsBrick())
                eventBrickList.add(WhenBackPressedBrick())
                eventBrickList.add(WhenConditionBrick(WhenConditionScript(Formula(defaultIf))))
                if (!isBackgroundSprite) {
                    eventBrickList.add(WhenBounceOffBrick(WhenBounceOffScript(null)))
                }
                eventBrickList.add(WhenBackgroundChangesBrick())
                eventBrickList.add(WhenClonedBrick())
                eventBrickList.add(CloneBrick())
                eventBrickList.add(DeleteThisCloneBrick())
                eventBrickList.add(WhenClonedWithNameBrick(WhenClonedWithNameScript(Formula("clone"))))
                eventBrickList.add(CloneAndNameBrick("clone"))
                eventBrickList.add(WhenBeforeUpdateBrick())
                eventBrickList.add(WhenAfterUpdateBrick())
                eventBrickList.add(WhenAppMinimizedBrick())
                eventBrickList.add(WhenAppRestoredBrick())
                eventBrickList.add(WhenWindowResizedBrick())
                eventBrickList.add(WhenMqttMessageBrick(WhenMqttMessageScript(Formula("room_1"))))
                if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
                    eventBrickList.add(WhenNfcBrick())
                }
                //eventBrickList.add(WhenMouseButtonClickedBrick())
                //eventBrickList.add(LockMouseBrick())
                //eventBrickList.add(UnlockMouseBrick())
                //eventBriFckList.add(WhenMouseWheelScrolledBrick())
                return eventBrickList
            }
        }

        val defaultIf = FormulaElement(
            FormulaElement.ElementType.OPERATOR,
            Operators.SMALLER_THAN.toString(),
            null
        )
        defaultIf.setLeftChild(FormulaElement(FormulaElement.ElementType.NUMBER, "1", null))
        defaultIf.setRightChild(FormulaElement(FormulaElement.ElementType.NUMBER, "2", null))
        val eventBrickList: MutableList<Brick> = ArrayList()
        val template = WhenStartedBrick()

        eventBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_event_touch), template))
        eventBrickList.add(WhenStartedBrick())
        eventBrickList.add(WhenBrick())
        eventBrickList.add(WhenTouchDownBrick())
        eventBrickList.add(WhenSpriteReleasedBrick())
        eventBrickList.add(WhenFingerMovedOverSpriteBrick())
        eventBrickList.add(WhenFingerMovedOnScreenBrick())
        eventBrickList.add(WhenBackPressedBrick())

        eventBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_event_messages), template))
        val broadcastMessages =
            ProjectManager.getInstance().currentProject?.broadcastMessageContainer?.broadcastMessages
        var broadcastMessage: String? = context.getString(R.string.brick_broadcast_default_value)
        if (broadcastMessages != null && broadcastMessages.size > 0) {
            broadcastMessage = broadcastMessages[0]
        }
        eventBrickList.add(BroadcastReceiverBrick(BroadcastScript(broadcastMessage)))
        eventBrickList.add(BroadcastBrick(broadcastMessage))
        eventBrickList.add(BroadcastWaitBrick(broadcastMessage))
        eventBrickList.add(BroadcastWithParamsReceiverBrick(broadcastMessage, null))
        eventBrickList.add(BroadcastWithParamsBrick(broadcastMessage, "any data..."))
        eventBrickList.add(WhenNotificationClickedBrick(WhenNotificationClickedScript(Formula(123456))))
        eventBrickList.add(WhenNotificationActionTriggeredBrick(
            WhenNotificationActionTriggeredScript(Formula("123"))))
        eventBrickList.add(WhenMqttMessageBrick(WhenMqttMessageScript(Formula("room_1"))))

        eventBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_event_conditions), template))
        eventBrickList.add(WhenConditionBrick(WhenConditionScript(Formula(defaultIf))))
        if (!isBackgroundSprite) {
            eventBrickList.add(WhenBounceOffBrick(WhenBounceOffScript(null)))
        }
        eventBrickList.add(WhenBackgroundChangesBrick())

        eventBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_event_cloning), template))
        eventBrickList.add(WhenClonedBrick())
        eventBrickList.add(CloneBrick())
        eventBrickList.add(DeleteThisCloneBrick())
        eventBrickList.add(WhenClonedWithNameBrick(WhenClonedWithNameScript(Formula("clone"))))
        eventBrickList.add(CloneAndNameBrick("clone"))

        eventBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_event_system), template))
        eventBrickList.add(WhenProjectExitsBrick())
        eventBrickList.add(WhenBeforeUpdateBrick())
        eventBrickList.add(WhenAfterUpdateBrick())
        eventBrickList.add(WhenAppMinimizedBrick())
        eventBrickList.add(WhenAppRestoredBrick())
        eventBrickList.add(WhenWindowResizedBrick())
        if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
            eventBrickList.add(WhenNfcBrick())
        }

        return eventBrickList
    }

    protected open fun setupControlCategoryList(context: Context): List<Brick> {
        val category = context.getString(R.string.category_control)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val ifConditionFormulaElement = FormulaElement(FormulaElement.ElementType.OPERATOR, Operators.SMALLER_THAN.toString(), null)
                ifConditionFormulaElement.setLeftChild(FormulaElement(FormulaElement.ElementType.NUMBER, "1", null))
                ifConditionFormulaElement.setRightChild(FormulaElement(FormulaElement.ElementType.NUMBER, "2", null))
                val ifConditionFormula = Formula(ifConditionFormulaElement)
                val controlBrickList: MutableList<Brick> = ArrayList()
                //controlBrickList.add(TestBrick("Ababuy!"))
                controlBrickList.add(WaitBrick(BrickValues.WAIT))
                controlBrickList.add(NoteBrick(context.getString(R.string.brick_note_default_value)))
                controlBrickList.add(ForeverBrick())
                controlBrickList.add(IfLogicBeginBrick(ifConditionFormula))
                controlBrickList.add(IfThenLogicBeginBrick(ifConditionFormula))
                controlBrickList.add(AsyncRepeatBrick(Formula(3)))
                controlBrickList.add(IntervalRepeatBrick(Formula(3), Formula(1)))
                controlBrickList.add(WaitUntilBrick(ifConditionFormula))
                controlBrickList.add(RepeatBrick(Formula(BrickValues.REPEAT)))
                controlBrickList.add(RepeatUntilBrick(ifConditionFormula))
                controlBrickList.add(ForVariableFromToBrick(Formula(BrickValues.FOR_LOOP_FROM), Formula(BrickValues.FOR_LOOP_TO)))
                controlBrickList.add(ForItemInUserListBrick())
                controlBrickList.add(RunAsSpriteBrick(Formula("Sprite")))
                controlBrickList.add(InstantBrick())
                controlBrickList.add(SpawnThreadBrick(Formula("thread1")))
                controlBrickList.add(WaitThreadBrick(Formula("thread1")))
                controlBrickList.add(StopThreadBrick(Formula("thread1")))
                controlBrickList.add(SceneTransitionBrick(null))
                controlBrickList.add(SceneStartBrick(null))
                controlBrickList.add(SceneIdBrick("1"))
                controlBrickList.add(ClearSceneBrick(null))
                controlBrickList.add(SetSaveScenesBrick(1))
                controlBrickList.add(SetStopSoundsBrick(1))
                controlBrickList.add(LaunchProjectBrick("project.newtrobat"))
                controlBrickList.add(ReturnToPreviousProjectBrick())
                if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
                    controlBrickList.add(PhiroIfLogicBeginBrick())
                }
                controlBrickList.add(ExitStageBrick())
                controlBrickList.add(StopScriptBrick(BrickValues.STOP_THIS_SCRIPT))
                controlBrickList.add(WaitTillIdleBrick())
                controlBrickList.add(TryCatchFinallyBrick())
                controlBrickList.add(WhenClonedBrick())
                controlBrickList.add(CloneBrick())
                controlBrickList.add(DeleteThisCloneBrick())
                controlBrickList.add(WhenClonedWithNameBrick(WhenClonedWithNameScript(Formula("clone"))))
                controlBrickList.add(CloneAndNameBrick("clone"))
                if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
                    controlBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
                }
                val broadcastMessages =
                    ProjectManager.getInstance().currentProject?.broadcastMessageContainer?.broadcastMessages
                var broadcastMessage: String? = context.getString(R.string.brick_broadcast_default_value)
                if (broadcastMessages != null && broadcastMessages.size > 0) {
                    broadcastMessage = broadcastMessages[0]
                }
                controlBrickList.add(BroadcastReceiverBrick(BroadcastScript(broadcastMessage)))
                controlBrickList.add(BroadcastBrick(broadcastMessage))
                controlBrickList.add(BroadcastWaitBrick(broadcastMessage))
                controlBrickList.add(TapAtBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START))
                controlBrickList.add(TapForBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_DURATION))
                controlBrickList.add(TouchAndSlideBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_X_GOAL, BrickValues.TOUCH_Y_GOAL, BrickValues.TOUCH_DURATION))
                controlBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
                controlBrickList.add(OpenAppBrick(Formula("com.android.settings")))
                return controlBrickList
            }
        }

        val ifConditionFormulaElement = FormulaElement(FormulaElement.ElementType.OPERATOR, Operators.SMALLER_THAN.toString(), null)
        ifConditionFormulaElement.setLeftChild(FormulaElement(FormulaElement.ElementType.NUMBER, "1", null))
        ifConditionFormulaElement.setRightChild(FormulaElement(FormulaElement.ElementType.NUMBER, "2", null))
        val ifConditionFormula = Formula(ifConditionFormulaElement)
        val controlBrickList: MutableList<Brick> = ArrayList()
        val template = WaitBrick(BrickValues.WAIT)

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_waiting), template))
        controlBrickList.add(WaitBrick(BrickValues.WAIT))
        controlBrickList.add(WaitUntilBrick(ifConditionFormula))
        controlBrickList.add(WaitTillIdleBrick())
        controlBrickList.add(WaitThreadBrick(Formula("thread1")))
        controlBrickList.add(NoteBrick(context.getString(R.string.brick_note_default_value)))

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_loops), template))
        controlBrickList.add(ForeverBrick())
        controlBrickList.add(RepeatBrick(Formula(BrickValues.REPEAT)))
        controlBrickList.add(RepeatUntilBrick(ifConditionFormula))
        controlBrickList.add(AsyncRepeatBrick(Formula(3)))
        controlBrickList.add(IntervalRepeatBrick(Formula(3), Formula(1)))
        controlBrickList.add(ForVariableFromToBrick(Formula(BrickValues.FOR_LOOP_FROM), Formula(BrickValues.FOR_LOOP_TO)))
        controlBrickList.add(ForItemInUserListBrick())
        controlBrickList.add(InstantBrick())
        controlBrickList.add(SpawnThreadBrick(Formula("thread1")))

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_conditions), template))
        controlBrickList.add(IfLogicBeginBrick(ifConditionFormula))
        controlBrickList.add(IfThenLogicBeginBrick(ifConditionFormula))
        controlBrickList.add(TryCatchFinallyBrick())
        if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
            controlBrickList.add(PhiroIfLogicBeginBrick())
        }

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_scenes), template))
        controlBrickList.add(SceneStartBrick(null))
        controlBrickList.add(SceneIdBrick("1"))
        controlBrickList.add(SceneTransitionBrick(null))
        controlBrickList.add(ClearSceneBrick(null))
        controlBrickList.add(SetSaveScenesBrick(1))
        controlBrickList.add(LaunchProjectBrick("project.newtrobat"))
        controlBrickList.add(ReturnToPreviousProjectBrick())

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_stopping), template))
        controlBrickList.add(StopScriptBrick(BrickValues.STOP_THIS_SCRIPT))
        controlBrickList.add(StopThreadBrick(Formula("thread1")))
        controlBrickList.add(ExitStageBrick())

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_cloning), template))
        controlBrickList.add(WhenClonedBrick())
        controlBrickList.add(CloneBrick())
        controlBrickList.add(DeleteThisCloneBrick())
        controlBrickList.add(WhenClonedWithNameBrick(WhenClonedWithNameScript(Formula("clone"))))
        controlBrickList.add(CloneAndNameBrick("clone"))
        controlBrickList.add(RunAsSpriteBrick(Formula("Sprite")))

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_messages), template))
        val broadcastMessages =
            ProjectManager.getInstance().currentProject?.broadcastMessageContainer?.broadcastMessages
        var broadcastMessage: String? = context.getString(R.string.brick_broadcast_default_value)
        if (broadcastMessages != null && broadcastMessages.size > 0) {
            broadcastMessage = broadcastMessages[0]
        }
        controlBrickList.add(BroadcastReceiverBrick(BroadcastScript(broadcastMessage)))
        controlBrickList.add(BroadcastBrick(broadcastMessage))
        controlBrickList.add(BroadcastWaitBrick(broadcastMessage))
        if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
            controlBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
        }

        controlBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_control_touches), template))
        controlBrickList.add(TapAtBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START))
        controlBrickList.add(TapForBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_DURATION))
        controlBrickList.add(TouchAndSlideBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_X_GOAL, BrickValues.TOUCH_Y_GOAL, BrickValues.TOUCH_DURATION))
        controlBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
        controlBrickList.add(OpenAppBrick(Formula("com.android.settings")))

        return controlBrickList
    }

    private fun setupUserBricksCategoryList(): List<Brick> {
        val currentSprite = ProjectManager.getInstance().currentSprite
        var userDefinedBricks: MutableList<Brick> = ArrayList()
        if (currentSprite != null) userDefinedBricks = currentSprite.userDefinedBrickList
        userDefinedBricks = ArrayList(userDefinedBricks)
        //if (BuildConfig.FEATURE_USER_REPORTERS_ENABLED) userDefinedBricks.add(ReportBrick())
        return userDefinedBricks
    }

    private fun setupChromecastCategoryList(context: Context): List<Brick> {
        val chromecastBrickList: MutableList<Brick> = ArrayList()
        chromecastBrickList.add(WhenGamepadButtonBrick(WhenGamepadButtonScript(context.getString(R.string.cast_gamepad_A))))
        return chromecastBrickList
    }

    protected open fun setupMotionCategoryList(
        context: Context,
        isBackgroundSprite: Boolean
    ): List<Brick> {
        val category = context.getString(R.string.category_motion)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val motionBrickList: MutableList<Brick> = ArrayList()
                motionBrickList.add(PlaceAtBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
                motionBrickList.add(SetXBrick(Formula(BrickValues.X_POSITION)))
                motionBrickList.add(SetYBrick(BrickValues.Y_POSITION))
                motionBrickList.add(ChangeXByNBrick(BrickValues.CHANGE_X_BY))
                motionBrickList.add(ChangeYByNBrick(BrickValues.CHANGE_Y_BY))
                motionBrickList.add(GoToBrick(null))
                if (!isBackgroundSprite) motionBrickList.add(IfOnEdgeBounceBrick())
                motionBrickList.add(MoveNStepsBrick(BrickValues.MOVE_STEPS))
                motionBrickList.add(TurnLeftBrick(BrickValues.TURN_DEGREES))
                motionBrickList.add(TurnRightBrick(BrickValues.TURN_DEGREES))
                motionBrickList.add(PointInDirectionBrick(BrickValues.POINT_IN_DIRECTION))
                motionBrickList.add(PointToBrick(null))
                motionBrickList.add(SetRotationStyleBrick())
                motionBrickList.add(GlideToBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.GLIDE_SECONDS))
                if (!isBackgroundSprite) {
                    motionBrickList.add(GoNStepsBackBrick(BrickValues.GO_BACK))
                    motionBrickList.add(ComeToFrontBrick())
                }
                motionBrickList.add(SetCameraFocusPointBrick())
                motionBrickList.add(VibrationBrick(BrickValues.VIBRATE_SECONDS))
                motionBrickList.add(SetPhysicsObjectTypeBrick(BrickValues.PHYSIC_TYPE))
                if (!isBackgroundSprite) motionBrickList.add(WhenBounceOffBrick(WhenBounceOffScript(null)))
                motionBrickList.add(SetHitboxBrick())
                motionBrickList.add(SetVelocityBrick(BrickValues.PHYSIC_VELOCITY))
                motionBrickList.add(ApplyForceBrick(10, 100))
                motionBrickList.add(ApplyImpulseBrick(100, 10))
                motionBrickList.add(TurnLeftSpeedBrick(BrickValues.PHYSIC_TURN_DEGREES))
                motionBrickList.add(TurnRightSpeedBrick(BrickValues.PHYSIC_TURN_DEGREES))
                motionBrickList.add(ApplyTorqueBrick(15))
                motionBrickList.add(ApplyAngularImpulseBrick(30))
                motionBrickList.add(SetGravityBrick(BrickValues.PHYSIC_GRAVITY))
                motionBrickList.add(SetMassBrick(BrickValues.PHYSIC_MASS))
                motionBrickList.add(SetDampingBrick(10f, 10f))
                motionBrickList.add(CreateRevoluteJointBrick("joint", "sprite2", 10, 200))
                motionBrickList.add(CreateDistanceJointBrick("joint", "sprite2", "100", "5", "0.3"))
                motionBrickList.add(CreateWeldJointBrick("joint", "sprite2", 10, 200))
                motionBrickList.add(CreatePrismaticJointBrick("joint", "sprite2", 10, 100, 20, 30))
                motionBrickList.add(CreatePulleyJointBrick("joint", "sprite1", "sprite2", 10, 100, 0, 0, 1f))
                motionBrickList.add(CreateGearJointBrick("joint", "jointA", "jointB", 1f))
                motionBrickList.add((DestroyJointBrick("joint")))
                motionBrickList.add(PerformRayCastBrick("ray", 0, 0, 300, 500))
                motionBrickList.add(SetBounceBrick(BrickValues.PHYSIC_BOUNCE_FACTOR * BrickValues.PHYSIC_MULTIPLIER))
                motionBrickList.add(SetFrictionBrick(BrickValues.PHYSIC_FRICTION * BrickValues.PHYSIC_MULTIPLIER))
                if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
                    motionBrickList.add(PhiroMotorMoveForwardBrick(PhiroMotorMoveForwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
                    motionBrickList.add(PhiroMotorMoveBackwardBrick(PhiroMotorMoveBackwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
                    motionBrickList.add(PhiroMotorStopBrick(PhiroMotorStopBrick.Motor.MOTOR_BOTH))
                }
                motionBrickList.add(FadeParticleEffectBrick())
                return motionBrickList
            }
        }

        val motionBrickList: MutableList<Brick> = ArrayList()
        val template = SetXBrick(BrickValues.X_POSITION)

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_coordinates) ?: "", template))
        motionBrickList.add(PlaceAtBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
        motionBrickList.add(SetXBrick(Formula(BrickValues.X_POSITION)))
        motionBrickList.add(SetYBrick(BrickValues.Y_POSITION))
        motionBrickList.add(ChangeXByNBrick(BrickValues.CHANGE_X_BY))
        motionBrickList.add(ChangeYByNBrick(BrickValues.CHANGE_Y_BY))
        motionBrickList.add(GoToBrick(null))
        motionBrickList.add(GlideToBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.GLIDE_SECONDS))

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_rotation) ?: "", template))
        motionBrickList.add(MoveNStepsBrick(BrickValues.MOVE_STEPS))
        motionBrickList.add(TurnLeftBrick(BrickValues.TURN_DEGREES))
        motionBrickList.add(TurnRightBrick(BrickValues.TURN_DEGREES))
        motionBrickList.add(PointInDirectionBrick(BrickValues.POINT_IN_DIRECTION))
        motionBrickList.add(PointToBrick(null))
        motionBrickList.add(SetRotationStyleBrick())

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_layers) ?: "", template))
        if (!isBackgroundSprite) {
            motionBrickList.add(GoNStepsBackBrick(BrickValues.GO_BACK))
            motionBrickList.add(ComeToFrontBrick())
        }
        motionBrickList.add(SetCameraFocusPointBrick())

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_physics_core) ?: "", template))
        motionBrickList.add(SetPhysicsObjectTypeBrick(BrickValues.PHYSIC_TYPE))
        motionBrickList.add(SetHitboxBrick())
        if (!isBackgroundSprite) {
            motionBrickList.add(IfOnEdgeBounceBrick())
            motionBrickList.add(WhenBounceOffBrick(WhenBounceOffScript(null)))
        }
        motionBrickList.add(VibrationBrick(BrickValues.VIBRATE_SECONDS))

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_physics_forces) ?: "", template))
        motionBrickList.add(SetVelocityBrick(BrickValues.PHYSIC_VELOCITY))
        motionBrickList.add(ApplyForceBrick(10, 100))
        motionBrickList.add(ApplyImpulseBrick(100, 10))
        motionBrickList.add(ApplyTorqueBrick(15))
        motionBrickList.add(ApplyAngularImpulseBrick(30))
        motionBrickList.add(TurnLeftSpeedBrick(BrickValues.PHYSIC_TURN_DEGREES))
        motionBrickList.add(TurnRightSpeedBrick(BrickValues.PHYSIC_TURN_DEGREES))
        motionBrickList.add(SetGravityBrick(BrickValues.PHYSIC_GRAVITY))
        motionBrickList.add(SetMassBrick(BrickValues.PHYSIC_MASS))
        motionBrickList.add(SetDampingBrick(10f, 10f))
        motionBrickList.add(SetBounceBrick(BrickValues.PHYSIC_BOUNCE_FACTOR * BrickValues.PHYSIC_MULTIPLIER))
        motionBrickList.add(SetFrictionBrick(BrickValues.PHYSIC_FRICTION * BrickValues.PHYSIC_MULTIPLIER))
        motionBrickList.add(PerformRayCastBrick("ray", 0, 0, 300, 500))

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_physics_joints) ?: "", template))
        motionBrickList.add(CreateRevoluteJointBrick("joint", "sprite2", 10, 200))
        motionBrickList.add(CreateDistanceJointBrick("joint", "sprite2", "100", "5", "0.3"))
        motionBrickList.add(CreateWeldJointBrick("joint", "sprite2", 10, 200))
        motionBrickList.add(CreatePrismaticJointBrick("joint", "sprite2", 10, 100, 20, 30))
        motionBrickList.add(CreatePulleyJointBrick("joint", "sprite1", "sprite2", 10, 100, 0, 0, 1f))
        motionBrickList.add(CreateGearJointBrick("joint", "jointA", "jointB", 1f))
        motionBrickList.add(DestroyJointBrick("joint"))

        motionBrickList.add(SubCategoryHeaderBrick(context?.getString(R.string.subcategory_motion_robots) ?: "", template))
        motionBrickList.add(FadeParticleEffectBrick())
        if (context != null && SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
            motionBrickList.add(PhiroMotorMoveForwardBrick(PhiroMotorMoveForwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
            motionBrickList.add(PhiroMotorMoveBackwardBrick(PhiroMotorMoveBackwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
            motionBrickList.add(PhiroMotorStopBrick(PhiroMotorStopBrick.Motor.MOTOR_BOTH))
        }

        return motionBrickList
    }

    protected open fun setupSoundCategoryList(context: Context): List<Brick> {
        val category = context.getString(R.string.category_sound)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val soundBrickList: MutableList<Brick> = ArrayList()
                soundBrickList.add(PlaySoundBrick())
                soundBrickList.add(PlaySoundAndWaitBrick())
                soundBrickList.add(PlaySoundAtBrick(BrickValues.PLAY_AT_DEFAULT_OFFSET))
                soundBrickList.add(SoundFileBrick("my_sound.mp3"))
                soundBrickList.add(SoundFilesBrick("my_sound_FROM_PROJECT_FILES.mp3"))
                soundBrickList.add(PrepareSoundBrick("my_sound.mp3", "sound"))
                soundBrickList.add(PlayPreparedSoundBrick("sound"))
                soundBrickList.add(StopSoundBrick())
                soundBrickList.add(StopAllSoundsBrick())
                soundBrickList.add(SetVolumeToBrick(BrickValues.SET_VOLUME_TO))
                soundBrickList.add(SetSoundVolumeBrick(50.0))
                soundBrickList.add(ChangeVolumeByNBrick(Formula(BrickValues.CHANGE_VOLUME_BY)))
                soundBrickList.add(ListenMicroBrick("100"))
                soundBrickList.add(StartRecordingBrick())
                soundBrickList.add(StopRecordingBrick("audio.mp3"))
                if (SettingsFragment.isAISpeechSynthetizationSharedPreferenceEnabled(context)) {
                    soundBrickList.add(SpeakBrick(context.getString(R.string.brick_speak_default_value)))
                    soundBrickList.add(SpeakAndWaitBrick(context.getString(R.string.brick_speak_default_value)))
                }
                if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
                    soundBrickList.add(PhiroPlayToneBrick(PhiroPlayToneBrick.Tone.DO, BrickValues.PHIRO_DURATION))
                }
                if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
                    soundBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
                    soundBrickList.add(StartListeningBrick())
                    soundBrickList.add(SetListeningLanguageBrick())
                }
                soundBrickList.add(SetStopSoundsBrick(1))
                soundBrickList.add(SetInstrumentBrick())
                soundBrickList.add(PlayNoteForBeatsBrick(BrickValues.DEFAULT_NOTE, BrickValues.PAUSED_BEATS_INT))
                soundBrickList.add(PlayDrumForBeatsBrick(BrickValues.PAUSED_BEATS_INT))
                soundBrickList.add(SetTempoBrick(BrickValues.DEFAULT_TEMPO))
                soundBrickList.add(ChangeTempoByNBrick(BrickValues.CHANGE_TEMPO))
                soundBrickList.add(PauseForBeatsBrick(BrickValues.PAUSED_BEATS_FLOAT))
                return soundBrickList
            }
        }
        val soundBrickList: MutableList<Brick> = ArrayList()
        val template = PlayPreparedSoundBrick()

        soundBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_sound_playback), template))
        soundBrickList.add(PlaySoundBrick())
        soundBrickList.add(PlaySoundAndWaitBrick())
        soundBrickList.add(PlaySoundAtBrick(BrickValues.PLAY_AT_DEFAULT_OFFSET))
        soundBrickList.add(PrepareSoundBrick("my_sound.mp3", "sound"))
        soundBrickList.add(PlayPreparedSoundBrick("sound"))
        soundBrickList.add(StopSoundBrick())
        soundBrickList.add(StopAllSoundsBrick())

        soundBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_sound_volume), template))
        soundBrickList.add(SetVolumeToBrick(BrickValues.SET_VOLUME_TO))
        soundBrickList.add(SetSoundVolumeBrick(50.0))
        soundBrickList.add(ChangeVolumeByNBrick(Formula(BrickValues.CHANGE_VOLUME_BY)))
        soundBrickList.add(SetStopSoundsBrick(1))

        soundBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_sound_files), template))
        soundBrickList.add(SoundFileBrick("my_sound.mp3"))
        soundBrickList.add(SoundFilesBrick("my_sound_FROM_PROJECT_FILES.mp3"))
        soundBrickList.add(StartRecordingBrick())
        soundBrickList.add(StopRecordingBrick("audio.mp3"))

        soundBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_sound_speech), template))
        soundBrickList.add(ListenMicroBrick("100"))
        if (SettingsFragment.isAISpeechSynthetizationSharedPreferenceEnabled(context)) {
            soundBrickList.add(SpeakBrick(context.getString(R.string.brick_speak_default_value)))
            soundBrickList.add(SpeakAndWaitBrick(context.getString(R.string.brick_speak_default_value)))
        }
        if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
            soundBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
            soundBrickList.add(StartListeningBrick())
            soundBrickList.add(SetListeningLanguageBrick())
        }

        soundBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_sound_music), template))
        soundBrickList.add(SetInstrumentBrick())
        soundBrickList.add(PlayNoteForBeatsBrick(BrickValues.DEFAULT_NOTE, BrickValues.PAUSED_BEATS_INT))
        soundBrickList.add(PlayDrumForBeatsBrick(BrickValues.PAUSED_BEATS_INT))
        soundBrickList.add(SetTempoBrick(BrickValues.DEFAULT_TEMPO))
        soundBrickList.add(ChangeTempoByNBrick(BrickValues.CHANGE_TEMPO))
        soundBrickList.add(PauseForBeatsBrick(BrickValues.PAUSED_BEATS_FLOAT))
        if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
            soundBrickList.add(PhiroPlayToneBrick(PhiroPlayToneBrick.Tone.DO, BrickValues.PHIRO_DURATION))
        }

        return soundBrickList
    }

    protected open fun setupLooksCategoryList(context: Context, isBackgroundSprite: Boolean): List<Brick> {
        val category = context.getString(R.string.category_looks)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val looksBrickList = mutableListOf<Brick>()
                if (!isBackgroundSprite) {
                    looksBrickList.add(SetLookBrick())
                    looksBrickList.add(SetLookByIndexBrick(BrickValues.SET_LOOK_BY_INDEX))
                }
                looksBrickList.add(SetWidthBrick(BrickValues.SET_SIZE_TO))
                looksBrickList.add(SetHeightBrick(BrickValues.SET_SIZE_TO))
                looksBrickList.add(ChangeWidthBrick(BrickValues.CHANGE_SIZE_BY))
                looksBrickList.add(ChangeHeightBrick(BrickValues.CHANGE_SIZE_BY))
                looksBrickList.add(NextLookBrick())
                looksBrickList.add(PreviousLookBrick())
                looksBrickList.add(PlayGifBrick("cat.gif"))
                looksBrickList.add(StopGifBrick())
                looksBrickList.add(PlaySpritesheetBrick(Formula(4), Formula(8), Formula(1), Formula(8), Formula(0.1)))
                looksBrickList.add(StopSpritesheetBrick())
                looksBrickList.add(SetSizeToBrick(BrickValues.SET_SIZE_TO))
                looksBrickList.add(ChangeSizeByNBrick(BrickValues.CHANGE_SIZE_BY))
                looksBrickList.add(HideBrick())
                looksBrickList.add(ShowBrick())
                looksBrickList.add(AskBrick(context.getString(R.string.brick_ask_default_question)))
                looksBrickList.add(BigAskBrick(context.getString(R.string.brick_ask_default_question), context.getString(R.string.ask_default_placeholder), context.getString(R.string.ask_default_ok), context.getString(R.string.ask_default_cancel), context.getString(R.string.ask_default_answer_name)))
                if (!isBackgroundSprite) {
                    looksBrickList.add(SayBubbleBrick(context.getString(R.string.brick_say_bubble_default_value)))
                    looksBrickList.add(SayForBubbleBrick(context.getString(R.string.brick_say_bubble_default_value), 1.0f))
                    looksBrickList.add(ThinkBubbleBrick(context.getString(R.string.brick_think_bubble_default_value)))
                    looksBrickList.add(ThinkForBubbleBrick(context.getString(R.string.brick_think_bubble_default_value), 1.0f))
                }
                looksBrickList.add(ShowTextBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
                looksBrickList.add(ShowTextColorSizeAlignmentBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
                looksBrickList.add(SetTransparencyBrick(BrickValues.SET_TRANSPARENCY))
                looksBrickList.add(ChangeTransparencyByNBrick(BrickValues.CHANGE_TRANSPARENCY_EFFECT))
                looksBrickList.add(SetBrightnessBrick(BrickValues.SET_BRIGHTNESS_TO))
                looksBrickList.add(ChangeBrightnessByNBrick(BrickValues.CHANGE_BRIGHTNESS_BY))
                looksBrickList.add(SetColorBrick(BrickValues.SET_COLOR_TO))
                looksBrickList.add(ChangeColorByNBrick(BrickValues.CHANGE_COLOR_BY))
                looksBrickList.add(SetCornerOffsetsBrick(Formula(0), Formula(0), Formula(50), Formula(100), Formula(0), Formula(0), Formula(0), Formula(0)))
                looksBrickList.add(EasePropertyBrick(0, 0, 100f, 250f, 1f))
                looksBrickList.add(FadeParticleEffectBrick())
                looksBrickList.add(ParticleEffectAdditivityBrick())
                looksBrickList.add(SetParticleColorBrick(BrickValues.PARTICLE_COLOR))
                looksBrickList.add(ClearGraphicEffectBrick())
                looksBrickList.add(SetCameraFocusPointBrick())
                looksBrickList.add(SetCameraPosition2Brick(100, 300))
                looksBrickList.add(SetCameraRotation2Brick(90.0))
                looksBrickList.add(SetCameraZoomBrick(3.0))
                looksBrickList.add(PinToCameraBrick())
                looksBrickList.add(UnpinFromCameraBrick())
                looksBrickList.add(WhenBackgroundChangesBrick())
                looksBrickList.add(SetBackgroundBrick())
                looksBrickList.add(SetBackgroundByIndexBrick(BrickValues.SET_LOOK_BY_INDEX))
                looksBrickList.add(SetBackgroundAndWaitBrick())
                looksBrickList.add(SetBackgroundByIndexAndWaitBrick(BrickValues.SET_LOOK_BY_INDEX))
                if (!ProjectManager.getInstance().currentProject.isCastProject) {
                    looksBrickList.add(CameraBrick())
                    looksBrickList.add(ChooseCameraBrick())
                    looksBrickList.add(FlashBrick())
                }
                when {
                    !isBackgroundSprite -> looksBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
                    ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> looksBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
                    else -> looksBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
                }
                looksBrickList.add(ScreenShotBrick())
                looksBrickList.add(PhotoBrick())
                looksBrickList.add(CutLookBrick(100, 200, 300, 400))
                looksBrickList.add(SaveLookBrick("my_actor.png"))
                looksBrickList.add(LookFileBrick("my_actor.png"))
                looksBrickList.add(SaveLookFilesBrick("look.png"))
                looksBrickList.add(SetLookFilesBrick("look.png"))
                looksBrickList.add(LookToTableBrick("rTable", "gTable", "bTable", "aTable"))
                looksBrickList.add(LookFromTableBrick("rTable", "gTable", "bTable", "aTable"))
                if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) looksBrickList.add(
                    PhiroRGBLightBrick(PhiroRGBLightBrick.Eye.BOTH, BrickValues.PHIRO_VALUE_RED, BrickValues.PHIRO_VALUE_GREEN, BrickValues.PHIRO_VALUE_BLUE)
                )
                looksBrickList.add(PaintNewLookBrick(context.getString(R.string.brick_paint_new_look_name)))
                looksBrickList.add(EditLookBrick())
                looksBrickList.add(CopyLookBrick(context.getString(R.string.brick_copy_look_name)))
                looksBrickList.add(DeleteLookBrick())
                looksBrickList.add(CreateDialogBrick("myDialog", context.getString(R.string.cbf_dialog_title), context.getString(R.string.cbf_dialog_answer)))
                looksBrickList.add(SetPositiveBrick("myDialog", context.getString(R.string.cbf_dialog_yes)))
                looksBrickList.add(SetNeutralBrick("myDialog", context.getString(R.string.cbf_dialog_later)))
                looksBrickList.add(SetNegativeBrick("myDialog", context.getString(R.string.cbf_dialog_no)))
                looksBrickList.add(AddEditBrick("myDialog", context.getString(R.string.cbf_dialog_edit_hint)))
                looksBrickList.add(AddRadioBrick("myDialog", context.getString(R.string.cbf_dialog_radio_hint)))
                looksBrickList.add(SetCallbackBrick("myDialog"))
                looksBrickList.add(ShowDialogBrick("myDialog"))
                looksBrickList.add(SquareBrick("square", "#ff0000", 0f, 0f, 100f, 100f, 1f, 0f, 0f))
                looksBrickList.add(DelSquareBrick("square"))

                looksBrickList.add(CreateBufferBrick("Map", "512", "512"))
                looksBrickList.add(SetBufferModeBrick(Formula("Map"), Formula(1), Formula(0)))
                looksBrickList.add(SetBufferAutoUpdateBrick(Formula("Map"), Formula(1)))
                looksBrickList.add(AddToBufferBrick("Map"))
                looksBrickList.add(RemoveFromBufferBrick(Formula("Map")))
                looksBrickList.add(SetBufferOnlyBrick(Formula(1)))
                looksBrickList.add(AddTextToBufferBrick("myText", "Map"))
                looksBrickList.add(SetTextBufferOnlyBrick("myText", 1))
                looksBrickList.add(SetBufferCameraBrick("Map", "100", "200", "1", "0"))
                looksBrickList.add(SetBufferCamera3DBrick(Formula("Map"), Formula(100), Formula(100), Formula(100), Formula(120), Formula(-20), Formula(0), Formula(67)))
                looksBrickList.add(ApplyBufferLookBrick("Map"))
                looksBrickList.add(BufferMaskBrick("Map", 1))
                looksBrickList.add(SetBufferShaderBrick("Map", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    
    texColor.rgb = 1.0 - texColor.rgb;
    
    gl_FragColor = v_color * texColor;
}"""))
                looksBrickList.add(SetBufferShaderUniformBrick("Map", "time", 1f, 0f, 0f, 1))
                looksBrickList.add(SaveBufferBrick(Formula("Map"), Formula("buffer.png")))
                looksBrickList.add(StartBufferRecordingBrick("myBuffer", "output.mp4", 30, 2000000))
                looksBrickList.add(StopBufferRecordingBrick())
                looksBrickList.add(SetMainRenderLoopsBrick(1f, 1f, 1f))
                looksBrickList.add(SetBufferEffectsBrick("Map", 1f, 1f))

                looksBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
                looksBrickList.add(OpenAppBrick(Formula("com.android.settings")))
                return looksBrickList
            }
        }

        val looksBrickList = mutableListOf<Brick>()
        val template = ShowBrick()

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_costumes), template))
        if (!isBackgroundSprite) {
            looksBrickList.add(SetLookBrick())
            looksBrickList.add(SetLookByIndexBrick(BrickValues.SET_LOOK_BY_INDEX))
            looksBrickList.add(NextLookBrick())
            looksBrickList.add(PreviousLookBrick())
        }
        looksBrickList.add(PlayGifBrick("cat.gif"))
        looksBrickList.add(StopGifBrick())
        looksBrickList.add(PlaySpritesheetBrick(Formula(4), Formula(8), Formula(1), Formula(8), Formula(0.1)))
        looksBrickList.add(StopSpritesheetBrick())
        looksBrickList.add(WhenBackgroundChangesBrick())
        looksBrickList.add(SetBackgroundBrick())
        looksBrickList.add(SetBackgroundByIndexBrick(BrickValues.SET_LOOK_BY_INDEX))
        looksBrickList.add(SetBackgroundAndWaitBrick())
        looksBrickList.add(SetBackgroundByIndexAndWaitBrick(BrickValues.SET_LOOK_BY_INDEX))
        looksBrickList.add(PaintNewLookBrick(context.getString(R.string.brick_paint_new_look_name)))
        looksBrickList.add(EditLookBrick())
        looksBrickList.add(CopyLookBrick(context.getString(R.string.brick_copy_look_name)))
        looksBrickList.add(DeleteLookBrick())

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_visibility), template))
        looksBrickList.add(ShowBrick())
        looksBrickList.add(HideBrick())
        looksBrickList.add(SetSizeToBrick(BrickValues.SET_SIZE_TO))
        looksBrickList.add(ChangeSizeByNBrick(BrickValues.CHANGE_SIZE_BY))
        looksBrickList.add(SetWidthBrick(BrickValues.SET_SIZE_TO))
        looksBrickList.add(SetHeightBrick(BrickValues.SET_SIZE_TO))
        looksBrickList.add(ChangeWidthBrick(BrickValues.CHANGE_SIZE_BY))
        looksBrickList.add(ChangeHeightBrick(BrickValues.CHANGE_SIZE_BY))

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_effects), template))
        looksBrickList.add(EasePropertyBrick(0, 0, 100f, 250f, 1f))
        looksBrickList.add(SetTransparencyBrick(BrickValues.SET_TRANSPARENCY))
        looksBrickList.add(ChangeTransparencyByNBrick(BrickValues.CHANGE_TRANSPARENCY_EFFECT))
        looksBrickList.add(SetBrightnessBrick(BrickValues.SET_BRIGHTNESS_TO))
        looksBrickList.add(ChangeBrightnessByNBrick(BrickValues.CHANGE_BRIGHTNESS_BY))
        looksBrickList.add(SetColorBrick(BrickValues.SET_COLOR_TO))
        looksBrickList.add(ChangeColorByNBrick(BrickValues.CHANGE_COLOR_BY))
        looksBrickList.add(SetCornerOffsetsBrick(Formula(0), Formula(0), Formula(50), Formula(100), Formula(0), Formula(0), Formula(0), Formula(0)))
        looksBrickList.add(FadeParticleEffectBrick())
        looksBrickList.add(ParticleEffectAdditivityBrick())
        looksBrickList.add(SetParticleColorBrick(BrickValues.PARTICLE_COLOR))
        looksBrickList.add(ClearGraphicEffectBrick())

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_dialogs), template))
        if (!isBackgroundSprite) {
            looksBrickList.add(SayBubbleBrick(context.getString(R.string.brick_say_bubble_default_value)))
            looksBrickList.add(SayForBubbleBrick(context.getString(R.string.brick_say_bubble_default_value), 1.0f))
            looksBrickList.add(ThinkBubbleBrick(context.getString(R.string.brick_think_bubble_default_value)))
            looksBrickList.add(ThinkForBubbleBrick(context.getString(R.string.brick_think_bubble_default_value), 1.0f))
        }
        looksBrickList.add(AskBrick(context.getString(R.string.brick_ask_default_question)))
        looksBrickList.add(BigAskBrick(context.getString(R.string.brick_ask_default_question), context.getString(R.string.ask_default_placeholder), context.getString(R.string.ask_default_ok), context.getString(R.string.ask_default_cancel), context.getString(R.string.ask_default_answer_name)))
        looksBrickList.add(ShowTextBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
        looksBrickList.add(ShowTextColorSizeAlignmentBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
        looksBrickList.add(CreateDialogBrick("myDialog", context.getString(R.string.cbf_dialog_title), context.getString(R.string.cbf_dialog_answer)))
        looksBrickList.add(SetPositiveBrick("myDialog", context.getString(R.string.cbf_dialog_yes)))
        looksBrickList.add(SetNeutralBrick("myDialog", context.getString(R.string.cbf_dialog_later)))
        looksBrickList.add(SetNegativeBrick("myDialog", context.getString(R.string.cbf_dialog_no)))
        looksBrickList.add(AddEditBrick("myDialog", context.getString(R.string.cbf_dialog_edit_hint)))
        looksBrickList.add(AddRadioBrick("myDialog", context.getString(R.string.cbf_dialog_radio_hint)))
        looksBrickList.add(SetCallbackBrick("myDialog"))
        looksBrickList.add(ShowDialogBrick("myDialog"))

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_camera2d), template))
        looksBrickList.add(SetCameraFocusPointBrick())
        looksBrickList.add(SetCameraPosition2Brick(100, 300))
        looksBrickList.add(SetCameraRotation2Brick(90.0))
        looksBrickList.add(SetCameraZoomBrick(3.0))
        looksBrickList.add(PinToCameraBrick())
        looksBrickList.add(UnpinFromCameraBrick())

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_camera_device), template))
        if (!ProjectManager.getInstance().currentProject.isCastProject) {
            looksBrickList.add(CameraBrick())
            looksBrickList.add(ChooseCameraBrick())
            looksBrickList.add(FlashBrick())
        }
        when {
            !isBackgroundSprite -> looksBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
            ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> looksBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
            else -> looksBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
        }
        looksBrickList.add(ScreenShotBrick())
        looksBrickList.add(PhotoBrick())
        looksBrickList.add(CutLookBrick(100, 200, 300, 400))
        looksBrickList.add(SaveLookBrick("my_actor.png"))
        looksBrickList.add(LookFileBrick("my_actor.png"))
        looksBrickList.add(SaveLookFilesBrick("look.png"))
        looksBrickList.add(SetLookFilesBrick("look.png"))
        looksBrickList.add(LookToTableBrick("rTable", "gTable", "bTable", "aTable"))
        looksBrickList.add(LookFromTableBrick("rTable", "gTable", "bTable", "aTable"))

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_primitives), template))
        looksBrickList.add(SquareBrick("square", "#ff0000", 0f, 0f, 100f, 100f, 1f, 0f, 0f))
        looksBrickList.add(DelSquareBrick("square"))
        looksBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
        looksBrickList.add(OpenAppBrick(Formula("com.android.settings")))
        if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) {
            looksBrickList.add(PhiroRGBLightBrick(PhiroRGBLightBrick.Eye.BOTH, BrickValues.PHIRO_VALUE_RED, BrickValues.PHIRO_VALUE_GREEN, BrickValues.PHIRO_VALUE_BLUE))
        }

        looksBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_looks_buffers), template))
        looksBrickList.add(CreateBufferBrick("Map", "512", "512"))
        looksBrickList.add(SetBufferModeBrick(Formula("Map"), Formula(1), Formula(0)))
        looksBrickList.add(SetBufferAutoUpdateBrick(Formula("Map"), Formula(1)))
        looksBrickList.add(AddToBufferBrick("Map"))
        looksBrickList.add(RemoveFromBufferBrick(Formula("Map")))
        looksBrickList.add(SetBufferOnlyBrick(Formula(1)))
        looksBrickList.add(AddTextToBufferBrick("myText", "Map"))
        looksBrickList.add(SetTextBufferOnlyBrick("myText", 1))
        looksBrickList.add(SetBufferCameraBrick("Map", "100", "200", "1", "0"))
        looksBrickList.add(SetBufferCamera3DBrick(Formula("Map"), Formula(100), Formula(100), Formula(100), Formula(120), Formula(-20), Formula(0), Formula(67)))
        looksBrickList.add(ApplyBufferLookBrick("Map"))
        looksBrickList.add(BufferMaskBrick("Map", 1))
        looksBrickList.add(SetBufferShaderBrick("Map", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    
    texColor.rgb = 1.0 - texColor.rgb;
    
    gl_FragColor = v_color * texColor;
}"""))
        looksBrickList.add(SetBufferShaderUniformBrick("Map", "time", 1f, 0f, 0f, 1))
        looksBrickList.add(SaveBufferBrick(Formula("Map"), Formula("buffer.png")))
        looksBrickList.add(StartBufferRecordingBrick("myBuffer", "output.mp4", 30, 2000000))
        looksBrickList.add(StopBufferRecordingBrick())
        looksBrickList.add(SetMainRenderLoopsBrick(1f, 1f, 1f))
        looksBrickList.add(SetBufferEffectsBrick("Map", 1f, 1f))

        return looksBrickList
    }

    private fun setupPenCategoryList(isBackgroundSprite: Boolean): List<Brick> {
        val penBrickList: MutableList<Brick> = ArrayList()
        if (!isBackgroundSprite) {
            penBrickList.add(PenDownBrick())
            penBrickList.add(PenUpBrick())
            penBrickList.add(SetPenSizeBrick(BrickValues.PEN_SIZE))
            penBrickList.add(SetPenColorBrick(BrickValues.PEN_COLOR_R, BrickValues.PEN_COLOR_G, BrickValues.PEN_COLOR_B))
            penBrickList.add(SetPenPropertyBrick(0, "1"))
            penBrickList.add(PenDrawLineBrick(0.0, 0.0, 100.0, 100.0, 5.0))
            penBrickList.add(PenDrawTriangleBrick(-50.0, -50.0, 0.0, 50.0, 50.0, -50.0, 1))
            penBrickList.add(PenDrawRectBrick(0.0, 0.0, 100.0, 100.0, 1))
            penBrickList.add(PenDrawCircleBrick(0.0, 0.0, 50.0, 0.0, 360.0, 1));
            penBrickList.add(PenFlushBrick())
            penBrickList.add(StampBrick())
            penBrickList.add(PenClearColorBrick("#FFFFFF", 50.0))
        }
        penBrickList.add(ClearBackgroundBrick())
        return penBrickList
    }

    protected open fun setupDataCategoryList(
        context: Context,
        isBackgroundSprite: Boolean
    ): List<Brick> {
        val category = context.getString(R.string.category_data)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val dataBrickList: MutableList<Brick> = ArrayList()
                dataBrickList.add(SetVariableBrick(BrickValues.SET_VARIABLE))
                dataBrickList.add(ChangeVariableBrick(BrickValues.CHANGE_VARIABLE))
                dataBrickList.add(SetVariableEasingBrick(1, 0f, 10f, 0f, 100f))
                dataBrickList.add(ShowTextBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
                dataBrickList.add(ShowTextColorSizeAlignmentBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
                dataBrickList.add(ShowVarFontBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR, "font.ttf"))
                dataBrickList.add(ShowText3Brick("myText", "Hello!", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
                dataBrickList.add(ShowTextFontBrick("myText", "Ababuy!", "font.ttf", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
                dataBrickList.add(ShowTextRotationBrick("myText", "Ababuy!", 90f, "font.ttf", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
                dataBrickList.add(SetTextPropertyBrick("myText", 0, "100"))
                dataBrickList.add(HideTextBrick())
                dataBrickList.add(HideText3Brick("myText"))
                dataBrickList.add(WriteVariableOnDeviceBrick())
                dataBrickList.add(ReadVariableFromDeviceBrick())
                dataBrickList.add(WriteVariableToFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
                dataBrickList.add(ReadVariableFromFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
                dataBrickList.add(WriteToFilesBrick("variable.txt"))
                dataBrickList.add(ReadFromFilesBrick("variable.txt"))
                dataBrickList.add(DeleteFilesBrick("variable.txt"))
                //dataBrickList.add(SaveLookBrick("my_actor.png"))
                dataBrickList.add(FileUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
                dataBrickList.add(FilesUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
                dataBrickList.add(ZipProjectFilesBrick("file1.txt, file2.png", "archive.zip"))
                dataBrickList.add(UnzipProjectFilesBrick("archive.zip"))
                dataBrickList.add(GetZipFileNamesBrick("myZip.zip"))
                dataBrickList.add(ZipBrick("myZip.zip", "my_actor.png,fileFromUrl.jpg"))
                dataBrickList.add(UnzipBrick("myZip.zip"))
                dataBrickList.add(AddItemToUserListBrick(BrickValues.ADD_ITEM_TO_USERLIST))
                dataBrickList.add(DeleteItemOfUserListBrick(BrickValues.DELETE_ITEM_OF_USERLIST))
                dataBrickList.add(ClearUserListBrick())
                dataBrickList.add(InsertItemIntoUserListBrick(BrickValues.INSERT_ITEM_INTO_USERLIST_VALUE, BrickValues.INSERT_ITEM_INTO_USERLIST_INDEX))
                dataBrickList.add(ReplaceItemInUserListBrick(BrickValues.REPLACE_ITEM_IN_USERLIST_VALUE, BrickValues.REPLACE_ITEM_IN_USERLIST_INDEX))
                dataBrickList.add(WriteListOnDeviceBrick())
                dataBrickList.add(ReadListFromDeviceBrick())
                dataBrickList.add(StoreCSVIntoUserListBrick(BrickValues.STORE_CSV_INTO_USERLIST_COLUMN, context.getString(R.string.brick_store_csv_into_userlist_data)))
                dataBrickList.add(SplitBrick(context.getString(R.string.brick_store_csv_into_userlist_data), ","))
                dataBrickList.add(StringToTableBrick("1,2,3\n4,5,6\n7,8,9", ",", "\n", "myTable"))
                dataBrickList.add(RegexBrick("panda ananas", "a[^n]+"))
                dataBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
                dataBrickList.add(PostWebRequestBrick("https://api.calfire.com/v2/texts?limit=50&offset=200",
                    "Content-Type:application/json",
                    "{\nusername=password\n}"))
                dataBrickList.add(CreateVarBrick("variable1", "0"))
                dataBrickList.add(DeleteVarBrick("variable1"))
                dataBrickList.add(DeleteVarsBrick())
                dataBrickList.add(CreateTableBrick("myTable", 5, 5))
                dataBrickList.add(InsertTableBrick("myTable", "1", 3, 2))
                dataBrickList.add(DeleteTableBrick("myTable"))
                dataBrickList.add(DeleteAllTablesBrick())
                dataBrickList.add(ShowToastBlock("Hello World!"))
                dataBrickList.add(CopyTextBrick(context.getString(R.string.cbf_copy_example)))
                dataBrickList.add(ListenMicroBrick("100"))
                dataBrickList.add(SquareBrick("square", "#ff0000", 0f, 0f, 100f, 100f, 1f, 0f, 0f))
                dataBrickList.add(DelSquareBrick("square"))
                when {
                    !isBackgroundSprite -> dataBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
                    ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> dataBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
                    else -> dataBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
                }
                dataBrickList.add(AskBrick(context.getString(R.string.brick_ask_default_question)))
                if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
                    dataBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
                }
                if (SettingsFragment.isEmroiderySharedPreferenceEnabled(context)) {
                    dataBrickList.add(WriteEmbroideryToFileBrick(context.getString(R.string.brick_default_embroidery_file)))
                }
                if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
                    dataBrickList.add(StartListeningBrick())
                }
                if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
                    dataBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
                }
                return dataBrickList
            }
        }

        val dataBrickList: MutableList<Brick> = ArrayList()
        val template = DeleteVarsBrick()

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_variables), template))
        dataBrickList.add(SetVariableBrick(BrickValues.SET_VARIABLE))
        dataBrickList.add(ChangeVariableBrick(BrickValues.CHANGE_VARIABLE))
        dataBrickList.add(SetVariableEasingBrick(1, 0f, 10f, 0f, 100f))
        dataBrickList.add(CreateVarBrick("variable1", "0"))
        dataBrickList.add(DeleteVarBrick("variable1"))
        dataBrickList.add(DeleteVarsBrick())

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_lists), template))
        dataBrickList.add(AddItemToUserListBrick(BrickValues.ADD_ITEM_TO_USERLIST))
        dataBrickList.add(DeleteItemOfUserListBrick(BrickValues.DELETE_ITEM_OF_USERLIST))
        dataBrickList.add(InsertItemIntoUserListBrick(BrickValues.INSERT_ITEM_INTO_USERLIST_VALUE, BrickValues.INSERT_ITEM_INTO_USERLIST_INDEX))
        dataBrickList.add(ReplaceItemInUserListBrick(BrickValues.REPLACE_ITEM_IN_USERLIST_VALUE, BrickValues.REPLACE_ITEM_IN_USERLIST_INDEX))
        dataBrickList.add(ClearUserListBrick())
        dataBrickList.add(CreateTableBrick("myTable", 5, 5))
        dataBrickList.add(InsertTableBrick("myTable", "1", 3, 2))
        dataBrickList.add(DeleteTableBrick("myTable"))
        dataBrickList.add(DeleteAllTablesBrick())

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_visuals), template))
        dataBrickList.add(ShowTextBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION))
        dataBrickList.add(ShowTextColorSizeAlignmentBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
        dataBrickList.add(ShowVarFontBrick(BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR, "font.ttf"))
        dataBrickList.add(ShowText3Brick("myText", "Hello!", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
        dataBrickList.add(ShowTextFontBrick("myText", "Ababuy!", "font.ttf", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
        dataBrickList.add(ShowTextRotationBrick("myText", "Ababuy!", 90f, "font.ttf", BrickValues.X_POSITION, BrickValues.Y_POSITION, BrickValues.RELATIVE_SIZE_IN_PERCENT, BrickValues.SHOW_VARIABLE_COLOR))
        dataBrickList.add(SetTextPropertyBrick("myText", 0, "100"))
        dataBrickList.add(HideTextBrick())
        dataBrickList.add(HideText3Brick("myText"))
        dataBrickList.add(ShowToastBlock("Hello World!"))
        dataBrickList.add(SquareBrick("square", "#ff0000", 0f, 0f, 100f, 100f, 1f, 0f, 0f))
        dataBrickList.add(DelSquareBrick("square"))

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_processing), template))
        dataBrickList.add(StoreCSVIntoUserListBrick(BrickValues.STORE_CSV_INTO_USERLIST_COLUMN, context.getString(R.string.brick_store_csv_into_userlist_data)))
        dataBrickList.add(SplitBrick(context.getString(R.string.brick_store_csv_into_userlist_data), ","))
        dataBrickList.add(StringToTableBrick("1,2,3\n4,5,6\n7,8,9", ",", "\n", "myTable"))
        dataBrickList.add(RegexBrick("panda ananas", "a[^n]+"))
        dataBrickList.add(CopyTextBrick(context.getString(R.string.cbf_copy_example)))

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_files), template))
        dataBrickList.add(WriteVariableOnDeviceBrick())
        dataBrickList.add(ReadVariableFromDeviceBrick())
        dataBrickList.add(WriteVariableToFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        dataBrickList.add(ReadVariableFromFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        dataBrickList.add(WriteToFilesBrick("variable.txt"))
        dataBrickList.add(ReadFromFilesBrick("variable.txt"))
        dataBrickList.add(DeleteFilesBrick("variable.txt"))
        dataBrickList.add(WriteListOnDeviceBrick())
        dataBrickList.add(ReadListFromDeviceBrick())
        dataBrickList.add(ZipProjectFilesBrick("file1.txt, file2.png", "archive.zip"))
        dataBrickList.add(UnzipProjectFilesBrick("archive.zip"))
        dataBrickList.add(GetZipFileNamesBrick("myZip.zip"))
        dataBrickList.add(ZipBrick("myZip.zip", "my_actor.png,fileFromUrl.jpg"))
        dataBrickList.add(UnzipBrick("myZip.zip"))

        dataBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_data_web), template))
        dataBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
        dataBrickList.add(PostWebRequestBrick("https://api.calfire.com/v2/texts?limit=50&offset=200",
            "Content-Type:application/json",
            "{\nusername=password\n}"))
        dataBrickList.add(FileUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
        dataBrickList.add(FilesUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
        when {
            !isBackgroundSprite -> dataBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
            ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> dataBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
            else -> dataBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
        }
        dataBrickList.add(AskBrick(context.getString(R.string.brick_ask_default_question)))
        dataBrickList.add(ListenMicroBrick("100"))
        if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
            dataBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
            dataBrickList.add(StartListeningBrick())
        }
        if (SettingsFragment.isEmroiderySharedPreferenceEnabled(context)) {
            dataBrickList.add(WriteEmbroideryToFileBrick(context.getString(R.string.brick_default_embroidery_file)))
        }
        if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
            dataBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
        }

        return dataBrickList
    }

    @SuppressWarnings("ComplexMethod")
    protected fun setupDeviceCategoryList(context: Context, isBackgroundSprite: Boolean): List<Brick> {
        val category = context.getString(R.string.category_device)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val deviceBrickList: MutableList<Brick> = ArrayList()
                deviceBrickList.add(ShowToastBlock("Hello World"))
                deviceBrickList.add(CopyTextBrick(context.getString(R.string.cbf_copy_example)))
                deviceBrickList.add(ShowNotificationBrick("123456", "NewCatroid", context.getString(R.string.cbf_notif_title), context.getString(R.string.cbf_notif_text), "icon.png"))
                deviceBrickList.add(NotificationCreateBrick("123456", "NewCatroid", context.getString(R.string.cbf_notif2_title), context.getString(R.string.cbf_notif2_text), "icon.png"))
                deviceBrickList.add(NotificationAddButtonBrick("123456", "123", "CLICK ME", "none.png", "Enter any text..."))
                deviceBrickList.add(NotificationShowBrick("123456", 60.0))
                deviceBrickList.add(NotificationCancelBrick("123456"))
                deviceBrickList.add(EnableBackgroundModeBrick(context.getString(R.string.cbf_bg_title), context.getString(R.string.cbf_bg_text)))
                deviceBrickList.add(DisableBackgroundModeBrick())
                deviceBrickList.add(ListenMicroBrick("100"))
                deviceBrickList.add(RunJSBrick("1 + 2"))
                deviceBrickList.add(RunLuaBrick(context.getString(R.string.cbf_lua_example)))
                deviceBrickList.add(LunoScriptBrick("MakeToast(\"Hey from Luno! :)\");"))
                deviceBrickList.add(
                    JavaCompileBrick(
                        Formula("Main.java"),
                        Formula("classes.dex"),
                        null
                    )
                )
                deviceBrickList.add(
                    JavaLoadAndRunBrick(
                        Formula("classes.dex, libs.jar"),
                        Formula("game.Main"),
                        Formula("onStart"),
                        Formula("Hello from NewCatroid!")
                    )
                )
                deviceBrickList.add(
                    JavaDownloadDepBrick(
                        Formula("org.jsoup:jsoup:1.13.1"),
                        Formula(""),
                        null,
                        true
                    )
                )
                deviceBrickList.add(ClearPythonEnvironmentBrick())
                deviceBrickList.add(LoadNativeModuleBrick("libz.so"))
                deviceBrickList.add(LoadPythonLibraryBrick("telebot.whl"))
                deviceBrickList.add(RunPythonScriptBrick("""import telebot

API_TOKEN = 'YOUR_API_TOKEN' #Telegram Bot API

bot = telebot.TeleBot(API_TOKEN)

@bot.message_handler(commands=['start'])
def send_welcome(message):
    bot.reply_to(message, "Welcome! I am a simple test bot.")

@bot.message_handler(commands=['hello'])
def send_hello(message):
    bot.reply_to(message, "Hello there!")

print("Bot is starting...")
bot.polling()
print("Bot has stopped.")""", "myVar"))
                deviceBrickList.add(RunShellBrick("pip install --file pyTelegramBotApi"))
                deviceBrickList.add(OpenFileBrick("fileFromUrl.txt"))
                deviceBrickList.add(MoveFilesBrick("variable.txt"))
                deviceBrickList.add(MoveDownloadsBrick("variable.txt"))
                deviceBrickList.add(CopyProjectFileBrick("variable.txt", "copy_variable.txt"))
                //deviceBrickList.add(OpenFilesBrick("my_actor.png"))
                deviceBrickList.add(OrientationBrick())
                deviceBrickList.add(CreateWebUrlBrick("myWebView", "https://google.com", "0", "0", "500", "700"))
                deviceBrickList.add(CreateWebFileBrick("myWebView", context.getString(R.string.cbf_web_html_full), "0", "0", "500", "700"))
                deviceBrickList.add(SetWebBrick("myWebView"))
                deviceBrickList.add(EvalWebBrick("Android.postMessage(\"Hello from WebView!\");", "myWebView"))
                deviceBrickList.add(CreateVideoBrick("myVideoPlayer", "video.mp4", 0, 0, 750, 500, 1, 0))
                deviceBrickList.add(PlayVideoBrick("myVideoPlayer"))
                deviceBrickList.add(PauseVideoBrick("myVideoPlayer"))
                deviceBrickList.add(SeekVideoBrick("myVideoPlayer", 30))
                deviceBrickList.add(CreateTextFieldBrick("myTextField", "", 300, 500, 300, 200, 22, "#FFFFFF", "#88000000", context.getString(R.string.cbf_textfield_hint), "#CCCCCC", "left", 0, 5, -1, "text", context.getString(R.string.cbf_textfield_default)))
                //deviceBrickList.add(NativeLayerBrick(0))
                deviceBrickList.add(AttachSOBrick("glView", "mylib.so"))
                deviceBrickList.add(CreateGLViewBrick("glView", 100, 200, 500, 300))
                deviceBrickList.add(SetViewPositionBrick("myVideoPlayer", 100, 200))
                deviceBrickList.add(SetNativeParentBrick("myTextView", "myScrollView"))
                deviceBrickList.add(
                    CreateScrollViewBrick(
                        "myScrollView",
                        1,
                        "#22000000",
                        true,
                        16,
                        1,
                        100,
                        200,
                        500,
                        600
                    )
                )
                deviceBrickList.add(
                    CreateTextLabelBrick(
                        "myTextView",
                        context.getString(R.string.cbf_label_html),
                        "#FFFFFF",
                        18.0,
                        "#44000000",
                        "fonts/myfont.ttf",
                        8,
                        1.2,
                        1,
                        1,
                        0,
                        150,
                        250,
                        400,
                        150
                    )
                )
                deviceBrickList.add(
                    CreateButtonBrick(
                        "myButton",
                        context.getString(R.string.cbf_button_text),
                        "#FFFFFF",
                        18.0,
                        "#00C853",
                        12,
                        100,
                        300,
                        350,
                        100
                    )
                );
                deviceBrickList.add(
                    CreateSwitchBrick(
                        "mySwitch",
                        context.getString(R.string.cbf_switch_text),
                        1,
                        100,
                        450,
                        400,
                        80
                    )
                )
                deviceBrickList.add(
                    CreateImageViewBrick(
                        "myImage",
                        "https://image.pollinations.ai/prompt/cat",
                        16,
                        1,
                        100,
                        100,
                        300,
                        300
                    )
                )
                deviceBrickList.add(CreateSliderBrick("mySlider", 0.0, 1.0, 0.5, 100, 420, 500, 60))
                deviceBrickList.add(NativeViewConfigBrick("myWebView", 1, 0.5f))
                deviceBrickList.add(NativeViewControlBrick("myWebView", 5, "https://google.com"))
                deviceBrickList.add(NativeViewListenerBrick("myWebView", 0, null))
                deviceBrickList.add(NativeViewAnimateBrick("myButton", 0, 1.0, 500, 29))
                deviceBrickList.add(NativeViewBindSpriteBrick("myWebView", "Sprite1", 1, 50, 100))
                deviceBrickList.add(DeleteWebBrick("myWebView"))
                deviceBrickList.add(BindVmOutputBrick())
                deviceBrickList.add(RunVm2Brick("-kernel \"%PROJECT_FILES%/bzImage\" -initrd \"%PROJECT_FILES%/core.gz\" -append \"console=ttyS0 quiet\""))
                deviceBrickList.add(RunVMBrick("1024", "2", "myDisk.qcow2", "flash.iso"))
                deviceBrickList.add(CreateDiskBrick("myDisk.qcow2", "10G"))
                deviceBrickList.add(ToggleDisplayBrick(1))
                deviceBrickList.add(VmSetMonitorSizeBrick(1000, 800))
                deviceBrickList.add(MouseEventBrick("0", "100", 1))
                deviceBrickList.add(VmRelativeMouseMoveBrick(0, 100, 1))
                deviceBrickList.add(KeyEventBrick("a", 1))
                deviceBrickList.add(SendVmInputBrick("ls ~/"))
                deviceBrickList.add(StopVMBrick())
                deviceBrickList.add(RunChip8Brick("tetris.ch8"))
                deviceBrickList.add(ScreenShotBrick())
                deviceBrickList.add(ResetTimerBrick())
                deviceBrickList.add(TestBrick())
                deviceBrickList.add(HideStatusBarBrick())
                deviceBrickList.add(ChooseFileBrick())
                deviceBrickList.add(ExportProjectFileBrick("file.txt"))
                deviceBrickList.add(ShareBrick(context.getString(R.string.cbf_share_text)));
                deviceBrickList.add(SaveToInternalStorageBrick("file.txt", "myAwesomeApp/file.txt"))
                deviceBrickList.add(LoadFromInternalStorageBrick("myAwesomeApp/file.txt"))
                deviceBrickList.add(ApplyShaderToImageBrick("image.png", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main() {
    vec4 original = texture2D(u_texture, v_texCoords);
    float roughness = original.r;
    float metallic = 0.0;
    gl_FragColor = vec4(1.0, roughness, metallic, 1.0);
}"""))
                deviceBrickList.add(WhenBrick())
                deviceBrickList.add(WhenTouchDownBrick())
                if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
                    deviceBrickList.add(WhenNfcBrick())
                    deviceBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
                }
                deviceBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
                when {
                    !isBackgroundSprite -> deviceBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
                    ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> deviceBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
                    else -> deviceBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
                }
                deviceBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
                deviceBrickList.add(OpenAppBrick(Formula("com.android.settings")))
                deviceBrickList.add(VibrationBrick(BrickValues.VIBRATE_SECONDS))

                if (SettingsFragment.isAISpeechSynthetizationSharedPreferenceEnabled(context)) {
                    deviceBrickList.add(SpeakBrick(context.getString(R.string.brick_speak_default_value)))
                    deviceBrickList.add(SpeakAndWaitBrick(context.getString(R.string.brick_speak_default_value)))
                }

                if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
                    deviceBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
                    deviceBrickList.add(StartListeningBrick())
                }
                if (ProjectManager.getInstance().currentProject != null && !ProjectManager.getInstance().currentProject.isCastProject) {
                    deviceBrickList.add(CameraBrick())
                    deviceBrickList.add(ChooseCameraBrick())
                    deviceBrickList.add(FlashBrick())
                }
                deviceBrickList.add(WriteVariableOnDeviceBrick())
                deviceBrickList.add(ReadVariableFromDeviceBrick())
                deviceBrickList.add(WriteVariableToFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
                deviceBrickList.add(ReadVariableFromFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
                deviceBrickList.add(WriteListOnDeviceBrick())
                deviceBrickList.add(ReadListFromDeviceBrick())
                deviceBrickList.add(TapAtBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START))
                deviceBrickList.add(TapForBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_DURATION))
                deviceBrickList.add(TouchAndSlideBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_X_GOAL, BrickValues.TOUCH_Y_GOAL, BrickValues.TOUCH_DURATION))
                if (SettingsFragment.isCastSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupChromecastCategoryList(context))
                if (SettingsFragment.isMindstormsNXTSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupLegoNxtCategoryList())
                if (SettingsFragment.isMindstormsEV3SharedPreferenceEnabled(context)) deviceBrickList.addAll(setupLegoEv3CategoryList())
                if (SettingsFragment.isDroneSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupDroneCategoryList())
                if (SettingsFragment.isJSSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupJumpingSumoCategoryList())
                if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupPhiroProCategoryList())
                if (SettingsFragment.isArduinoSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupArduinoCategoryList())
                if (SettingsFragment.isRaspiSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupRaspiCategoryList())
                if (SettingsFragment.isEmroiderySharedPreferenceEnabled(context)) deviceBrickList.addAll(setupEmbroideryCategoryList(context))
                return deviceBrickList
            }
        }

        val deviceBrickList: MutableList<Brick> = ArrayList()
        val template = ResetTimerBrick()

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_scripting), template))
        deviceBrickList.add(RunJSBrick("1 + 2"))
        deviceBrickList.add(RunLuaBrick(context.getString(R.string.cbf_lua_example)))
        deviceBrickList.add(LunoScriptBrick("MakeToast(\"Hey from Luno! :)\");"))
        deviceBrickList.add(
            JavaCompileBrick(
                Formula("Main.java"),
                Formula("classes.dex"),
                null
            )
        )
        deviceBrickList.add(
            JavaLoadAndRunBrick(
                Formula("classes.dex, libs.jar"),
                Formula("game.Main"),
                Formula("onStart"),
                Formula("Hello from NewCatroid!")
            )
        )
        deviceBrickList.add(
            JavaDownloadDepBrick(
                Formula("org.jsoup:jsoup:1.13.1"),
                Formula(""),
                null,
                true
            )
        )
        deviceBrickList.add(RunPythonScriptBrick("""import telebot
API_TOKEN = 'YOUR_API_TOKEN' #Telegram Bot API
bot = telebot.TeleBot(API_TOKEN)
@bot.message_handler(commands=['start'])
def send_welcome(message):
    bot.reply_to(message, "Welcome! I am a simple test bot.")
print("Bot is starting...")
bot.polling()""", "myVar"))
        deviceBrickList.add(LoadNativeModuleBrick("libz.so"))
        deviceBrickList.add(LoadPythonLibraryBrick("telebot.whl"))
        deviceBrickList.add(ClearPythonEnvironmentBrick())
        deviceBrickList.add(RunShellBrick("pip install --file pyTelegramBotApi"))

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_vm), template))
        deviceBrickList.add(RunVMBrick("1024", "2", "myDisk.qcow2", "flash.iso"))
        deviceBrickList.add(RunVm2Brick("-kernel \"%PROJECT_FILES%/bzImage\" -initrd \"%PROJECT_FILES%/core.gz\" -append \"console=ttyS0 quiet\""))
        deviceBrickList.add(StopVMBrick())
        deviceBrickList.add(CreateDiskBrick("myDisk.qcow2", "10G"))
        deviceBrickList.add(ToggleDisplayBrick(1))
        deviceBrickList.add(VmSetMonitorSizeBrick(1000, 800))
        deviceBrickList.add(MouseEventBrick("0", "100", 1))
        deviceBrickList.add(VmRelativeMouseMoveBrick(0, 100, 1))
        deviceBrickList.add(KeyEventBrick("a", 1))
        deviceBrickList.add(SendVmInputBrick("ls ~/"))
        deviceBrickList.add(BindVmOutputBrick())
        deviceBrickList.add(RunChip8Brick("tetris.ch8"))

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_views), template))
        deviceBrickList.add(CreateWebUrlBrick("myWebView", "https://google.com", "0", "0", "500", "700"))
        deviceBrickList.add(CreateWebFileBrick("myWebView", context.getString(R.string.cbf_web_html_simple), "0", "0", "500", "700"))
        deviceBrickList.add(SetWebBrick("myWebView"))
        deviceBrickList.add(EvalWebBrick("Android.postMessage(\"Hello from WebView!\");", "myWebView"))
        deviceBrickList.add(CreateVideoBrick("myVideoPlayer", "video.mp4", 0, 0, 750, 500, 1, 0))
        deviceBrickList.add(PlayVideoBrick("myVideoPlayer"))
        deviceBrickList.add(PauseVideoBrick("myVideoPlayer"))
        deviceBrickList.add(SeekVideoBrick("myVideoPlayer", 30))
        deviceBrickList.add(CreateTextFieldBrick("myTextField", "", 300, 500, 300, 200, 22, "#FFFFFF", "#88000000", context.getString(R.string.cbf_textfield_hint), "#CCCCCC", "left", 0, 5, -1, "text", context.getString(R.string.cbf_textfield_default)))
        deviceBrickList.add(AttachSOBrick("glView", "mylib.so"))
        deviceBrickList.add(CreateGLViewBrick("glView", 100, 200, 500, 300))
        deviceBrickList.add(SetViewPositionBrick("myVideoPlayer", 100, 200))
        deviceBrickList.add(SetNativeParentBrick("myTextView", "myScrollView"))
        deviceBrickList.add(
            CreateScrollViewBrick(
                "myScrollView",
                1,
                "#22000000",
                true,
                16,
                1,
                100,
                200,
                500,
                600
            )
        )
        deviceBrickList.add(
            CreateTextLabelBrick(
                "myTextView",
                context.getString(R.string.cbf_label_html),
                "#FFFFFF",
                18.0,
                "#44000000",
                "fonts/myfont.ttf",
                8,
                1.2,
                1,
                1,
                0,
                150,
                250,
                400,
                150
            )
        )
        deviceBrickList.add(
            CreateButtonBrick(
                "myButton",
                context.getString(R.string.cbf_button_text),
                "#FFFFFF",
                18.0,
                "#00C853",
                12,
                100,
                300,
                350,
                100
            )
        )
        deviceBrickList.add(
            CreateSwitchBrick(
                "mySwitch",
                context.getString(R.string.cbf_switch_text),
                1,
                100,
                450,
                400,
                80
            )
        )
        deviceBrickList.add(
            CreateImageViewBrick(
                "myImage",
                "https://image.pollinations.ai/prompt/cat",
                16,
                1,
                100,
                100,
                300,
                300
            )
        )
        deviceBrickList.add(CreateSliderBrick("mySlider", 0.0, 1.0, 0.5, 100, 420, 500, 60))
        deviceBrickList.add(NativeViewConfigBrick("myWebView", 1, 0.5f))
        deviceBrickList.add(NativeViewControlBrick("myWebView", 5, "https://google.com"))
        deviceBrickList.add(NativeViewListenerBrick("myWebView", 0, null))
        deviceBrickList.add(NativeViewAnimateBrick("myButton", 0, 1.0, 500, 29))
        deviceBrickList.add(NativeViewBindSpriteBrick("myWebView", "Sprite1", 1, 50, 100))
        deviceBrickList.add(DeleteWebBrick("myWebView"))

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_storage), template))
        deviceBrickList.add(OpenFileBrick("fileFromUrl.txt"))
        deviceBrickList.add(ChooseFileBrick())
        deviceBrickList.add(MoveFilesBrick("variable.txt"))
        deviceBrickList.add(MoveDownloadsBrick("variable.txt"))
        deviceBrickList.add(CopyProjectFileBrick("variable.txt", "copy_variable.txt"))
        deviceBrickList.add(ExportProjectFileBrick("file.txt"))
        deviceBrickList.add(ShareBrick(context.getString(R.string.cbf_share_text)));
        deviceBrickList.add(SaveToInternalStorageBrick("file.txt", "myAwesomeApp/file.txt"))
        deviceBrickList.add(LoadFromInternalStorageBrick("myAwesomeApp/file.txt"))
        deviceBrickList.add(WriteVariableOnDeviceBrick())
        deviceBrickList.add(ReadVariableFromDeviceBrick())
        deviceBrickList.add(WriteVariableToFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        deviceBrickList.add(ReadVariableFromFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        deviceBrickList.add(WriteListOnDeviceBrick())
        deviceBrickList.add(ReadListFromDeviceBrick())

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_hardware), template))
        deviceBrickList.add(OrientationBrick())
        deviceBrickList.add(VibrationBrick(BrickValues.VIBRATE_SECONDS))
        deviceBrickList.add(ScreenShotBrick())
        deviceBrickList.add(TestBrick())
        deviceBrickList.add(HideStatusBarBrick())
        if (ProjectManager.getInstance().currentProject != null && !ProjectManager.getInstance().currentProject.isCastProject) {
            deviceBrickList.add(CameraBrick())
            deviceBrickList.add(ChooseCameraBrick())
            deviceBrickList.add(FlashBrick())
        }

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_speech), template))
        deviceBrickList.add(ShowToastBlock("Hello World"))
        deviceBrickList.add(CopyTextBrick(context.getString(R.string.cbf_copy_example)))
        deviceBrickList.add(ShowNotificationBrick("123456", "NewCatroid", context.getString(R.string.cbf_notif_title), context.getString(R.string.cbf_notif_text), "icon.png"))
        deviceBrickList.add(NotificationCreateBrick("123456", "NewCatroid", context.getString(R.string.cbf_notif2_title), context.getString(R.string.cbf_notif2_text), "icon.png"))
        deviceBrickList.add(NotificationAddButtonBrick("123456", "123", "CLICK ME", "none.png", "Enter any text..."))
        deviceBrickList.add(NotificationShowBrick("123456", 60.0))
        deviceBrickList.add(NotificationCancelBrick("123456"))
        deviceBrickList.add(EnableBackgroundModeBrick(context.getString(R.string.cbf_bg_title), context.getString(R.string.cbf_bg_text)))
        deviceBrickList.add(DisableBackgroundModeBrick())
        deviceBrickList.add(ListenMicroBrick("100"))
        deviceBrickList.add(AskBrick(context.getString(R.string.brick_ask_default_question)))
        if (SettingsFragment.isAISpeechSynthetizationSharedPreferenceEnabled(context)) {
            deviceBrickList.add(SpeakBrick(context.getString(R.string.brick_speak_default_value)))
            deviceBrickList.add(SpeakAndWaitBrick(context.getString(R.string.brick_speak_default_value)))
        }
        if (SettingsFragment.isAISpeechRecognitionSharedPreferenceEnabled(context)) {
            deviceBrickList.add(AskSpeechBrick(context.getString(R.string.brick_ask_speech_default_question)))
            deviceBrickList.add(StartListeningBrick())
        }

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_emulation), template))
        deviceBrickList.add(TapAtBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START))
        deviceBrickList.add(TapForBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_DURATION))
        deviceBrickList.add(TouchAndSlideBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_X_GOAL, BrickValues.TOUCH_Y_GOAL, BrickValues.TOUCH_DURATION))
        deviceBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
        deviceBrickList.add(OpenUrlBrick(BrickValues.OPEN_IN_BROWSER))
        deviceBrickList.add(OpenAppBrick(Formula("com.android.settings")))
        when {
            !isBackgroundSprite -> deviceBrickList.add(LookRequestBrick(BrickValues.LOOK_REQUEST))
            ProjectManager.getInstance().currentProject.xmlHeader.islandscapeMode() -> deviceBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST_LANDSCAPE))
            else -> deviceBrickList.add(BackgroundRequestBrick(BrickValues.BACKGROUND_REQUEST))
        }

        deviceBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_device_misc), template))
        deviceBrickList.add(ResetTimerBrick())
        deviceBrickList.add(ApplyShaderToImageBrick("image.png", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main() {
    vec4 original = texture2D(u_texture, v_texCoords);
    float roughness = original.r;
    float metallic = 0.0;
    gl_FragColor = vec4(1.0, roughness, metallic, 1.0);
}"""))
        deviceBrickList.add(WhenBrick())
        deviceBrickList.add(WhenTouchDownBrick())
        if (SettingsFragment.isNfcSharedPreferenceEnabled(context)) {
            deviceBrickList.add(WhenNfcBrick())
            deviceBrickList.add(SetNfcTagBrick(context.getString(R.string.brick_set_nfc_tag_default_value)))
        }

        if (SettingsFragment.isCastSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupChromecastCategoryList(context))
        if (SettingsFragment.isMindstormsNXTSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupLegoNxtCategoryList())
        if (SettingsFragment.isMindstormsEV3SharedPreferenceEnabled(context)) deviceBrickList.addAll(setupLegoEv3CategoryList())
        if (SettingsFragment.isDroneSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupDroneCategoryList())
        if (SettingsFragment.isJSSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupJumpingSumoCategoryList())
        if (SettingsFragment.isPhiroSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupPhiroProCategoryList())
        if (SettingsFragment.isArduinoSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupArduinoCategoryList())
        if (SettingsFragment.isRaspiSharedPreferenceEnabled(context)) deviceBrickList.addAll(setupRaspiCategoryList())
        if (SettingsFragment.isEmroiderySharedPreferenceEnabled(context)) deviceBrickList.addAll(setupEmbroideryCategoryList(context))

        return deviceBrickList
    }

    private fun setupLegoNxtCategoryList(): List<Brick> {
        val legoNXTBrickList: MutableList<Brick> = ArrayList()
        legoNXTBrickList.add(LegoNxtMotorTurnAngleBrick(LegoNxtMotorTurnAngleBrick.Motor.MOTOR_A, BrickValues.LEGO_ANGLE))
        legoNXTBrickList.add(LegoNxtMotorStopBrick(LegoNxtMotorStopBrick.Motor.MOTOR_A))
        legoNXTBrickList.add(LegoNxtMotorMoveBrick(LegoNxtMotorMoveBrick.Motor.MOTOR_A, BrickValues.LEGO_SPEED))
        legoNXTBrickList.add(LegoNxtPlayToneBrick(BrickValues.LEGO_FREQUENCY, BrickValues.LEGO_DURATION))
        return legoNXTBrickList
    }

    private fun setupLegoEv3CategoryList(): List<Brick> {
        val legoEV3BrickList: MutableList<Brick> = ArrayList()
        legoEV3BrickList.add(LegoEv3MotorTurnAngleBrick(LegoEv3MotorTurnAngleBrick.Motor.MOTOR_A, BrickValues.LEGO_ANGLE))
        legoEV3BrickList.add(LegoEv3MotorMoveBrick(LegoEv3MotorMoveBrick.Motor.MOTOR_A, BrickValues.LEGO_SPEED))
        legoEV3BrickList.add(LegoEv3MotorStopBrick(LegoEv3MotorStopBrick.Motor.MOTOR_A))
        legoEV3BrickList.add(LegoEv3PlayToneBrick(BrickValues.LEGO_FREQUENCY, BrickValues.LEGO_DURATION, BrickValues.LEGO_VOLUME))
        legoEV3BrickList.add(LegoEv3SetLedBrick(LegoEv3SetLedBrick.LedStatus.LED_GREEN))
        return legoEV3BrickList
    }

    private fun setupDroneCategoryList(): List<Brick> {
        val droneBrickList: MutableList<Brick> = ArrayList()
        droneBrickList.add(DroneTakeOffLandBrick())
        droneBrickList.add(DroneEmergencyBrick())
        droneBrickList.add(DroneMoveUpBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneMoveDownBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneMoveLeftBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneMoveRightBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneMoveForwardBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneMoveBackwardBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneTurnLeftBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneTurnRightBrick(BrickValues.DRONE_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.DRONE_MOVE_BRICK_DEFAULT_POWER_PERCENT))
        droneBrickList.add(DroneFlipBrick())
        droneBrickList.add(DronePlayLedAnimationBrick())
        droneBrickList.add(DroneSwitchCameraBrick())
        return droneBrickList
    }

    private fun setupJumpingSumoCategoryList(): List<Brick> {
        val jumpingSumoBrickList: MutableList<Brick> = ArrayList()
        jumpingSumoBrickList.add(JumpingSumoMoveForwardBrick(BrickValues.JUMPING_SUMO_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.JUMPING_SUMO_MOVE_BRICK_DEFAULT_MOVE_POWER_PERCENT))
        jumpingSumoBrickList.add(JumpingSumoMoveBackwardBrick(BrickValues.JUMPING_SUMO_MOVE_BRICK_DEFAULT_TIME_MILLISECONDS, BrickValues.JUMPING_SUMO_MOVE_BRICK_DEFAULT_MOVE_POWER_PERCENT))
        jumpingSumoBrickList.add(JumpingSumoAnimationsBrick(JumpingSumoAnimationsBrick.Animation.SPIN))
        jumpingSumoBrickList.add(JumpingSumoSoundBrick(JumpingSumoSoundBrick.Sounds.DEFAULT, BrickValues.JUMPING_SUMO_SOUND_BRICK_DEFAULT_VOLUME_PERCENT))
        jumpingSumoBrickList.add(JumpingSumoNoSoundBrick())
        jumpingSumoBrickList.add(JumpingSumoJumpLongBrick())
        jumpingSumoBrickList.add(JumpingSumoJumpHighBrick())
        jumpingSumoBrickList.add(JumpingSumoRotateLeftBrick(BrickValues.JUMPING_SUMO_ROTATE_DEFAULT_DEGREE))
        jumpingSumoBrickList.add(JumpingSumoRotateRightBrick(BrickValues.JUMPING_SUMO_ROTATE_DEFAULT_DEGREE))
        jumpingSumoBrickList.add(JumpingSumoTurnBrick())
        jumpingSumoBrickList.add(JumpingSumoTakingPictureBrick())
        return jumpingSumoBrickList
    }

    private fun setupPhiroProCategoryList(): List<Brick> {
        val phiroProBrickList: MutableList<Brick> = ArrayList()
        phiroProBrickList.add(PhiroMotorMoveForwardBrick(PhiroMotorMoveForwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
        phiroProBrickList.add(PhiroMotorMoveBackwardBrick(PhiroMotorMoveBackwardBrick.Motor.MOTOR_LEFT, BrickValues.PHIRO_SPEED))
        phiroProBrickList.add(PhiroMotorStopBrick(PhiroMotorStopBrick.Motor.MOTOR_BOTH))
        phiroProBrickList.add(PhiroPlayToneBrick(PhiroPlayToneBrick.Tone.DO, BrickValues.PHIRO_DURATION))
        phiroProBrickList.add(PhiroRGBLightBrick(PhiroRGBLightBrick.Eye.BOTH, BrickValues.PHIRO_VALUE_RED, BrickValues.PHIRO_VALUE_GREEN, BrickValues.PHIRO_VALUE_BLUE))
        phiroProBrickList.add(PhiroIfLogicBeginBrick())
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_FRONT_LEFT))
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_FRONT_RIGHT))
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_SIDE_LEFT))
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_SIDE_RIGHT))
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_BOTTOM_LEFT))
        phiroProBrickList.add(SetVariableBrick(Sensors.PHIRO_BOTTOM_RIGHT))
        return phiroProBrickList
    }

    private fun setupArduinoCategoryList(): List<Brick> {
        val arduinoBrickList: MutableList<Brick> = ArrayList()
        arduinoBrickList.add(ArduinoSendDigitalValueBrick(BrickValues.ARDUINO_DIGITAL_INITIAL_PIN_NUMBER, BrickValues.ARDUINO_DIGITAL_INITIAL_PIN_VALUE))
        arduinoBrickList.add(ArduinoSendPWMValueBrick(BrickValues.ARDUINO_PWM_INITIAL_PIN_NUMBER, BrickValues.ARDUINO_PWM_INITIAL_PIN_VALUE))
        return arduinoBrickList
    }

    private fun setupRaspiCategoryList(): List<Brick> {
        val defaultScript = RaspiInterruptScript("3", "pressed")
        val raspiBrickList: MutableList<Brick> = ArrayList()
        raspiBrickList.add(WhenRaspiPinChangedBrick(defaultScript))
        raspiBrickList.add(RaspiIfLogicBeginBrick(Formula(BrickValues.RASPI_DIGITAL_INITIAL_PIN_NUMBER)))
        raspiBrickList.add(RaspiSendDigitalValueBrick(BrickValues.RASPI_DIGITAL_INITIAL_PIN_NUMBER, BrickValues.RASPI_DIGITAL_INITIAL_PIN_VALUE))
        raspiBrickList.add(RaspiPwmBrick(BrickValues.RASPI_DIGITAL_INITIAL_PIN_NUMBER, BrickValues.RASPI_PWM_INITIAL_FREQUENCY, BrickValues.RASPI_PWM_INITIAL_PERCENTAGE))
        return raspiBrickList
    }

    private fun setupEmbroideryCategoryList(context: Context): List<Brick> {
        val embroideryBrickList: MutableList<Brick> = ArrayList()
        embroideryBrickList.add(StitchBrick())
        embroideryBrickList.add(SetThreadColorBrick(Formula(BrickValues.THREAD_COLOR)))
        embroideryBrickList.add(RunningStitchBrick(Formula(BrickValues.STITCH_LENGTH)))
        embroideryBrickList.add(ZigZagStitchBrick(Formula(BrickValues.ZIGZAG_STITCH_LENGTH), Formula(BrickValues.ZIGZAG_STITCH_WIDTH)))
        embroideryBrickList.add(TripleStitchBrick(Formula(BrickValues.STITCH_LENGTH)))
        embroideryBrickList.add(SewUpBrick())
        embroideryBrickList.add(StopRunningStitchBrick())
        embroideryBrickList.add(WriteEmbroideryToFileBrick(context.getString(R.string.brick_default_embroidery_file)))
        return embroideryBrickList
    }

    private fun setupPlotCategoryList(context: Context): List<Brick> {
        val plotBrickList: MutableList<Brick> = ArrayList()
        plotBrickList.add(StartPlotBrick())
        plotBrickList.add(StopPlotBrick())
        plotBrickList.add(SavePlotBrick(context.getString(R.string.brick_default_plot_file)))
        return plotBrickList
    }

    private fun setupNeuralCategoryList(context: Context): List<Brick> {
        val category = context.getString(R.string.category_neural)
        val prefKey = getPreferenceKeyForCategory(category, context)
        if (prefKey != null) {
            val defaultValue = category == context.getString(R.string.category_threed)
            val isGroupingEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(prefKey, defaultValue)

            if (!isGroupingEnabled) {
                val neuralBrickList: MutableList<Brick> = ArrayList()
                //neuralBrickList.add(SetDnsBrick("dns.comss.one"))
                neuralBrickList.add(SetGeminiKeyBrick("api_key"))
                neuralBrickList.add(AskGeminiBrick("Hello!"))
                neuralBrickList.add(AskGemini2Brick("Hello! How are you?", "models/gemini-flash-lite-latest"))
                neuralBrickList.add(AskGPTBrick(context.getString(R.string.cbf_gpt_prompt), context.getString(R.string.cbf_gpt_system)))
                neuralBrickList.add(CreateFloatBrick("FloatArray"))
                neuralBrickList.add(PutFloatBrick("FloatArray", 1, 0))
                neuralBrickList.add(TableToFloatBrick("myTable", "FloatArray"))
                neuralBrickList.add(DeleteFloatBrick("FloatArray", 0))
                neuralBrickList.add(LoadNNBrick("model.onnx"))
                neuralBrickList.add(PredictNNBrick("FloatArray"))
                neuralBrickList.add(UnloadNNBrick())
                neuralBrickList.add(ResizeImgBrick("image.png", 64, 64))
                neuralBrickList.add(GrayscaleImgBrick("image.png"))
                neuralBrickList.add(NormalizeImgBrick("image.png", "rTable", "gTable", "bTable"))
                neuralBrickList.add(CutLookBrick(100, 200, 300, 400))
                neuralBrickList.add(ApplyShaderToImageBrick("image.png", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main() {
    vec4 original = texture2D(u_texture, v_texCoords);
    float roughness = original.r;
    float metallic = 0.0;
    gl_FragColor = vec4(1.0, roughness, metallic, 1.0);
}"""))
                return neuralBrickList
            }
        }

        val neuralBrickList: MutableList<Brick> = ArrayList()
        val template = UnloadNNBrick()

        neuralBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_neural_llm), template))
        neuralBrickList.add(SetGeminiKeyBrick("api_key"))
        neuralBrickList.add(AskGeminiBrick("Hello!"))
        neuralBrickList.add(AskGemini2Brick("Hello! How are you?", "models/gemini-flash-lite-latest"))
        neuralBrickList.add(AskGPTBrick(context.getString(R.string.cbf_gpt_prompt), context.getString(R.string.cbf_gpt_system)))

        neuralBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_neural_local), template))
        neuralBrickList.add(LoadNNBrick("model.onnx"))
        neuralBrickList.add(PredictNNBrick("FloatArray"))
        neuralBrickList.add(UnloadNNBrick())

        neuralBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_neural_tensors), template))
        neuralBrickList.add(CreateFloatBrick("FloatArray"))
        neuralBrickList.add(PutFloatBrick("FloatArray", 1, 0))
        neuralBrickList.add(TableToFloatBrick("myTable", "FloatArray"))
        neuralBrickList.add(DeleteFloatBrick("FloatArray", 0))

        neuralBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_neural_vision), template))
        neuralBrickList.add(ResizeImgBrick("image.png", 64, 64))
        neuralBrickList.add(GrayscaleImgBrick("image.png"))
        neuralBrickList.add(NormalizeImgBrick("image.png", "rTable", "gTable", "bTable"))
        neuralBrickList.add(CutLookBrick(100, 200, 300, 400))
        neuralBrickList.add(ApplyShaderToImageBrick("image.png", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main() {
    vec4 original = texture2D(u_texture, v_texCoords);
    float roughness = original.r;
    float metallic = 0.0;
    gl_FragColor = vec4(1.0, roughness, metallic, 1.0);
}"""))

        return neuralBrickList
    }

    private fun setupPocketensorCategoryList(context: Context): List<Brick> {
        val pocketensorBrickList: MutableList<Brick> = ArrayList()

        pocketensorBrickList.add(PtSetTrainingBrick(1))
        pocketensorBrickList.add(PtCreateTensorBrick("tensor", "10,4", 0f, true))
        pocketensorBrickList.add(PtCreateNormalTensorBrick("noise", "1,10", 0f, 1f, false))
        pocketensorBrickList.add(PtSetTensorBrick("tensor", "1,2,3"))
        pocketensorBrickList.add(PtSetByIndexBrick("tensor", 0, 1f))
        pocketensorBrickList.add(PtOpBrick())
        pocketensorBrickList.add(PtReshapeBrick("tensor", "20,2"))
        pocketensorBrickList.add(PtSliceBrick("res", "in", 0, 5))
        pocketensorBrickList.add(PtDropoutBrick("drop", "x", 0.2f))
        pocketensorBrickList.add(PtBackwardBrick("loss"))
        pocketensorBrickList.add(PtZeroGradBrick())
        pocketensorBrickList.add(PtClipGradBrick(5f))
        pocketensorBrickList.add(PtStepBrick(0.01f))
        pocketensorBrickList.add(MLStepAdamBrick(Formula(0.01f)))
        pocketensorBrickList.add(MLStepAdamWBrick(0.001f, 0.01f))
        pocketensorBrickList.add(PtLayerLinearBrick("fc1", "x", "out", 10, 5))
        pocketensorBrickList.add(PtConv2DBrick("conv1", "img", "features", 1, 16, 3, 1))
        pocketensorBrickList.add(PtMaxPool2DBrick("pooled", "features", 2, 2))
        pocketensorBrickList.add(PtGruCellBrick("gru1", "x", "hin", "hout", 8, 16))
        pocketensorBrickList.add(PtLstmCellBrick("lstm1", "x", "hin", "cin", "hout", "cout", 8, 16))
        pocketensorBrickList.add(PtEmbeddingBrick("emb1", "tokens", "vectors", 100, 16))
        pocketensorBrickList.add(PtAttentionBrick("attn1", "x", "context", 8))
        pocketensorBrickList.add(MLSaveBrick("model.bin"))
        pocketensorBrickList.add(MLLoadBrick("model.bin"))

        return pocketensorBrickList
    }

    private fun setupFast2dCategoryList(context: Context): List<Brick> {
        val fast2dBrickList: MutableList<Brick> = ArrayList()

        fast2dBrickList.add(Fast2DCreateBrick("object"))
        fast2dBrickList.add(Fast2DDeleteBrick("object"))
        fast2dBrickList.add(Fast2DSetPositionBrick("object", 100.0, 200.0))
        fast2dBrickList.add(Fast2DSetRotationBrick("object", 90.0))
        fast2dBrickList.add(Fast2DSetScaleBrick("object", 200.0, 15.0))
        fast2dBrickList.add(Fast2DSetVelocityBrick("object", 10.0, 5.0))
        fast2dBrickList.add(Fast2DSetAngularVelocityBrick("object", 10.0))
        fast2dBrickList.add(Fast2DSetTextureBrick("object", "image.png"))
        fast2dBrickList.add(Fast2DSetColorBrick("object", 255.0, 0.0, 0.0, 120.0))
        fast2dBrickList.add(Fast2DSetCameraBrick(100.0, 200.0, 2.0))

        fast2dBrickList.add(Fast2DMakePhysicsBrick("object", 1.0, "CIRCLE", 10.0, 0.7, 0.2))
        fast2dBrickList.add(Fast2DSetCollisionFilterBrick("object", 0.0, -1.0))
        fast2dBrickList.add(Fast2DApplyForceBrick("object", 100.0, 300.0))
        fast2dBrickList.add(Fast2DApplyImpulseBrick("object", 10.0, 5.0))
        fast2dBrickList.add(Fast2DSetPhysicsVelocityBrick("object", 10.0, 2.0))
        fast2dBrickList.add(Fast2DSetGravityBrick(0f, -9.8f));

        return fast2dBrickList
    }

    private fun setupFileCategoryList(context: Context): List<Brick> {
        val fileBrickList: MutableList<Brick> = ArrayList()

        fileBrickList.add(WriteVariableToFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        fileBrickList.add(ReadVariableFromFileBrick(context.getString(R.string.brick_write_variable_to_file_default_value)))
        fileBrickList.add(WriteToFilesBrick("variable.txt"))
        fileBrickList.add(ReadFromFilesBrick("variable.txt"))
        fileBrickList.add(DeleteFilesBrick("variable.txt"))
        fileBrickList.add(FileUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
        fileBrickList.add(FilesUrlBrick("http://e95814zx.beget.tech/map.jpg", "fileFromUrl.jpg"))
        fileBrickList.add(ZipProjectFilesBrick("file1.txt, file2.png", "archive.zip"))
        fileBrickList.add(UnzipProjectFilesBrick("archive.zip"))
        fileBrickList.add(GetZipFileNamesBrick("myZip.zip"))
        fileBrickList.add(ZipBrick("myZip.zip", "my_actor.png,fileFromUrl.jpg"))
        fileBrickList.add(UnzipBrick("myZip.zip"))
        fileBrickList.add(OpenFileBrick("fileFromUrl.txt"))
        fileBrickList.add(MoveFilesBrick("variable.txt"))
        fileBrickList.add(MoveDownloadsBrick("variable.txt"))
        fileBrickList.add(CopyProjectFileBrick("variable.txt", "copy_variable.txt"))
        fileBrickList.add(ChooseFileBrick())
        fileBrickList.add(ExportProjectFileBrick("file.txt"))
        fileBrickList.add(ShareBrick(context.getString(R.string.cbf_share_text)));
        fileBrickList.add(SaveToInternalStorageBrick("file.txt", "myAwesomeApp/file.txt"))
        fileBrickList.add(LoadFromInternalStorageBrick("myAwesomeApp/file.txt"))
        fileBrickList.add(SaveLookBrick("my_actor.png"))
        fileBrickList.add(LookFileBrick("my_actor.png"))
        fileBrickList.add(SaveLookFilesBrick("look.png"))
        fileBrickList.add(SetLookFilesBrick("look.png"))
        fileBrickList.add(SoundFileBrick("my_sound.mp3"))
        fileBrickList.add(SoundFilesBrick("my_sound_FROM_PROJECT_FILES.mp3"))
        fileBrickList.add(PrepareSoundBrick("my_sound.mp3", "sound"))
        fileBrickList.add(StartRecordingBrick())
        fileBrickList.add(StopRecordingBrick("audio.mp3"))
        fileBrickList.add(LoadNNBrick("model.onnx"))
        fileBrickList.add(ResizeImgBrick("image.png", 64, 64))
        fileBrickList.add(GrayscaleImgBrick("image.png"))
        fileBrickList.add(NormalizeImgBrick("image.png", "rTable", "gTable", "bTable"))
        fileBrickList.add(ApplyShaderToImageBrick("image.png", """attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}""", """#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
void main() {
    vec4 original = texture2D(u_texture, v_texCoords);
    float roughness = original.r;
    float metallic = 0.0;
    gl_FragColor = vec4(1.0, roughness, metallic, 1.0);
}"""))
        fileBrickList.add(UploadFileBrick(Formula("https://"), Formula("file.txt"), 0, Formula("application/"), 0))

        fileBrickList.add(CreateVideoBrick("myVideoPlayer", "video.mp4", 0, 0, 750, 500, 1, 0))
        fileBrickList.add(LoadNativeModuleBrick("libz.so"))
        fileBrickList.add(LoadPythonLibraryBrick("telebot.whl"))
        fileBrickList.add(RunVm2Brick("-kernel \"%PROJECT_FILES%/bzImage\" -initrd \"%PROJECT_FILES%/core.gz\" -append \"console=ttyS0 quiet\""))
        fileBrickList.add(RunVMBrick("1024", "2", "myDisk.qcow2", "flash.iso"))

        fileBrickList.add(Base64ToFileBrick("", "image.png"))

        fileBrickList.add(GenerateKeyBrick("keystore.jks", "123456", "alias", "KEBAB_337"))
        fileBrickList.add(SignApkBrick("input.apk", "output.apk", "keystore.jks", "123456", "alias"))
        fileBrickList.add(UpdateManifestBrick(Formula("input.apk"), Formula("com.example.app"), Formula("My App"), Formula(1), Formula("v1.0"), Formula(24), Formula(33), Formula(1), Formula("INTERNET\nMANAGE_EXTERNAL_STORAGE"), Formula("")))
        fileBrickList.add(ExtractFileBrick("input.apk", "assets/project.zip", "extracted_project.zip"))
        fileBrickList.add(AddFileToApkBrick("input.apk", "assets/project.zip", "my_project.zip"))
        fileBrickList.add(DeleteFromApkBrick("input.apk", "assets/image.png"))

        return fileBrickList
    }

    private fun setupThreedCategoryList(context: Context): List<Brick> {
        val threedBrickList: MutableList<Brick> = ArrayList()

        val template = CreateCubeBrick()

        // Objects & Meshes
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_objects), template))
        threedBrickList.add(Create3dObjectBrick("myObject", "model.obj"))
        threedBrickList.add(CreateCubeBrick("myObject"))
        threedBrickList.add(CreateSphereBrick("myObject"))
        threedBrickList.add(ThreedCreateCylinderBrick("myObject"))
        threedBrickList.add(Replace3DModelBrick("myObject", "weapon.glb"));
        threedBrickList.add(Remove3dObjectBrick("myObject"))
        threedBrickList.add(RemoveObjectsByPrefixBrick("wall_"))
        threedBrickList.add(CloneObjectBrick("myObject", "clonedObject"))
        threedBrickList.add(SetSpawnInvisibleBrick("myObject"))
        threedBrickList.add(SetActiveBrick("myObject", true))
        threedBrickList.add(SetObjectColorBrick("myObject", 1.0, 0.0, 0.0))
        threedBrickList.add(SetObjectTextureBrick("myObject", "texture.png"))
        threedBrickList.add(BakeByPrefixBrick("wall_", "bakedObject"))
        threedBrickList.add(
            Export3dObjectToGlbBrick(
                Formula("myObject"),
                Formula("exported_model.glb")
            )
        )

        // Motion & Hierarchy
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_transform), template))
        threedBrickList.add(Set3dPositionBrick("myObject", 10.0, -5.0, 0.0))
        threedBrickList.add(Set3dRotationBrick("myObject", 1.0, 0.0, 0.0))
        threedBrickList.add(Set3dScaleBrick("myObject", 2.0, 1.0, 1.5))
        threedBrickList.add(ObjectLookAtBrick("myObject", 0.0, 0.0, 0.0))
        threedBrickList.add(ObjectLookAtCorrectBrick("myObject", 0.0, 0.0, 0.0))
        threedBrickList.add(ThreedAlignNormalBrick("myObject", -1.0, 0.0, 0.0))
        threedBrickList.add(Ease3DPropertyBrick(Formula("myObject"), 0, 0, Formula(0), Formula(10), Formula(0.1)));
        threedBrickList.add(SetParentBrick("child", "parent"))
        threedBrickList.add(RemoveParentBrick("child"))
        threedBrickList.add(AttachToCameraBrick("myObject"))
        threedBrickList.add(DetachFromCameraBrick("myObject"))

        // Camera
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_camera), template))
        threedBrickList.add(SetCameraPositionBrick(200.0, 200.0, 200.0))
        threedBrickList.add(CameraLookAtBrick(0.0, 0.0, 0.0))
        threedBrickList.add(SetCameraRotationBrick(0.0, 180.0, 0.0))
        threedBrickList.add(RotateCameraByBrick(0.0f, 180.0f, 0.0f))
        threedBrickList.add(SetCameraRangeBrick(0.1, 2500.0))
        threedBrickList.add(CameraSettingsBrick(67f, 2f, 2f))
        threedBrickList.add(SetThirdPersonCameraBrick("myObject", 10.0, 10.0, -20.0))
        threedBrickList.add(CameraTrackingBrick("myObject", 0, 0f, 1f, 0f, 0f, 0f, 0f))
        threedBrickList.add(SetFreeCameraBrick())
        threedBrickList.add(CameraTouchControlBrick(1, 1f, 0f, 0f, 50f, 50f))

        // Physics
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_physics), template))
        threedBrickList.add(SetPhysicsStateBrick("myObject", 2, 0, 1.0))
        threedBrickList.add(SetRotationLockBrick("myObject", true, false, true))
        threedBrickList.add(Set3dGravityBrick(0.0, -9.81, 0.0))
        threedBrickList.add(Set3dVelocityBrick("myObject", 0.0, 25.0, 0.0))
        threedBrickList.add(Apply3dForceBrick("myObject", 5.0, 0.0, 0.0))
        threedBrickList.add(Apply3dTorqueBrick("myObject", 0.0, 5.0, 0.0))
        threedBrickList.add(Set3dFrictionBrick("myObject", 0.7))
        threedBrickList.add(SetRestitutionBrick("myObject", 0.5))
        threedBrickList.add(SetCCDBrick("myObject", true))
        threedBrickList.add(CastRayBrick("ray", 100.0, 100.0, 100.0, -1.0, -1.0, -1.0))
        threedBrickList.add(AttachRaySensorBrick("ray", "myObject", 1f, 1f, 1f, 0f, -1f, 0f, 100f))
        threedBrickList.add(CreatePointJointBrick("joint", "objA", "objB"))
        threedBrickList.add(ThreedCreateFixedConstraintBrick("joint", "objA", "objB"))
        threedBrickList.add(AddHingeBrick())
        threedBrickList.add(
            CreateSpringConstraintBrick(
                "spring1", "car", "wheel_fl",
                0.23, -0.1, 0.57,
                0.0, 0.0, 0.0,
                false, true, false,
                0.0, -0.3, 0.0,
                0.0, 0.3, 0.0,
                15000.0, 300.0
            )
        )
        threedBrickList.add(SetHingeMotorBrick("joint", 45.0, 10.0))
        threedBrickList.add(RemoveJointBrick("joint"))

        // Environment
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_render), template))
        threedBrickList.add(SetAmbientLightBrick(0.8, 0.8, 0.8))
        threedBrickList.add(SetDirectionalLightBrick("sun", 0.8, 0.8, 0.8, -1.0, -0.8, -0.2))
        threedBrickList.add(SetSkyColorBrick(0.5, 0.6, 1.0))
        threedBrickList.add(SetFogBrick(0.5f, 0.6f, 1f, 0.1f))
        threedBrickList.add(EnablePbrRenderBrick(1))
        threedBrickList.add(SetBackgroundLightBrick(0.3))
        threedBrickList.add(SetSkyboxBrick("skybox.hdr"))
        threedBrickList.add(SetPointLightBrick("sun", 100.0, 100.0, 0.0, 255, 255, 230, 5.0, 300.0))
        threedBrickList.add(SetSpotLightBrick("sun", 100.0, 100.0, 0.0, 0.3, -0.4, 0.2, 255, 255, 230, 5.0, 60.0, 1.0, 300.0))
        threedBrickList.add(SetDirectionalLight2Brick(0.3, -0.2, -0.3, 5.0))
        threedBrickList.add(ConfigureLightBrick("sun", 0, "5.0"))
        threedBrickList.add(RemovePbrLightBrick("sun"))
        threedBrickList.add(PromoteLightBrick("sun"))
        threedBrickList.add(SetMaxPointLightsBrick(5))
        threedBrickList.add(SetShadowQualityBrick(Formula(100), Formula(2048)))

        // Materials
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_shaders), template))
        threedBrickList.add(SetShaderCodeBrick("""attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec2 a_texCoord0;

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

varying vec2 v_texCoord;

void main() {
    v_texCoord = a_texCoord0;
    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_position, 1.0);
}""", """#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_texCoord;

uniform sampler2D u_diffuseTexture;
uniform float u_time;

void main() {
    // 1. Берем базовый цвет из текстуры объекта
    vec4 baseColor = texture2D(u_diffuseTexture, v_texCoord);

    // 2. Создаем цвет для пульсации (ярко-фиолетовый)
    vec3 pulseColor = vec3(1.0, 0.0, 1.0);

    // 3. Вычисляем коэффициент пульсации на основе времени
    // sin(u_time * 5.0) колеблется от -1 до 1. Мы преобразуем это в диапазон от 0 до 1.
    float pulse = (sin(u_time * 5.0) + 1.0) * 0.5;

    // 4. Смешиваем базовый цвет с цветом пульсации
    vec3 finalColor = mix(baseColor.rgb, pulseColor, pulse);

    // 5. Устанавливаем итоговый цвет пикселя, сохраняя исходную прозрачность
    gl_FragColor = vec4(finalColor, baseColor.a);
}"""))
        threedBrickList.add(SetScreenShaderBrick("""attribute vec4 a_position;
attribute vec2 a_texCoord0;
varying vec2 v_texCoords;

void main() {
    v_texCoords = a_texCoord0;
    gl_Position = a_position;
}""", """#ifdef GL_ES
precision highp float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float u_time;

const float DISTORTION = 0.22;   // Сила рыбьего глаза
const float ZOOM = 1.12;         // Приближение
const float CHROMATIC = 0.007;   // Сила RGB-расщепления по краям

void main() {
    vec2 uv = v_texCoords;

    vec2 cc = uv - vec2(0.5);
    float dist = dot(cc, cc);

    vec2 distortedUV = vec2(0.5) + cc * (1.0 + DISTORTION * dist) / ZOOM;

    if (distortedUV.x < 0.0 || distortedUV.x > 1.0 || distortedUV.y < 0.0 || distortedUV.y > 1.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    vec2 splitOffset = cc * CHROMATIC * dist * 3.0;
    float r = texture2D(u_texture0, distortedUV - splitOffset).r;
    float g = texture2D(u_texture0, distortedUV).g;
    float b = texture2D(u_texture0, distortedUV + splitOffset).b;
    vec3 color = vec3(r, g, b);

    color.r *= 1.02;
    color.g *= 1.04;
    color.b *= 0.93;

    float scanline = sin(distortedUV.y * 650.0 + u_time * 2.5) * 0.03;
    color -= vec3(scanline);

    float vignette = smoothstep(0.35, 0.75, dist);
    color *= (1.0 - vignette * 0.55);

    gl_FragColor = vec4(color, 1.0);
}"""))
        threedBrickList.add(SetObjectShaderBrick("myObject", """attribute vec3 a_position;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform float u_time;

uniform float u_waveSpeed;
uniform float u_waveHeight;
uniform float u_waveScale;

varying vec3 v_worldPos;
varying vec3 v_normal;

void main() {
    vec4 worldPos = u_worldTrans * vec4(a_position, 1.0);
    
    float waveSpeed = u_waveSpeed > 0.001 ? u_waveSpeed : 1.0;
    float waveHeight = u_waveHeight > 0.001 ? u_waveHeight : 0.05;
    float waveScale = u_waveScale > 0.001 ? u_waveScale : 0.2;

    float wave = sin(worldPos.x * waveScale + u_time * waveSpeed) * 
                 cos(worldPos.z * waveScale + u_time * waveSpeed * 0.8) * waveHeight;
    worldPos.y += wave;

    v_worldPos = worldPos.xyz;
    v_normal = vec3(0.0, 1.0, 0.0); 

    gl_Position = u_projViewTrans * worldPos;
}""", """#ifdef GL_ES
precision highp float;
#endif

varying vec3 v_worldPos;
varying vec3 v_normal;

uniform float u_time;
uniform vec3 u_customCameraPosition;

uniform vec3 u_waterColor;
uniform vec3 u_skyColor;
uniform float u_waveSpeed;
uniform float u_waveScale;

void main() {
    vec3 waterColor = length(u_waterColor) > 0.001 ? u_waterColor : vec3(0.02, 0.35, 0.45);
    vec3 skyColor = length(u_skyColor) > 0.001 ? u_skyColor : vec3(0.7, 0.85, 0.95);
    float waveSpeed = u_waveSpeed > 0.001 ? u_waveSpeed : 1.2;
    float waveScale = u_waveScale > 0.001 ? u_waveScale : 0.3;

    float speed = u_time * waveSpeed;
    vec2 waveUV1 = v_worldPos.xz * waveScale + vec2(speed * 0.03, speed * 0.015);
    vec2 waveUV2 = v_worldPos.xz * waveScale * 1.5 - vec2(speed * 0.01, speed * 0.025);

    float nX = sin(waveUV1.x * 10.0) * cos(waveUV2.y * 12.0) * 0.1;
    float nZ = cos(waveUV1.y * 8.0) * sin(waveUV2.x * 10.0) * 0.1;
    vec3 waveNormal = normalize(v_normal + vec3(nX, 0.0, nZ));

    vec3 viewDir = normalize(u_customCameraPosition - v_worldPos);
    vec3 lightDir = normalize(vec3(0.3, 1.0, 0.4)); 

    float fresnel = pow(1.0 - max(0.0, dot(v_normal, viewDir)), 3.0);

    vec3 halfDir = normalize(lightDir + viewDir);
    float spec = pow(max(0.0, dot(waveNormal, halfDir)), 64.0) * 1.5;

    vec3 finalColor = mix(waterColor, skyColor, fresnel);
    finalColor += vec3(spec);

    gl_FragColor = vec4(finalColor, 0.9);
}"""))
        threedBrickList.add(SetShaderUniformVec3Brick("lightColor", 0.4, 1.0, 0.4))
        threedBrickList.add(SetShaderUniformFloatBrick("meaningOfLife", 42.0))
        threedBrickList.add(SetObjectShaderUniformBrick(Formula("myObject"), Formula("waveSpeed"), Formula(2.5f), Formula(""), Formula("")))
        threedBrickList.add(SetMaterialBrick("myObject", 255.0, 0.0, 255.0, 255.0, 100.0, 0.0, "none.png", "none.png", "none.png"))
        threedBrickList.add(SetTextureTilingBrick("myObject", 10f, 10f))
        threedBrickList.add(SetEmissiveBrick("myObject", 1f, 1f, 1f, 1f, 10f, "emissive_texture.png"))
        threedBrickList.add(ConfigureMaterialBrick("myObject", 1, "0.8"));
        threedBrickList.add(SetAnisotropicFilterBrick("myObject", 2.0))

        // Animations
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_animation), template))
        threedBrickList.add(PlayAnimationBrick("myObject", "idle", -1, 1.0, 0.2))
        threedBrickList.add(SetAnimationSpeedBrick("myObject", 10f))
        threedBrickList.add(StopAnimationBrick("myObject"))
        threedBrickList.add(KeyframeAnimationBrick())
        threedBrickList.add(ThreedAttachObjectToBoneBrick("attachedObject", "hand", "Hand_R", 0.0, 0.0, 0.0))
        threedBrickList.add(ThreedBindBoneToObjectBrick("Hand_R", "hand", "myObject"))

        // Voxels
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_voxels), template))
        threedBrickList.add(VoxelCreateWorldBrick(Formula("chunk1"), Formula(16), Formula(16), Formula(16), Formula(16), Formula(0), Formula(0)))
        threedBrickList.add(VoxelConfigBrick("1", 0, 0, 0))
        threedBrickList.add(VoxelSetTransparentBrick(1, 1))
        threedBrickList.add(VoxelSetBlockBrick("chunk1", 0.0, 0.0, 0.0, 1, 0))
        threedBrickList.add(VoxelLoadStringBrick(Formula("chunk1"), Formula("1$1#0$0&1$1#0$0"), Formula("$"), Formula("#"), Formula("&")))
        threedBrickList.add(VoxelBuildBrick(Formula("chunk1"), Formula("atlas.png"), Formula(16), Formula(16)))
        threedBrickList.add(VoxelDeleteBrick(Formula("chunk1")))

        // Sound
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_sound), template))
        threedBrickList.add(PrepareSoundBrick2("sound.mp3", "sound"))
        threedBrickList.add(PrepareMusicAs3DSoundBrick("sound.mp3", "sound"))
        threedBrickList.add(PlaySoundAtPositionBrick("sound", "soundInstance"))
        threedBrickList.add(Set3DSoundMaxDistanceBrick("soundInstance", 250f))
        threedBrickList.add(SetSoundInstanceVolumeBrick("soundInstance", 60f))
        threedBrickList.add(SetSoundInstancePitchBrick("soundInstance", 140f))
        threedBrickList.add(Set3DSoundPositionBrick("soundInstance", 10, 10, 10))
        threedBrickList.add(StopSoundBrick2("soundInstance"))
        threedBrickList.add(SetGlobalSoundVolumeBrick(60.0))

        // Scenes, Effects & AI
        threedBrickList.add(SubCategoryHeaderBrick(context.getString(R.string.subcategory_3d_effects), template))
        threedBrickList.add(SetAIBrick("myObject", 1, "followingObject", 2f, 1f, 10f, 1f, 1))
        threedBrickList.add(LoadSceneBrick("my_level.rscene"))
        threedBrickList.add(LoadSceneAdditiveBrick("my_level.rscene"))
        threedBrickList.add(SetPostProcessingBrick(1, 4, Formula(2)))
        threedBrickList.add(SetPostProcessingNewBrick())
        threedBrickList.add(SetRenderResolutionBrick(0.5f, 1))
        threedBrickList.add(SetFpsBrick(24))
        threedBrickList.add(CreateParticlesBrick("particles"))
        threedBrickList.add(SetParticleEmissionBrick("particles", 10f))
        threedBrickList.add(ConfigureParticlesBrick("particles", 0, "15.0"));
        threedBrickList.add(DeleteParticlesBrick("particles"))

        return threedBrickList
    }

    private fun setupInternetCategoryList(context: Context): List<Brick> {
        val internetBrickList: MutableList<Brick> = ArrayList()
        internetBrickList.add(StartServerBrick("1234"))
        internetBrickList.add(ConnectServerBrick("127.0.0.1", "1234"))
        internetBrickList.add(ListenServerBrick())
        internetBrickList.add(SendServerBrick("Hi"))
        internetBrickList.add(StopServerBrick())
        internetBrickList.add(WriteBaseBrick("firebase_id", "key", "hello"))
        internetBrickList.add(ReadBaseBrick("firebase_id", "key"))
        internetBrickList.add(DeleteBaseBrick("firebase_id", "key"))
        internetBrickList.add(UploadFileBrick(Formula("https://"), Formula("file.txt"), 0, Formula("application/"), 0))
        internetBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
        internetBrickList.add(PostWebRequestBrick("https://api.calfire.com/v2/texts?limit=50&offset=200",
            "Content-Type:application/json",
            "{\nusername=password\n}"))
        internetBrickList.add(HttpCreateBrick("my_request", "GET", "https://api.example.com/data"))
        internetBrickList.add(HttpConfigBrick("my_request", 0, "User-Agent", "NewCatroidClient"))
        internetBrickList.add(
            HttpBodyBrick(
                "my_request",
                "{\"text\": \"hello\"}",
                "application/json"
            )
        )
        internetBrickList.add(HttpAttachFileBrick("my_request", "image.png", "file", "image/png"))
        internetBrickList.add(HttpSendBrick("my_request"))
        internetBrickList.add(HttpSaveFileBrick("my_request", "downloaded_image.png"))

        internetBrickList.add(WhenMqttMessageBrick(WhenMqttMessageScript(Formula("room_1"))))
        internetBrickList.add(MqttConnectBrick("my_lobby", "broker.emqx.io", 1883))
        internetBrickList.add(MqttJoinRoomBrick("my_lobby", "room_1", "my_secret_game_salt"))
        internetBrickList.add(
            MqttPublishBrick(
                "my_lobby",
                "room_1",
                "my_secret_game_salt",
                "player_x: 150, player_y: 340"
            )
        )
        internetBrickList.add(MqttDisconnectBrick("my_lobby"))
        //internetBrickList.add(SetDnsBrick("dns.comss.one"))
        return internetBrickList
    }

    private fun setupAssertionsCategoryList(context: Context): List<Brick> {
        val assertionsBrickList: MutableList<Brick> = ArrayList()
        assertionsBrickList.add(AssertEqualsBrick())
        assertionsBrickList.add(AssertUserListsBrick())
        assertionsBrickList.add(ParameterizedBrick())
        assertionsBrickList.add(WaitTillIdleBrick())
        assertionsBrickList.add(TapAtBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START))
        assertionsBrickList.add(TapForBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_DURATION))
        assertionsBrickList.add(TouchAndSlideBrick(BrickValues.TOUCH_X_START, BrickValues.TOUCH_Y_START, BrickValues.TOUCH_X_GOAL, BrickValues.TOUCH_Y_GOAL, BrickValues.TOUCH_DURATION))
        assertionsBrickList.add(FinishStageBrick())
        assertionsBrickList.add(StoreCSVIntoUserListBrick(BrickValues.STORE_CSV_INTO_USERLIST_COLUMN, context.getString(R.string.brick_store_csv_into_userlist_data)))
        assertionsBrickList.add(WebRequestBrick(context.getString(R.string.brick_web_request_default_value)))
        return assertionsBrickList
    }

    fun searchList(searchBrick: Brick, list: List<Brick>): Boolean = list.any { it == searchBrick.javaClass }

    fun getBrickCategory(brick: Brick, isBackgroundSprite: Boolean, context: Context): String {
        val res = context.resources
        val config = res.configuration
        val savedLocale = config.locale
        config.locale = Locale.ENGLISH
        res.updateConfiguration(config, null)
        var category: String
        category = when {
            searchList(brick, setupControlCategoryList(context)) -> res.getString(R.string.category_control)
            searchList(brick, setupEventCategoryList(context, isBackgroundSprite)) -> res.getString(R.string.category_event)
            searchList(brick, setupMotionCategoryList(context, isBackgroundSprite)) -> res.getString(R.string.category_motion)
            searchList(brick, setupSoundCategoryList(context)) -> res.getString(R.string.category_sound)
            searchList(brick, setupLooksCategoryList(context, isBackgroundSprite)) -> res.getString(R.string.category_looks)
            searchList(brick, setupPenCategoryList(isBackgroundSprite)) -> res.getString(R.string.category_pen)
            searchList(brick, setupDataCategoryList(context, isBackgroundSprite)) -> res.getString(R.string.category_data)
            searchList(brick, setupLegoNxtCategoryList()) -> res.getString(R.string.category_lego_nxt)
            searchList(brick, setupLegoEv3CategoryList()) -> res.getString(R.string.category_lego_ev3)
            searchList(brick, setupArduinoCategoryList()) -> res.getString(R.string.category_arduino)
            searchList(brick, setupDroneCategoryList()) -> res.getString(R.string.category_drone)
            searchList(brick, setupJumpingSumoCategoryList()) -> res.getString(R.string.category_jumping_sumo)
            searchList(brick, setupPhiroProCategoryList()) -> res.getString(R.string.category_phiro)
            searchList(brick, setupRaspiCategoryList()) -> res.getString(R.string.category_raspi)
            searchList(brick, setupChromecastCategoryList(context)) -> res.getString(R.string.category_cast)
            searchList(brick, setupEmbroideryCategoryList(context)) -> res.getString(R.string.category_embroidery)
            searchList(brick, setupAssertionsCategoryList(context)) -> res.getString(R.string.category_assertions)
            else -> "No Match"
        }

        when (brick) {
            is AskBrick -> category = res.getString(R.string.category_looks)
            is AskSpeechBrick -> category = res.getString(R.string.category_sound)
            is LookRequestBrick -> category = res.getString(R.string.category_looks)
            is BackgroundRequestBrick -> category = res.getString(R.string.category_looks)
            is WhenClonedBrick -> category = res.getString(R.string.category_control)
            is WhenBackgroundChangesBrick -> category = res.getString(R.string.category_event)
            is SetVariableBrick -> category = res.getString(R.string.category_data)
            is WebRequestBrick -> category = res.getString(R.string.category_data)
            is StoreCSVIntoUserListBrick -> category = res.getString(R.string.category_data)
            is UserDefinedBrick -> category = res.getString(R.string.category_user_bricks)
            is UserDefinedReceiverBrick -> category = res.getString(R.string.category_user_bricks)
            is ParameterizedEndBrick -> category = res.getString(R.string.category_assertions)
            is WriteEmbroideryToFileBrick -> category = res.getString(R.string.category_embroidery)
        }

        config.locale = savedLocale
        res.updateConfiguration(config, null)
        return category
    }

    fun setupFavoriteBricksCategoryList(context: Context, isBackgroundSprite: Boolean): List<Brick> {
        val favoriteClassNames = org.catrobat.catroid.utils.FavoriteBricksManager.getFavoriteClassNames(context)
        if (favoriteClassNames.isEmpty()) {
            return emptyList()
        }
        val allBricks = getAllAvailableBricks(context, isBackgroundSprite)
        return allBricks.filter { it.javaClass.name in favoriteClassNames }
    }

    fun getAllAvailableBricks(context: Context, isBackgroundSprite: Boolean): List<Brick> {
        val all = mutableListOf<Brick>()
        all.addAll(setupEventCategoryList(context, isBackgroundSprite))
        all.addAll(setupControlCategoryList(context))
        all.addAll(setupMotionCategoryList(context, isBackgroundSprite))
        all.addAll(setupSoundCategoryList(context))
        all.addAll(setupLooksCategoryList(context, isBackgroundSprite))
        all.addAll(setupPenCategoryList(isBackgroundSprite))
        all.addAll(setupDataCategoryList(context, isBackgroundSprite))
        all.addAll(setupDeviceCategoryList(context, isBackgroundSprite))
        all.addAll(setupLegoNxtCategoryList())
        all.addAll(setupLegoEv3CategoryList())
        all.addAll(setupDroneCategoryList())
        all.addAll(setupJumpingSumoCategoryList())
        all.addAll(setupPhiroProCategoryList())
        all.addAll(setupChromecastCategoryList(context))
        all.addAll(setupRaspiCategoryList())
        all.addAll(setupEmbroideryCategoryList(context))
        all.addAll(setupPlotCategoryList(context))
        all.addAll(setupNeuralCategoryList(context))
        all.addAll(setupPocketensorCategoryList(context))
        all.addAll(setupFast2dCategoryList(context))
        all.addAll(setupFileCategoryList(context))
        all.addAll(setupThreedCategoryList(context))
        all.addAll(setupInternetCategoryList(context))
        all.addAll(setupAssertionsCategoryList(context))
        all.addAll(setupLibrariesCategoryList())

        return all.distinctBy { it.javaClass.name }
    }
}
