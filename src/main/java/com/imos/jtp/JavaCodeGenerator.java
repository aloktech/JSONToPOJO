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

/**
 *
 * @author alok.meher
 */
public class JavaCodeGenerator {

    public boolean jacksonProperty = false;
    public String folderPath = "";
    private String javaFileAP;

    public void generate(SchemaData schemaData, String folderPath) {
        try {
            if (new File(folderPath).exists()) {
                this.folderPath = folderPath;
            } else {
                if ("".equals(folderPath) || ".".equals(folderPath)) {
                    this.folderPath = System.getProperty("user.dir");
                } else {
                    this.folderPath = System.getProperty("user.dir") + File.separator + folderPath;
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
            javaFileAP = ((this.folderPath + File.separator + name).replaceAll("\\.", File.separator)) + ".java";
            compiler.run(null, null, null, new File(javaFileAP).getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
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
                                ex.printStackTrace();
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
                    Logger.getLogger(JavaCodeGenerator.class.getName()).log(Level.SEVERE, null, ex);
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
