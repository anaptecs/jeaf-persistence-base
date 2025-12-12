/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;

@PersistenceConfig(
    persistenceUnitConfigClass = InvalidMappingFilesRef.class,
    applicationManagedConnectionDefinition = MyJUnitPersistenceUnit.class)

@PersistenceUnit(name = "InvalidManagedClassesRef", mappingFilesDefinition = String.class)
public interface InvalidMappingFilesRef {
}
