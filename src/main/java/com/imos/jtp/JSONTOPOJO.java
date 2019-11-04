/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 *
 * @author alok.meher
 */
public class JSONTOPOJO {

    private static final Logger LOG = LogManager.getLogger(JSONTOPOJO.class);
    private static final boolean PROD_MODE = true;
    private static String jsonFileName;
    private static String packageFolderPath;

    public static void main(String[] args) {
        try {
            if (PROD_MODE) {
                LOG.info("POJO Class generation started");
                if (args.length != 2) {
                    System.out.println("Enter: <JSON File> <Package folder>");
                    return;
                }
                jsonFileName = System.getProperty("user.dir") + File.separator + args[0];
                if (!new File(jsonFileName).exists()) {
                    System.out.println("Enter: <JSON File> <Package folder>");
                    return;
                }
                if (".".equals(args[1]) || "".equals(args[1])) {
                    packageFolderPath = System.getProperty("user.dir");
                } else {
                    packageFolderPath = System.getProperty("user.dir") + File.separator + args[1];
                }
                if (!new File(packageFolderPath).exists()) {
                    System.out.println("Enter: <JSON File> <Package folder>");
                    return;
                } else {
                    new File(packageFolderPath).mkdirs();
                }
            } else {
                jsonFileName = "src/main/resources/inputSchema.json";
                packageFolderPath = "src/main/java";
                packageFolderPath = "src/main/java/test";
            }

            String strData = new String(Files.readAllBytes(Paths.get(jsonFileName)));
            JSONObject json = new JSONObject(strData);
            JSONObject jsonData = json.getJSONObject("data");
            String packageName = json.getString("package_name");
            String className = json.getString("class_name");
            boolean validationRequired = json.optBoolean("validation");
            new JSONParser(packageFolderPath).parseJSONObject(jsonData, packageName, className, validationRequired);
            deleteClassFiles(new File(packageFolderPath));
            LOG.info("All generated *.class files are deleted");
            LOG.info("All POJO Class are generated successfully");
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    private static void deleteClassFiles(File folder) {
        if (folder.isDirectory()) {
            File[] subFile = folder.listFiles();
            if (subFile != null) {
                for (File file : subFile) {
                    if (file.isDirectory()) {
                        deleteClassFiles(file);
                    } else if (file.getName().endsWith(".class")) {
                        file.delete();
                    }
                }
            }
        }
    }

}
