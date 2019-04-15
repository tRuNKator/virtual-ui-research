import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import common.isOverrided
import common.loadAllClassesFromJar
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URLClassLoader

fun main(args: Array<String>) {
    val jars = args.map { File(it).toURI().toURL() }.toTypedArray()

    val loader = URLClassLoader(jars, ClassLoader.getSystemClassLoader())
    val libClasses = loadAllClassesFromJar(File(args.first()), loader)

    val androidClasses = loadAllClassesFromJar(File(args.last()), loader)

    val viewClass = androidClasses.first { it.name == "android.view.View" }
    val groupClass = androidClasses.first { it.name == "android.view.ViewGroup" }

    val fileSpec = FileSpec.builder(libraryPackage, "dsl")

    (libClasses + androidClasses)
        .filter {
            it.name !in listOf(
                "android.webkit.WebView",
                "android.widget.AutoCompleteTextView",
                "android.widget.MultiAutoCompleteTextView",
                "android.widget.TimePicker",
                "android.widget.AnalogClock",
                "android.inputmethodservice.ExtractEditText",
                "android.inputmethodservice.KeyboardView",
                "android.appwidget.AppWidgetHostView",
                "android.opengl.GLSurfaceView",
                // Replaced by AppCompat
                "android.widget.SearchView",
                "android.widget.Toolbar",

                // Restrict
                "androidx.appcompat.widget.ActionBarContextView",
                "androidx.appcompat.widget.ActionBarContainer",
                "androidx.appcompat.widget.ActionBarOverlayLayout",
                "androidx.appcompat.widget.ActivityChooserView",
                "androidx.appcompat.widget.ButtonBarLayout",
                "androidx.appcompat.widget.ContentFrameLayout",
                "androidx.appcompat.widget.ScrollingTabContainerView",
                "androidx.appcompat.widget.AlertDialogLayout",
                "androidx.appcompat.widget.DialogTitle",
                "androidx.appcompat.widget.FitWindowsFrameLayout",
                "androidx.appcompat.widget.FitWindowsLinearLayout",
                //
                "androidx.appcompat.widget.AppCompatAutoCompleteTextView",
                "androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView",
                "androidx.appcompat.widget.ActionMenuView"
            )
        }
        .filter { it.name.startsWith("androidx.appcompat.widget.") }
        .filter {
            viewClass.isAssignableFrom(it) &&
                !it.isAnnotationPresent(java.lang.Deprecated::class.java) &&
                !it.isMemberClass
        }
        .map { mkComponent(it, groupClass) }
        .forEach {
            fileSpec.addFunction(createTypeDsl(it.type, it.group))
            fileSpec.addType(createType(it))
        }

    println(fileSpec.build())
}

private fun mkComponent(clazz: Class<*>, groupClass: Class<*>): ComponentDesc {
    val typeBound = clazz.typeParameters.firstOrNull()?.bounds?.firstOrNull()?.asTypeName()
    val type =
        if (typeBound != null) {
            clazz.asClassName().parameterizedBy(typeBound)
        } else {
            clazz.asClassName()
        }

    return ComponentDesc(
        type,
        clazz.superclass.asClassName(),
        clazz
            .declaredMethods
            .filter {
                it.name.matches(Regex("set[A-Z].+"))
                    && it.parameterCount == 1
                    && Modifier.isPublic(it.modifiers)
                    && !Modifier.isStatic(it.modifiers)
                    && !it.isAnnotationPresent(java.lang.Deprecated::class.java)
                    && !it.isOverrided()
                    && !isBlockedMethod(it)
            }
            .map {
                PropertyDescription(
                    it.name,
                    it.parameters[0].type.asTypeName()
                )
            },
        groupClass.isAssignableFrom(clazz),
        Modifier.isAbstract(clazz.modifiers),
        typeBound
    )
}

fun isBlockedMethod(method: Method): Boolean {
    val name = method.name
    val parameter = method.parameters[0].type.name
    if (name == "setHintTextColor" && parameter == "android.content.res.ColorStateList") return true
    if (name == "setLinkTextColor" && parameter == "android.content.res.ColorStateList") return true
    if (name == "setTextColor" && parameter == "android.content.res.ColorStateList") return true
    if (name == "setButtonDrawable" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setCheckMarkDrawable" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setLeftStripDrawable" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setLogo" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setNavigationIcon" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setRightStripDrawable" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setSelectedDateVerticalBar" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setSelector" && parameter == "android.graphics.drawable.Drawable") return true
    if (name == "setColorFilter" && parameter == "android.graphics.ColorFilter") return true
    if (name == "setHint" && parameter == "int") return true
    if (name == "setLogoDescription" && parameter == "int") return true
    if (name == "setNavigationContentDescription" && parameter == "int") return true
    if (name == "setSubtitle" && parameter == "int") return true
    if (name == "setText" && parameter == "int") return true
    if (name == "setTitle" && parameter == "int") return true
    return false
}
