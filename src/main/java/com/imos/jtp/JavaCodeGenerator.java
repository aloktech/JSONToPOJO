/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.joor.ReflectException;

/**
 *
 * @author alok.meher
 */
public class JavaCodeGenerator {

    public boolean jacksonProperty = false;
    public String folderPath = "";
    private String javaFileAP;
    private String javaClassAP;
    private String classPath;

    public void generate(SchemaData schemaData, String folderPath) {
        try {
            System.out.println(folderPath);
            if (new File(folderPath).exists()) {
                this.folderPath = folderPath;
            } else {
                if ("".equals(folderPath)) {
                    this.folderPath = System.getProperty("user.dir");
                } else {
                    this.folderPath = System.getProperty("user.dir") + File.separator + folderPath;
                }
                new File(this.folderPath).mkdirs();
            }
            this.classPath = this.folderPath;
            System.out.println(this.folderPath);

            TypeSpec.Builder jsonPOJOBuilder = TypeSpec.classBuilder(schemaData.getClassName())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Getter.class)
                    .addAnnotation(Setter.class)
                    .addSuperinterface(Serializable.class);

            schemaData.getJsonKeys()
                    .forEach(data -> {
                        generateAttributes(data, schemaData, jsonPOJOBuilder);
                    });

            TypeSpec javaPOJO = jsonPOJOBuilder.build();
            JavaFile javaFile = JavaFile.builder(schemaData.getPackageName(), javaPOJO)
                    .build();

            javaFile.writeTo(new File(this.folderPath));

            String name = schemaData.getPackageName() + "." + schemaData.getClassName();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            javaFileAP = ((this.folderPath + File.separator + name).replaceAll("\\.", File.separator)) + ".java";
            javaClassAP = ((this.folderPath + File.separator + name).replaceAll("\\.", File.separator)) + ".class";
            System.out.println(javaFileAP);
            compiler.run(null, null, null, new File(javaFileAP).getAbsolutePath());
            System.out.println(name);
        } catch (IOException | ReflectException e) {
            e.printStackTrace();
        }
    }

    private void generateAttributes(SchemaJSONKeyData data, SchemaData schemaData, TypeSpec.Builder jsonPOJOBuilder) {
        try {
            AnnotationSpec jsonFieldAnnoSpec = null;
            if (jacksonProperty) {
                jsonFieldAnnoSpec = AnnotationSpec.builder(JsonProperty.class)
                        .addMember(JSONKeys.VALUE, CodeBlock.of("$S", data.getJsonFieldName()))
                        .build();
            } else {
                jsonFieldAnnoSpec = AnnotationSpec.builder(JSONField.class)
                        .addMember(JSONKeys.JSON_FIELD_NAME, CodeBlock.of("$S", data.getJsonFieldName()))
                        .build();
            }
            TypeName typeName = findTypeName(data.getDataType());
            FieldSpec.Builder fieldBuilder = null;
            if (typeName == TypeName.OBJECT) {
                if (null != data.getDataType()) {
                    switch (data.getDataType()) {
                        case OBJECT:
                            try {
                                File classFile = new File(this.folderPath + File.separator + data.getDataType().getClassPackage().replaceAll("\\.", File.separator) + File.separator + toCamelCaseForClass(data.getKeyName()) + ".class");
//                                File classFileOnly = new File(this.folderPath + File.separator + data.getDataType().getClassPackage().replaceAll("\\.", File.separator) + File.separator + toCamelCaseForClass(data.getKeyName()));
                                System.out.println(classFile.getAbsolutePath());
                                ClassLoader loader = URLClassLoader.newInstance(new URL[]{classFile.toURI().toURL()}, getClass().getClassLoader());
                                fieldBuilder = FieldSpec.builder(Class.forName(data.getDataType().getClassPackage() + "." + toCamelCaseForClass(data.getKeyName()), true, loader), data.getKeyName());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
//                            fieldBuilder = FieldSpec.builder(Class.forName(data.getDataType().getClassPackage() + "." + toCamelCaseForClass(data.getKeyName())), data.getKeyName());

                            AnnotationSpec notNull = AnnotationSpec.builder(NotNull.class)
                                    .addMember("message", CodeBlock.of("$S", data.getJsonFieldName().replaceAll("_", "\\.")))
                                    .build();
                            AnnotationSpec valid = AnnotationSpec.builder(Valid.class)
                                    .build();
                            if (schemaData.isValidationRequired()) {
                                fieldBuilder = fieldBuilder.addAnnotation(valid);
                                fieldBuilder = fieldBuilder.addAnnotation(notNull);
                            }
                            break;

                        case STRING:
                            fieldBuilder = FieldSpec.builder(String.class, data.getKeyName());
                            if (schemaData.isValidationRequired()) {
                                notNull = AnnotationSpec.builder(NotNull.class)
                                        .addMember("message", CodeBlock.of("$S", data.getJsonFieldName().replaceAll("_", "\\.")))
                                        .build();
                                AnnotationSpec notEmpty = AnnotationSpec.builder(NotEmpty.class)
                                        .addMember("message", CodeBlock.of("$S", data.getJsonFieldName().replaceAll("_", "\\.")))
                                        .build();
                                fieldBuilder = fieldBuilder.addAnnotation(notNull);
                                fieldBuilder = fieldBuilder.addAnnotation(notEmpty);
                            }
                            break;
                    }
                }
            } else if (typeName instanceof ParameterizedTypeName) {
                typeName = ParameterizedTypeName.get(List.class, Class.forName(data.getDataType().getClassPackage() + "." + toCamelCaseForClass(data.getKeyName())));
                fieldBuilder = FieldSpec.builder(typeName, data.getKeyName());
            } else {
                fieldBuilder = FieldSpec.builder(typeName, data.getKeyName());
            }

            if (fieldBuilder != null && jsonFieldAnnoSpec != null) {
                FieldSpec field = fieldBuilder.addModifiers(Modifier.PRIVATE)
                        .addAnnotation(jsonFieldAnnoSpec)
                        .build();
                jsonPOJOBuilder.addField(field);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private <T> TypeName findTypeName(DataType dataType) {
        switch (dataType) {
            case BOOLEAN:
                return TypeName.BOOLEAN;
            case INTEGER:
                return TypeName.INT;
            case LONG:
                return TypeName.LONG;
            case DOUBLE:
                return TypeName.DOUBLE;
            case ARRAY:
                return ParameterizedTypeName.get(List.class, Field.class);
            case STRING:
            case DATE:
            case OBJECT:
                return TypeName.OBJECT;
            default:
                System.out.println("Invalid");
        }
        return TypeName.VOID;
    }

    private String toCamelCaseForClass(String name) {
        if (!name.contains("_")) {
            return String.valueOf(name.charAt(0)).toUpperCase() + name.substring(1);
        } else {
            String strData = Arrays.stream(name.split("_"))
                    .map(data -> data.trim())
                    .filter(data -> !data.isEmpty())
                    .map(data -> String.valueOf(data.charAt(0)).toUpperCase() + data.substring(1))
                    .collect(Collectors.joining());
            return String.valueOf(strData.charAt(0)).toLowerCase() + strData.substring(1);

        }
    }
}
