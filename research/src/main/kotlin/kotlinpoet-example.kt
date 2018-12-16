import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File

fun main(args: Array<String>) {
    val components = listOf(
        ComponentDesc(
            ClassName.bestGuess("android.widget.TextView"),
            ClassName.bestGuess("android.view.View"),
            listOf(
                PropertyDescription("setFile", File::class.asTypeName()),
                PropertyDescription("setText", String::class.asTypeName()),
                PropertyDescription("setTextSize", Float::class.asTypeName())
            ),
            false,
            false
        ),
        ComponentDesc(
            ClassName.bestGuess("android.widget.LinearLayout"),
            ClassName.bestGuess("android.view.ViewGroup"),
            listOf(
                PropertyDescription("setFile", File::class.asTypeName()),
                PropertyDescription("setText", String::class.asTypeName()),
                PropertyDescription("setTextSize", Float::class.asTypeName())
            ),
            true,
            false
        )
    )

    printComponents(components)
}

fun printComponents(components: List<ComponentDesc>) {
    val fileSpecBuild = FileSpec
        .builder(libraryPackage, "dsl")

    components.forEach {
        fileSpecBuild.addFunction(createTypeDsl(it.type, it.group))
        fileSpecBuild.addType(createType(it))
    }

    println(fileSpecBuild.build())
}

data class ComponentDesc(
    val type: ClassName,
    val parentType: ClassName?,
    val properties: List<PropertyDescription>,
    val group: Boolean,
    val isAbstract: Boolean
)

data class PropertyDescription(val methodName: String, val type: TypeName)

fun createType(comp: ComponentDesc): TypeSpec {
    val clazz = comp.type.simpleName
    val className = ClassName.bestGuess("${clazz}_")
    val viewBuilder = TypeSpec
        .classBuilder(className)
        .addModifiers(KModifier.OPEN)

    if (comp.type.canonicalName == "android.view.View") {
        viewBuilder
            .superclass(ClassName.bestGuess("VirtualNode"))
    } else if (comp.parentType != null && comp.parentType.canonicalName !in listOf("java.lang.Object")) {
        val sc = comp.parentType.simpleName + "_"

        viewBuilder
            .superclass(ClassName.bestGuess(sc))
    }

    comp.properties
        .map { mkCompProps(comp.type, it.methodName, it.type) }
        .forEach { viewBuilder.addProperties(it) }

    mkCreateEmpty(comp.type, comp.isAbstract)?.let { viewBuilder.addFunction(it) }

    return viewBuilder.build()
}

private fun mkCreateEmpty(clazz: ClassName, isAbstract: Boolean): FunSpec? =
    if (isAbstract) null
    else FunSpec.builder("createEmpty")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("context", ClassName.bestGuess("android.content.Context"))
        .addCode("return %T(context)", clazz)
        .build()

private fun mkCompProps(inputViewClass: TypeName, methodName: String, methodType: TypeName): List<PropertySpec> {
    val name = toPropertyName(methodName)
    val privateProp = "_$name"
    val fixedType = fixPropertyType(methodType)

    val p1 = PropertySpec
        .builder(name, fixedType)
        .mutable(true)
        .getter(
            FunSpec.getterBuilder()
                .addCode("throw IllegalStateException()")
                .build()
        )
        .setter(
            FunSpec.setterBuilder()
                .addParameter("value", fixedType)
                .addCode(
                    """
                        $privateProp.set(value)
                        props += $privateProp
                        """.trimIndent()
                )
                .build()
        )
        .build()

    val propertyType = ClassName.bestGuess("Property")
    val defValue = getDefaultValue(fixedType)

    val p2 = PropertySpec
        .builder(
            privateProp,
            propertyType.parameterizedBy(fixedType.copy(defValue == null), inputViewClass),
            KModifier.PRIVATE
        )
        .initializer("%T(%L, %T::%L)", propertyType, defValue, inputViewClass, methodName)
        .build()

    return listOf(p1, p2)
}

private fun fixPropertyType(methodType: TypeName): TypeName = when (methodType) {
    ClassName.bestGuess("java.lang.CharSequence") -> ClassName.bestGuess("kotlin.CharSequence")
    ClassName.bestGuess("java.lang.String") -> ClassName.bestGuess("kotlin.String")
    ClassName.bestGuess("java.lang.Object") -> ClassName.bestGuess("kotlin.Any")
    else -> {
        if (methodType is ParameterizedTypeName)
            methodType.rawType.parameterizedBy(
                *methodType.typeArguments.map { fixPropertyType(it) }.toTypedArray()
            )
        else methodType
    }
}

private fun toPropertyName(methodName: String): String =
    methodName[3].toLowerCase() + methodName.substring(4)

fun getDefaultValue(type: TypeName): String? =
    when (type) {
        Float::class.asTypeName() -> "0.0f"
        Int::class.asTypeName() -> "0"
        Long::class.asTypeName() -> "0L"
        Boolean::class.asTypeName() -> "false"
        else -> null
    }
