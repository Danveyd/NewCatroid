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
            // Логика загрузки действий из файла
            // Например, можно прочитать файл, создать объекты TemporalAction и добавить их в список
        }

        fun loadBricksFromFile(filePath: String) {
            // Логика загрузки кирпичей из файла
            // Точно так же как и с действиями
        }
    }
}
