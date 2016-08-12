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
import si.sed.commons.utils.SEDLogger;

/**
 * Sets up the outgoing chain to build a ebms 3.0 (AS4) form message. First it
 * will create Messaging object according pmode configuratin added as
 * "PMode.class" param in message context. For user message attachments are
 * added (and compressed according to pmode settings ) In the end encryption and
 * security interceptors are configured.
 *
 * @author Jože Rihtaršič
 */
public class EBMSOutFaultInterceptor extends AbstractEBMSInterceptor {

    /**
     * Logger for EBMSOutInterceptor class
     */
    protected final static SEDLogger LOG = new SEDLogger(EBMSOutFaultInterceptor.class);

   

    /**
     * Contstructor EBMSOutInterceptor for setting instance in a phase
     * Phase.PRE_PROTOCOL
     */
    public EBMSOutFaultInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    /**
     * Method transforms message to ebMS 3.0 (AS4) message form and sets
     * signature and encryption interceptors.
     *
     * @param msg: SoapMessage handled in CXF bus
     */
    @Override
    public void handleMessage(SoapMessage msg) {
        long l = LOG.logStart(msg);
        LOG.log("handleMessage");

   
        LOG.logEnd(l);
    }

   

}
