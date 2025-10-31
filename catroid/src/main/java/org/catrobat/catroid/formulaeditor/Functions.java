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
package org.catrobat.catroid.formulaeditor;

import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.utils.EnumUtils;

import java.util.EnumSet;

@LunoClass
public enum Functions {

	SIN, COS, TAN, LN, LOG, SQRT, RAND, ROUND, ROUNDTO, ABS, PI, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, POWER, FLOOR, CEIL,
	MAX,
	MIN, TRUE, FALSE, LENGTH,
	LETTER, SUBTEXT, FILE, TO_HEX, TO_DEC, CLAMP, DISTAN, UPPER, LOWER, REVERSE, VAR, VARNAME, VARVALUE, RANDOM_STR, REPLACE, CONTAINS_STR, REPEAT, TABLE_X, TABLE_Y, TABLE_ELEMENT, TABLE_JOIN, JOIN, JOIN3, DISTANCE, JOINNUMBER, REGEX, LIST_ITEM, CONTAINS, INDEX_OF_ITEM, NUMBER_OF_ITEMS,
	LUA, FLOATARRAY, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT, VIDEO_PLAYING, VIDEO_TIME, JSON_GET, JSON_SET, JSON_IS_VALID,
	ARDUINOANALOG,
	ARDUINODIGITAL, RASPIDIGITAL,
	MULTI_FINGER_X, MULTI_FINGER_Y, MULTI_FINGER_TOUCHED, INDEX_CURRENT_TOUCH, COLLIDES_WITH_COLOR,

	COLOR_TOUCHES_COLOR, COLOR_AT_XY, COLOR_EQUALS_COLOR, TEXT_BLOCK_X, TEXT_BLOCK_Y,
	TEXT_BLOCK_SIZE, TEXT_BLOCK_FROM_CAMERA, TEXT_BLOCK_LANGUAGE_FROM_CAMERA, IF_THEN_ELSE, FLATTEN, CONNECT, FIND,

	// -- 3D ---
	GET_3D_POSITION_X,
	GET_3D_POSITION_Y,
	GET_3D_POSITION_Z,
	GET_3D_ROTATION_YAW,
	GET_3D_ROTATION_PITCH,
	GET_3D_ROTATION_ROLL,
	GET_3D_SCALE_X,
	GET_3D_SCALE_Y,
	GET_3D_SCALE_Z,
	GET_3D_DISTANCE,
	GET_DIRECTION_X,
	GET_DIRECTION_Y,
	GET_ANGLE,
	GET_RAY_DISTANCE,
	GET_RAY_HIT_OBJECT,
	GET_CAMERA_POS_X, GET_CAMERA_POS_Y, GET_CAMERA_POS_Z,
	GET_CAMERA_DIR_X, GET_CAMERA_DIR_Y, GET_CAMERA_DIR_Z,
	GET_CAMERA_ROTATION_YAW, GET_CAMERA_ROTATION_PITCH, GET_CAMERA_ROTATION_ROLL,
	GET_3D_VELOCITY_X, GET_3D_VELOCITY_Y, GET_3D_VELOCITY_Z,
	OBJECT_TOUCHES_OBJECT,
	GET_RAY_HIT_X, GET_RAY_HIT_Y, GET_RAY_HIT_Z, GET_RAY_HIT_NORMAL_X, GET_RAY_HIT_NORMAL_Y, GET_RAY_HIT_NORMAL_Z, RAY_DID_HIT,

	ID_OF_DETECTED_OBJECT, OBJECT_WITH_ID_VISIBLE,
	IS_MOUSE_BUTTON_DOWN,
	RAY_DID_HIT2,
	RAY_HIT_SPRITE_NAME,
	RAY_HIT_X,
	RAY_HIT_Y,
	RAY_HIT_DISTANCE;

	private static final String TAG = Functions.class.getSimpleName();
	public static EnumSet<Functions> TEXT = EnumSet.of(LENGTH, LETTER, SUBTEXT, CLAMP, TO_HEX, TO_DEC, DISTAN, UPPER, LOWER, REVERSE, VAR, VARNAME, VARVALUE, JOIN, JOIN3, REPLACE, CONTAINS_STR, REPEAT, RANDOM_STR, JOINNUMBER,
			REGEX, TABLE_X, TABLE_Y, TABLE_ELEMENT, TABLE_JOIN, FLOATARRAY, LUA, VIEW_X, VIEW_Y, VIEW_WIDTH, VIEW_HEIGHT, VIDEO_PLAYING, VIDEO_TIME, FILE,
			GET_3D_POSITION_X, GET_3D_POSITION_Y, GET_3D_POSITION_Z, GET_3D_ROTATION_YAW, GET_3D_ROTATION_PITCH, GET_3D_ROTATION_ROLL, GET_3D_SCALE_X, GET_3D_SCALE_Y, GET_3D_SCALE_Z, GET_3D_DISTANCE, GET_DIRECTION_X, GET_DIRECTION_Y, GET_ANGLE,
			GET_RAY_DISTANCE, GET_RAY_HIT_OBJECT, GET_CAMERA_POS_X, GET_CAMERA_POS_Y, GET_CAMERA_POS_Z, GET_CAMERA_DIR_X, GET_CAMERA_DIR_Y, GET_CAMERA_DIR_Z,
			GET_3D_VELOCITY_X, GET_3D_VELOCITY_Y, GET_3D_VELOCITY_Z, GET_CAMERA_ROTATION_YAW, GET_CAMERA_ROTATION_PITCH, GET_CAMERA_ROTATION_ROLL, OBJECT_TOUCHES_OBJECT, JSON_GET, JSON_SET, JSON_IS_VALID, GET_RAY_HIT_X, GET_RAY_HIT_Y, GET_RAY_HIT_Z, GET_RAY_HIT_NORMAL_X, GET_RAY_HIT_NORMAL_Y, GET_RAY_HIT_NORMAL_Z,
			IS_MOUSE_BUTTON_DOWN);
	public static final EnumSet<Functions> LIST = EnumSet.of(LIST_ITEM, CONTAINS, INDEX_OF_ITEM,
			NUMBER_OF_ITEMS, FLATTEN, CONNECT, FIND);
	public static final EnumSet<Functions> BOOLEAN = EnumSet.of(TRUE, FALSE, CONTAINS,
			MULTI_FINGER_TOUCHED, COLLIDES_WITH_COLOR, COLOR_TOUCHES_COLOR, COLOR_EQUALS_COLOR, RAY_DID_HIT);

	public static boolean isFunction(String value) {
		return EnumUtils.isValidEnum(Functions.class, value);
	}

	public static boolean isBoolean(Functions function) {
		return BOOLEAN.contains(function);
	}

	public static Functions getFunctionByValue(String value) {
		return EnumUtils.getEnum(Functions.class, value);
	}

	public static void addText(Functions func) {
		TEXT.add(func);
	}
}
