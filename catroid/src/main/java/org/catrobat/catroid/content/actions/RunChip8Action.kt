package org.catrobat.catroid.content.actions

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.ui.Image
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import kotlin.random.Random

class RunChip8Action : Action() {
    var scope: Scope? = null
    var filenameFormula: Formula? = null

    private var screenImage: Image? = null
    private var pixmap: Pixmap? = null
    private var texture: Texture? = null

    private val memory = ByteArray(4096)
    private val v = IntArray(16)
    private var iReg = 0
    private var pc = 0x200
    private val stack = IntArray(16)
    private var sp = 0
    private var delayTimer = 0
    private val gfx = BooleanArray(64 * 32)
    private var drawFlag = false

    private var initialized = false
    private var timeAccumulator = 0f

    private val fontset = intArrayOf(
        0xF0, 0x90, 0x90, 0x90, 0xF0, 0x20, 0x60, 0x20, 0x20, 0x70,
        0xF0, 0x10, 0xF0, 0x80, 0xF0, 0xF0, 0x10, 0xF0, 0x10, 0xF0,
        0x90, 0x90, 0xF0, 0x10, 0x10, 0xF0, 0x80, 0xF0, 0x10, 0xF0,
        0xF0, 0x80, 0xF0, 0x90, 0xF0, 0xF0, 0x10, 0x20, 0x40, 0x40,
        0xF0, 0x90, 0xF0, 0x90, 0xF0, 0xF0, 0x90, 0xF0, 0x10, 0xF0,
        0xF0, 0x90, 0xF0, 0x90, 0x90, 0xE0, 0x90, 0xE0, 0x90, 0xE0,
        0xF0, 0x80, 0x80, 0x80, 0xF0, 0xE0, 0x90, 0x90, 0x90, 0xE0,
        0xF0, 0x80, 0xF0, 0x80, 0xF0, 0xF0, 0x80, 0xF0, 0x80, 0x80
    )


    private fun getVarValue(name: String, defaultValue: Float): Float {
        val variable = scope?.project?.getUserVariable(name)
        return variable?.value?.toString()?.toFloatOrNull() ?: defaultValue
    }


    private fun ensureVariable(name: String, initialValue: Any) {
        if (scope?.project?.getUserVariable(name) == null) {
            scope?.project?.userVariables?.add(UserVariable(name, initialValue))
        }
    }

    private fun initEmulator() {
        val filename = filenameFormula?.interpretString(scope) ?: return
        val file = scope?.project?.getFile(filename)

        if (file == null || !file.exists()) {
            setConsole(CatroidApplication.getAppContext().getString(R.string.chip8_file_not_found, filename))
            return
        }


        ensureVariable("chip8_key", -1)
        ensureVariable("chip8_console", CatroidApplication.getAppContext().getString(R.string.chip8_loading))
        ensureVariable("chip8_speed", 10)
        ensureVariable("chip8_x", -320)
        ensureVariable("chip8_y", -160)
        ensureVariable("chip8_width", 640)
        ensureVariable("chip8_height", 320)

        for (i in fontset.indices) memory[0x50 + i] = fontset[i].toByte()

        val romBytes = file.readBytes()
        System.arraycopy(romBytes, 0, memory, 0x200, romBytes.size)

        pixmap = Pixmap(64, 32, Pixmap.Format.RGBA8888)
        pixmap?.setColor(0f, 0f, 0f, 1f)
        pixmap?.fill()

        texture = Texture(pixmap)
        screenImage = Image(texture)

        val stageListener = StageActivity.activeStageActivity.get()?.stageListener
        stageListener?.stage?.addActor(screenImage)

        setConsole(CatroidApplication.getAppContext().getString(R.string.chip8_loaded))
        initialized = true
    }

    override fun act(delta: Float): Boolean {
        if (!initialized) initEmulator()
        if (!initialized) return true


        val speed = getVarValue("chip8_speed", 10f).toInt().coerceIn(1, 100)
        val posX = getVarValue("chip8_x", -320f)
        val posY = getVarValue("chip8_y", -160f)
        val width = getVarValue("chip8_width", 640f)
        val height = getVarValue("chip8_height", 320f)


        screenImage?.setPosition(posX, posY)
        screenImage?.setSize(width, height)


        for (k in 0 until speed) {
            emulateCycle()
        }

        timeAccumulator += delta
        if (timeAccumulator >= 1f / 60f) {
            if (delayTimer > 0) delayTimer--
            timeAccumulator -= (1f / 60f)
        }

        if (drawFlag) {
            pixmap?.let { pm ->
                pm.setColor(0f, 0f, 0f, 1f)
                pm.fill()
                pm.setColor(1f, 1f, 1f, 1f)

                for (y in 0 until 32) {
                    for (x in 0 until 64) {
                        if (gfx[x + (y * 64)]) pm.drawPixel(x, y)
                    }
                }
                texture?.draw(pm, 0, 0)
            }
            drawFlag = false
        }

        return false
    }

