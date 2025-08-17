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
package org.catrobat.catroid.formulaeditor;

import android.content.Context;
import android.util.Log;

import org.catrobat.catroid.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.VisibleForTesting;

import com.danvexteam.lunoscript_annotations.LunoClass;

@LunoClass
public class InternFormula {
	private static final String TAG = InternFormula.class.getSimpleName();

	public enum CursorTokenPosition {
		LEFT, MIDDLE, RIGHT
	}

	public enum CursorTokenPropertiesAfterModification {
		LEFT, RIGHT, SELECT, DO_NOT_MODIFY
	}

	public enum TokenSelectionType {
		USER_SELECTION, PARSER_ERROR_SELECTION
	}

	@VisibleForTesting
	public ExternInternRepresentationMapping externInternRepresentationMapping;

	private List<InternToken> internTokenFormulaList;
	private String externFormulaString;

	@VisibleForTesting
	public InternFormulaTokenSelection internFormulaTokenSelection;

	@VisibleForTesting
	public int externCursorPosition;

	@VisibleForTesting
	public InternToken cursorPositionInternToken;
	private int cursorPositionInternTokenIndex;
	private CursorTokenPosition cursorTokenPosition;

	private InternFormulaParser internTokenFormulaParser;

	public InternFormula(List<InternToken> internTokenList) {

		this.internTokenFormulaList = internTokenList;
		this.externFormulaString = null;
		this.externInternRepresentationMapping = new ExternInternRepresentationMapping();
		this.internFormulaTokenSelection = null;
		this.externCursorPosition = 0;
		this.cursorPositionInternTokenIndex = 0;
	}

	public InternFormula(List<InternToken> internTokenList, InternFormulaTokenSelection internFormulaTokenSelection,
			int externCursorPosition) {
		this.internTokenFormulaList = internTokenList;
		this.externFormulaString = null;
		externInternRepresentationMapping = new ExternInternRepresentationMapping();
		this.internFormulaTokenSelection = internFormulaTokenSelection;
		this.externCursorPosition = externCursorPosition;

		updateInternCursorPosition();
	}

	public void insertTokens(Context context, List<InternToken> tokensToInsert) {
		if (tokensToInsert == null || tokensToInsert.isEmpty()) {
			Log.w(TAG, "insertTokens: tokensToInsert is null or empty.");
			return;
		}

		// Определяем, есть ли активное выделение
		if (isTokenSelected()) {
			// Если есть выделение, заменяем его новыми токенами
			int startIndex = internFormulaTokenSelection.getStartIndex();
			int endIndex = internFormulaTokenSelection.getEndIndex();

			// Удаляем старые токены в диапазоне выделения
			// Идем с конца, чтобы не нарушать индексы
			for (int i = endIndex; i >= startIndex; i--) {
				if (i < internTokenFormulaList.size()) { // Проверка на выход за границы
					internTokenFormulaList.remove(i);
				} else {
					Log.w(TAG, "insertTokens: Attempted to remove token at index " + i + " but list size is " + internTokenFormulaList.size());
				}
			}

			// Вставляем новые токены на место начала старого выделения
			internTokenFormulaList.addAll(startIndex, tokensToInsert);

			// Обновляем позицию курсора, чтобы он был после вставленных токенов
			cursorPositionInternTokenIndex = startIndex + tokensToInsert.size() -1; // Индекс последнего вставленного токена
			cursorPositionInternToken = internTokenFormulaList.get(cursorPositionInternTokenIndex); // Обновляем токен курсора

			// Очищаем выделение после замены
			internFormulaTokenSelection = null;

			// Устанавливаем внешнюю позицию курсора справа от последнего вставленного токена
			// Это делается после generateExternFormulaStringAndInternExternMapping
			// updateExternCursorPosition(CursorTokenPropertiesAfterModification.RIGHT); // или конкретнее

		} else {
			// Если нет выделения, вставляем токены в текущую позицию курсора
			int insertAtIndex;

			if (cursorPositionInternToken == null && internTokenFormulaList.isEmpty()) {
				// Формула пуста, вставляем в начало
				insertAtIndex = 0;
			} else if (cursorPositionInternToken == null && !internTokenFormulaList.isEmpty()) {
				// Курсор не установлен, но формула не пуста, вставляем в конец (или начало, зависит от логики)
				// Предположим, что externCursorPosition уже установлен корректно FormulaEditorEditText
				// и updateInternCursorPosition его обработал.
				// Если cursorTokenPosition == null, это может означать, что курсор в самом начале или самом конце.
				// Используем externCursorPosition, чтобы определить, куда вставлять.
				// Это более сложная логика, так как externCursorPosition может быть между представлениями токенов.
				// Простой вариант: если cursorPositionInternToken == null, но список не пуст,
				// и externCursorPosition == 0, то вставляем в начало. Иначе - в конец.
				if (externCursorPosition == 0 && internTokenFormulaList.size() > 0) {
					insertAtIndex = 0; // Вставка в самое начало, если externCursorPosition = 0
				} else {
					// Вставляем в позицию, соответствующую externCursorPosition, или в конец
					// Если externCursorPosition указывает на начало токена, internTokenIndex будет этим токеном
					// Если externCursorPosition указывает на конец токена, internTokenIndex будет следующим
					// updateInternCursorPosition должен был установить cursorPositionInternTokenIndex
					// и cursorTokenPosition корректно.

					if (cursorPositionInternTokenIndex >= 0 && cursorPositionInternTokenIndex < internTokenFormulaList.size()) {
						if (cursorTokenPosition == CursorTokenPosition.LEFT) {
							insertAtIndex = cursorPositionInternTokenIndex;
						} else if (cursorTokenPosition == CursorTokenPosition.RIGHT) {
							insertAtIndex = cursorPositionInternTokenIndex + 1;
						} else { // MIDDLE или неопределенное состояние - вставляем перед
							insertAtIndex = cursorPositionInternTokenIndex;
						}
					} else if (cursorPositionInternTokenIndex == -1 && internTokenFormulaList.isEmpty()) {
						// Список пуст, вставляем в начало
						insertAtIndex = 0;
					} else if (cursorPositionInternTokenIndex == -1 && !internTokenFormulaList.isEmpty() && externCursorPosition == 0) {
						// Список не пуст, курсор в начале
						insertAtIndex = 0;
					}
					else {
						// В остальных случаях (например, курсор в конце)
						insertAtIndex = internTokenFormulaList.size();
					}
				}
			} else {
				// Курсор установлен на конкретный токен
				switch (cursorTokenPosition) {
					case LEFT:
						insertAtIndex = cursorPositionInternTokenIndex;
						break;
					case MIDDLE: // Обычно вставка "поверх" или слева от "среднего"
						insertAtIndex = cursorPositionInternTokenIndex;
						break;
					case RIGHT:
						insertAtIndex = cursorPositionInternTokenIndex + 1;
						break;
					default:
						// Неожиданное состояние, вставляем в конец как запасной вариант
						Log.w(TAG, "insertTokens: Unexpected cursorTokenPosition: " + cursorTokenPosition);
						insertAtIndex = internTokenFormulaList.size();
						break;
				}
			}
			// Защита от выхода за границы
			if (insertAtIndex > internTokenFormulaList.size()) {
				insertAtIndex = internTokenFormulaList.size();
			}
			if (insertAtIndex < 0) {
				insertAtIndex = 0;
			}


			internTokenFormulaList.addAll(insertAtIndex, tokensToInsert);

			// Обновляем позицию курсора, чтобы он был после вставленных токенов
			// cursorPositionInternTokenIndex теперь указывает на первый из вставленных токенов
			cursorPositionInternTokenIndex = insertAtIndex + tokensToInsert.size() -1;
			if (cursorPositionInternTokenIndex < internTokenFormulaList.size() && cursorPositionInternTokenIndex >=0) {
				cursorPositionInternToken = internTokenFormulaList.get(cursorPositionInternTokenIndex);
			} else if (!internTokenFormulaList.isEmpty()){
				cursorPositionInternTokenIndex = internTokenFormulaList.size() -1;
				cursorPositionInternToken = internTokenFormulaList.get(cursorPositionInternTokenIndex);
			} else {
				cursorPositionInternToken = null;
				cursorPositionInternTokenIndex = -1;
			}
			// Устанавливаем внешнюю позицию курсора справа от последнего вставленного токена
			// Это будет сделано после generateExternFormulaStringAndInternExternMapping
			// updateExternCursorPosition(CursorTokenPropertiesAfterModification.RIGHT);
		}

		// Перегенерировать внешнее представление и маппинг
		generateExternFormulaStringAndInternExternMapping(context);

		// Обновить внешнюю позицию курсора ПОСЛЕ генерации маппинга.
		// Мы хотим, чтобы курсор был справа от последнего добавленного токена.
		if (!tokensToInsert.isEmpty()) {
			// cursorPositionInternTokenIndex уже указывает на последний вставленный токен
			// или на позицию после него, если вставка была в конец.
			// Если cursorPositionInternTokenIndex - это индекс последнего вставленного, то ставим курсор справа от него.
			setExternCursorPositionRightTo(cursorPositionInternTokenIndex);
		} else if (!internTokenFormulaList.isEmpty()){
			// Если ничего не вставили, но список не пуст, и был сброс выделения
			setExternCursorPositionLeftTo(0); // Или какая-то другая логика для курсора по умолчанию
		} else {
			// Список пуст
			externCursorPosition = 0; // Или 1, в зависимости от вашей логики пустого поля
		}

		// Обновляем внутреннее состояние курсора на основе новой внешней позиции
		updateInternCursorPosition();

		Log.d(TAG, "Tokens inserted. New state size: " + internTokenFormulaList.size() +
				", new extern cursor: " + externCursorPosition);
	}

