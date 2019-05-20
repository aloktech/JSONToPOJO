/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author alok.meher
 */
@Getter
@Setter
@ToString
public class SchemaData {

    @NotNull
    private String packageName;
    @NotNull
    private String className;
    private boolean extended;
    private boolean serializable = true;
    private boolean validationRequired;
    private String extendedClassName;
    private List<SchemaJSONKeyData> jsonKeys = new ArrayList<>();
}
