/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.jrc.msh.exception;

/**
 *
 * @author Jože Rihtaršič
 */
public enum MSHExceptionCode {

  /**
     *
     */
  EmptyMail("MSH:0001", "EmptyMail", "Empty Mail", 0),

  /**
     *
     */
  InvalidMail("MSH:0002", "InvalidMail", "Invalid mail! Errors: %s", 1),

  /**
     *
     */
  InvalidPModeId("MSH:0101", "InvalidPModeId", "PMode with id: %s not exists", 1),

  /**
     *
     */
  MissingPMode("MSH:0102", "MissingPMode", "Missing pMode for mail %s", 1),

  /**
     *
     */
  InvalidPMode("MSH:0103", "InvalidPModeId", "Invalid PMode: %s. Error: %s", 2),

  /**
     *
     */
  ErrorCreatingClient("MSH:0104", "ErrorCreatingClient",
      "Error creating client for submition mail %s", 1),
  /**
     *
     */
  ErrorCreatingSOAPMessage("MSH:0104", "ErrorCreatingSOAPMessage",
      "Error creating SOAPMessage for submition mail %s", 1),

  /**
     *
     */
  SecuritySettingsException("MSH:0105", "SecuritySettingsException",
      "Invalid PMode: %s. Error: %s", 3);

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

  /**
   *
   * @return
   */
  public String getCode() {
    return code;
  }

  /**
   *
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   *
   * @return
   */
  public String getDescriptionFormat() {
    return description;
  }

  /**
   *
   * @return
   */
  public int getDescParamCount() {
    return paramCount;
  }

}
