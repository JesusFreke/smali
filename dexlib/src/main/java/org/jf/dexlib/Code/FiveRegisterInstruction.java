package org.jf.dexlib.Code;

public interface FiveRegisterInstruction {
    byte getRegCount();
    byte getRegisterA();
    byte getRegisterD();
    byte getRegisterE();
    byte getRegisterF();
    byte getRegisterG();
}
