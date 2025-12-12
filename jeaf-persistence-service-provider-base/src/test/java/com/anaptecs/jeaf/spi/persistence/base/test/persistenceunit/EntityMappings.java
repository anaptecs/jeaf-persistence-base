/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.ManagedClasses;
import com.anaptecs.jeaf.spi.persistence.annotations.MappingFiles;

@ManagedClasses(managedClasses = { String.class, Integer.class })

@MappingFiles(
    mappingFiles = { "com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml",
      "com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml",
      "com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml" })

public interface EntityMappings {

}
