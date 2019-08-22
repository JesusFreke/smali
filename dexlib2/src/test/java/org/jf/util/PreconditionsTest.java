package org.jf.util;

import java.util.ArrayList;
import java.util.List;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.dexlib2.util.Preconditions;
import org.junit.Assert;
import org.junit.Test;

public class PreconditionsTest {

  @Test
  public void checkArrayPayloadElements() {
    int intSize = 4;
    int bigNumber = 16843071;
    List<Number> numbers = new ArrayList<>();
    numbers.add(bigNumber);

    // Shouldn't throw any arrays, payload is correct.
    Preconditions.checkArrayPayloadElements(intSize, numbers);
  }
}
