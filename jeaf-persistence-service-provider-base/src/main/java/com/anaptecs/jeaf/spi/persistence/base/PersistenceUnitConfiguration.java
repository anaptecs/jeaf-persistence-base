/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base;

import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

public class PersistenceUnitConfiguration {
  /**
   * 
   * @param pConfigurationFileName Name of configuration file. The parameter must not be null. The passed file name will
   * be extended with <code>pBasePackage</code>.
   * @param pBasePackagePath Path under which the file should be found in the classpath. The parameter may be null.
   * @return
   */
  public List<PersistenceUnitInfo> resolvePersistenceUnitConfigurations( String pConfigurationFileName,
      String pBasePackagePath ) {

    return null;

  }
}
