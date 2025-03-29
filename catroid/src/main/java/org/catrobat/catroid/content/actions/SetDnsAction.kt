/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

package org.catrobat.catroid.content.actions

import CustomDns
import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import com.google.gson.Gson
import okhttp3.Call
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Callback
import org.catrobat.catroid.content.GeminiManager
import java.io.IOException
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class SetDnsAction() : TemporalAction() {
    var scope: Scope? = null
    var dns: Formula? = null

    override fun update(percent: Float) {
        GeminiManager.dns_server = dns?.interpretString(scope) ?: ""
    }
}