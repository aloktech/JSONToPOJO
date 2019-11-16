/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import com.imos.jtp.support.SecondValidation;
import com.imos.jtp.support.FirstValidation;
import com.alibaba.fastjson.annotation.JSONField;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 *
 * @author alok.meher
 */
@Log4j2
public class JavaCodeGenerator {

    public boolean jacksonProperty = false;
    public String folderPath = "";

    public void generateValidator(SchemaData schemaData) {
        TypeSpec.Builder jsonPOJOBuilder = TypeSpec.interfaceBuilder("FirstValidation")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Serializable.class);
        TypeSpec javaPOJO = jsonPOJOBuilder.build();
        JavaFile javaFile = JavaFile.builder(schemaData.getPackageName() + ".validator", javaPOJO)
                .build();
        try {
            javaFile.writeTo(new File(this.folderPath));
        } catch (IOException ex) {
            Logger.getLogger(JavaCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }

        jsonPOJOBuilder = TypeSpec.interfaceBuilder("SecondValidation")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(Serializable.class);
        javaPOJO = jsonPOJOBuilder.build();
        javaFile = JavaFile.builder(schemaData.getPackageName() + ".validator", javaPOJO)
                .build();
        try {
            javaFile.writeTo(new File(this.folderPath));
        } catch (IOException ex) {
            Logger.getLogger(JavaCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void generate(SchemaData schemaData, String folderPath) {
        try {
            if (new File(folderPath).exists()) {
                this.folderPath = folderPath;
            } else {
                this.folderPath = System.getProperty("user.dir");
                if (!"".equals(folderPath) && !".".equals(folderPath)) {
                    this.folderPath += File.separator + folderPath;
                }
                new File(this.folderPath).mkdirs();
            }
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
            String finalFilePath = this.folderPath + File.separator + name;
            String classFilePath = finalFilePath.substring(0, finalFilePath.lastIndexOf(File.separator));
            String packageFilePath = finalFilePath.substring(finalFilePath.lastIndexOf(File.separator));
            packageFilePath = packageFilePath.replaceAll("\\.", File.separator);
            finalFilePath = classFilePath + packageFilePath + ".java";
            compiler.run(null, null, null, finalFilePath);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void generateAttributes(SchemaJSONKeyData data, SchemaData schemaData, TypeSpec.Builder jsonPOJOBuilder) {
        try {
            AnnotationSpec jsonFieldAnnoSpec = null;
            if (jacksonProperty) {
//                jsonFieldAnnoSpec = AnnotationSpec.builder(JsonProperty.class)
//                        .addMember(JSONKeys.VALUE, CodeBlock.of("$S", data.getJsonFieldName()))
//                        .build();
            } else {
                jsonFieldAnnoSpec = AnnotationSpec.builder(JSONField.class)
                        .addMember(JSONKeys.JSON_FIELD_NAME, CodeBlock.of("$S", data.getJsonFieldName()))
                        .build();
            }
            TypeName typeName = findTypeName(data.getDataType());
            FieldSpec.Builder fieldBuilder = null;
            if (typeName == TypeName.OBJECT) {
                if (null != data.getDataType()) {
                    AnnotationSpec notNull;
                    switch (data.getDataType()) {
                        case OBJECT:
                            try {
                                File classFile = new File(this.folderPath);
                                ClassLoader loader = URLClassLoader.newInstance(new URL[]{classFile.toURI().toURL()}, getClass().getClassLoader());
                                Class<?> cls = loader.loadClass(data.getDataType().getClassPackage() + "." + toCamelCaseForClass(data.getKeyName()));
                                fieldBuilder = FieldSpec.builder(cls, data.getKeyName());

                                if (schemaData.isValidationRequired()) {
                                    notNull = AnnotationSpec.builder(NotNull.class)
                                            .addMember("message", CodeBlock.of("$S", "{" + data.getJsonFieldName().replaceAll("_", "\\.") + "}"))
                                            .addMember("groups", CodeBlock.of("$T.class", FirstValidation.class))
                                            .build();
                                    AnnotationSpec valid = AnnotationSpec.builder(Valid.class)
                                            .build();
                                    fieldBuilder = fieldBuilder.addAnnotation(valid);
                                    fieldBuilder = fieldBuilder.addAnnotation(notNull);
                                }
                            } catch (Exception ex) {
                                log.error(ex.getMessage());
                            }
                            break;
                        case STRING:
                            fieldBuilder = FieldSpec.builder(String.class, data.getKeyName());
                            if (schemaData.isValidationRequired()) {
                                notNull = AnnotationSpec.builder(NotNull.class)
                                        .addMember("message", CodeBlock.of("$S", "{" + data.getJsonFieldName().replaceAll("_", "\\.") + "}"))
                                        .addMember("groups", CodeBlock.of("$T.class", FirstValidation.class))
                                        .build();
                                AnnotationSpec notEmpty = AnnotationSpec.builder(NotEmpty.class)
                                        .addMember("message", CodeBlock.of("$S", "{" + data.getJsonFieldName().replaceAll("_", "\\.") + "}"))
                                        .addMember("groups", CodeBlock.of("$T.class", SecondValidation.class))
                                        .build();
                                fieldBuilder = fieldBuilder.addAnnotation(notNull);
                                fieldBuilder = fieldBuilder.addAnnotation(notEmpty);
                            }
                            break;
                    }
                }
            } else if (typeName instanceof ParameterizedTypeName) {
                File classFile = new File(this.folderPath);
                ClassLoader loader;
                try {
                    loader = URLClassLoader.newInstance(new URL[]{classFile.toURI().toURL()}, getClass().getClassLoader());
                    Class<?> cls = loader.loadClass(data.getDataType().getClassPackage() + "." + toCamelCaseForClass(data.getKeyName()));
                    typeName = ParameterizedTypeName.get(List.class, cls);
                    fieldBuilder = FieldSpec.builder(typeName, data.getKeyName());
                } catch (MalformedURLException ex) {
                    log.error(ex.getMessage());
                }
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
            log.error(e.getMessage());
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
