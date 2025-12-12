/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import javax.persistence.spi.PersistenceUnitTransactionType;

import com.anaptecs.jeaf.spi.persistence.annotations.ApplicationManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.annotations.Property;
import com.mysql.cj.jdbc.Driver;

@PersistenceConfig(
    persistenceUnitConfigClass = MyJUnitPersistenceUnit.class,
    applicationManagedConnectionDefinition = MyJUnitPersistenceUnit.class)

@PersistenceUnit(
    name = "MyJUnitPersistenceUnit",
    managedClassesDefinition = EntityMappings.class,
    mappingFilesDefinition = EntityMappings.class,

    properties = { @Property(name = "key1", value = "value1"), @Property(name = "key2", value = "value2") })

@ApplicationManagedConnections(
    transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL,
    jdbcDriver = Driver.class,
    connectionURL = "jdbc:mysql://localhost:3306/jeaf_test?useSSL=false&serverTimezone=CET",

    properties = { @Property(name = "appManagedKey1", value = "value2") })

public interface MyJUnitPersistenceUnit {
}
