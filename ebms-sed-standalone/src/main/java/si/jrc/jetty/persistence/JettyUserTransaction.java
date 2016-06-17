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
package si.jrc.jetty.persistence;

import javax.persistence.EntityTransaction;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 *
 * @author Joze Rihtarsic
 */
public class JettyUserTransaction implements UserTransaction {

    EntityTransaction met;
    int miTimeOutTransaction = -1;

    public JettyUserTransaction(EntityTransaction et) {
        met = et;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        met.begin();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        met.commit();
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        met.rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        met.setRollbackOnly();
    }

    @Override
    public int getStatus() throws SystemException {

        return met.isActive() ? Status.STATUS_ACTIVE : Status.STATUS_NO_TRANSACTION;
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        miTimeOutTransaction = seconds * 100;
    }

}
