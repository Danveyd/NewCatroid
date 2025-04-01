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

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.common.ScreenModes;
import org.catrobat.catroid.common.ScreenValues;
import org.luaj.vm2.ast.Str;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class XmlHeader implements Serializable {

	private static final long serialVersionUID = 1L;

	private String programName;
	private String description;
	private String notesAndCredits;

	@XStreamAlias("screenWidth")
	public int virtualScreenWidth = 0;
	@XStreamAlias("screenHeight")
	public int virtualScreenHeight = 0;

	@XStreamAlias("physicsWidthArea")
	public float physicsWidthArea = 3.0f;

	@XStreamAlias("physicsHeightArea")
	public float physicsHeightArea = 2.0f;

	public ScreenModes screenMode = ScreenModes.STRETCH;

	public Boolean customResolution = false;

	private double catrobatLanguageVersion;
	private boolean landscapeMode;
	private boolean isCastProject = false;
	@SuppressWarnings("unused")
	public boolean scenesEnabled = true;
	private String listeningLanguageTag = "";

	//==============================================================================================
	// mutable fields only used by Catroweb (share.catrob.at website) so far
	//==============================================================================================
	private String applicationBuildName = "";
	private int applicationBuildNumber = 0;
	private String applicationName = "";
	private String applicationVersion = "";
	private String applicationBuildType = "";
	private String deviceName = "";
	private String platform = "";
	private String platformVersion = "";
	private String tags = "";
	//----------------------------------------------------------------------------------------------

	//==============================================================================================
	// immutable (i.e. read-only) fields only used and updated by Catroweb during upload
	//==============================================================================================
	//
	// ***  CATROBAT REMIX SPECIFICATION REQUIREMENT ***
	//
	//  Keep in mind that the remixGrandparentsUrlString-field (respectively remixOf-XML-field)
	//  (see below) is used by Catroweb's web application "share.catrob.at" only.
	//  Once new Catrobat programs get uploaded, Catroweb automatically updates this XML-field
	//  and sets the program as being remixed!
	//  In order to do so, Catroweb takes the value from the url-XML-field (see above) and assigns
	//  it to this XML-field.
	//
	//  With that said, the only correct way to set a new remix-URL (e.g. when two programs get
	//  merged locally) is to assign it to the remixParentsUrlString-field.
	//
	//  How to deal with re-merged programs?
	//    If you plan to merge a program A with another (already) merged program B, you have to put
	//    the url of A's parent and all urls of B's parents together into one single string
	//    and assign it to the remixParentsUrlString-field.
	//    The same process is repeated for successive re-merges...
	//    For more details, please have a look at the generateRemixUrlsStringForMergedProgram()
	//    method in Utils.java
	//
	@SuppressWarnings("unused")
	@XStreamAlias("remixOf")
	private String remixGrandparentsUrlString = "";
	@XStreamAlias("url")
	private String remixParentsUrlString = "";
	@SuppressWarnings("unused")
	private String userHandle = "";
	@SuppressWarnings("unused")
	private String dateTimeUpload = "";
	@SuppressWarnings("unused")
	private String mediaLicense = "";
	@SuppressWarnings("unused")
	private String programLicense = "";
	//----------------------------------------------------------------------------------------------

	public XmlHeader() {
	}

	public int getVirtualScreenHeight() {
		if(customResolution) {
			WindowManager windowManager = (WindowManager) CatroidApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics displayMetrics = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(displayMetrics);

			int width = displayMetrics.widthPixels;
			int height = displayMetrics.heightPixels;
			Log.d("CustomRes", "Y: " + String.valueOf(height));
			virtualScreenHeight = height;
			return height;
		}
		Log.d("CustomRes", "def: Y: " + String.valueOf(virtualScreenHeight));
		return virtualScreenHeight;
	}

	public int getVirtualScreenWidth() {
		if(customResolution) {
			WindowManager windowManager = (WindowManager) CatroidApplication.getAppContext().getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics displayMetrics = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(displayMetrics);

			int width = displayMetrics.widthPixels;
			int height = displayMetrics.heightPixels;
			Log.d("CustomRes", "X: " + String.valueOf(width));
			virtualScreenWidth = width;
			return width;
		}
		Log.d("CustomRes", "def: X: " + String.valueOf(virtualScreenWidth));
		return virtualScreenWidth;
	}

	public void setVirtualScreenHeight(int height) {
		virtualScreenHeight = height;
	}

	public void setVirtualScreenWidth(int width) {
		virtualScreenWidth = width;
	}

	public String getProjectName() {
		return programName;
	}

	public void setProjectName(String programName) {
		this.programName = programName;
	}

	public String getDescription() {
		return description;
	}

	public String getNotesAndCredits() {
		return notesAndCredits;
	}

	public String getUserHandle() {
		return userHandle;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setNotesAndCredits(String notesAndCredits) {
		this.notesAndCredits = notesAndCredits;
	}

	public double getCatrobatLanguageVersion() {
		return catrobatLanguageVersion;
	}

	public void setCatrobatLanguageVersion(double catrobatLanguageVersion) {
		this.catrobatLanguageVersion = catrobatLanguageVersion;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getApplicationBuildName() {
		return applicationBuildName;
	}

	public void setApplicationBuildName(String applicationBuildName) {
		this.applicationBuildName = applicationBuildName;
	}

	public int getApplicationBuildNumber() {
		return applicationBuildNumber;
	}

	public void setApplicationBuildNumber(int applicationBuildNumber) {
		this.applicationBuildNumber = applicationBuildNumber;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setPhysicsWidthArea(float value) {
		physicsWidthArea = value;
	}

	public void setPhysicsHeightArea(float value) {
		physicsHeightArea = value;
	}

	public float getPhysicsWidthArea() {
		return physicsWidthArea;
	}

	public float getPhysicsHeightArea() {
		return physicsHeightArea;
	}

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public String getPlatformVersion() {
		return platformVersion;
	}

	public void setPlatformVersion(String platformVersion) {
		this.platformVersion = platformVersion;
	}

	public void setScreenMode(ScreenModes screenMode) {
		this.screenMode = screenMode;
	}

	public ScreenModes getScreenMode() {
		return this.screenMode;
	}

	public void setCustomResolution(Boolean screenMode) {
		this.customResolution = screenMode;
	}

	public Boolean getCustomResolution() {
		return this.customResolution;
	}

	public boolean islandscapeMode() {
		return landscapeMode;
	}

	public void setlandscapeMode(boolean landscapeMode) {
		this.landscapeMode = landscapeMode;
	}

	public void setIsCastProject(boolean isCastProject) {
		this.isCastProject = isCastProject;
	}

	public boolean isCastProject() {
		return isCastProject;
	}

	public List<String> getTags() {
		return Arrays.asList(this.tags.split(","));
	}

	public void setTags(List<String> tags) {
		this.tags = TextUtils.join(",", tags);
	}

	public String getRemixParentsUrlString() {
		return this.remixParentsUrlString;
	}

	public void setRemixParentsUrlString(String remixParentsUrlString) {
		this.remixParentsUrlString = remixParentsUrlString;
	}

	public String getApplicationBuildType() {
		return applicationBuildType;
	}

	public void setApplicationBuildType(String applicationBuildType) {
		this.applicationBuildType = applicationBuildType;
	}

	public String getListeningLanguageTag() {
		return listeningLanguageTag;
	}

	public void setListeningLanguageTag(String listeningLanguageTag) {
		this.listeningLanguageTag = listeningLanguageTag;
	}
}
