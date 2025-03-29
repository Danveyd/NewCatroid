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

package org.catrobat.catroid.stage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.utils.ShowTextUtils;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

import java.util.List;
import java.util.Locale;

import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_CENTERED;
import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_RIGHT;
import static org.catrobat.catroid.utils.ShowTextUtils.DEFAULT_TEXT_SIZE;
import static org.catrobat.catroid.utils.ShowTextUtils.DEFAULT_X_OFFSET;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateAlignmentValuesForText;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateColorRGBs;
import static org.catrobat.catroid.utils.ShowTextUtils.getStringAsInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isNumberAndInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isValidColorString;
import static org.catrobat.catroid.utils.ShowTextUtils.sanitizeTextSize;

public class ShowTextActor extends Actor {

	private static final int DEFAULT_ALIGNMENT = ALIGNMENT_STYLE_CENTERED;
	private float textSize;
	private int xPosition;
	private int yPosition;
	private String color;
	private UserVariable variableToShow;
	private String variableNameToCompare;
	private int alignment;
	private Sprite sprite;
	private AndroidStringProvider androidStringProvider;

	private Typeface typeface;

	private boolean isTextWrapped;

	private float rotation = 0;

	private Boolean isText;

	public ShowTextActor(Boolean text, UserVariable userVariable, int xPosition, int yPosition, float relativeSize,
			String color, Sprite sprite, int alignment, AndroidStringProvider androidStringProvider) {
		this.variableToShow = userVariable;
		this.variableNameToCompare = variableToShow.getName();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = alignment;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	public void setFont(Typeface typeface) {
		this.typeface = typeface; // Устанавливаем переданный шрифт
	}

	public void setWrap(boolean wrap) {
		this.isTextWrapped = wrap; // Устанавливаем переданный шрифт
	}

	public void setRotation(float angle) {
		this.rotation = angle; // Устанавливаем переданный шрифт
	}

	public ShowTextActor(Boolean text, String name, int xPosition, int yPosition, float relativeSize,
						 String color, Sprite sprite, int alignment, AndroidStringProvider androidStringProvider) {
		this.variableToShow = null;
		this.variableNameToCompare = name;
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = alignment;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	public ShowTextActor(Boolean text, UserVariable userVariable, int xPosition, int yPosition, float relativeSize,
			String color, Sprite sprite, AndroidStringProvider androidStringProvider) {
		this.variableToShow = userVariable;
		this.variableNameToCompare = variableToShow.getName();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = DEFAULT_ALIGNMENT;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		drawVariables(ProjectManager.getInstance().getCurrentProject().getUserVariables(), batch);
		drawVariables(ProjectManager.getInstance().getCurrentProject().getMultiplayerVariables(), batch);
		drawVariables(sprite.getUserVariables(), batch);
	}

	private void drawVariables(List<UserVariable> variableList, Batch batch) {
		if (variableList == null) {
			return;
		}

		if(this.isText) {
			drawText(batch,
					String.valueOf(this.variableToShow.getValue()),
					xPosition, yPosition, color);
		} else if (variableToShow.isDummy()) {
			drawText(batch,
					CatroidApplication.getAppContext().getString(R.string.no_variable_selected),
					xPosition, yPosition, color);
		} else {
			for (UserVariable variable : variableList) {
				if (variable.getName().equals(variableToShow.getName())) {
					String variableValueString;
					Object value = variable.getValue();
					if (value instanceof Boolean) {
						variableValueString = androidStringProvider.getTrueOrFalse((Boolean) value);
					} else {
						variableValueString = variable.getValue().toString();
					}
					if (variableValueString.isEmpty()) {
						continue;
					}
					if (variable.getVisible()) {
						if (isNumberAndInteger(variableValueString)) {
							drawText(batch, getStringAsInteger(variableValueString), xPosition, yPosition, color);
						} else {
							drawText(batch, variableValueString, xPosition, yPosition, color);
						}
					}
					break;
				}
			}
		}
	}

	public void drawText(Batch batch, String text, float posX, float posY, String color) {
		// Convert to bitmap
		Paint paint = new Paint();
		float textSizeInPx = sanitizeTextSize(textSize);
		paint.setTextSize(textSizeInPx);

		// Устанавливаем шрифт, если он выбран
		if (this.typeface != null) {
			paint.setTypeface(this.typeface);
		}

		if (isValidColorString(color)) {
			color = color.toUpperCase(Locale.getDefault());
			int[] rgb;
			rgb = calculateColorRGBs(color);
			paint.setColor((0xFF000000) | (rgb[0] << 16) | (rgb[1] << 8) | (rgb[2]));
			batch.setColor((float) rgb[0] / 255, (float) rgb[1] / 255, (float) rgb[2] / 255, 1);
		} else {
			paint.setColor(Color.BLACK);
		}

		float baseline = -paint.ascent(); // Базовая линия текста
		paint.setAntiAlias(true);

		String[] lines = text.split("\n"); // Разбиваем текст на строки по символу \n
		float totalHeight = 0;

		for (String line : lines) {
			if (!line.isEmpty()) {
				totalHeight += textSizeInPx; // Добавляем высоту каждой строки
			}
		}

		posY -= totalHeight / 2;

		if (this.isTextWrapped) {
			for (int i = lines.length - 1; i >= 0; i--) {
				String line = lines[i];
				if (line.isEmpty()) {
					continue;
				}

				// Измеряем ширину строки
				int lineWidth = (int) paint.measureText(line);
				int availableWidth = (int) Math.ceil(ScreenValues.currentScreenResolution.getWidth() + 2 * Math.abs(posX));

				/*if (lineWidth > availableWidth) {
					// Если строка слишком длинная, пропускаем её
					continue;
				}*/

				// Устанавливаем позицию X для выравнивания
				// Устанавливаем позицию X для выравнивания
				float adjustedPosX = posX; // Создаем вспомогательную переменную для позиции X
				switch (alignment) {
					case ALIGNMENT_STYLE_CENTERED:
						adjustedPosX -= lineWidth / 2; // Центрируем строку
						break;
					case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
						adjustedPosX -= lineWidth; // Выравниваем вправо
						break;
					// По умолчанию - выравнивание влево (ничего не меняем)
				}

				// Convert to bitmap
				Bitmap bitmap = Bitmap.createBitmap(lineWidth, (int) (baseline + paint.descent()), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				canvas.drawText(line, 0, baseline, paint);

				// Convert to texture
				Texture tex = new Texture(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.getTextureObjectHandle());
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
				bitmap.recycle();

				// Draw and dispose
				batch.draw(tex, adjustedPosX, posY);
				batch.flush();
				tex.dispose();

				posY += textSizeInPx; // Смещение вниз для следующей строки
			}
		} else {
			// Если перенос строк не требуется, рисуем текст целиком
			int lineWidth = (int) paint.measureText(text);
			int availableWidth = (int) Math.ceil(ScreenValues.currentScreenResolution.getWidth() + 2 * Math.abs(posX));

			if (lineWidth <= availableWidth) {
				// Устанавливаем позицию X для выравнивания
				switch (alignment) {
					case ALIGNMENT_STYLE_CENTERED:
						posX -= lineWidth / 2; // Центрируем текст
						break;
					case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
						posX -= lineWidth; // Выравниваем вправо
						break;
					// Выравнивание влево по умолчанию
				}

				// Convert to bitmap
				Bitmap bitmap = Bitmap.createBitmap(lineWidth, (int) (baseline + paint.descent()), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(bitmap);
				canvas.drawText(text, 0, baseline, paint);

				// Convert to texture
				Texture tex = new Texture(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.getTextureObjectHandle());
				GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
				bitmap.recycle();

				// Draw and dispose
				batch.draw(tex, posX, posY);
				batch.flush();
				tex.dispose();
			}
		}

		// Корректируем позицию Y для центрирования текста
		if (this.isTextWrapped) {
			posY -= totalHeight / 2; // Приводим позицию Y к центру
		} else {
			posY -= textSizeInPx / 2; // Приводим позицию Y к центру
		}

		batch.setColor(1, 1, 1, 1); // Сбрасываем цвет
	}

	public void setPositionX(int xPosition) {
		this.xPosition = xPosition;
	}

	public void setPositionY(int yPosition) {
		this.yPosition = yPosition;
	}

	public String getVariableNameToCompare() {
		return variableNameToCompare;
	}

	public Sprite getSprite() {
		return sprite;
	}
}
