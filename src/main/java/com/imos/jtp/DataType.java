/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author alok.meher
 */
public enum DataType {
    
    STRING("String"),
    INTEGER("Integer"),
    LONG("Long"),
    DATE("Date"),
    DOUBLE("Double"),
    OBJECT("Object"),
    ARRAY("Array"),
    BOOLEAN("Boolean"),
    NONE("None");

    @Getter
    private final String dataType;

    @Getter
    @Setter
    private String classPackage = "java.lang";

    private DataType(String dataType) {
        this.dataType = dataType;
    }
}
