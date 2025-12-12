/**
 * Copyright 2004 - 2020 anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.anaptecs.jeaf.spi.persistence.PersistenceServiceProviderMessages;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.PersistenceUnitInfoImpl;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.CompletePersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.ConnectionLessPersistenceConfiguration;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.EmptyPersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidApplicationManagedConnectionRef;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidConnectionPoolSettings;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidContainerManagedConnectionsRef;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidJDBCConnection;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidJNDILookup;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidManagedClassesRef;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidMappingFilesRef;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.MyJEEPersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.MyJUnitPersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.NoDataSourceDefinition;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.NonJTAPersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.PersistenceUnitMissing;
import com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.TooManyDataSourceDefinitions;
import com.anaptecs.jeaf.xfun.api.checks.AssertionFailedError;
import com.anaptecs.jeaf.xfun.api.errorhandling.JEAFSystemException;
import com.mysql.cj.jdbc.Driver;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PersistenceUnitInfoTest {
  @Test
  @Order(10)
  public void testPersistenceUnitCoreFeatures( ) {
    PersistenceUnit lAnnotation = MyJUnitPersistenceUnit.class.getAnnotation(PersistenceUnit.class);
    assertNotNull(lAnnotation);

    // Check attributes of persistence unit
    PersistenceUnitInfoImpl lPersistenceUnitInfo = new PersistenceUnitInfoImpl(MyJUnitPersistenceUnit.class);
    assertEquals(lAnnotation.name(), lPersistenceUnitInfo.getPersistenceUnitName());

    assertEquals(lAnnotation.sharedCacheMode(), lPersistenceUnitInfo.getSharedCacheMode());
    assertNull(lPersistenceUnitInfo.getPersistenceProviderClassName());
    assertEquals(lAnnotation.validationMode(), lPersistenceUnitInfo.getValidationMode());
    assertFalse(lPersistenceUnitInfo.excludeUnlistedClasses());
    assertEquals(0, lPersistenceUnitInfo.getJarFileUrls().size());

    // Check properties that should be generated from the configuration
    Properties lProperties = lPersistenceUnitInfo.getProperties();
    assertEquals(lAnnotation.showSQL(), lProperties.getProperty("hibernate.show_sql"));
    assertEquals(lAnnotation.formatSQL(), lProperties.getProperty("hibernate.format_sql"));
    assertEquals("value1", lProperties.getProperty("key1"));
    assertEquals("value2", lProperties.getProperty("key2"));

    // Check resolution of managed classes
    List<String> lManagedClassNames = lPersistenceUnitInfo.getManagedClassNames();
    assertEquals(2, lManagedClassNames.size());
    assertEquals(String.class.getName(), lManagedClassNames.get(0));
    assertEquals(Integer.class.getName(), lManagedClassNames.get(1));

    // Check resolution of mapping files.
    List<String> lMappingFileNames = lPersistenceUnitInfo.getMappingFileNames();
    assertEquals(3, lMappingFileNames.size());

    assertEquals("com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml",
        lMappingFileNames.get(0));
    assertEquals("com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml",
        lMappingFileNames.get(0));
    assertEquals("com/anaptecs/jeaf/serviceproviders/persistence/hibernate/SessionBO.hbm.xml",
        lMappingFileNames.get(0));

    // Check handling of JTA data sources
    assertNull(lPersistenceUnitInfo.getJtaDataSource());
    assertNull(lPersistenceUnitInfo.getNonJtaDataSource());

    assertNull(lPersistenceUnitInfo.getPersistenceUnitRootUrl());

    // Check unsupported handling of transformers
    try {
      lPersistenceUnitInfo.addTransformer(null);
      fail("Exception expected.");
    }
    catch (AssertionFailedError e) {
      assertEquals(
          "Assertion failed. Internal error. Transformers are not supported by this persistence unit implementation.",
          e.getMessage());
    }

    // Check additional operations
    assertEquals("2.1", lPersistenceUnitInfo.getPersistenceXMLSchemaVersion());
    assertNotNull(lPersistenceUnitInfo.getClassLoader());
    assertNull(lPersistenceUnitInfo.getNewTempClassLoader());

    // Test cases with advanced core configurations
    lAnnotation = CompletePersistenceUnit.class.getAnnotation(PersistenceUnit.class);
    assertNotNull(lAnnotation);
    lPersistenceUnitInfo = new PersistenceUnitInfoImpl(CompletePersistenceUnit.class);
    assertEquals(lAnnotation.name(), lPersistenceUnitInfo.getPersistenceUnitName());
    List<URL> lFileUrls = lPersistenceUnitInfo.getJarFileUrls();
    assertEquals(2, lFileUrls.size());
    assertEquals("file:lib/jeaf-hibernate5-service-provider.jar", lFileUrls.get(0).toString());
    assertEquals("file:lib/my-domain-objects.jar", lFileUrls.get(1).toString());
    lManagedClassNames = lPersistenceUnitInfo.getManagedClassNames();
    assertEquals(0, lManagedClassNames.size());
    lMappingFileNames = lPersistenceUnitInfo.getMappingFileNames();
    assertEquals(0, lMappingFileNames.size());
    assertEquals(HibernatePersistenceProvider.class.getName(), lPersistenceUnitInfo.getPersistenceProviderClassName());
    assertEquals(lAnnotation.sharedCacheMode(), lPersistenceUnitInfo.getSharedCacheMode());
    assertEquals(lAnnotation.validationMode(), lPersistenceUnitInfo.getValidationMode());
  }

  @Test
  @Order(20)
  public void testApplicationManagerConnections( ) {
    PersistenceUnit lAnnotation = MyJUnitPersistenceUnit.class.getAnnotation(PersistenceUnit.class);
    assertNotNull(lAnnotation);

    // Check attributes of persistence unit
    PersistenceUnitInfoImpl lPersistenceUnitInfo = new PersistenceUnitInfoImpl(MyJUnitPersistenceUnit.class);
    assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL, lPersistenceUnitInfo.getTransactionType());

    // Check connection settings.
    Properties lProperties = lPersistenceUnitInfo.getProperties();
    // jdbcDriver
    assertEquals(Driver.class.getName(), lProperties.get("hibernate.connection.driver_class"));
    // dialect
    assertNull(lProperties.get("hibernate.dialect"));
    // connectionURL
    assertEquals("jdbc:mysql://localhost:3306/jeaf_test?useSSL=false&serverTimezone=CET",
        lProperties.get("hibernate.connection.url"));
    // username
    assertNull(lProperties.get("hibernate.connection.username"));
    // password
    assertNull(lProperties.get("hibernate.connection.password"));
    // connectionPool settings
    assertEquals("0", lProperties.get("hibernate.c3p0.min_size"));
    assertEquals("1", lProperties.get("hibernate.c3p0.max_size"));
    assertEquals("1", lProperties.get("hibernate.c3p0.acquire_increment"));
    assertEquals("0", lProperties.get("hibernate.c3p0.max_statements"));
    assertEquals("100", lProperties.get("hibernate.c3p0.idle_test_period"));
    assertNull(lProperties.get("hibernate.c3p0.timeout"));

    // properties
    assertEquals("value2", lProperties.get("appManagedKey1"));

    // Also check attributes for persistence unit with complete parameter set.
    lAnnotation = CompletePersistenceUnit.class.getAnnotation(PersistenceUnit.class);
    assertNotNull(lAnnotation);
    lPersistenceUnitInfo = new PersistenceUnitInfoImpl(CompletePersistenceUnit.class);
    assertEquals(PersistenceUnitTransactionType.JTA, lPersistenceUnitInfo.getTransactionType());
    lProperties = lPersistenceUnitInfo.getProperties();

    // jdbcDriver
    assertEquals(Driver.class.getName(), lProperties.get("hibernate.connection.driver_class"));
    // dialect
    assertEquals("A.B.C", lProperties.get("hibernate.dialect"));
    // connectionURL
    assertEquals("jdbc:mysql://localhost:3306/jeaf_test?useSSL=false&serverTimezone=CET",
        lProperties.get("hibernate.connection.url"));
    // username
    assertEquals("root", lProperties.get("hibernate.connection.username"));
    // password
    assertEquals("HELLO", lProperties.get("hibernate.connection.password"));
    // connectionPool settings
    assertEquals("5", lProperties.get("hibernate.c3p0.min_size"));
    assertEquals("50", lProperties.get("hibernate.c3p0.max_size"));
    assertEquals("2", lProperties.get("hibernate.c3p0.acquire_increment"));
    assertEquals("10000", lProperties.get("hibernate.c3p0.max_statements"));
    assertEquals("100", lProperties.get("hibernate.c3p0.idle_test_period"));
    assertEquals("600", lProperties.get("hibernate.c3p0.timeout"));

    lPersistenceUnitInfo = new PersistenceUnitInfoImpl(InvalidConnectionPoolSettings.class);
    lProperties = lPersistenceUnitInfo.getProperties();
    assertEquals("true", lProperties.getProperty("hibernate.show_sql"));
    assertEquals("true", lProperties.getProperty("hibernate.format_sql"));
    assertEquals(Driver.class.getName(), lProperties.get("hibernate.connection.driver_class"));
    assertEquals("justAnURL", lProperties.get("hibernate.connection.url"));
    assertEquals("value1", lProperties.getProperty("key1"));
    assertEquals("value2", lProperties.getProperty("key2"));
    assertEquals(6, lProperties.size());
  }

  @Test
  @Order(30)
  public void testContainerManagerConnections( ) throws NamingException {
    // Set system property for JNDI lookups and bind dummy datasource
    System.setProperty("java.naming.factory.initial", "org.osjava.sj.SimpleJndiContextFactory");
    System.setProperty("org.osjava.sj.jndi.shared", "true");
    InitialContext lInitialContext = new InitialContext();
    MyDatasource lDataSource = new MyDatasource();
    lInitialContext.bind("JTADataSource", lDataSource);

    PersistenceUnit lAnnotation = MyJUnitPersistenceUnit.class.getAnnotation(PersistenceUnit.class);
    assertNotNull(lAnnotation);

    // Check attributes of persistence unit
    PersistenceUnitInfoImpl lPersistenceUnitInfo = new PersistenceUnitInfoImpl(MyJEEPersistenceUnit.class);
    assertEquals(PersistenceUnitTransactionType.JTA, lPersistenceUnitInfo.getTransactionType());
    assertEquals(true, lPersistenceUnitInfo.excludeUnlistedClasses());

    // Check access to data sources
    assertEquals(lDataSource, lPersistenceUnitInfo.getJtaDataSource());
    assertNull(lPersistenceUnitInfo.getNonJtaDataSource());

    // Resolve properties
    Properties lProperties = lPersistenceUnitInfo.getProperties();
    assertEquals(4, lProperties.size());
    assertEquals(lAnnotation.showSQL(), lProperties.getProperty("hibernate.show_sql"));
    assertEquals(lAnnotation.formatSQL(), lProperties.getProperty("hibernate.format_sql"));
    assertEquals("4711", lProperties.getProperty("containerKey1"));
    assertEquals("4712", lProperties.getProperty("containerKey2"));

    // Test non-JTA data source
    lDataSource = new MyDatasource();
    lInitialContext.bind("MyNonJTADataSource", lDataSource);
    lPersistenceUnitInfo = new PersistenceUnitInfoImpl(NonJTAPersistenceUnit.class);
    assertEquals(PersistenceUnitTransactionType.RESOURCE_LOCAL, lPersistenceUnitInfo.getTransactionType());
    assertNull(lPersistenceUnitInfo.getJtaDataSource());
    assertEquals(lDataSource, lPersistenceUnitInfo.getNonJtaDataSource());
  }

  @Test
  @Order(40)
  public void testPersistenceUnitConfigurationErrors( ) {
    // Test persistence configuration without any connection settings.
    try {
      new PersistenceUnitInfoImpl(ConnectionLessPersistenceConfiguration.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.NO_CONNECTIONS_DEFINED, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Invalid persistence configuration in class 'com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.ConnectionLessPersistenceConfiguration'. Neither container managed connections nor application managed connections are defined."));
    }

    // Persistence unit without any classes.
    try {
      new PersistenceUnitInfoImpl(EmptyPersistenceUnit.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.NO_PERSISTENT_CLASSES, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Invalid persistence unit 'EmptyPersistenceUnit'. Classes of persistence unit are not defined at all. Neither as JAR files, nor as managed classes nor through mapping files."));
    }
    // Referenced persistence unit class does not have required annotation
    try {
      new PersistenceUnitInfoImpl(PersistenceUnitMissing.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "@PersistenceConfig annotation of class com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.PersistenceUnitMissing declares that class com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.PersistenceUnitMissing has annotation @PersistenceUnit. However the class does not have this annotation. Please correct the configuration of the persistence unit."));
    }

    try {
      new PersistenceUnitInfoImpl(String.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.CLASS_DOES_NOT_DECLARE_PERSISTENCE_CONFIG, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Class java.lang.String does not have annotation @PersistenceConfig. Please correct your configuration."));
    }

    try {
      new PersistenceUnitInfoImpl(InvalidContainerManagedConnectionsRef.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "@PersistenceConfig annotation of class com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidContainerManagedConnectionsRef declares that class java.lang.String has annotation @ContainerManagedConnections. However the class does not have this annotation. Please correct the configuration of the persistence unit."));
    }

    // No datasource defined
    try {
      new PersistenceUnitInfoImpl(NoDataSourceDefinition.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.NO_DATASOURCE_DEFINED, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Invalid configuration for container managed connections. Annotation ContainerManagedConnections of class 'com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.NoDataSourceDefinition' defines neither a JTA AND nor a non-JTA datasource. You either have to define a JTA or a non-JTA datasource for a container managed persistence unit but not none."));

    }

    // Too many datasources defined.
    try {
      new PersistenceUnitInfoImpl(TooManyDataSourceDefinitions.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.JTA_AND_NON_JTA_DATASOURCE_DEFINED, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Invalid configuration for container managed connections. Annotation ContainerManagedConnections of class 'com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.TooManyDataSourceDefinitions' defines a JTA AND a non-JTA datasource. You either have to define a JTA or a non-JTA datasource for a container managed persistence unit but not both."));
    }

    // Invalid application managed connection reference
    try {
      new PersistenceUnitInfoImpl(InvalidApplicationManagedConnectionRef.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "@PersistenceConfig annotation of class com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidApplicationManagedConnectionRef declares that class java.lang.String has annotation @ApplicationManagedConnections. However the class does not have this annotation. Please correct the configuration of the persistence unit."));
    }

    // Invalid JDBC connection URL.
    try {
      new PersistenceUnitInfoImpl(InvalidJDBCConnection.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.JDBC_CONNECTION_URL_MISSING, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "Invalid JDBC connection URL defined in @ApplicationManagedConnections annotation of class com.anaptecs.jeaf.spi.persistence.base.test.persistenceunit.InvalidJDBCConnection. Please correct your configuration."));
    }

    // Invalid managed classes reference
    try {
      new PersistenceUnitInfoImpl(InvalidManagedClassesRef.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_UNIT, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "@PersistenceUnit 'InvalidManagedClassesRef' declares that class java.lang.String has annotation @ManagedClasses. However the class does not have this annotation. Please correct the configuration of the persistence unit."));
    }

    // Invalid mapping files reference
    try {
      new PersistenceUnitInfoImpl(InvalidMappingFilesRef.class);
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_UNIT, e.getErrorCode());
      assertTrue(e.getMessage().endsWith(
          "@PersistenceUnit 'InvalidManagedClassesRef' declares that class java.lang.String has annotation @MappingFiles. However the class does not have this annotation. Please correct the configuration of the persistence unit."));
    }

    // Invalid JNDI-Path
    PersistenceUnitInfoImpl lPersistenceUnitInfo = new PersistenceUnitInfoImpl(InvalidJNDILookup.class);
    assertNull(lPersistenceUnitInfo.getJtaDataSource());
    try {
      lPersistenceUnitInfo.getNonJtaDataSource();
      fail("Exception expected.");
    }
    catch (JEAFSystemException e) {
      assertEquals(PersistenceServiceProviderMessages.DATASOURCE_LOOKUP_FAILED, e.getErrorCode());
      assertTrue(e.getMessage()
          .endsWith("JNDI Lookup for datasource 'NotExistingDataSource' failed. Details: NotExistingDataSource"));
    }
  }

  @Test
  @Order(50)
  public void testSystemPropertiesReplacement( ) {
    // jarFiles
    // showSQL
    // formatSQL
    // properties (PersistenceUnit, ApplicationManagedConnections, ContainerManagedConnections)

    // ApplicationManagedConnections: connectionURL, username, password, properties
    // Connection pool: minSize, maxSize, incrementSize, preparedStatementCacheSize, idleTestPeriod, timeout
  }
}

class MyDatasource implements DataSource {

  @Override
  public <T> T unwrap(Class<T> pIface) throws SQLException {
    return null;
  }

  @Override
  public boolean isWrapperFor(Class<?> pIface) throws SQLException {
    return false;
  }

  @Override
  public PrintWriter getLogWriter( ) throws SQLException {
    return null;
  }

  @Override
  public void setLogWriter(PrintWriter pOut) throws SQLException {
  }

  @Override
  public void setLoginTimeout(int pSeconds) throws SQLException {
  }

  @Override
  public int getLoginTimeout( ) throws SQLException {
    return 0;
  }

  @Override
  public Logger getParentLogger( ) throws SQLFeatureNotSupportedException {
    return null;
  }

  @Override
  public Connection getConnection( ) throws SQLException {
    return null;
  }

  @Override
  public Connection getConnection(String pUsername, String pPassword) throws SQLException {
    return null;
  }

}
