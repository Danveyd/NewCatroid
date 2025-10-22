package org.catrobat.catroid.lunoscript.processor

import com.danvexteam.lunoscript_annotations.LunoClass
import com.danvexteam.lunoscript_annotations.LunoFunction
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import java.io.File
import java.lang.reflect.Modifier
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import javax.lang.model.element.Element
import javax.tools.FileObject

@AutoService(Processor::class)
class LunoScriptProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(
            LunoFunction::class.java.name,
            LunoClass::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun getSourceFile(element: Element): FileObject? {
        var e = element
        while (e.enclosingElement?.kind != ElementKind.PACKAGE) {
            e = e.enclosingElement ?: return null
        }
        val packageElement = processingEnv.elementUtils.getPackageOf(e)
        val simpleName = e.simpleName.toString()
        return try {
            processingEnv.filer.getResource(StandardLocation.SOURCE_PATH, packageElement.qualifiedName.toString(), "$simpleName.kt")
        } catch (e: Exception) {
            null
        }
    }

    private fun getKotlinType(typeName: TypeName): TypeName {
        if (typeName !is ClassName) return typeName
        return when (typeName.canonicalName) {
            "java.lang.String" -> ClassName("kotlin", "String")
            "java.lang.Double" -> ClassName("kotlin", "Double")
            "java.lang.Boolean" -> ClassName("kotlin", "Boolean")
            "java.lang.Integer" -> ClassName("kotlin", "Int")
            "java.lang.Float" -> ClassName("kotlin", "Float")
            "java.lang.Object" -> ClassName("kotlin", "Any")
            "java.io.File" -> ClassName("java.io", "File")
            "org.catrobat.catroid.utils.lunoscript.LunoValue.Float" -> ClassName("kotlin", "Float")
            else -> typeName
        }
    }


    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val generatedClassName = "GeneratedLunoRegistry"
        val packageName = "org.catrobat.catroid.utils.lunoscript.generated"

        val interpreterClass = ClassName("org.catrobat.catroid.utils.lunoscript", "Interpreter")
        val lunoValueClass = ClassName("org.catrobat.catroid.utils.lunoscript", "LunoValue")
        val asSpecificKotlinTypeFun = MemberName("org.catrobat.catroid.utils.lunoscript", "asSpecificKotlinType", true)
        val callableNativeFuncClass = ClassName("org.catrobat.catroid.utils.lunoscript", "CallableNativeLunoFunction")
        val lunoRuntimeErrorClass = ClassName("org.catrobat.catroid.utils.lunoscript", "LunoRuntimeError")

        val registerFunctionBuilder = FunSpec.builder("registerAllNatives")
            .addParameter("interpreter", interpreterClass)

        val annotatedFunctions = roundEnv.getElementsAnnotatedWith(LunoFunction::class.java)
            .filter { it.enclosingElement.kind != ElementKind.CLASS }

        for (funcElement in annotatedFunctions) {
            if (funcElement !is ExecutableElement) continue

            val enclosingElement = funcElement.enclosingElement
            if (enclosingElement !is TypeElement) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't process function not in class or top-level file.", funcElement)
                continue
            }

            val containerClassName = enclosingElement.asClassName()
            val functionName = funcElement.simpleName.toString()
            val kotlinFuncReference = MemberName(containerClassName, functionName)

            val lunoFuncName = funcElement.getAnnotation(LunoFunction::class.java).name.ifEmpty { functionName }
            val arity = funcElement.parameters.size

            registerFunctionBuilder.beginControlFlow("interpreter.defineNative(%S, %L..%L) { _, arguments ->", lunoFuncName, arity, arity)

            registerFunctionBuilder.addStatement("val funcNameForError = %S", lunoFuncName)
            val argNames = funcElement.parameters.mapIndexed { index, param ->
                val argName = "arg$index"

                val javaType = param.asType().asTypeName().copy(nullable = false)
                val paramType = getKotlinType(javaType)

                registerFunctionBuilder.addStatement(
                    "val %L = arguments[%L].%M<%T>(funcNameForError, %L)",
                    argName, index, asSpecificKotlinTypeFun, paramType, index + 1
                )
                argName
            }
            val joinedArgs = argNames.joinToString(", ")

            if (funcElement.returnType.kind != TypeKind.VOID) {
                registerFunctionBuilder.addStatement("val result = %M(%L)", kotlinFuncReference, joinedArgs)
                registerFunctionBuilder.addStatement("%T.fromKotlin(result)", lunoValueClass)
            } else {
                registerFunctionBuilder.addStatement("%M(%L)", kotlinFuncReference, joinedArgs)
                registerFunctionBuilder.addStatement("%T.Null", lunoValueClass)
            }
            registerFunctionBuilder.endControlFlow()
        }

        val annotatedClasses = roundEnv.getElementsAnnotatedWith(LunoClass::class.java)
        for (classElement in annotatedClasses) {
            if (classElement !is TypeElement) continue

            val classTypeName = classElement.asClassName()
            val lunoClassName = classElement.getAnnotation(LunoClass::class.java).name.ifEmpty { classElement.simpleName.toString() }

            val constructors = classElement.enclosedElements.filter {
                it.kind == ElementKind.CONSTRUCTOR
            }.map { it as ExecutableElement }

            if (constructors.isEmpty() && classElement.kind != ElementKind.INTERFACE) {
                continue
            }

            registerFunctionBuilder.beginControlFlow(
                "interpreter.defineNative(%S, %T.NativeCallable(%T(%S, %L..%L) { _, args ->",
                lunoClassName,
                lunoValueClass,
                callableNativeFuncClass,
                lunoClassName,
                constructors.minOfOrNull { it.parameters.size } ?: 0,
                constructors.maxOfOrNull { it.parameters.size } ?: 0
            )

            registerFunctionBuilder.beginControlFlow("when (args.size)")

            for (ctor in constructors) {
                val paramsCount = ctor.parameters.size
                registerFunctionBuilder.beginControlFlow("%L ->", paramsCount)

                val argNames = ctor.parameters.mapIndexed { index, param ->
                    val argName = "arg$index"
                    val paramType = getKotlinType(param.asType().asTypeName().copy(nullable = false))
                    registerFunctionBuilder.addStatement(
                        "val %L = args[%L].%M<%T>(%S, %L)",
                        argName,                            // %L -> val arg0
                        index,                              // %L -> args[0]
                        asSpecificKotlinTypeFun,            // %M -> .asSpecificKotlinType
                        paramType,                          // %T -> <String>
                        "$lunoClassName constructor",       // %S -> ("LookData constructor",
                        index + 1                           // %L -> 1)
                    )
                    argName
                }
                val joinedArgs = argNames.joinToString(", ")

                registerFunctionBuilder.addStatement("%T.NativeObject(%T(%L))", lunoValueClass, classTypeName, joinedArgs)
                registerFunctionBuilder.endControlFlow()
            }

            registerFunctionBuilder.addStatement("else -> throw %T(%S, -1)", lunoRuntimeErrorClass, "Invalid argument count for $lunoClassName constructor")
            registerFunctionBuilder.endControlFlow() // when
            registerFunctionBuilder.endControlFlow() // }))
        }

        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addFunction(registerFunctionBuilder.build())
            .build()
        val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"] ?: run {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "kapt.kotlin.generated option not found")
            return true
        }
        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
        return true
    }
}