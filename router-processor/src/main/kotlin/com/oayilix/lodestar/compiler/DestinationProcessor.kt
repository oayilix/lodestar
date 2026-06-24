package com.oayilix.lodestar.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong

class DestinationProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(DESTINATION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        if (symbols.isEmpty()) return emptyList()

        logger.info("process called, found ${symbols.size} @Destination elements")

        val mappings = symbols.mapNotNull { clazz ->
            val annotation = clazz.annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == DESTINATION_ANNOTATION
            } ?: return@mapNotNull null

            val url = annotation.arguments
                .firstOrNull { it.name?.asString() == "url" }
                ?.value as? String ?: return@mapNotNull null

            val description = annotation.arguments
                .firstOrNull { it.name?.asString() == "description" }
                ?.value as? String ?: "no description"

            val className = clazz.qualifiedName?.asString() ?: return@mapNotNull null

            logger.info("url = $url, description = $description, realClassName = $className")
            url to className
        }

        if (mappings.isEmpty()) return emptyList()

        val generatedClassName = "RouterMapping_${classNameCounter.incrementAndGet()}"
        logger.info("generate class $generatedClassName")

        val fileContent = buildString {
            appendLine("package com.oayilix.lodestar.mapping;")
            appendLine()
            appendLine("import java.util.HashMap;")
            appendLine("import java.util.Map;")
            appendLine()
            appendLine("public class $generatedClassName {")
            appendLine("    public static Map<String, String> get() {")
            appendLine("        Map<String, String> mapping = new HashMap<>();")
            mappings.forEach { (url, className) ->
                appendLine("        mapping.put(\"$url\", \"$className\");")
            }
            appendLine("        return mapping;")
            appendLine("    }")
            appendLine("}")
        }

        codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            "com.oayilix.lodestar.mapping",
            generatedClassName,
            "java"
        ).use { output: OutputStream ->
            output.write(fileContent.toByteArray(Charsets.UTF_8))
        }

        logger.info("write kotlin file to filer, success")
        return emptyList()
    }

    companion object {
        private const val DESTINATION_ANNOTATION = "com.oayilix.lodestar.annotations.Destination"
        private val classNameCounter = AtomicLong(System.nanoTime())
    }
}
