/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.ApplicationManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.ManagedClasses;
import com.anaptecs.jeaf.spi.persistence.annotations.MappingFiles;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.mysql.cj.jdbc.Driver;

@PersistenceConfig(
    persistenceUnitConfigClass = EmptyPersistenceUnit.class,
    applicationManagedConnectionDefinition = EmptyPersistenceUnit.class)

@PersistenceUnit(
    name = "EmptyPersistenceUnit",
    managedClassesDefinition = EmptyPersistenceUnit.class,
    mappingFilesDefinition = EmptyPersistenceUnit.class,
    jarFiles = {})

@ApplicationManagedConnections(connectionURL = "abc", jdbcDriver = Driver.class)

@ManagedClasses(managedClasses = {})
@MappingFiles(mappingFiles = {})
public interface EmptyPersistenceUnit {

}
