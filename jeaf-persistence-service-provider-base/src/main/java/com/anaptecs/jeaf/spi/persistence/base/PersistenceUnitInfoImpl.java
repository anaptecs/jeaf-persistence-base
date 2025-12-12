package com.anaptecs.jeaf.spi.persistence.base;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import com.anaptecs.jeaf.spi.persistence.PersistenceServiceProviderMessages;
import com.anaptecs.jeaf.spi.persistence.annotations.ApplicationManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.ConnectionPool;
import com.anaptecs.jeaf.spi.persistence.annotations.ContainerManagedConnections;
import com.anaptecs.jeaf.spi.persistence.annotations.ManagedClasses;
import com.anaptecs.jeaf.spi.persistence.annotations.MappingFiles;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceConfig;
import com.anaptecs.jeaf.spi.persistence.annotations.PersistenceUnit;
import com.anaptecs.jeaf.spi.persistence.annotations.Property;
import com.anaptecs.jeaf.tools.api.Tools;
import com.anaptecs.jeaf.tools.api.string.StringTools;
import com.anaptecs.jeaf.xfun.api.XFun;
import com.anaptecs.jeaf.xfun.api.checks.Assert;
import com.anaptecs.jeaf.xfun.api.checks.Check;
import com.anaptecs.jeaf.xfun.api.config.ConfigurationProvider;
import com.anaptecs.jeaf.xfun.api.errorhandling.JEAFSystemException;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {
  /**
   * Constant for used JPA version.
   */
  public static final String JPA_VERSION = "2.1";

  /**
   * Reference to PersistenceConfig annotation that is used to configure this persistence unit. The reference is never
   * null.
   */
  private final PersistenceConfig persistenceConfig;

  /**
   * Reference to PersistenceUnit annotation that is used to configure this persistence unit. The reference is never
   * null.
   */
  private final PersistenceUnit persistenceUnit;

  /**
   * Reference to ContainerManagedConnections annotation that is used to configure this persistence unit. The reference
   * may be null.
   */
  private final ContainerManagedConnections containerManagedConnectionConfig;

  /**
   * Reference to ApplicationManagedConnections annotation that is used to configure this persistence unit. The
   * reference may be null.
   */
  private final ApplicationManagedConnections applicationManagedConnectionConfig;

  /**
   * Reference to ConnectionPool annotation that is used to configure this persistence unit. The reference may be null.
   */
  private final ConnectionPool connectionPoolConfig;

  /**
   * Reference to ManagedClasses annotation that is used to configure this persistence unit. The reference may be null.
   */
  private final ManagedClasses managedClassesConfig;

  /**
   * Reference to MappingFiles annotation that is used to configure this persistence unit. The reference may be null.
   */
  private final List<MappingFiles> mappingFilesConfig;

  /**
   * Initialize object.
   * 
   * @param pClass Reference to class with annotation {@link PersistenceConfig}. The parameter must not be null and the
   * passed class must have annotation {@link PersistenceConfig}.
   */
  public PersistenceUnitInfoImpl( Class<?> pClass ) {
    // Check parameter
    Check.checkInvalidParameterNull(pClass, "pClass");

    // Resolve persistence configuration.
    persistenceConfig = this.resolvePersistenceConfig(pClass);

    // Also resolve dependent annotations.
    persistenceUnit = this.resolvePersistenceUnit(persistenceConfig, pClass);
    containerManagedConnectionConfig = this.resolveContainerManagedConnectionConfig(persistenceConfig, pClass);
    applicationManagedConnectionConfig = this.resolveApplicationManagedConnectionConfig(persistenceConfig, pClass);
    connectionPoolConfig = this.resolveConnectionPoolConfig(applicationManagedConnectionConfig);
    managedClassesConfig = this.resolveManagedClassesConfig(persistenceUnit);
    mappingFilesConfig = this.resolveMappingFilesConfig(persistenceUnit);

    // Check that either container or application manager connection is defined.
    if (containerManagedConnectionConfig == null && applicationManagedConnectionConfig == null) {
      throw new JEAFSystemException(PersistenceServiceProviderMessages.NO_CONNECTIONS_DEFINED, pClass.getName());
    }
    // Either jarFiles or managed classes or mapping files must be defined.
    else {
      boolean lJarFilesPresent = persistenceUnit.jarFiles().length > 0;
      boolean lManagedClassesPresent;
      if (managedClassesConfig != null && managedClassesConfig.managedClasses().length > 0) {
        lManagedClassesPresent = true;
      }
      else {
        lManagedClassesPresent = false;
      }
      boolean lMappingFilesPresent;
      if (mappingFilesConfig != null && mappingFilesConfig.size() > 0) {
        lMappingFilesPresent = true;
      }
      else {
        lMappingFilesPresent = false;
      }

      // Persistent classes are not defined at all.
      if (lJarFilesPresent == false && lManagedClassesPresent == false && lMappingFilesPresent == false) {
        throw new JEAFSystemException(PersistenceServiceProviderMessages.NO_PERSISTENT_CLASSES, persistenceUnit.name());
      }
    }
  }

  /**
   * Method resolves the persistence configuration form the passed class. Therefore the class must be annotated
   * with @PersistenceConfig
   * 
   * @param pClass Class from which the persistence configuration should be resolved. The parameter must not be null.
   * @return {@link PersistenceConfig} Persistence configuration from the passed class. The method never returns null.
   */
  private PersistenceConfig resolvePersistenceConfig( Class<?> pClass ) {
    // Check parameter.
    Assert.assertNotNull(pClass, "pClass");

    // Resolve annotation
    PersistenceConfig lAnnotation = pClass.getAnnotation(PersistenceConfig.class);
    if (lAnnotation != null) {
      return lAnnotation;
    }
    else {
      throw new JEAFSystemException(PersistenceServiceProviderMessages.CLASS_DOES_NOT_DECLARE_PERSISTENCE_CONFIG,
          pClass.getName());
    }
  }

  /**
   * Method resolves the persistence unit that belongs to the passed persistence configuration.
   * 
   * @param pPersistenceConfig Persistence configuration from which the persistence unit should be resolved. The
   * parameter must not be null.
   * @param pClass Class that holds the persistence configuration. The parameter must not be null.
   * @return {@link PersistenceUnit} Persistence unit that belongs to the persistence configuration. The method never
   * returns null.
   */
  private PersistenceUnit resolvePersistenceUnit( PersistenceConfig pPersistenceConfig, Class<?> pClass ) {
    // Check parameters.
    Assert.assertNotNull(pPersistenceConfig, "pPersistenceConfig");
    Assert.assertNotNull(pClass, "pClass");

    // Resolve class that holds the persistence unit annotation
    Class<?> lPersistenceUnitConfigClass = pPersistenceConfig.persistenceUnitConfigClass();
    PersistenceUnit lAnnotation = lPersistenceUnitConfigClass.getAnnotation(PersistenceUnit.class);

    // Required Annotation is really present
    if (lAnnotation != null) {
      return lAnnotation;
    }
    // Configuration error. @PersistenceUnitConfig references a class that does not have annotation
    // @ContainerManagedConnection
    else {
      throw new JEAFSystemException(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG,
          pClass.getName(), lPersistenceUnitConfigClass.getName(), PersistenceUnit.class.getSimpleName());
    }
  }

  /**
   * Method resolves the container managed connection configuration from the passed persistence unit configuration.
   * 
   * @param pPersistenceUnitConfig Persistence unit configuration from which the container manager connection
   * configuration should be resolved. The parameter must not be null.
   * @param pClass Class that holds the persistence configuration. The parameter must not be null.
   * @return {@link ContainerManagedConnections} Container managed connection configuration or null if none if
   * configured.
   */
  private ContainerManagedConnections resolveContainerManagedConnectionConfig( PersistenceConfig pPersistenceConfig,
      Class<?> pClass ) {
    // Check parameters.
    Assert.assertNotNull(pPersistenceConfig, "pPersistenceConfig");
    Assert.assertNotNull(pClass, "pClass");

    Class<?> lContainerManagedConnectionDefinition = pPersistenceConfig.containerManagedConnectionDefinition();

    // Container managed connection is defined.
    ContainerManagedConnections lContainerManagedConnection;
    if (lContainerManagedConnectionDefinition.equals(Object.class) == false) {
      ContainerManagedConnections lAnnotation =
          lContainerManagedConnectionDefinition.getAnnotation(ContainerManagedConnections.class);

      // Required Annotation is really present
      if (lAnnotation != null) {
        // Container managed connections must define either JTA or non-JTA datasource, but not both or none.
        String lJTADataSource = lAnnotation.jtaDataSource();
        String lNonJTADataSource = lAnnotation.nonJTADataSource();
        StringTools lStringTools = Tools.getStringTools();

        // JTA and non-JTA datasource are both provided
        if (lStringTools.isRealString(lJTADataSource) && lStringTools.isRealString(lNonJTADataSource)) {
          throw new JEAFSystemException(PersistenceServiceProviderMessages.JTA_AND_NON_JTA_DATASOURCE_DEFINED,
              ContainerManagedConnections.class.getSimpleName(), pClass.getName());
        }
        // Neither JTA nor non-JTA datasource are provided.
        else if (lStringTools.isRealString(lJTADataSource) == false
            && lStringTools.isRealString(lNonJTADataSource) == false) {

          throw new JEAFSystemException(PersistenceServiceProviderMessages.NO_DATASOURCE_DEFINED,
              ContainerManagedConnections.class.getSimpleName(), pClass.getName());
        }
        // Datasource configuration is correct.
        else {
          lContainerManagedConnection = lAnnotation;
        }
      }
      // Configuration error. @PersistenceUnitConfig references a class that does not have annotation
      // @ContainerManagedConnection
      else {
        throw new JEAFSystemException(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG,
            pClass.getName(), lContainerManagedConnectionDefinition.getName(),
            ContainerManagedConnections.class.getSimpleName());
      }
    }
    // Persistence unit does not make use of container managed connections.
    else {
      lContainerManagedConnection = null;
    }
    return lContainerManagedConnection;
  }

  /**
   * Method resolves the application managed connection configuration from the passed persistence unit configuration.
   * 
   * @param pPersistenceUnitConfig Persistence unit configuration from which the application manager connection
   * configuration should be resolved. The parameter must not be null.
   * @return {@link ApplicationManagedConnections} Application managed connection configuration or null if none if
   * configured.
   */
  private ApplicationManagedConnections resolveApplicationManagedConnectionConfig( PersistenceConfig pPersistenceConfig,
      Class<?> pClass ) {

    // Check parameters.
    Assert.assertNotNull(pPersistenceConfig, "pPersistenceConfig");
    Assert.assertNotNull(pClass, "pClass");

    Class<?> lApplicationManagedConnectionDefinition = pPersistenceConfig.applicationManagedConnectionDefinition();

    // Application managed connection is defined.
    ApplicationManagedConnections lApplicationManagedConnection;
    if (lApplicationManagedConnectionDefinition.equals(Object.class) == false) {
      ApplicationManagedConnections lAnnotation =
          lApplicationManagedConnectionDefinition.getAnnotation(ApplicationManagedConnections.class);
      // Required annotation is really present
      if (lAnnotation != null) {
        lApplicationManagedConnection = lAnnotation;

        // Ensure that a connection URL is provided
        StringTools lTools = Tools.getStringTools();
        if (lTools.isRealString(lApplicationManagedConnection.connectionURL()) == false) {
          throw new JEAFSystemException(PersistenceServiceProviderMessages.JDBC_CONNECTION_URL_MISSING,
              ApplicationManagedConnections.class.getSimpleName(), lApplicationManagedConnectionDefinition.getName());
        }
      }
      // Configuration error. @PersistenceUnitConfig references a class that does not have annotation
      // @ApplicationManagedConnection
      else {
        throw new JEAFSystemException(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_CONFIG,
            pClass.getName(), lApplicationManagedConnectionDefinition.getName(),
            ApplicationManagedConnections.class.getSimpleName());
      }
    }
    // Persistence unit does not make use of application managed connections.
    else {
      lApplicationManagedConnection = null;
    }
    return lApplicationManagedConnection;
  }

  /**
   * Method resolves the connection pool configuration of the passed application managed connection.
   * 
   * @param pApplicationManagedConnection Application managed connection from which the connection pool configuration
   * should be resolved. The parameter may be null.
   * @return {@link ConnectionPool} Connection pool configuration or null if none is defined.
   */
  private ConnectionPool resolveConnectionPoolConfig( ApplicationManagedConnections pApplicationManagedConnection ) {
    ConnectionPool lConnectionPool;
    if (pApplicationManagedConnection != null) {
      lConnectionPool = pApplicationManagedConnection.connectionPool();
    }
    else {
      lConnectionPool = null;
    }
    return lConnectionPool;
  }

  /**
   * Method resolves the managed classes configuration for the passed persistence unit.
   * 
   * @param pPersistenceUnit Persistence unit configuration from which the managed classes should be resolved. The
   * parameter must not be null.
   * @return {@link ManagedClasses} Managed classes configuration of the persistence unit or null if none is defined.
   */
  private ManagedClasses resolveManagedClassesConfig( PersistenceUnit pPersistenceUnit ) {
    // Check parameter
    Assert.assertNotNull(pPersistenceUnit, "pPersistenceUnit");

    Class<?> lManagedClassesDefinition = pPersistenceUnit.managedClassesDefinition();

    // Managed classes are defined.
    ManagedClasses lManagedClasses;
    if (lManagedClassesDefinition.equals(Object.class) == false) {
      ManagedClasses lAnnotation = lManagedClassesDefinition.getAnnotation(ManagedClasses.class);

      // Required annotation is really present.
      if (lAnnotation != null) {
        lManagedClasses = lAnnotation;
      }
      // Configuration error. @PersistenceUnit references a class that does not have annotation @JPAManagedClasses
      else {
        throw new JEAFSystemException(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_UNIT,
            pPersistenceUnit.name(), lManagedClassesDefinition.getName(), ManagedClasses.class.getSimpleName());
      }
    }
    // Persistence unit does not define managed classes.
    else {
      lManagedClasses = null;
    }
    return lManagedClasses;
  }

  /**
   * Method resolves the mapping files configuration for the passed persistence unit.
   * 
   * @param pPersistenceUnitConfig Persistence unit configuration from which the mapping files configuration should be
   * resolved. The parameter must not be null.
   * @return {@link MappingFiles} Mapping files configuration of the persistence unit or null if none if defined.
   */
  private List<MappingFiles> resolveMappingFilesConfig( PersistenceUnit pPersistenceUnitConfig ) {
    // Check parameter
    Assert.assertNotNull(pPersistenceUnitConfig, "pPersistenceUnitConfig");

    Class<?>[] lMappingFilesDefinition = persistenceUnit.mappingFilesDefinition();

    // Mapping files are defined.
    List<MappingFiles> lMappingFiles = new ArrayList<>(lMappingFilesDefinition.length);
    for (int i = 0; i < lMappingFilesDefinition.length; i++) {
      Class<?> lNext = lMappingFilesDefinition[i];
      if (lNext.equals(Object.class) == false) {
        MappingFiles lAnnotation = lNext.getAnnotation(MappingFiles.class);
        if (lAnnotation != null) {
          lMappingFiles.add(lAnnotation);
        }
        // Configuration error. @PersistenceUnit references a class that does not have annotation @JPAMappingFiles
        else {
          throw new JEAFSystemException(PersistenceServiceProviderMessages.INVALID_REF_IN_PERSISTENCE_UNIT,
              pPersistenceUnitConfig.name(), lNext.getName(), MappingFiles.class.getSimpleName());
        }
      }
      // Persistence unit does not define mapping files.
      else {
        // Nothing to do.
      }
    }
    return lMappingFiles;
  }

  /**
   * Method returns if the persistence unit uses application managed connections or not.
   * 
   * @return boolean Method returns true if the persistence unit uses application managed connections and false in case
   * of container managed connections.
   */
  private boolean hasApplicationManagedConnections( ) {
    boolean lApplicationManagedConnections;
    if (applicationManagedConnectionConfig != null) {
      lApplicationManagedConnections = true;
    }
    else {
      lApplicationManagedConnections = false;
    }
    return lApplicationManagedConnections;
  }

  /**
   * @see PersistenceUnitInfo#getPersistenceUnitName()
   */
  @Override
  public String getPersistenceUnitName( ) {
    return persistenceUnit.name();
  }

  /**
   * @see PersistenceUnitInfo#getPersistenceProviderClassName()
   */
  @Override
  public String getPersistenceProviderClassName( ) {
    // Try to resolve the configured persistence provider if none is required then we will fall back to default and
    // return null.
    Class<? extends PersistenceProvider> lPersistenceProvider = persistenceUnit.persistenceProvider();

    // No specific persistence provider defined
    String lPersistenceProviderClassName;
    if (lPersistenceProvider.equals(PersistenceProvider.class)) {
      lPersistenceProviderClassName = null;
    }
    // Persistence provider is defined explicitly.
    else {
      lPersistenceProviderClassName = lPersistenceProvider.getName();
    }
    return lPersistenceProviderClassName;
  }

  /**
   * @see PersistenceUnitInfo#getTransactionType()
   */
  @Override
  public PersistenceUnitTransactionType getTransactionType( ) {
    // Depending on the connection mechanism we have to retrieve the information from different sources
    PersistenceUnitTransactionType lTransactionType;

    // Application managed connections
    if (this.hasApplicationManagedConnections() == true) {
      lTransactionType = applicationManagedConnectionConfig.transactionType();
    }
    // Container managed connections
    else {
      // It is ensured that there either is a JTA or a non-JTA datasource but never both or none.
      if (Tools.getStringTools().isRealString(containerManagedConnectionConfig.jtaDataSource())) {
        lTransactionType = PersistenceUnitTransactionType.JTA;
      }
      // Resource local transactions should be used.
      else {
        lTransactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
      }
    }
    return lTransactionType;
  }

  /**
   * @see PersistenceUnitInfo#getJtaDataSource()
   */
  @Override
  public DataSource getJtaDataSource( ) {
    // Using container managed connections
    DataSource lJTADataSource;
    if (containerManagedConnectionConfig != null) {
      // If a JTA data source is defined then it has to be looked up via JNDI
      String lJTADataSourceName = containerManagedConnectionConfig.jtaDataSource();
      if (Tools.getStringTools().isRealString(lJTADataSourceName)) {
        lJTADataSource = this.lookupDataSource(lJTADataSourceName);
      }
      else {
        lJTADataSource = null;
      }
    }
    // Persistence unit uses application managed connections, thus we will return null here.
    else {
      lJTADataSource = null;
    }
    return lJTADataSource;
  }

  /**
   * @see PersistenceUnitInfo#getNonJtaDataSource()
   */
  @Override
  public DataSource getNonJtaDataSource( ) {
    // Using container managed connections
    DataSource lJTADataSource;
    if (containerManagedConnectionConfig != null) {
      // If a JTA data source is defined then it has to be looked up via JNDI
      String lJTADataSourceName = containerManagedConnectionConfig.nonJTADataSource();
      if (Tools.getStringTools().isRealString(lJTADataSourceName)) {
        lJTADataSource = this.lookupDataSource(lJTADataSourceName);
      }
      else {
        lJTADataSource = null;
      }
    }
    // Persistence unit uses application managed connections, thus we will return null here.
    else {
      lJTADataSource = null;
    }
    return lJTADataSource;
  }

  private DataSource lookupDataSource( String lJTADataSourceName ) {
    try {
      InitialContext lInitialContext = new InitialContext();
      return (DataSource) lInitialContext.lookup(lJTADataSourceName);
    }
    catch (NamingException e) {
      throw new JEAFSystemException(PersistenceServiceProviderMessages.DATASOURCE_LOOKUP_FAILED, lJTADataSourceName,
          e.getMessage());
    }
  }

  /**
   * @see PersistenceUnitInfo#getMappingFileNames()
   */
  @Override
  public List<String> getMappingFileNames( ) {
    // Mapping files are defined.
    List<String> lMappingFiles;
    if (mappingFilesConfig.isEmpty() == false) {
      lMappingFiles = new ArrayList<>();
      for (MappingFiles lNext : mappingFilesConfig) {
        lMappingFiles.addAll(Arrays.asList(lNext.mappingFiles()));
      }
    }
    else {
      lMappingFiles = Collections.emptyList();
    }
    return lMappingFiles;
  }

  /**
   * @see PersistenceUnitInfo#getJarFileUrls()
   */
  @Override
  public List<URL> getJarFileUrls( ) {
    // This implementation is equivalent to org.hibernate.boot.archive.internal.ArchiveHelper.getURLFromPath(...)
    // and org.hibernate.jpa.boot.internal.PersistenceXmlParser.bindPersistenceUnit(...)

    int lSize = persistenceUnit.jarFiles().length;
    List<URL> lURLs = new ArrayList<>(lSize);
    for (int i = 0; i < lSize; i++) {
      // Resolve configured path
      String lPath = persistenceUnit.jarFiles()[i];
      // Try to create URL from configured jar file.
      try {
        lURLs.add(new URL(lPath));
      }
      // As it was not a real URL we give a file path a try.
      catch (MalformedURLException e) {
        try {
          // Consider it as a file path
          lURLs.add(new URL("file:" + lPath));
        }
        catch (MalformedURLException ee) {
          throw new IllegalArgumentException("Unable to find jar:" + lPath, ee);
        }
      }
    }
    return lURLs;
  }

  /**
   * @see PersistenceUnitInfo#getPersistenceUnitRootUrl()
   */
  @Override
  public URL getPersistenceUnitRootUrl( ) {
    return null;
  }

  /**
   * @see PersistenceUnitInfo#getManagedClassNames()
   */
  @Override
  public List<String> getManagedClassNames( ) {
    // Managed classes are defined
    List<String> lClassNames;
    if (managedClassesConfig != null) {
      Class<?>[] lManagedClasses = managedClassesConfig.managedClasses();
      int lSize = lManagedClasses.length;
      lClassNames = new ArrayList<>(lSize);
      for (int i = 0; i < lSize; i++) {
        lClassNames.add(lManagedClasses[i].getName());
      }
    }
    // No managed classes configured
    else {
      lClassNames = Collections.emptyList();
    }
    return lClassNames;
  }

  /**
   * @see PersistenceUnitInfo#excludeUnlistedClasses()
   */
  @Override
  public boolean excludeUnlistedClasses( ) {
    boolean lExcludeUnlistedClasses;
    if (this.hasApplicationManagedConnections() == true) {
      // Feature is only applicable to container manager persistence units.
      lExcludeUnlistedClasses = false;
    }
    else {
      lExcludeUnlistedClasses = containerManagedConnectionConfig.excludeUnlistedClasses();
    }
    return lExcludeUnlistedClasses;
  }

  /**
   * @see PersistenceUnitInfo#getSharedCacheMode()
   */
  @Override
  public SharedCacheMode getSharedCacheMode( ) {
    return persistenceUnit.sharedCacheMode();
  }

  /**
   * @see PersistenceUnitInfo#getValidationMode()
   */
  @Override
  public ValidationMode getValidationMode( ) {
    return persistenceUnit.validationMode();
  }

  /**
   * @see PersistenceUnitInfo#getProperties()
   */
  public Properties getProperties( ) {
    // Add generic properties
    ConfigurationProvider lHelper = XFun.getConfigurationProvider();
    Properties lProperties = new Properties();
    String lShowSQL = lHelper.replaceSystemProperties(persistenceUnit.showSQL());
    lProperties.put("hibernate.show_sql", lShowSQL);
    String lFormatSQL = lHelper.replaceSystemProperties(persistenceUnit.formatSQL());
    lProperties.put("hibernate.format_sql", lFormatSQL);

    // Add properties that were configured directly on the persistence unit as properties
    Property[] lPropertiesConfig = persistenceUnit.properties();
    for (int i = 0; i < lPropertiesConfig.length; i++) {
      Property lNextProperty = lPropertiesConfig[i];
      lProperties.put(lNextProperty.name(), lNextProperty.value());
    }

    // Add properties that are specific for application managed connections.
    if (this.hasApplicationManagedConnections() == true) {
      // Data base connection parameters are returned as properties.
      String lJDBCDriverName = applicationManagedConnectionConfig.jdbcDriver().getName();
      String lConnectionURL = lHelper.replaceSystemProperties(applicationManagedConnectionConfig.connectionURL());
      String lDialect = applicationManagedConnectionConfig.dialect();
      String lUsername = lHelper.replaceSystemProperties(applicationManagedConnectionConfig.username());
      String lPassword = lHelper.replaceSystemProperties(applicationManagedConnectionConfig.password());

      // Map connection settings to their matching persistence unit properties, but only if they have real values.
      StringTools lTools = Tools.getStringTools();
      lProperties.put("hibernate.connection.driver_class", lJDBCDriverName);
      if (lTools.isRealString(lDialect)) {
        lProperties.put("hibernate.dialect", lDialect);
      }
      lProperties.put("hibernate.connection.url", lConnectionURL);
      if (lTools.isRealString(lUsername)) {
        lProperties.put("hibernate.connection.username", lUsername);
      }
      if (lTools.isRealString(lPassword)) {
        lProperties.put("hibernate.connection.password", lPassword);
      }

      // Connection pool settings are also returned as properties.
      String lMinSize = lHelper.replaceSystemProperties(connectionPoolConfig.minSize());
      String lMaxSize = lHelper.replaceSystemProperties(connectionPoolConfig.maxSize());
      String lIncrementSize = lHelper.replaceSystemProperties(connectionPoolConfig.incrementSize());
      String lPreparedStatementCacheSize =
          lHelper.replaceSystemProperties(connectionPoolConfig.preparedStatementCacheSize());
      String lTimeout = lHelper.replaceSystemProperties(connectionPoolConfig.timeout());
      String lIdleTestPeriod = lHelper.replaceSystemProperties(connectionPoolConfig.idleTestPeriod());

      // Map connection pool settings to their matching c3p0 properties
      if (lTools.isRealString(lMinSize)) {
        lProperties.put("hibernate.c3p0.min_size", lMinSize);
      }
      if (lTools.isRealString(lMaxSize)) {
        lProperties.put("hibernate.c3p0.max_size", lMaxSize);
      }
      if (lTools.isRealString(lIncrementSize)) {
        lProperties.put("hibernate.c3p0.acquire_increment", lIncrementSize);
      }
      if (lTools.isRealString(lPreparedStatementCacheSize)) {
        lProperties.put("hibernate.c3p0.max_statements", lPreparedStatementCacheSize);
      }
      if (lTools.isRealString(lTimeout)) {
        lProperties.put("hibernate.c3p0.timeout", lTimeout);
      }
      if (lTools.isRealString(lIdleTestPeriod)) {
        lProperties.put("hibernate.c3p0.idle_test_period", lIdleTestPeriod);
      }

      // Add properties that were configured directly on annotation @ApplicationManagedConnections as properties
      lPropertiesConfig = applicationManagedConnectionConfig.properties();
      for (int i = 0; i < lPropertiesConfig.length; i++) {
        Property lNextProperty = lPropertiesConfig[i];
        lProperties.put(lNextProperty.name(), lNextProperty.value());
      }
    }
    // Resolve properties for container managed connections
    else {
      // Add properties that were configured directly on annotation @ContainerManagedConnections as properties
      lPropertiesConfig = containerManagedConnectionConfig.properties();
      for (int i = 0; i < lPropertiesConfig.length; i++) {
        Property lNextProperty = lPropertiesConfig[i];
        lProperties.put(lNextProperty.name(), lNextProperty.value());
      }
    }

    // Return properties.
    return lProperties;
  }

  /**
   * @see PersistenceUnitInfo#getPersistenceXMLSchemaVersion()
   */
  @Override
  public String getPersistenceXMLSchemaVersion( ) {
    return JPA_VERSION;
  }

  /**
   * @see PersistenceUnitInfo#getClassLoader()
   */
  @Override
  public ClassLoader getClassLoader( ) {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * @see PersistenceUnitInfo#getNewTempClassLoader()
   */
  @Override
  public ClassLoader getNewTempClassLoader( ) {
    return null;
  }

  @Override
  public void addTransformer( ClassTransformer pTransformer ) {
    // Transformers are not supported by this implementation.
    Assert.internalError("Transformers are not supported by this persistence unit implementation.");
  }
}