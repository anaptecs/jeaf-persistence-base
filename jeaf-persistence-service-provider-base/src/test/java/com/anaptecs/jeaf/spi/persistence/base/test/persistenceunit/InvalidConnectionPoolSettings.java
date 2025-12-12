/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit;

import com.anaptecs.jeaf.spi.persistence.annotations.ApplicationManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.ConnectionPool;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.mysql.cj.jdbc.Driver;

@PersistenceConfig(
    persistenceUnitConfigClass = MyJUnitPersistenceUnit.class,
    applicationManagedConnectionDefinition = InvalidConnectionPoolSettings.class)

@ApplicationManagedConnections(
    jdbcDriver = Driver.class,
    connectionURL = "justAnURL",

    // Define connection pools settings which are all not real strings.
    connectionPool = @ConnectionPool(
        minSize = " ",
        maxSize = "",
        incrementSize = "   ",
        preparedStatementCacheSize = "",
        timeout = "   ",
        idleTestPeriod = "\t"))
public interface InvalidConnectionPoolSettings {

}
