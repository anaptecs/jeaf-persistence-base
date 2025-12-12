/*
 * anaptecs GmbH, Burgstr. 96, 72764 Reutlingen, Germany
 *
 * Copyright 2004 - 2013 All rights reserved.
 */
package com.anaptecs.jeaf.spi.persistence.base.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anaptecs.jeaf.spi.persistence.ClassID;
import com.anaptecs.jeaf.spi.persistence.PersistentObject;
import com.anaptecs.jeaf.spi.persistence.base.ObjectIDGenerator;
import com.anaptecs.jeaf.xfun.types.Base36;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Class tests the generation of a unique object id on base36 Transfers a classid and gets the generatied object id
 *
 * @author JEAF Development Team
 * @version 1.0
 *
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ObjectIDGeneratorTest {
  /**
   * Test of method getNextObjectId(). Creates a Object of the ObjectIDGenerator, whitch gets the next ObjectID.
   *
   * @throws Exception if an error occurs during the execution of the test case.
   */
  @Test
  @Order(10)
  public void testGetNextObjectId( ) throws Exception {
    ObjectIDGenerator lObjectIDGenerator = new ObjectIDGenerator("123456");

    final ClassID lClassID = ClassID.createClassID(432, MyTestPO.class);
    String lObjectID = lObjectIDGenerator.getNextObjectID(lClassID);

    assertTrue(lObjectID.startsWith(new Base36("1").toString()),
        "Generated object id does not start with the initial sequence number");
    assertTrue(lObjectID.endsWith(lClassID.toString()), "Generated object id does not end with class id.");
    assertEquals(15, lObjectID.length());
  }
}

class MyTestPO extends PersistentObject {
  static final int CLASS_ID = 432;

  @Override
  public ClassID getClassID( ) {
    return null;
  }
}
