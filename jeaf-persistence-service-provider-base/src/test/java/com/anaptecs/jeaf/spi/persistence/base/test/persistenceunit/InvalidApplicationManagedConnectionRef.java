/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;

@PersistenceConfig(
    persistenceUnitConfigClass = MyJUnitPersistenceUnit.class,
    applicationManagedConnectionDefinition = String.class)
public interface InvalidApplicationManagedConnectionRef {
}
