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
package si.sed.commons;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SEDValues {
    public static String EBMS_QUEUE_JNDI = "queue/MSHQueue";
    
    public static String EBMS_QUEUE_PARAM_MAIL_ID = "ebms_queue_mail_id";
    public static String EBMS_QUEUE_PARAM_PMODE_ID = "ebms_queue_pmode_id";
    
    public static String EBMS_QUEUE_PARAM_RETRY = "ebms_queue_mail_retry";    
    public static String EBMS_QUEUE_PARAM_DELAY = "ebms_queue_mail_delay";    
    public static String EBMS_QUEUE_DELAY_HQ = "_HQ_SCHED_DELIVERY"; // hornet value for delay!
}