    private fun emulateCycle() {
        val opcode = (memory[pc].toInt() and 0xFF).shl(8) or (memory[pc + 1].toInt() and 0xFF)
        val x = (opcode and 0x0F00).shr(8)
        val y = (opcode and 0x00F0).shr(4)
        val nnn = opcode and 0x0FFF
        val nn = opcode and 0x00FF
        val n = opcode and 0x000F
        pc += 2


        val pressedKey = getVarValue("chip8_key", -1f).toInt()

        when (opcode and 0xF000) {
            0x0000 -> when (opcode) {
                0x00E0 -> { gfx.fill(false); drawFlag = true }
                0x00EE -> pc = stack[--sp]
            }
            0x1000 -> pc = nnn
            0x2000 -> { stack[sp++] = pc; pc = nnn }
            0x3000 -> if (v[x] == nn) pc += 2
            0x4000 -> if (v[x] != nn) pc += 2
            0x5000 -> if (v[x] == v[y]) pc += 2
            0x6000 -> v[x] = nn
            0x7000 -> v[x] = (v[x] + nn) and 0xFF
            0x8000 -> when (n) {
                0 -> v[x] = v[y]
                1 -> v[x] = v[x] or v[y]
                2 -> v[x] = v[x] and v[y]
                3 -> v[x] = v[x] xor v[y]
                4 -> {
                    val sum = v[x] + v[y]
                    v[0xF] = if (sum > 0xFF) 1 else 0
                    v[x] = sum and 0xFF
                }
                5 -> {
                    v[0xF] = if (v[x] > v[y]) 1 else 0
                    v[x] = (v[x] - v[y]) and 0xFF
                }
                6 -> { v[0xF] = v[x] and 0x1; v[x] = v[x].shr(1) }
                7 -> {
                    v[0xF] = if (v[y] > v[x]) 1 else 0
                    v[x] = (v[y] - v[x]) and 0xFF
                }
                0xE -> { v[0xF] = v[x].shr(7); v[x] = (v[x].shl(1)) and 0xFF }
            }
            0x9000 -> if (v[x] != v[y]) pc += 2
            0xA000 -> iReg = nnn
            0xB000 -> pc = nnn + v[0]
            0xC000 -> v[x] = Random.nextInt(256) and nn
            0xD000 -> {
                val px = v[x] % 64
                val py = v[y] % 32
                v[0xF] = 0
                for (row in 0 until n) {
                    val spriteByte = memory[iReg + row].toInt()
                    for (col in 0 until 8) {
                        if ((spriteByte and (0x80.shr(col))) != 0) {
                            val idx = ((px + col) % 64) + (((py + row) % 32) * 64)
                            if (gfx[idx]) v[0xF] = 1
                            gfx[idx] = !gfx[idx]
                        }
                    }
                }
                drawFlag = true
            }
            0xE000 -> when (nn) {
                0x9E -> if (pressedKey == v[x]) pc += 2
                0xA1 -> if (pressedKey != v[x]) pc += 2
            }
            0xF000 -> when (nn) {
                0x07 -> v[x] = delayTimer
                0x0A -> {
                    if (pressedKey in 0..15) v[x] = pressedKey
                    else pc -= 2
                }
                0x15 -> delayTimer = v[x]
                0x18 -> {}
                0x1E -> iReg += v[x]
                0x29 -> iReg = (v[x] * 5) + 0x50
                0x33 -> {
                    memory[iReg] = (v[x] / 100).toByte()
                    memory[iReg + 1] = ((v[x] / 10) % 10).toByte()
                    memory[iReg + 2] = ((v[x] % 100) % 10).toByte()
                }
                0x55 -> for (i in 0..x) memory[iReg + i] = v[i].toByte()
                0x65 -> for (i in 0..x) v[i] = memory[iReg + i].toInt() and 0xFF
            }
        }
    }

    private fun setConsole(msg: String) {
        val variableList = scope?.project
        val existingVar = variableList?.getUserVariable("chip8_console")

        if (existingVar != null) {
            existingVar.value = msg
        } else {
            variableList?.userVariables?.add(UserVariable("chip8_console", msg))
        }
    }


    override fun reset() {
        super.reset()
        texture?.dispose()
        pixmap?.dispose()
        screenImage?.remove()
    }
}