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
package si.jrc.msh.interceptor;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class EBMSInFaultInterceptor extends AbstractEBMSInterceptor {



    /**
     *
     */
    protected final static SEDLogger LOG = new SEDLogger(EBMSInFaultInterceptor.class);

 
    PModeManager mPModeManage = new PModeManager();
    EBMSUtils mebmsUtils = new EBMSUtils();
    CryptoCoverageChecker checker = new CryptoCoverageChecker();
    WSS4JInInterceptor wssInterceptor = new WSS4JInInterceptor();

    /**
     *
     */
    public EBMSInFaultInterceptor() {
        // super(Phase.USER_PROTOCOL);
        super(Phase.PRE_PROTOCOL); // user preprotocol for generating receipt
        // in user_protocol wss in removed!
        getAfter().add(WSS4JInInterceptor.class.getName());
    }

    /**
     *
     * @param phase
     */
    public EBMSInFaultInterceptor(String phase) {
        super(phase);
    }


    /**
     *
     * @param msg
     */
    @Override
    public void handleMessage(SoapMessage msg) {
        long l = LOG.logStart();
        LOG.log("handleMessage ");
       
        LOG.logEnd(l);
    }


}
