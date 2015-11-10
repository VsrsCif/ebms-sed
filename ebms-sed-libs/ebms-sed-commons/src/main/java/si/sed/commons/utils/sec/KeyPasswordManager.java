/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package si.sed.commons.utils.sec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;


/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class KeyPasswordManager {

    private final Properties passwords = new Properties();
    protected final SEDLogger mlog = new SEDLogger(KeyPasswordManager.class);
   
    private static KeyPasswordManager mInstance=null;
    private KeyPasswordManager(){}
    
    public static KeyPasswordManager getInstance() throws SEDSecurityException{
        if (mInstance == null){
            mInstance = new KeyPasswordManager();
            mInstance.init();
            
        }
        return mInstance;
    }
   

    private void init() throws SEDSecurityException {
        String fileProperty = System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, "") + File.separator + SEDSystemProperties.SYS_KEY_PASSWD_DEF;
        try (FileInputStream fis = new FileInputStream(fileProperty)) {
            passwords.load(fis);
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.PasswordFileError, ex, ex.getMessage());
            
        }
    }

    
    public String getPasswordForAlias(String alias) {
        
        return passwords.getProperty(alias);
    }
}
