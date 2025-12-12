/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.jpa.HibernatePersistenceProvider;

import com.anaptecs.jeaf.spi.persistence.annotations.ApplicationManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.ConnectionPool;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.annotations.Property;
import com.mysql.cj.jdbc.Driver;

@PersistenceConfig(
    persistenceUnitConfigClass = CompletePersistenceUnit.class,
    applicationManagedConnectionDefinition = CompletePersistenceUnit.class)

@PersistenceUnit(
    name = "FullFletchedPersistenceUnit",

    jarFiles = { "lib/jeaf-hibernate5-service-provider.jar", "lib/my-domain-objects.jar" },

    properties = { @Property(name = "key1", value = "value1"), @Property(name = "key2", value = "value2") },

    formatSQL = "true",
    showSQL = "true",

    persistenceProvider = HibernatePersistenceProvider.class,

    sharedCacheMode = SharedCacheMode.ALL,
    validationMode = ValidationMode.CALLBACK)

@ApplicationManagedConnections(
    transactionType = PersistenceUnitTransactionType.JTA,
    jdbcDriver = Driver.class,
    dialect = "A.B.C",
    connectionURL = "jdbc:mysql://localhost:3306/jeaf_test?useSSL=false&serverTimezone=CET",
    username = "root",
    password = "HELLO",

    // Connection pool settings
    connectionPool = @ConnectionPool(
        minSize = "5",
        maxSize = "50",
        incrementSize = "2",
        preparedStatementCacheSize = "10000",
        timeout = "600",
        idleTestPeriod = "100"))

public interface CompletePersistenceUnit {

}
