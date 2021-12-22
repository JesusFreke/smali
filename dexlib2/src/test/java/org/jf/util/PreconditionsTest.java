package org.jf.util;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.util.Preconditions;
import org.junit.Assert;
import org.junit.Test;

public class PreconditionsTest {

  private void verifyArrayPayloadElementIsValid(int elementWidth, long value) {
    Preconditions.checkArrayPayloadElements(elementWidth, ImmutableList.of(value));
  }

  private void verifyArrayPayloadElementIsInvalid(int elementWidth, long value) {
    try {
      Preconditions.checkArrayPayloadElements(elementWidth, ImmutableList.of(value));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      // expected exception
    }
  }

  @Test
  public void checkArrayPayloadElements() {
    verifyArrayPayloadElementIsValid(8, Long.MAX_VALUE);
    verifyArrayPayloadElementIsValid(8, Long.MIN_VALUE);
    verifyArrayPayloadElementIsValid(4, Integer.MAX_VALUE);
    verifyArrayPayloadElementIsValid(4, Integer.MIN_VALUE);
    verifyArrayPayloadElementIsValid(2, Short.MAX_VALUE);
    verifyArrayPayloadElementIsValid(2, Short.MIN_VALUE);
    verifyArrayPayloadElementIsValid(2, Character.MAX_VALUE);
    verifyArrayPayloadElementIsValid(2, Character.MIN_VALUE);
    verifyArrayPayloadElementIsValid(1, Byte.MAX_VALUE);
    verifyArrayPayloadElementIsValid(1, Byte.MIN_VALUE);

    verifyArrayPayloadElementIsInvalid(4, ((long) Integer.MAX_VALUE) + 1);
    verifyArrayPayloadElementIsInvalid(4, ((long) Integer.MIN_VALUE) - 1);
    verifyArrayPayloadElementIsInvalid(2, ((long) Short.MIN_VALUE) - 1);
    //Since short and character have the same size, but different ranges
    // and cannot be distinguished here, the valid interval is
    //[Short.MIN_VALUE, Character.MAX_VALUE], i.e. [-32768, 65535]
    verifyArrayPayloadElementIsInvalid(2, ((long) Character.MAX_VALUE) + 1);
    verifyArrayPayloadElementIsInvalid(2, ((long) Short.MIN_VALUE) - 1);
    verifyArrayPayloadElementIsInvalid(1, ((long) Byte.MAX_VALUE) + 1);
    verifyArrayPayloadElementIsInvalid(1, ((long) Byte.MIN_VALUE) - 1);
  }
}
