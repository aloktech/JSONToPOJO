/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.jtp;

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
public class SchemaJSONKeyData {
    
    @NotNull
    private String jsonFieldName;
    private String jsonFieldLabel;
    @NotNull
    private String keyName;
    private String dateFormat;
    private boolean validate;
    private boolean notNull;
    private boolean notBlank;
    private boolean emailId;
    private boolean pattern;
    @NotNull
    private DataType dataType;
}
