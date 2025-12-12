/*
 * anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 * 
 * Copyright 2004 - 2013 All rights reserved.
 */

package com.anaptecs.jeaf.spi.persistence.base;

import com.anaptecs.jeaf.spi.persistence.ClassID;
import com.anaptecs.jeaf.xfun.api.checks.Assert;
import com.anaptecs.jeaf.xfun.types.Base36;

/**
 * Class generates a new object id for every persistent object.
 * 
 * @author JEAF Development Team
 * @version 1.0
 */
public final class ObjectIDGenerator {
  /**
   * Constant defines the initial value for the sequence number of an object id.
   */
  private static final String INITIAL_SEQUENCE_VALUE = "0";

  /**
   * Constant defines that the maximum length for sequence numbers of object ids is limited to 6 digits.
   */
  public static final int MAX_SEQUENCE_LENGTH = 6;

  /**
   * Constant defines that the maximum length for a session id is limited to 6 digits.
   */
  public static final int MAX_SESSION_ID_LENGTH = 6;

  /**
   * Attribute contains the next value for an object id.
   */
  private Base36 currentSequenceNumber;

  /**
   * Attribute contains the actual session id.
   */
  private final Base36 sessionId;

  /**
   * Initialize object. Gets the class id of the invoking class and fetches the session id the next id gets initialized.
   * 
   * @param pCurrentSession Session id of the current session as string. The passed string must be a valid base 36
   * encoded number and must not have more characters than defined by constant MAX_SESSION_ID_LENGTH.
   */
  public ObjectIDGenerator( String pCurrentSession ) {
    // Initialize next object id.
    currentSequenceNumber = new Base36(INITIAL_SEQUENCE_VALUE, MAX_SEQUENCE_LENGTH);
    sessionId = new Base36(pCurrentSession, MAX_SESSION_ID_LENGTH);
  }

  /**
   * Method returns the next available object id for a business object with the passed class id.
   * 
   * @param pClassID ClassID of the business object for which a new object id is required. The parameter must not be
   * null.
   * @return String New object id for the business object with the passed class id. The method never returns null.
   */
  public String getNextObjectID( ClassID pClassID ) {
    // Check parameter.
    Assert.assertNotNull(pClassID, "pClassID");

    // In order to generate as less garbage as possible we use a string buffer to create the OID.
    StringBuffer lBuffer = new StringBuffer(MAX_SEQUENCE_LENGTH + MAX_SESSION_ID_LENGTH + ClassID.MAX_LENGTH);

    // To protect the object id generation from multi threading problems the increment of the next id has to be
    // synchronized.
    synchronized (this) {
      currentSequenceNumber = currentSequenceNumber.increment();
      lBuffer.append(currentSequenceNumber.toString());
    }

    lBuffer.append(sessionId.toString());
    lBuffer.append(pClassID.toString());
    return lBuffer.toString();
  }
}
