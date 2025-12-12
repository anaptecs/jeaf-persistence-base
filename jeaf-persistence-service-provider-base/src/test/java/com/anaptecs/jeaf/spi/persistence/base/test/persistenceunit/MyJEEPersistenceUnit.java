/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.ContainerManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.annotations.Property;

@PersistenceConfig(
    persistenceUnitConfigClass = MyJEEPersistenceUnit.class,
    containerManagedConnectionDefinition = MyJEEPersistenceUnit.class)

@PersistenceUnit(
    name = "MyJEEPersistenceUnit",
    jarFiles = "lib/jeaf-hibernate5-service-provider.jar",
    managedClassesDefinition = EntityMappings.class,
    mappingFilesDefinition = EntityMappings.class)

@ContainerManagedConnections(
    jtaDataSource = "JTADataSource",
    excludeUnlistedClasses = true,

    // Container managed connection properties.
    properties = { @Property(name = "containerKey1", value = "4711"),
      @Property(name = "containerKey2", value = "4712") })

public interface MyJEEPersistenceUnit {

}
