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

package org.catrobat.catroid.test.ui

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertNull
import org.catrobat.catroid.formulaeditor.FormulaEditorEditText
import org.catrobat.catroid.formulaeditor.InternToken
import org.catrobat.catroid.formulaeditor.InternTokenType
import org.catrobat.catroid.ui.FormulaEditorClipboard
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class FormulaEditorClipboardTest {

    @Mock
    lateinit var formulaEditorEditText: FormulaEditorEditText

    @InjectMocks
    lateinit var controller: FormulaEditorClipboard

    @Captor
    lateinit var captor: ArgumentCaptor<List<InternToken>>

    @Test
    fun shouldCopy() {

    }

    @Test
    fun shouldCopyNothing() {

    }

    @Test
    fun shouldCopyWholeContent() {

    }

    @Test
    fun shouldCopySelectedPart() {

    }

    @Test
    fun shouldPaste() {

    }

    @Test
    fun shouldNotPasteNothing() {

    }

    private fun assertSameValuesButDifferentObjects(
        expected: List<InternToken>,
        result: List<InternToken>
    ) {
        assertEquals(expected.size, result.size)
        for (i in expected.indices) {
            assertEquals(expected[i].internTokenType, result[i].internTokenType)
            assertEquals(expected[i].tokenStringValue, result[i].tokenStringValue)
            assertNotSame(expected[i], result[i])
        }
    }
}
