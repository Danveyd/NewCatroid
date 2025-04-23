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

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.AskBrick;
import org.catrobat.catroid.content.bricks.AskSpeechBrick;
import org.catrobat.catroid.content.bricks.ChangeVariableBrick;
import org.catrobat.catroid.content.bricks.ConnectServerBrick;
import org.catrobat.catroid.content.bricks.CopyTextBrick;
import org.catrobat.catroid.content.bricks.CreateVarBrick;
import org.catrobat.catroid.content.bricks.CreateWebFileBrick;
import org.catrobat.catroid.content.bricks.CreateWebUrlBrick;
import org.catrobat.catroid.content.bricks.DeleteVarBrick;
import org.catrobat.catroid.content.bricks.DeleteVarsBrick;
import org.catrobat.catroid.content.bricks.DeleteWebBrick;
import org.catrobat.catroid.content.bricks.FileUrlBrick;
import org.catrobat.catroid.content.bricks.HideTextBrick;
import org.catrobat.catroid.content.bricks.ListenMicroBrick;
import org.catrobat.catroid.content.bricks.ListenServerBrick;
import org.catrobat.catroid.content.bricks.LookFileBrick;
import org.catrobat.catroid.content.bricks.ReadVariableFromDeviceBrick;
import org.catrobat.catroid.content.bricks.RunJSBrick;
import org.catrobat.catroid.content.bricks.SaveLookBrick;
import org.catrobat.catroid.content.bricks.SceneIdBrick;
import org.catrobat.catroid.content.bricks.ScreenShotBrick;
import org.catrobat.catroid.content.bricks.SendServerBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.ShowTextBrick;
import org.catrobat.catroid.content.bricks.ShowTextColorSizeAlignmentBrick;
import org.catrobat.catroid.content.bricks.ShowToastBlock;
import org.catrobat.catroid.content.bricks.SoundFileBrick;
import org.catrobat.catroid.content.bricks.StartServerBrick;
import org.catrobat.catroid.content.bricks.StopServerBrick;
import org.catrobat.catroid.content.bricks.UserVariableBrickInterface;
import org.catrobat.catroid.content.bricks.WebRequestBrick;
import org.catrobat.catroid.content.bricks.WriteVariableOnDeviceBrick;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;

import static org.junit.Assert.assertSame;

@RunWith(Parameterized.class)
public class CloneBrickUpdateVariableTest {
	private static final String VARIABLE_NAME = "test_variable";
	private static final UserVariable USER_VARIABLE = new UserVariable(VARIABLE_NAME);

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"SetVariableBrick", new SetVariableBrick()},
				{"ChangeVariableBrick", new ChangeVariableBrick()},
				{"AskBrick", new AskBrick()},
				{"AskSpeechBrick", new AskSpeechBrick()},
				{"ScreenShotBrick", new ScreenShotBrick()},
				{"HideTextBrick", new HideTextBrick()},
				{"ShowTextBrick", new ShowTextBrick()},
				{"ShowTextColorSizeAlignmentBrick", new ShowTextColorSizeAlignmentBrick()},
				{"WebRequestBrick", new WebRequestBrick()},
				{"ReadVariableFromDeviceBrick", new ReadVariableFromDeviceBrick()},
				{"WriteVariableOnDeviceBrick", new WriteVariableOnDeviceBrick()},
				{"CreateVarBrick", new CreateVarBrick()},
				{"DeleteVarBrick", new DeleteVarBrick()},
				{"DeleteVarsBrick", new DeleteVarsBrick()},
				{"ShowToastBlock", new ShowToastBlock()},
				{"SoundFileBrick", new SoundFileBrick()},
				{"LookFileBrick", new LookFileBrick()},
				{"SaveLookBrick", new SaveLookBrick()},
				{"SceneIdBrick", new SceneIdBrick()},
				{"FileUrlBrick", new FileUrlBrick()},
				{"StartServerBrick", new StartServerBrick()},
				{"SendServerBrick", new SendServerBrick()},
				{"StopServerBrick", new StopServerBrick()},
				{"ConnectServerBrick", new ConnectServerBrick()},
				{"ListenServerBrick", new ListenServerBrick()},
				{"CopyTextBrick", new CopyTextBrick()},
				{"ListenMicroBrick", new ListenMicroBrick()},
				{"RunJSBrick", new RunJSBrick()},
				{"CreateWebUrlBrick", new CreateWebUrlBrick()},
				{"CreateWebFileBrick", new CreateWebFileBrick()},
				{"DeleteWebBrick", new DeleteWebBrick()},
		});
	}

	@Parameterized.Parameter
	public String name;

	@Parameterized.Parameter(1)
	public UserVariableBrickInterface brick;

	private Sprite sprite;
	private Sprite clonedSprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "testProject");
		ProjectManager.getInstance().setCurrentProject(project);
		sprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(sprite);

		StartScript script = new StartScript();
		sprite.addScript(script);
		sprite.addUserVariable(new UserVariable(VARIABLE_NAME));
		script.addBrick(brick);
		brick.setUserVariable(USER_VARIABLE);
		clonedSprite = new SpriteController().copy(sprite, project, project.getDefaultScene());
	}

	@Test
	public void testClonedSpriteAndBrickVariableSame() {
		UserVariableBrickInterface clonedBrick = (UserVariableBrickInterface) clonedSprite.getScript(0).getBrick(0);

		UserVariable clonedVariable = clonedSprite.getUserVariable(VARIABLE_NAME);
		UserVariable clonedVariableFromBrick = clonedBrick.getUserVariable();

		assertNotNull(clonedVariable);
		assertSame(clonedVariable, clonedVariableFromBrick);
	}

	@Test
	public void testOriginalAndClonedVariableEquals() {
		UserVariable spriteVariable = sprite.getUserVariable(VARIABLE_NAME);

		UserVariableBrickInterface clonedBrick = (UserVariableBrickInterface) clonedSprite.getScript(0).getBrick(0);
		UserVariable clonedVariableFromBrick = clonedBrick.getUserVariable();

		assertEquals(spriteVariable, clonedVariableFromBrick);
	}

	@Test
	public void testOriginalAndClonedVariableNotSame() {
		UserVariable spriteVariable = sprite.getUserVariable(VARIABLE_NAME);

		UserVariable clonedVariable = clonedSprite.getUserVariable(VARIABLE_NAME);

		UserVariableBrickInterface clonedBrick = (UserVariableBrickInterface) clonedSprite.getScript(0).getBrick(0);
		UserVariable clonedVariableFromBrick = clonedBrick.getUserVariable();

		assertNotSame(spriteVariable, clonedVariable);
		assertNotSame(spriteVariable, clonedVariableFromBrick);
	}
}

