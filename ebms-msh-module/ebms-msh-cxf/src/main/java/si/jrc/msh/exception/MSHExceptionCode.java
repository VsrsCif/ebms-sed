/*
* Copyright 2015, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
 */
package si.jrc.msh.exception;

/**
 *
 * @author Jože Rihtaršič
 */
public enum MSHExceptionCode {

    EmptyMail("MSH:0001", "EmptyMail", "Empty Mail", 0),
    InvalidMail("MSH:0002", "InvalidMail", "Invalid mail! Errors: %s", 1),
    InvalidPModeId("MSH:0101", "InvalidPModeId", "PMode with id: %s not exists", 1),
    InvalidPMode("MSH:0102", "InvalidPModeId", "Invalid PMode: %s. Error: %s", 2);

    String code;
    String name;
    String description;
    int paramCount;

    MSHExceptionCode(String cd, String nm, String desc, int pc) {
        code = cd;
        name = nm;
        description = desc;
        paramCount = pc;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescriptionFormat() {
        return description;
    }

    public int getDescParamCount() {
        return paramCount;
    }

}