	public void setCursorAndSelection(int externCursorPosition, boolean isSelected) {
		this.externCursorPosition = externCursorPosition;

		updateInternCursorPosition();
		internFormulaTokenSelection = null;

		if (isSelected
				|| externInternRepresentationMapping.getInternTokenByExternIndex(externCursorPosition) != ExternInternRepresentationMapping.MAPPING_NOT_FOUND
				&& (getFirstLeftInternToken(externCursorPosition - 1) == cursorPositionInternToken || cursorPositionInternToken
				.isFunctionParameterBracketOpen())
				&& ((cursorPositionInternToken.isFunctionName())
				|| (cursorPositionInternToken.isFunctionParameterBracketOpen() && cursorTokenPosition == CursorTokenPosition.LEFT)
				|| (cursorPositionInternToken.isSensor()) || (cursorPositionInternToken.isUserVariable())
				|| (cursorPositionInternToken.isUserList()) || (cursorPositionInternToken.isString()))) {
			selectCursorPositionInternToken(TokenSelectionType.USER_SELECTION);
		}
	}

	public void handleKeyInput(int resourceId, Context context, String name) {

		List<InternToken> keyInputInternTokenList = new InternFormulaKeyboardAdapter()
				.createInternTokenListByResourceId(resourceId, name);

		CursorTokenPropertiesAfterModification cursorTokenPropertiesAfterInput = CursorTokenPropertiesAfterModification
				.DO_NOT_MODIFY;

		if (resourceId == R.id.formula_editor_keyboard_delete) {
			cursorTokenPropertiesAfterInput = handleDeletion();
		} else if (isTokenSelected()) {
			cursorTokenPropertiesAfterInput = replaceSelection(keyInputInternTokenList);
		} else if (cursorTokenPosition == null) {
			cursorTokenPropertiesAfterInput = insertRightToCurrentToken(keyInputInternTokenList);
		} else {
			switch (cursorTokenPosition) {
				case LEFT:
					cursorTokenPropertiesAfterInput = insertLeftToCurrentToken(keyInputInternTokenList);
					break;
				case MIDDLE:
					cursorTokenPropertiesAfterInput = replaceCursorPositionInternTokenByTokenList(keyInputInternTokenList);
					break;
				case RIGHT:
					cursorTokenPropertiesAfterInput = insertRightToCurrentToken(keyInputInternTokenList);
					break;
			}
		}

		generateExternFormulaStringAndInternExternMapping(context);
		updateExternCursorPosition(cursorTokenPropertiesAfterInput);
		updateInternCursorPosition();
	}

