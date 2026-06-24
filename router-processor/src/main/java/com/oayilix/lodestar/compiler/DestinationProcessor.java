package com.oayilix.lodestar.compiler;

import com.oayilix.lodestar.annotations.Destination;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@AutoService(Processor.class)
public class DestinationProcessor extends AbstractProcessor {

    private static final String TAG = "DestinationProcessor";

    /**
     * 告诉编译器当前注解处理器支持处理哪些注解
     * 在这里返回之后，Javac 就会帮我们收集对应的注解，传给 DestinationProcessor
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(
                Destination.class.getCanonicalName()
        );
    }

    /**
     * 编译器帮我们收集到我们需要的注解后，会回调的方法
     * @param set 编译器帮我们收集到的注解信息
     * @param roundEnvironment 当前的编译环境
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // 避免多次调用 process
        if (roundEnvironment.processingOver()) {
            return false;
        }

        print("process called");
        // 获取所有标记了 @Destination 注解的类的信息
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Destination.class);
        print("all Destination elements size = " + elements.size());
        // 当未搜集到 @Destination 注解标注的类的信息时，跳过
        if (elements.isEmpty()) {
            print("process finish");
            return false;
        }

        parseRoutes(elements);

        print("process finish");

        return false;
    }

    private void parseRoutes(Set<? extends Element> elements) {

        print("generate method get()");
        ClassName hashMap = ClassName.get("java.util", "HashMap");
        ClassName map = ClassName.get("java.util", "Map");
        ClassName string = ClassName.get("java.lang", "String");
        ParameterizedTypeName mapOfStringString = ParameterizedTypeName.get(map, string, string);

        MethodSpec.Builder builder = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(mapOfStringString)
                .addStatement("$T mapping = new $T<>()", mapOfStringString, hashMap);
        for (Element element : elements) {
            final TypeElement typeElement = (TypeElement) element;
            // 尝试在当前类上获取 @Destination 的信息
            final Destination destination = typeElement.getAnnotation(Destination.class);
            if (destination == null) {
                continue;
            }
            final String url = destination.url();
            final String description = destination.description();
            final String realClassName = typeElement.getQualifiedName().toString();
            print("url = " + url);
            print("description = " + description);
            print("realClassName = " + realClassName);

            builder.addStatement("mapping.put($S, $S)", url, realClassName);
        }
        builder.addStatement("return mapping");
        MethodSpec get = builder.build();

        String className = "RouterMapping_" + System.currentTimeMillis();   // 生成的类的类名
        print("generate class " + className);
        TypeSpec clazzRouterMapping = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(get)
                .build();

        print("generate java file");
        JavaFile javaFile = JavaFile.builder("com.oayilix.lodestar.mapping", clazzRouterMapping)
                .build();

        print("write java file to...");
        try {
            javaFile.writeTo(processingEnv.getFiler());
            print("java file write to filer, success");
        } catch (IOException e) {
            print("java file write to filer, error = " + e);
        }

//        // 生成的类的类名
//        String className = "RouterMapping_" + System.currentTimeMillis();
//
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder
//                .append("package com.oayilix.lodestar.mapping;\n\n")
//                .append("import java.util.HashMap;\n")
//                .append("import java.util.Map;\n\n")
//                .append("public class ").append(className).append(" {\n\n")
//                .append("    public static Map<String, String> get() {\n\n")
//                .append("        Map<String, String> mapping = new HashMap<>();\n");
//
//        // 遍历所有 @Destination 注解标注的类
//        for (Element element : elements) {
//            final TypeElement typeElement = (TypeElement) element;
//            // 尝试在当前类上获取 @Destination 的信息
//            final Destination destination = typeElement.getAnnotation(Destination.class);
//            if (destination == null) {
//                continue;
//            }
//            final String url = destination.url();
//            final String description = destination.description();
//            final String realClassName = typeElement.getQualifiedName().toString();
//            print("url = " + url);
//            print("description = " + description);
//            print("realClassName = " + realClassName);
//
//            stringBuilder.append("        mapping.put(\"")
//                    .append(url)
//                    .append("\", \"")
//                    .append(realClassName)
//                    .append("\");\n");
//        }
//        stringBuilder.append("        return mapping;\n")
//                .append("    }\n")
//                .append("}");
//
//        // 写入自动生成的类到本地文件
//        String mappingFullClassName = "com.oayilix.lodestar.mapping." + className;
//        print("mappingFullClassName = " + mappingFullClassName);
//        print("class content = \n" + stringBuilder);
//        try {
//            JavaFileObject source = processingEnv.getFiler().createSourceFile(mappingFullClassName);
//            Writer writer = source.openWriter();
//            writer.write(stringBuilder.toString());
//            writer.flush();
//            writer.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void print(String text) {
        System.out.println(TAG + " >>>>>> " + text);
    }
}