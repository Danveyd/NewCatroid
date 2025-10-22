import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.bricks.FormulaBrick

class LibaryBricks {
    companion object {
        private val actions: MutableList<TemporalAction> = mutableListOf()
        private val bricks: MutableList<FormulaBrick> = mutableListOf()

        fun addAction(action: TemporalAction) {
            actions.add(action)
        }

        fun addBrick(brick: FormulaBrick) {
            bricks.add(brick)
        }

        fun getActions(): List<TemporalAction> {
            return actions
        }

        fun getBricks(): List<FormulaBrick> {
            return bricks
        }

        fun loadActionsFromFile(filePath: String) {

        }

        fun loadBricksFromFile(filePath: String) {

        }
    }
}