	public void updateVariableReferences(String oldName, String newName, Context context) {
		for (InternToken internToken : internTokenFormulaList) {
			internToken.updateVariableReferences(oldName, newName);
		}
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public void updateListReferences(String oldName, String newName, Context context) {
		for (InternToken internToken : internTokenFormulaList) {
			internToken.updateListReferences(oldName, newName);
		}
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public void updateCollisionFormula(String oldName, String newName, Context context) {
		for (InternToken internToken : internTokenFormulaList) {
			internToken.updateCollisionFormula(oldName, newName);
		}
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public void updateSensorTokens(String oldName, String newName, Context context) {
		for (InternToken internToken : internTokenFormulaList) {
			internToken.updateSensorTokens(oldName, newName);
		}
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public void updateCollisionFormulaToVersion(Context context) {
		for (InternToken internToken : internTokenFormulaList) {
			internToken.updateCollisionFormulaToVersion();
		}
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public void updateInternCursorPosition() {
		int cursorPositionTokenIndex = externInternRepresentationMapping
				.getInternTokenByExternIndex(externCursorPosition);

		int leftCursorPositionTokenIndex = externInternRepresentationMapping
				.getInternTokenByExternIndex(externCursorPosition - 1);

		int leftleftCursorPositionTokenIndex = externInternRepresentationMapping
				.getInternTokenByExternIndex(externCursorPosition - 2);

		if (cursorPositionTokenIndex != ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			if (leftCursorPositionTokenIndex != ExternInternRepresentationMapping.MAPPING_NOT_FOUND
					&& cursorPositionTokenIndex == leftCursorPositionTokenIndex) {
				cursorTokenPosition = CursorTokenPosition.MIDDLE;
			} else {
				cursorTokenPosition = CursorTokenPosition.LEFT;
			}
		} else if (leftCursorPositionTokenIndex != ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			cursorTokenPosition = CursorTokenPosition.RIGHT;
		} else if (leftleftCursorPositionTokenIndex != ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			cursorTokenPosition = CursorTokenPosition.RIGHT;
			leftCursorPositionTokenIndex = leftleftCursorPositionTokenIndex;
		} else {

			cursorTokenPosition = null;
			this.cursorPositionInternToken = null;
			return;
		}

		switch (cursorTokenPosition) {
			case LEFT:
				this.cursorPositionInternToken = internTokenFormulaList.get(cursorPositionTokenIndex);
				this.cursorPositionInternTokenIndex = cursorPositionTokenIndex;
				break;
			case MIDDLE:
				this.cursorPositionInternToken = internTokenFormulaList.get(cursorPositionTokenIndex);
				this.cursorPositionInternTokenIndex = cursorPositionTokenIndex;
				break;
			case RIGHT:
				this.cursorPositionInternToken = internTokenFormulaList.get(leftCursorPositionTokenIndex);
				this.cursorPositionInternTokenIndex = leftCursorPositionTokenIndex;
				break;
		}
	}

	private void updateExternCursorPosition(CursorTokenPropertiesAfterModification cursorTokenPropertiesAfterInput) {
		switch (cursorTokenPropertiesAfterInput) {
			case LEFT:
				setExternCursorPositionLeftTo(cursorPositionInternTokenIndex);
				break;
			case RIGHT:
				setExternCursorPositionRightTo(cursorPositionInternTokenIndex);
				break;
		}
	}

	private CursorTokenPropertiesAfterModification replaceSelection(List<InternToken> tokenListToInsert) {

		if (InternFormulaUtils.isPeriodToken(tokenListToInsert)) {
			tokenListToInsert = new LinkedList<InternToken>();
			tokenListToInsert.add(new InternToken(InternTokenType.NUMBER, "0."));
		}

		int internTokenSelectionStart = internFormulaTokenSelection.getStartIndex();
		int internTokenSelectionEnd = internFormulaTokenSelection.getEndIndex();

		if (internTokenSelectionStart > internTokenSelectionEnd || internTokenSelectionStart < 0
				|| internTokenSelectionEnd < 0) {

			internFormulaTokenSelection = null;
			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		List<InternToken> tokenListToRemove = new LinkedList<>();
		for (int tokensToRemove = 0; tokensToRemove <= internTokenSelectionEnd - internTokenSelectionStart; tokensToRemove++) {
			tokenListToRemove.add(internTokenFormulaList.get(internTokenSelectionStart + tokensToRemove));
		}

		if (InternFormulaUtils.isFunction(tokenListToRemove)) {
			cursorPositionInternToken = tokenListToRemove.get(0);
			cursorPositionInternTokenIndex = internTokenSelectionStart;
			return replaceCursorPositionInternTokenByTokenList(tokenListToInsert);
		} else {
			replaceInternTokens(tokenListToInsert, internTokenSelectionStart, internTokenSelectionEnd);

			return setCursorPositionAndSelectionAfterInput(internTokenSelectionStart);
		}
	}

	public void deleteSelection(Context context) {
		CursorTokenPropertiesAfterModification cursorTokenPropertiesAfterInput = handleDeletion();
		updateExternCursorPosition(cursorTokenPropertiesAfterInput);
		generateExternFormulaStringAndInternExternMapping(context);
		updateInternCursorPosition();
	}

	private void deleteInternTokens(int deleteIndexStart, int deleteIndexEnd) {
		List<InternToken> tokenListToInsert = new LinkedList<InternToken>();
		replaceInternTokens(tokenListToInsert, deleteIndexStart, deleteIndexEnd);
	}

	private void replaceInternTokens(List<InternToken> tokenListToInsert, int replaceIndexStart, int replaceIndexEnd) {
		for (int tokensToRemove = replaceIndexEnd - replaceIndexStart; tokensToRemove >= 0; tokensToRemove--) {
			internTokenFormulaList.remove(replaceIndexStart);
		}
		internTokenFormulaList.addAll(replaceIndexStart, tokenListToInsert);
	}

	private CursorTokenPropertiesAfterModification handleDeletion() {
		CursorTokenPropertiesAfterModification cursorTokenPropertiesAfterModification = CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		if (internFormulaTokenSelection != null) {
			deleteInternTokens(internFormulaTokenSelection.getStartIndex(), internFormulaTokenSelection.getEndIndex());

			cursorPositionInternTokenIndex = internFormulaTokenSelection.getStartIndex();
			cursorPositionInternToken = null;

			internFormulaTokenSelection = null;

			cursorTokenPropertiesAfterModification = CursorTokenPropertiesAfterModification.LEFT;
		} else {
			switch (cursorTokenPosition) {
				case LEFT:
					InternToken firstLeftInternToken = getFirstLeftInternToken(externCursorPosition - 1);
					if (firstLeftInternToken == null) {
						cursorTokenPropertiesAfterModification = CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
					} else {
						if (firstLeftInternToken.getInternTokenType() == InternTokenType.FUNCTION_PARAMETER_DELIMITER) {
							setExternCursorPositionLeftTo(internTokenFormulaList.indexOf(firstLeftInternToken));
							break;
						}

						int firstLeftInternTokenIndex = internTokenFormulaList.indexOf(firstLeftInternToken);

						cursorTokenPropertiesAfterModification = deleteInternTokenByIndex(firstLeftInternTokenIndex);
					}
					break;

				case MIDDLE:
					cursorTokenPropertiesAfterModification = deleteInternTokenByIndex(cursorPositionInternTokenIndex);
					break;

				case RIGHT:
					InternToken internToken = getFirstLeftInternToken(externCursorPosition);
					if (internToken.getInternTokenType() == InternTokenType.FUNCTION_PARAMETER_DELIMITER) {
						setExternCursorPositionLeftTo(internTokenFormulaList.indexOf(internToken));
						break;
					}

					cursorTokenPropertiesAfterModification = deleteInternTokenByIndex(cursorPositionInternTokenIndex);
					break;
			}
		}

		return cursorTokenPropertiesAfterModification;
	}

	private CursorTokenPropertiesAfterModification deleteInternTokenByIndex(int internTokenIndex) {

		InternToken tokenToDelete = internTokenFormulaList.get(internTokenIndex);

		switch (tokenToDelete.getInternTokenType()) {
			case NUMBER:
				int externNumberOffset = externInternRepresentationMapping.getExternTokenStartOffset(
						externCursorPosition, internTokenIndex);

				if (externNumberOffset == -1) {
					return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
				}

				InternToken modifiedToken = InternFormulaUtils.deleteNumberByOffset(tokenToDelete, externNumberOffset);

				if (modifiedToken == null) {
					internTokenFormulaList.remove(internTokenIndex);

					cursorPositionInternTokenIndex = internTokenIndex;
					cursorPositionInternToken = null;
					return CursorTokenPropertiesAfterModification.LEFT;
				}

				externCursorPosition--;
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;

			case FUNCTION_NAME:
				List<InternToken> functionInternTokens = InternFormulaUtils.getFunctionByName(internTokenFormulaList,
						internTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
				}

				int lastListIndex = functionInternTokens.size() - 1;
				InternToken lastFunctionToken = functionInternTokens.get(lastListIndex);
				int endIndexToDelete = internTokenFormulaList.indexOf(lastFunctionToken);

				deleteInternTokens(internTokenIndex, endIndexToDelete);
				setExternCursorPositionLeftTo(internTokenIndex);

				cursorPositionInternTokenIndex = internTokenIndex;
				cursorPositionInternToken = null;

				return CursorTokenPropertiesAfterModification.LEFT;

			case FUNCTION_PARAMETERS_BRACKET_OPEN:
				functionInternTokens = InternFormulaUtils.getFunctionByFunctionBracketOpen(internTokenFormulaList,
						internTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
				}

				int functionInternTokensLastIndex = functionInternTokens.size() - 1;

				int startDeletionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endIndexToDelete = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				deleteInternTokens(startDeletionIndex, endIndexToDelete);

				cursorPositionInternTokenIndex = startDeletionIndex;
				cursorPositionInternToken = null;
				return CursorTokenPropertiesAfterModification.LEFT;

			case FUNCTION_PARAMETERS_BRACKET_CLOSE:
				functionInternTokens = InternFormulaUtils.getFunctionByFunctionBracketClose(internTokenFormulaList,
						internTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
				}

				functionInternTokensLastIndex = functionInternTokens.size() - 1;

				startDeletionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endIndexToDelete = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				deleteInternTokens(startDeletionIndex, endIndexToDelete);

				cursorPositionInternTokenIndex = startDeletionIndex;
				cursorPositionInternToken = null;
				return CursorTokenPropertiesAfterModification.LEFT;

			case FUNCTION_PARAMETER_DELIMITER:
				functionInternTokens = InternFormulaUtils.getFunctionByParameterDelimiter(internTokenFormulaList,
						internTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
				}

				functionInternTokensLastIndex = functionInternTokens.size() - 1;

				startDeletionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endIndexToDelete = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				deleteInternTokens(startDeletionIndex, endIndexToDelete);

				cursorPositionInternTokenIndex = startDeletionIndex;
				cursorPositionInternToken = null;
				return CursorTokenPropertiesAfterModification.LEFT;

			default:
				deleteInternTokens(internTokenIndex, internTokenIndex);

				cursorPositionInternTokenIndex = internTokenIndex;
				cursorPositionInternToken = null;
				return CursorTokenPropertiesAfterModification.LEFT;
		}
	}

	@VisibleForTesting
	public void setExternCursorPositionLeftTo(int internTokenIndex) {
		if (internTokenFormulaList.size() < 1) {
			externCursorPosition = 1;
			return;
		}
		if (internTokenIndex >= internTokenFormulaList.size()) {
			setExternCursorPositionRightTo(internTokenFormulaList.size() - 1);
			return;
		}

		int externTokenStartIndex = externInternRepresentationMapping.getExternTokenStartIndex(internTokenIndex);
		if (externTokenStartIndex == ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			return;
		}

		externCursorPosition = externTokenStartIndex;
		cursorTokenPosition = CursorTokenPosition.LEFT;
	}

	@VisibleForTesting
	public void setExternCursorPositionRightTo(int internTokenIndex) {

		if (internTokenFormulaList.size() < 1) {
			return;
		}
		if (internTokenIndex >= internTokenFormulaList.size()) {
			internTokenIndex = internTokenFormulaList.size() - 1;
		}

		int externTokenEndIndex = externInternRepresentationMapping.getExternTokenEndIndex(internTokenIndex);
		if (externTokenEndIndex == ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			return;
		}

		externCursorPosition = externTokenEndIndex;
		cursorTokenPosition = CursorTokenPosition.RIGHT;
	}

	public void generateExternFormulaStringAndInternExternMapping(Context context) {
		InternToExternGenerator internToExternGenerator = new InternToExternGenerator(context);

		internToExternGenerator.generateExternStringAndMapping(internTokenFormulaList);
		externFormulaString = internToExternGenerator.getGeneratedExternFormulaString();
		externInternRepresentationMapping = internToExternGenerator.getGeneratedExternInternRepresentationMapping();
	}

	public String trimExternFormulaString(Context context) {
		InternToExternGenerator internToExternGenerator = new InternToExternGenerator(context);

		internToExternGenerator.trimExternString(internTokenFormulaList);
		externFormulaString = internToExternGenerator.getGeneratedExternFormulaString();
		externInternRepresentationMapping = internToExternGenerator.getGeneratedExternInternRepresentationMapping();
		return externFormulaString;
	}

	@VisibleForTesting
	public void selectCursorPositionInternToken(TokenSelectionType internTokenSelectionType) {

		internFormulaTokenSelection = null;
		if (cursorPositionInternToken == null) {
			return;
		}

		switch (cursorPositionInternToken.getInternTokenType()) {
			case FUNCTION_NAME:
				List<InternToken> functionInternTokens = InternFormulaUtils.getFunctionByName(internTokenFormulaList,
						cursorPositionInternTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return;
				}

				int lastListIndex = functionInternTokens.size() - 1;
				InternToken lastFunctionToken = functionInternTokens.get(lastListIndex);

				int endSelectionIndex = internTokenFormulaList.indexOf(lastFunctionToken);

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						cursorPositionInternTokenIndex, endSelectionIndex);
				break;

			case FUNCTION_PARAMETERS_BRACKET_OPEN:
				functionInternTokens = InternFormulaUtils.getFunctionByFunctionBracketOpen(internTokenFormulaList,
						cursorPositionInternTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return;
				}

				int functionInternTokensLastIndex = functionInternTokens.size() - 1;

				int startSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						startSelectionIndex, endSelectionIndex);
				break;

			case FUNCTION_PARAMETERS_BRACKET_CLOSE:
				functionInternTokens = InternFormulaUtils.getFunctionByFunctionBracketClose(internTokenFormulaList,
						cursorPositionInternTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return;
				}

				functionInternTokensLastIndex = functionInternTokens.size() - 1;

				startSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						startSelectionIndex, endSelectionIndex);
				break;

			case FUNCTION_PARAMETER_DELIMITER:
				functionInternTokens = InternFormulaUtils.getFunctionByParameterDelimiter(internTokenFormulaList,
						cursorPositionInternTokenIndex);

				if (functionInternTokens == null || functionInternTokens.size() == 0) {
					return;
				}

				functionInternTokensLastIndex = functionInternTokens.size() - 1;

				startSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens.get(0));
				endSelectionIndex = internTokenFormulaList.indexOf(functionInternTokens
						.get(functionInternTokensLastIndex));

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						startSelectionIndex, endSelectionIndex);
				break;

			case BRACKET_OPEN:
				List<InternToken> bracketsInternTokens = InternFormulaUtils.generateTokenListByBracketOpen(
						internTokenFormulaList, cursorPositionInternTokenIndex);

				if (bracketsInternTokens == null || bracketsInternTokens.size() == 0) {
					return;
				}

				int bracketsInternTokensLastIndex = bracketsInternTokens.size() - 1;

				startSelectionIndex = cursorPositionInternTokenIndex;
				endSelectionIndex = internTokenFormulaList.indexOf(bracketsInternTokens
						.get(bracketsInternTokensLastIndex));

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						startSelectionIndex, endSelectionIndex);
				break;

			case BRACKET_CLOSE:
				bracketsInternTokens = InternFormulaUtils.generateTokenListByBracketClose(internTokenFormulaList,
						cursorPositionInternTokenIndex);

				if (bracketsInternTokens == null || bracketsInternTokens.size() == 0) {
					return;
				}

				bracketsInternTokensLastIndex = bracketsInternTokens.size() - 1;

				startSelectionIndex = internTokenFormulaList.indexOf(bracketsInternTokens.get(0));
				endSelectionIndex = internTokenFormulaList.indexOf(bracketsInternTokens
						.get(bracketsInternTokensLastIndex));

				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						startSelectionIndex, endSelectionIndex);
				break;

			default:
				internFormulaTokenSelection = new InternFormulaTokenSelection(internTokenSelectionType,
						cursorPositionInternTokenIndex, cursorPositionInternTokenIndex);
				break;
		}
	}

	private CursorTokenPropertiesAfterModification insertLeftToCurrentToken(List<InternToken> internTokensToInsert) {

		InternToken firstLeftInternToken = null;
		if (cursorPositionInternTokenIndex > 0) {
			firstLeftInternToken = internTokenFormulaList.get(cursorPositionInternTokenIndex - 1);
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isNumberToken(internTokensToInsert)) {

			String numberToInsert = internTokensToInsert.get(0).getTokenStringValue();

			InternFormulaUtils.insertIntoNumberToken(cursorPositionInternToken, 0, numberToInsert);
			externCursorPosition++;

			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isPeriodToken(internTokensToInsert)) {
			String numberString = cursorPositionInternToken.getTokenStringValue();
			if (numberString.contains(".")) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			InternFormulaUtils.insertIntoNumberToken(cursorPositionInternToken, 0, "0.");
			externCursorPosition += 2;

			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (firstLeftInternToken != null && firstLeftInternToken.isNumber()
				&& InternFormulaUtils.isNumberToken(internTokensToInsert)) {

			firstLeftInternToken.appendToTokenStringValue(internTokensToInsert);

			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (firstLeftInternToken != null && firstLeftInternToken.isNumber()
				&& InternFormulaUtils.isPeriodToken(internTokensToInsert)) {

			String numberString = firstLeftInternToken.getTokenStringValue();
			if (numberString.contains(".")) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			firstLeftInternToken.appendToTokenStringValue(".");

			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (InternFormulaUtils.isPeriodToken(internTokensToInsert)) {
			internTokenFormulaList.add(cursorPositionInternTokenIndex, new InternToken(InternTokenType.NUMBER, "0."));

			cursorPositionInternToken = null;
			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		internTokenFormulaList.addAll(cursorPositionInternTokenIndex, internTokensToInsert);
		return setCursorPositionAndSelectionAfterInput(cursorPositionInternTokenIndex);
	}

	private CursorTokenPropertiesAfterModification insertRightToCurrentToken(List<InternToken> internTokensToInsert) {

		if (cursorPositionInternToken == null) {

			if (InternFormulaUtils.isPeriodToken(internTokensToInsert)) {
				internTokensToInsert = new LinkedList<InternToken>();
				internTokensToInsert.add(new InternToken(InternTokenType.NUMBER, "0."));
			}
			internTokenFormulaList.addAll(0, internTokensToInsert);

			return setCursorPositionAndSelectionAfterInput(0);
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isNumberToken(internTokensToInsert)) {

			cursorPositionInternToken.appendToTokenStringValue(internTokensToInsert);

			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isPeriodToken(internTokensToInsert)) {
			String numberString = cursorPositionInternToken.getTokenStringValue();
			if (numberString.contains(".")) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}
			cursorPositionInternToken.appendToTokenStringValue(".");

			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		if (InternFormulaUtils.isPeriodToken(internTokensToInsert)) {

			internTokenFormulaList.add(cursorPositionInternTokenIndex + 1,
					new InternToken(InternTokenType.NUMBER, "0."));

			cursorPositionInternToken = null;
			cursorPositionInternTokenIndex = cursorPositionInternTokenIndex + 1;
			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		internTokenFormulaList.addAll(cursorPositionInternTokenIndex + 1, internTokensToInsert);
		return setCursorPositionAndSelectionAfterInput(cursorPositionInternTokenIndex + 1);
	}

	private CursorTokenPropertiesAfterModification setCursorPositionAndSelectionAfterInput(int insertedInternTokenIndex) {
		if (internTokenFormulaList.isEmpty()) {
			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		InternToken insertedInternToken = internTokenFormulaList.get(insertedInternTokenIndex);

		if (insertedInternToken.getInternTokenType() == InternTokenType.FUNCTION_NAME) {
			List<InternToken> functionInternTokenList = InternFormulaUtils.getFunctionByName(
					internTokenFormulaList, insertedInternTokenIndex);

			if (functionInternTokenList.size() >= 4) {
				List<List<InternToken>> functionParameters = InternFormulaUtils
						.getFunctionParameterInternTokensAsLists(functionInternTokenList);
				List<InternToken> functionFirstParameter = functionParameters.get(0);
				String functionName = functionInternTokenList.get(0).getTokenStringValue();

				if (functionFirstParameter.isEmpty()) {
					internFormulaTokenSelection = null;
					cursorPositionInternTokenIndex = insertedInternTokenIndex + 1;
				} else {
					if (userListNotFirstParameter(functionName, functionFirstParameter.get(0))) {
						functionFirstParameter = functionParameters.get(1);
						insertedInternTokenIndex += 2;
					}
					internFormulaTokenSelection = new InternFormulaTokenSelection(TokenSelectionType.USER_SELECTION,
							insertedInternTokenIndex + 2,
							insertedInternTokenIndex + functionFirstParameter.size() + 1);
					cursorPositionInternTokenIndex = internFormulaTokenSelection.getEndIndex();
				}
			} else {
				cursorPositionInternTokenIndex = insertedInternTokenIndex + functionInternTokenList.size() - 1;
				internFormulaTokenSelection = null;
			}
		} else {
			cursorPositionInternTokenIndex = insertedInternTokenIndex;
			internFormulaTokenSelection = null;
		}
		cursorPositionInternToken = null;
		return CursorTokenPropertiesAfterModification.RIGHT;
	}

	private boolean userListNotFirstParameter(String functionName,
			InternToken functionFirstParameter) {
		return (functionName.equals(Functions.LIST_ITEM.name()) || functionName.equals(Functions.INDEX_OF_ITEM.name())) && !functionFirstParameter.isUserList();
	}

	@VisibleForTesting
	public CursorTokenPropertiesAfterModification replaceCursorPositionInternTokenByTokenList(
			List<InternToken> internTokensToReplaceWith) {

		Log.i(TAG, "replaceCursorPositionInternTokenByTokenList:enter");

		if (cursorPositionInternToken.isNumber() && internTokensToReplaceWith.size() == 1
				&& internTokensToReplaceWith.get(0).isOperator()) {

			int externNumberOffset = externInternRepresentationMapping.getExternTokenStartOffset(externCursorPosition,
					cursorPositionInternTokenIndex);
			List<InternToken> replaceList = InternFormulaUtils.insertOperatorToNumberToken(cursorPositionInternToken, externNumberOffset, internTokensToReplaceWith.get(0));
			replaceInternTokens(replaceList, cursorPositionInternTokenIndex, cursorPositionInternTokenIndex);

			return setCursorPositionAndSelectionAfterInput(cursorPositionInternTokenIndex);
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isNumberToken(internTokensToReplaceWith)) {

			InternToken numberTokenToInsert = internTokensToReplaceWith.get(0);

			int externNumberOffset = externInternRepresentationMapping.getExternTokenStartOffset(externCursorPosition,
					cursorPositionInternTokenIndex);

			if (externNumberOffset == -1) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			InternFormulaUtils.insertIntoNumberToken(cursorPositionInternToken, externNumberOffset,
					numberTokenToInsert.getTokenStringValue());

			externCursorPosition++;
			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (cursorPositionInternToken.isNumber() && InternFormulaUtils.isPeriodToken(internTokensToReplaceWith)) {

			String numberString = cursorPositionInternToken.getTokenStringValue();
			if (numberString.contains(".")) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			int externNumberOffset = externInternRepresentationMapping.getExternTokenStartOffset(externCursorPosition,
					cursorPositionInternTokenIndex);

			if (externNumberOffset == -1) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			InternFormulaUtils.insertIntoNumberToken(cursorPositionInternToken, externNumberOffset, ".");
			externCursorPosition++;

			return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
		}

		if (cursorPositionInternToken.isFunctionName()) {

			List<InternToken> functionInternTokens = InternFormulaUtils.getFunctionByName(internTokenFormulaList,
					cursorPositionInternTokenIndex);

			if (functionInternTokens == null) {
				return CursorTokenPropertiesAfterModification.DO_NOT_MODIFY;
			}

			int lastListIndex = functionInternTokens.size() - 1;
			InternToken lastFunctionToken = functionInternTokens.get(lastListIndex);
			int endIndexToReplace = internTokenFormulaList.indexOf(lastFunctionToken);

			List<InternToken> tokensToInsert = InternFormulaUtils.replaceFunctionByTokens(functionInternTokens,
					internTokensToReplaceWith);

			replaceInternTokens(tokensToInsert, cursorPositionInternTokenIndex, endIndexToReplace);

			return setCursorPositionAndSelectionAfterInput(cursorPositionInternTokenIndex);
		}

		if (InternFormulaUtils.isPeriodToken(internTokensToReplaceWith)) {
			internTokenFormulaList.add(cursorPositionInternTokenIndex + 1,
					new InternToken(InternTokenType.NUMBER, "0."));

			cursorPositionInternToken = null;
			cursorPositionInternTokenIndex = cursorPositionInternTokenIndex + 1;
			return CursorTokenPropertiesAfterModification.RIGHT;
		}

		replaceInternTokens(internTokensToReplaceWith, cursorPositionInternTokenIndex, cursorPositionInternTokenIndex);

		return setCursorPositionAndSelectionAfterInput(cursorPositionInternTokenIndex);
	}

	public InternToken getFirstLeftInternToken(int externIndex) {
		for (int searchIndex = externIndex; searchIndex >= 0; searchIndex--) {
			if (externInternRepresentationMapping.getInternTokenByExternIndex(searchIndex) != ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
				int internTokenIndex = externInternRepresentationMapping.getInternTokenByExternIndex(searchIndex);
				return internTokenFormulaList.get(internTokenIndex);
			}
		}
		return null;
	}

	public int getExternCursorPosition() {
		return this.externCursorPosition;
	}

	public InternFormulaParser getInternFormulaParser() {
		internTokenFormulaParser = new InternFormulaParser(internTokenFormulaList);
		return internTokenFormulaParser;
	}

	public void selectParseErrorTokenAndSetCursor() {
		if (internTokenFormulaParser == null || internTokenFormulaList.size() == 0) {
			return;
		}

		int internErrorTokenIndex = internTokenFormulaParser.getErrorTokenIndex();

		if (internErrorTokenIndex < 0) {
			return;
		}

		if (internErrorTokenIndex >= internTokenFormulaList.size()) {
			internErrorTokenIndex = internTokenFormulaList.size() - 1;
		}

		setExternCursorPositionRightTo(internErrorTokenIndex);
		cursorPositionInternTokenIndex = internErrorTokenIndex;
		cursorPositionInternToken = internTokenFormulaList.get(cursorPositionInternTokenIndex);
		selectCursorPositionInternToken(TokenSelectionType.PARSER_ERROR_SELECTION);
	}

	public TokenSelectionType getExternSelectionType() {
		if (!isTokenSelected()) {
			return null;
		}

		return internFormulaTokenSelection.getTokenSelectionType();
	}

	public void selectWholeFormula() {

		if (internTokenFormulaList.size() == 0) {
			return;
		}

		internFormulaTokenSelection = new InternFormulaTokenSelection(TokenSelectionType.USER_SELECTION, 0,
				internTokenFormulaList.size() - 1);
	}

	public InternFormulaState getInternFormulaState() {

		List<InternToken> deepCopyOfInternTokenFormula = new LinkedList<InternToken>();
		InternFormulaTokenSelection deepCopyOfInternFormulaTokenSelection = null;

		for (InternToken tokenToCopy : internTokenFormulaList) {
			deepCopyOfInternTokenFormula.add(tokenToCopy.deepCopy());
		}

		if (isTokenSelected()) {
			deepCopyOfInternFormulaTokenSelection = internFormulaTokenSelection.deepCopy();
		}

		return new InternFormulaState(deepCopyOfInternTokenFormula, deepCopyOfInternFormulaTokenSelection,
				externCursorPosition);
	}

	public InternFormulaTokenSelection getSelection() {
		return internFormulaTokenSelection;
	}

	public int getExternSelectionStartIndex() {
		if (internFormulaTokenSelection == null) {
			return -1;
		}

		int externSelectionStartIndex = externInternRepresentationMapping
				.getExternTokenStartIndex(internFormulaTokenSelection.getStartIndex());

		if (externSelectionStartIndex == ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			return -1;
		}

		return externSelectionStartIndex;
	}

	public int getExternSelectionEndIndex() {
		if (internFormulaTokenSelection == null) {
			return -1;
		}

		int externSelectionEndIndex = externInternRepresentationMapping
				.getExternTokenEndIndex(internFormulaTokenSelection.getEndIndex());

		if (externSelectionEndIndex == ExternInternRepresentationMapping.MAPPING_NOT_FOUND) {
			return -1;
		}

		return externSelectionEndIndex;
	}

	public void addTokens(Context context, List<InternToken> tokens) {
		CursorTokenPropertiesAfterModification cursorTokenPropertiesAfterInput =
				CursorTokenPropertiesAfterModification
						.DO_NOT_MODIFY;

		if (isTokenSelected()) {
			int startIndex = internFormulaTokenSelection.getStartIndex();
			replaceInternTokens(tokens, startIndex, internFormulaTokenSelection.getEndIndex());
			cursorTokenPropertiesAfterInput = setCursorPositionAndSelectionAfterInput(startIndex);
		} else if (cursorTokenPosition == null) {
			cursorTokenPropertiesAfterInput = insertRightToCurrentToken(tokens);
		} else {
			switch (cursorTokenPosition) {
				case LEFT:
					cursorTokenPropertiesAfterInput = insertLeftToCurrentToken(tokens);
					break;
				case MIDDLE:
					cursorTokenPropertiesAfterInput = replaceCursorPositionInternTokenByTokenList(tokens);
					break;
				case RIGHT:
					cursorTokenPropertiesAfterInput = insertRightToCurrentToken(tokens);
					break;
			}
		}

		generateExternFormulaStringAndInternExternMapping(context);
		updateExternCursorPosition(cursorTokenPropertiesAfterInput);
		updateInternCursorPosition();
	}

	public List<InternToken> getSelectedTokenForCopy() {
		List<InternToken> tokens = new ArrayList<>();
		if (internTokenFormulaList != null && internFormulaTokenSelection != null) {
			for (int i = internFormulaTokenSelection.getStartIndex(); i <= internFormulaTokenSelection.getEndIndex(); i++) {
				tokens.add(internTokenFormulaList.get(i));
			}
			return tokens;
		}
		return null;
	}

	private InternToken getSelectedToken() {
		if (internFormulaTokenSelection == null || internFormulaTokenSelection.getTokenSelectionType() != TokenSelectionType.USER_SELECTION) {
			return null;
		}
		int currentIndex = 0;
		for (InternToken token : internTokenFormulaList) {
			if (token.getInternTokenType() == InternTokenType.STRING
					&& internFormulaTokenSelection.getStartIndex() == currentIndex) {
				return token;
			}
			currentIndex++;
		}
		return null;
	}

	public boolean isSelectedTokenFirstParamOfRegularExpression() {

		boolean isFirstParamInRegularExpression = false;

		if (internFormulaTokenSelection != null) {
			int indexOfSelectedTokenInTokenSelection = internFormulaTokenSelection.getStartIndex();

			if (indexOfSelectedTokenInTokenSelection >= 2) {

				isFirstParamInRegularExpression =
						isSelectedTokenTypeString(indexOfSelectedTokenInTokenSelection)
								&& isTokenBeforeSelectedTypeBracketOpen(indexOfSelectedTokenInTokenSelection)
								&& isTwoTokensBeforeSelectedAFunctionAndNamedRegex(indexOfSelectedTokenInTokenSelection);
			}
		}
		return isFirstParamInRegularExpression;
	}

	private boolean isSelectedTokenTypeString(int index) {
		boolean isStringOrFunction = true;

		InternTokenType typeFunction = InternTokenType.FUNCTION_NAME;
		InternTokenType typeString = InternTokenType.STRING;

		InternTokenType selectedTokenType = internTokenFormulaList.get(index).getInternTokenType();

		if (!(selectedTokenType == typeString || selectedTokenType == typeFunction)) {
			isStringOrFunction = false;
		}
		return isStringOrFunction;
	}

	private boolean isTokenBeforeSelectedTypeBracketOpen(int index) {
		boolean isBracket = true;
		InternTokenType typeBracketOpen = InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN;
		if (!(internTokenFormulaList.get(index - 1).getInternTokenType()
				== typeBracketOpen)) {
			isBracket = false;
		}
		return isBracket;
	}

	private boolean isTwoTokensBeforeSelectedAFunctionAndNamedRegex(int index) {
		boolean isRegex = true;
		InternTokenType typeFunctionName = InternTokenType.FUNCTION_NAME;
		String stringOfRegularExpression = Functions.REGEX.name();
		InternToken functionToken = internTokenFormulaList.get(index - 2);
		if (!(functionToken.getInternTokenType() == typeFunctionName
				&& functionToken.getTokenStringValue().equals(stringOfRegularExpression))) {
			isRegex = false;
		}
		return isRegex;
	}

	public int getIndexOfInternTokenSelection() {
		if (internFormulaTokenSelection != null) {
			return internFormulaTokenSelection.getStartIndex();
		}
		return -1;
	}

	public void setSelectionToFirstParamOfRegularExpressionAtInternalIndex(int indexOfRegularExpression) {
		if (indexOfRegularExpression < 0) {
			return;
		}

		int indexOfFirstParam = indexOfRegularExpression + 2;

		cursorPositionInternTokenIndex = indexOfFirstParam;
		updateExternCursorPosition(CursorTokenPropertiesAfterModification.RIGHT);
		setCursorAndSelection(externCursorPosition, true);
	}

	public int getIndexOfCorrespondingRegularExpression() {
		int indexOfSelectedToken = -1;
		if (internFormulaTokenSelection != null) {
			indexOfSelectedToken = internFormulaTokenSelection.getStartIndex();

			InternToken selectedToken = internTokenFormulaList.get(indexOfSelectedToken);

			InternTokenType selectedType = selectedToken.getInternTokenType();
			String selectedStringValue = selectedToken.getTokenStringValue();

			if (selectedType == InternTokenType.FUNCTION_NAME
					&& selectedStringValue.equals(Functions.REGEX.name())) {
				return indexOfSelectedToken;
			} else {
				return getIndexOfRegularExpressionIfParamIsSelected(indexOfSelectedToken);
			}
		}
		return -1;
	}

	private int getIndexOfRegularExpressionIfParamIsSelected(int index) {
		if (index >= 2) {
			int bracketCount = 0;

			for (int i = index - 1; i >= 0; i--) {
				InternToken iteratedToken = internTokenFormulaList.get(i);
				if (iteratedToken.getInternTokenType() == InternTokenType.FUNCTION_PARAMETERS_BRACKET_CLOSE) {
					bracketCount -= 1;
				} else if (iteratedToken.getInternTokenType() == InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN) {
					bracketCount += 1;
				}

				if (bracketCount == 1 && i > 0) {
					InternToken functionToken = internTokenFormulaList.get(i - 1);
					if (functionToken.getInternTokenType() == InternTokenType.FUNCTION_NAME
							&& functionToken.getTokenStringValue().equals(Functions.REGEX.name())) {
						return i - 1;
					}
				}
			}
		}
		return -1;
	}

	public String getSelectedText() {
		InternToken token = getSelectedToken();
		if (token == null) {
			return null;
		}

		return token.getTokenStringValue();
	}

	public void overrideSelectedText(String string, Context context) {
		InternToken token = getSelectedToken();
		if (token == null) {
			return;
		}

		token.setTokenStringValue(string);
		generateExternFormulaStringAndInternExternMapping(context);
	}

	public String getExternFormulaString() {
		return externFormulaString;
	}

	private boolean isTokenSelected() {
		return internFormulaTokenSelection != null;
	}

	public boolean isThereSomethingToDelete() {
		if (internFormulaTokenSelection != null) {
			return true;
		}
		return !(cursorTokenPosition == null
				|| (cursorTokenPosition == CursorTokenPosition.LEFT && getFirstLeftInternToken(externCursorPosition - 1) == null));
	}

	public void setInternTokenFormulaList(List<InternToken> list) {
		internTokenFormulaList = list;
	}

	public List<InternToken> getInternTokenFormulaList() {
		return internTokenFormulaList;
	}
}
