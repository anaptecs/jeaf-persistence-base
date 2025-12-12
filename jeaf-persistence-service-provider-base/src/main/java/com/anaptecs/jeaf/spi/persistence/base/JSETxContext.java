/*
 * anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 * 
 * Copyright 2004 - 2013 All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.anaptecs.jeaf.core.api.Component;
import com.anaptecs.jeaf.core.api.MessageConstants;
import com.anaptecs.jeaf.core.servicechannel.jpa.JPATxContext;
import com.anaptecs.jeaf.core.spi.ComponentImplementation;
import com.anaptecs.jeaf.xfun.api.XFun;
import com.anaptecs.jeaf.xfun.api.checks.Assert;
import com.anaptecs.jeaf.xfun.api.trace.Trace;

/**
 * Class provides an JPA Transaction Context implementation for the use within JSE environments.
 * 
 * @author JEAF Development Team
 * @version 1.0
 */
public final class JSETxContext extends JPATxContext {
  /**
   * Default Serial Version UID
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize object.
   */
  public JSETxContext( ) {
    // Nothing to do.
  }

  /**
   * Method checks whether the transaction represented by this transaction context is marked for roll back only.
   * 
   * @return boolean The method returns true if the transaction is marked for roll back only. and false in all other
   * cases.
   * @see com.anaptecs.jeaf.core.api.TxContext#getRollbackOnly()
   */
  @Override
  public boolean getRollbackOnly( ) {
    // Check for all entity managers if the rollback only flag is set.
    boolean lRollbackOnly = false;
    for (EntityManager lEntityManager : this.getAllEntityManagers()) {
      // Determine transaction status using entity manager.
      EntityTransaction lTransaction = lEntityManager.getTransaction();
      if (lTransaction.isActive() == true) {
        boolean lStatus = lTransaction.getRollbackOnly();
        if (lStatus == true) {
          lRollbackOnly = true;
          break;
        }
      }
    }

    return lRollbackOnly;
  }

  /**
   * Method marks the transaction that is represented by this transaction context for roll back only. This means that
   * the transaction can no longer be committed.
   * 
   * @see com.anaptecs.jeaf.core.api.TxContext#setRollbackOnly()
   */
  @Override
  public void setRollbackOnly( ) {
    // Delegate call to entity manager.
    final EntityManager lEntityManager = this.getCurrentEntityManager();
    lEntityManager.getTransaction().setRollbackOnly();
  }

  /**
   * Method performs a lookup for the entity manager for the passed component. This method is based on the design that
   * every JEAF Component has its own entity manager. In this implementation the entity manager is created in the way as
   * it is defined by JPA for JSE environments.
   * 
   * @param pComponent Component whose entity manager should be returned by this method. The parameter is never null.
   * @return {@link EntityManager} Entity manager for the passed component. The method must not return null.
   * @see JPATxContext#lookupEntityManager(Component)
   */
  @Override
  protected EntityManager lookupEntityManager( ComponentImplementation pComponent ) {
    // Check parameter for null.
    Assert.assertNotNull(pComponent, "pComponent");

    // Get factory and use it to create a new entity manager.
    final EntityManagerFactory lFactory = this.lookupEntityManagerFactory(pComponent);
    EntityManager lEntityManager = lFactory.createEntityManager();

    // Make sure that the entity manager belongs to the current transaction.
    EntityTransaction lTransaction = lEntityManager.getTransaction();
    if (lTransaction.isActive() == false) {
      lTransaction.begin();
    }

    // Return created entity manager
    return lEntityManager;
  }

  /**
   * Method performs a lookup for the entity manager factory for the passed component. This method is based on the
   * design that every JEAF Component may have its own entity manager.
   * 
   * @param pComponent Component whose entity manager should be returned by this method. The parameter is never null.
   * @return {@link EntityManagerFactory} Entity manager factory for the passed component. The method must not return
   * null.
   */
  private EntityManagerFactory lookupEntityManagerFactory( ComponentImplementation pComponent ) {
    // Check parameter for null.
    Assert.assertNotNull(pComponent, "pComponent");

    // Try to get cached factory or create it if it does not exist yet.
    final String lPersistenceUnitName = pComponent.getPersistenceUnitName();
    EntityManagerFactory lFactory = JPATxContext.getCachedEntityManagerFactory(lPersistenceUnitName);

    // Factory was not yet used.
    if (lFactory == null) {
      // Load properties for persistence unit if defined
      Trace lTrace = XFun.getTrace();
      Properties lProperties = new Properties();

      try {
        final ResourceBundle lBundle = ResourceBundle.getBundle(lPersistenceUnitName);
        final Enumeration<String> lKeys = lBundle.getKeys();
        while (lKeys.hasMoreElements()) {
          final String lKey = lKeys.nextElement();
          lProperties.setProperty(lKey, lBundle.getString(lKey));
        }
        lTrace.write(MessageConstants.USING_JPA_CONFIGURATION_FROM_PROPERTIES, lPersistenceUnitName);
      }
      // No properties file defined for persistence unit.
      catch (MissingResourceException e) {
        // No exception handling required.
        lTrace.write(MessageConstants.USING_JPA_CONFIGURATION_FROM_PERSISTENCE_XML, lPersistenceUnitName);
      }
      lFactory = Persistence.createEntityManagerFactory(lPersistenceUnitName, lProperties);

      // Add factory to static map with all factories.
      this.registerEntityManagerFactory(lPersistenceUnitName, lFactory);
    }

    // Return entity manager factory.
    return lFactory;
  }
}