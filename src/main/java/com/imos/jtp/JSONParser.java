/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author alok.meher
 */
@Log4j2
public class JSONParser {
    
    private final String folderPath;
    private final JavaCodeGenerator generator;

    public JSONParser(String folderPath) {
        this.folderPath = folderPath;
        generator = new JavaCodeGenerator();
    }

    Lock lock = new ReentrantLock();

    private void parseJSONArray(JSONArray array, String packageName, String className, boolean validationRequired) {
        lock.lock();
        try {
            List<SchemaJSONKeyData> keys = new ArrayList<>();
            for (int i = 0, n = array.length(); i < n; i++) {
                Object value = array.get(i);
                checkDataType(packageName, value, className, keys, validationRequired);
            }
        } catch (JSONException e) {
           log.error(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void parseJSONObject(JSONObject json, String packageName, String className, boolean validationRequired) {
        lock.lock();
        try {
            List<SchemaJSONKeyData> keys = new ArrayList<>();
            json.keySet()
                    .forEach(key -> {
                        Object value = json.get(key);
                        checkDataType(packageName, value, key, keys, validationRequired);
                    });
            SchemaData schemaData = new SchemaData();
            schemaData.setPackageName(packageName);
            schemaData.setClassName(toCamelCaseForClass(className));
            schemaData.setJsonKeys(keys);
            schemaData.setValidationRequired(validationRequired);
            generator.generate(schemaData, folderPath);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void checkDataType(String packageName, Object value, String key, List<SchemaJSONKeyData> keys, boolean validationRequired) throws JSONException {
        lock.lock();
        try {
            DataType dataType = findDataType(value, key);
            switch (dataType) {
                case OBJECT:
                    SchemaJSONKeyData keyDataObject = new SchemaJSONKeyData();
                    keyDataObject.setDataType(dataType);
                    dataType.setClassPackage(packageName);
                    keyDataObject.setKeyName(toCamelCaseForField(key));
                    keyDataObject.setJsonFieldName(key);
                    keyDataObject.setJsonFieldLabel(key);
                    keys.add(keyDataObject);
                    JSONObject childjson = (JSONObject) value;
                    parseJSONObject(childjson, packageName, toCamelCaseForClass(key), validationRequired);
                    break;
                case ARRAY:
                    SchemaJSONKeyData keyDataArray = new SchemaJSONKeyData();
                    keyDataArray.setDataType(dataType);
                    dataType.setClassPackage(packageName);
                    keyDataArray.setKeyName(toCamelCaseForField(key));
                    keyDataArray.setJsonFieldLabel(key);
                    keyDataArray.setJsonFieldName(key);
                    keys.add(keyDataArray);
                    JSONArray childarray = (JSONArray) value;
                    parseJSONArray(childarray, packageName, toCamelCaseForClass(key), validationRequired);
                    break;
                default:
                    SchemaJSONKeyData keyData = new SchemaJSONKeyData();
                    keyData.setDataType(dataType);
                    keyData.setKeyName(toCamelCaseForField(key));
                    keyData.setJsonFieldLabel(key);
                    keyData.setJsonFieldName(key);
                    keyData.setValidate(validationRequired);
                    keys.add(keyData);
                    break;
            }
        } catch (JSONException e) {
            log.error(e.getMessage());
        } finally {
            lock.unlock();
        }

    }

    private static DataType findDataType(Object obj, String key) throws JSONException {
        if (obj instanceof JSONObject) {
            return DataType.OBJECT;
        } else if (obj instanceof JSONArray) {
            return DataType.ARRAY;
        } else if (obj instanceof Boolean) {
            return DataType.BOOLEAN;
        } else if (obj instanceof Integer) {
            return DataType.INTEGER;
        } else if (obj instanceof Long) {
            return DataType.LONG;
        } else if (obj instanceof Double) {
            return DataType.DOUBLE;
        } else if (obj instanceof String) {
            return DataType.STRING;
        } else {
            return DataType.NONE;
        }
    }

    private static String toCamelCaseForClass(String name) {
        if (!name.contains("_")) {
            return String.valueOf(name.charAt(0)).toUpperCase() + name.substring(1);
        } else {
            return toCamelCaseForField(name);
        }
    }

    private static String toCamelCaseForField(String name) {
        String strData = Arrays.stream(name.split("_"))
                .map(data -> data.trim())
                .filter(data -> !data.isEmpty())
                .map(data -> String.valueOf(data.charAt(0)).toUpperCase() + data.substring(1))
                .collect(Collectors.joining());
        return String.valueOf(strData.charAt(0)).toLowerCase() + strData.substring(1);
    }

}
