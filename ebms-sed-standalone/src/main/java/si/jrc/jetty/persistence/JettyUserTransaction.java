/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
 * @author sluzba
 */
public class JettyUserTransaction implements UserTransaction {

    EntityTransaction met;

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
        // ingore
    }

}
