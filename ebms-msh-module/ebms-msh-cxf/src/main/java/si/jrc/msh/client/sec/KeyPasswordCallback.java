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
package si.jrc.msh.client.sec;


import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.sec.KeyPasswordManager;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class KeyPasswordCallback implements CallbackHandler {

    protected final SEDLogger mlog = new SEDLogger(KeyPasswordCallback.class);

    @Override
    public void handle(Callback[] callbacks) throws  UnsupportedCallbackException {
        long l = mlog.logStart();
        for (Callback callback : callbacks) {
            WSPasswordCallback pc = (WSPasswordCallback) callback;
            String pass;
            String identifier = pc.getIdentifier();
            try {
                pass = KeyPasswordManager.getInstance().getPasswordForAlias(identifier);                
            } catch (SEDSecurityException ex) {
                throw  new UnsupportedCallbackException(callback, ex.getMessage()); 
            }
            if (pass != null) {
                pc.setPassword(pass);
                return;
            } else {
                String msg = String.format("Missing password for key with alias '%s'.", identifier);
                mlog.logError(l,msg ,null);
                throw  new UnsupportedCallbackException(callback, msg); 
            }
        }
    }
}
