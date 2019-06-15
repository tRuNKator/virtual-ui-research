# Библиотека

[![](https://jitpack.io/v/y2k/virtual-ui-lib.svg)](https://jitpack.io/#y2k/virtual-ui-lib)

# Пример компонента

#### Описание компонента в стиле _The Elm Architecture_

[todolist-example.kt](android/src/main/java/y2k/android/todolist%20example.kt)

```kotlin
object TodoListComponent : TeaComponent<Msg, Model> {

    data class Model(val todos: List<String>, val text: String)

    sealed class Msg {
        object Add : Msg()
        object DeleteAll : Msg()
        class Delete(val title: String) : Msg()
        class Changed(val text: CharSequence) : Msg()
    }

    override val init = Model(emptyList(), "")

    override fun update(model: Model, msg: Msg): Model =
        when (msg) {
            is Msg.Add -> model.copy(todos = model.todos + model.text, text = "")
            is Msg.DeleteAll -> model.copy(todos = emptyList())
            is Msg.Delete -> model.copy(todos = model.todos.filterNot { it == msg.title })
            is Msg.Changed -> model.copy(text = "" + msg.text)
        }

    override fun view(model: Model, dispatch: (Msg) -> Unit): VirtualNode =
        linearLayout {
            orientation = LinearLayout.VERTICAL
            padding = Quadruple(20, 20, 20, 20)

            editableView {
                onTextChanged = { dispatch(Msg.Changed(it)) }
                text = model.text

                appCompatEditText {
                    hintCharSequence = "Enter text..."
                }
            }

            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END

                appCompatButton {
                    enabled = model.text.isNotBlank()
                    textCharSequence = "Add item"
                    onClickListener = OnClickListener { dispatch(Msg.Add) }
                }
                appCompatButton {
                    enabled = model.todos.isNotEmpty()
                    textCharSequence = "Clear all"
                    onClickListener = OnClickListener { dispatch(Msg.DeleteAll) }
                }
            }

            scrollView {
                linearLayout {
                    orientation = LinearLayout.VERTICAL

                    model.todos.forEach { item ->
                        viewItem(item, dispatch)
                    }
                }
            }
        }

    private fun viewItem(title: String, dispatch: (Msg) -> Unit): VirtualNode =
        linearLayout {
            orientation = LinearLayout.HORIZONTAL

            fillHorizontal {
                appCompatTextView {
                    textCharSequence = title
                }
            }
            appCompatButton {
                textCharSequence = "Delete"
                onClickListener = OnClickListener { dispatch(Msg.Delete(title)) }
            }
        }
}
```

#### Активити для запуска

```kotlin
class TodoListActivity : AppCompatActivity() {

    private lateinit var root: VirtualHostView
    private lateinit var server: Closeable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        root = VirtualHostView(this).also(::setContentView)

        SimpleTea.run(root, TodoListComponent)
    }

    override fun onResume() {
        super.onResume()
        server = HotReloadServer.start(root)
    }

    override fun onPause() {
        super.onPause()
        server.close()
    }
}
```

#### JUnit тест для запуска hot-reload

```kotlin
class HotReloadRunners {

    @Test
    fun `run TodoList`() {
        HotReloadClient.send {
            TodoListComponent.view(
                TodoListComponent.init.copy(todos = List(5) { "Item #$it" })) {}
        }
    }
}
```
