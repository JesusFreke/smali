package org.jf.dexlib2.dexbacked;

import java.nio.MappedByteBuffer;
import org.jf.util.ExceptionWithContext;
import org.jf.util.Hex;

public class MappedDexReader extends  DexReader<DexBuffer> {
  public final MappedDexBuffer mappedDexBuf;
  private int offset;

  public MappedDexReader(MappedDexBuffer mappedDexBuf, int offset) {
    super(new DexBuffer(new byte[] {}), 0);
    this.mappedDexBuf = mappedDexBuf;
    this.offset = offset;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public void setOffset(int offset) {
    this.offset = offset;
  }

  @Override
  public int readSleb128() {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result <= 0x7f) {
      result = (result << 25) >> 25;
    } else {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
      if (currentByteValue <= 0x7f) {
        result = (result << 18) >> 18;
      } else {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        result |= (currentByteValue & 0x7f) << 14;
        if (currentByteValue <= 0x7f) {
          result = (result << 11) >> 11;
        } else {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          result |= (currentByteValue & 0x7f) << 21;
          if (currentByteValue <= 0x7f) {
            result = (result << 4) >> 4;
          } else {
            currentByteValue = mappedByteBuf.get(end++) & 0xff;
            if (currentByteValue > 0x7f) {
              throw new ExceptionWithContext(
                  "Invalid sleb128 integer encountered at offset 0x%x", offset);
            }
            result |= currentByteValue << 28;
          }
        }
      }
    }

    offset = end - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public int peekSleb128Size() {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result > 0x7f) {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      if (currentByteValue > 0x7f) {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        if (currentByteValue > 0x7f) {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          if (currentByteValue > 0x7f) {
            currentByteValue = mappedByteBuf.get(end++) & 0xff;
            if (currentByteValue > 0x7f) {
              throw new ExceptionWithContext(
                  "Invalid sleb128 integer encountered at offset 0x%x", offset);
            }
          }
        }
      }
    }

    return end - (mappedDexBuf.baseOffset + offset);
  }

  @Override
  public int readSmallUleb128() {
    return readUleb128(false);
  }

  @Override
  public int peekSmallUleb128Size() {
    return peekUleb128Size(false);
  }

  private int readUleb128(boolean allowLarge) {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result > 0x7f) {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
      if (currentByteValue > 0x7f) {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        result |= (currentByteValue & 0x7f) << 14;
        if (currentByteValue > 0x7f) {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          result |= (currentByteValue & 0x7f) << 21;
          if (currentByteValue > 0x7f) {
            currentByteValue = mappedByteBuf.get(end++);

            // MSB shouldn't be set on last byte
            if (currentByteValue < 0) {
              throw new ExceptionWithContext(
                  "Invalid uleb128 integer encountered at offset 0x%x", offset);
            } else if ((currentByteValue & 0xf) > 0x07) {
              if (!allowLarge) {
                // for non-large uleb128s, we assume most significant bit of the result will not be
                // set, so that it can fit into a signed integer without wrapping
                throw new ExceptionWithContext(
                    "Encountered valid uleb128 that is out of range at offset 0x%x", offset);
              }
            }
            result |= currentByteValue << 28;
          }
        }
      }
    }

    offset = end - mappedDexBuf.baseOffset;
    return result;
  }

  private int peekUleb128Size(boolean allowLarge) {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result > 0x7f) {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      if (currentByteValue > 0x7f) {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        if (currentByteValue > 0x7f) {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          if (currentByteValue > 0x7f) {
            currentByteValue = mappedByteBuf.get(end++);

            // MSB shouldn't be set on last byte
            if (currentByteValue < 0) {
              throw new ExceptionWithContext(
                  "Invalid uleb128 integer encountered at offset 0x%x", offset);
            } else if ((currentByteValue & 0xf) > 0x07) {
              if (!allowLarge) {
                // for non-large uleb128s, we assume most significant bit of the result will not be
                // set, so that it can fit into a signed integer without wrapping
                throw new ExceptionWithContext(
                    "Encountered valid uleb128 that is out of range at offset 0x%x", offset);
              }
            }
          }
        }
      }
    }

    return end - (mappedDexBuf.baseOffset + offset);
  }

  /**
   * Reads a "large" uleb128. That is, one that may legitimately be greater than a signed int.
   *
   * <p>The value is returned as if it were signed. i.e. a value of 0xFFFFFFFF would be returned as
   * -1. It is up to the caller to handle the value appropriately.
   */
  @Override
  public int readLargeUleb128() {
    return readUleb128(true);
  }

  /**
   * Reads a "big" uleb128 that can legitimately be > 2^31. The value is returned as a signed
   * integer, with the expected semantics of re-interpreting an unsigned value as a signed value.
   *
   * @return The unsigned value, reinterpreted as a signed int
   */
  @Override
  public int readBigUleb128() {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result > 0x7f) {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
      if (currentByteValue > 0x7f) {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        result |= (currentByteValue & 0x7f) << 14;
        if (currentByteValue > 0x7f) {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          result |= (currentByteValue & 0x7f) << 21;
          if (currentByteValue > 0x7f) {
            currentByteValue = mappedByteBuf.get(end++);

            // MSB shouldn't be set on last byte
            if (currentByteValue < 0) {
              throw new ExceptionWithContext(
                  "Invalid uleb128 integer encountered at offset 0x%x", offset);
            }
            result |= currentByteValue << 28;
          }
        }
      }
    }

    offset = end - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public int peekBigUleb128Size() {
    int end = mappedDexBuf.baseOffset + offset;
    int currentByteValue;
    int result;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    result = mappedByteBuf.get(end++) & 0xff;
    if (result > 0x7f) {
      currentByteValue = mappedByteBuf.get(end++) & 0xff;
      if (currentByteValue > 0x7f) {
        currentByteValue = mappedByteBuf.get(end++) & 0xff;
        if (currentByteValue > 0x7f) {
          currentByteValue = mappedByteBuf.get(end++) & 0xff;
          if (currentByteValue > 0x7f) {
            currentByteValue = mappedByteBuf.get(end++);

            // MSB shouldn't be set on last byte
            if (currentByteValue < 0) {
              throw new ExceptionWithContext(
                  "Invalid uleb128 integer encountered at offset 0x%x", offset);
            }
          }
        }
      }
    }

    return end - (mappedDexBuf.baseOffset + offset);
  }

  @Override
  public void skipUleb128() {
    int end = mappedDexBuf.baseOffset + offset;
    byte currentByteValue;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    currentByteValue = mappedByteBuf.get(end++);
    if (currentByteValue < 0) { // if the MSB is set
      currentByteValue = mappedByteBuf.get(end++);
      if (currentByteValue < 0) { // if the MSB is set
        currentByteValue = mappedByteBuf.get(end++);
        if (currentByteValue < 0) { // if the MSB is set
          currentByteValue = mappedByteBuf.get(end++);
          if (currentByteValue < 0) { // if the MSB is set
            currentByteValue = mappedByteBuf.get(end++);
            if (currentByteValue < 0) {
              throw new ExceptionWithContext(
                  "Invalid uleb128 integer encountered at offset 0x%x", offset);
            }
          }
        }
      }
    }

    offset = end - mappedDexBuf.baseOffset;
  }

  @Override
  public int readSmallUint() {
    int o = offset;
    int result = mappedDexBuf.readSmallUint(o);
    offset = o + 4;
    return result;
  }

  @Override
  public int readSmallUint(int offset) {
    return mappedDexBuf.readSmallUint(offset);
  }

  @Override
  public int readOptionalUint() {
    int o = offset;
    int result = mappedDexBuf.readOptionalUint(o);
    offset = o + 4;
    return result;
  }

  @Override
  public int peekUshort() {
    return mappedDexBuf.readUshort(offset);
  }

  @Override
  public int readUshort() {
    int o = offset;
    int result = mappedDexBuf.readUshort(offset);
    offset = o + 2;
    return result;
  }

  @Override
  public int readUshort(int offset) {
    return mappedDexBuf.readUshort(offset);
  }

  @Override
  public int peekUbyte() {
    return mappedDexBuf.readUbyte(offset);
  }

  @Override
  public int readUbyte() {
    int o = offset;
    int result = mappedDexBuf.readUbyte(offset);
    offset = o + 1;
    return result;
  }

  @Override
  public int readUbyte(int offset) {
    return mappedDexBuf.readUbyte(offset);
  }

  @Override
  public long readLong() {
    int o = offset;
    long result = mappedDexBuf.readLong(offset);
    offset = o + 8;
    return result;
  }

  @Override
  public long readLong(int offset) {
    return mappedDexBuf.readLong(offset);
  }

  @Override
  public int readInt() {
    int o = offset;
    int result = mappedDexBuf.readInt(offset);
    offset = o + 4;
    return result;
  }

  @Override
  public int readInt(int offset) {
    return mappedDexBuf.readInt(offset);
  }

  @Override
  public int readShort() {
    int o = offset;
    int result = mappedDexBuf.readShort(offset);
    offset = o + 2;
    return result;
  }

  @Override
  public int readShort(int offset) {
    return mappedDexBuf.readShort(offset);
  }

  @Override
  public int readByte() {
    int o = offset;
    int result = mappedDexBuf.readByte(offset);
    offset = o + 1;
    return result;
  }

  @Override
  public int readByte(int offset) {
    return mappedDexBuf.readByte(offset);
  }

  @Override
  public int readSizedInt(int bytes) {
    int o = mappedDexBuf.baseOffset + offset;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    int result;
    switch (bytes) {
      case 4:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | (mappedByteBuf.get(o + 3) << 24);
        break;
      case 3:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | (mappedByteBuf.get(o + 2) << 16);
        break;
      case 2:
        result = (mappedByteBuf.get(o) & 0xff) | (mappedByteBuf.get(o + 1) << 8);
        break;
      case 1:
        result = mappedByteBuf.get(o);
        break;
      default:
        throw new ExceptionWithContext(
            "Invalid size %d for sized int at offset 0x%x", bytes, offset);
    }
    offset = o + bytes - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public int readSizedSmallUint(int bytes) {
    int o = mappedDexBuf.baseOffset + offset;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    int result = 0;
    switch (bytes) {
      case 4:
        int b = mappedByteBuf.get(o + 3);
        if (b < 0) {
          throw new ExceptionWithContext(
              "Encountered valid sized uint that is out of range at offset 0x%x", offset);
        }
        result = b << 24;
        // fall-through
      case 3:
        result |= (mappedByteBuf.get(o + 2) & 0xff) << 16;
        // fall-through
      case 2:
        result |= (mappedByteBuf.get(o + 1) & 0xff) << 8;
        // fall-through
      case 1:
        result |= (mappedByteBuf.get(o) & 0xff);
        break;
      default:
        throw new ExceptionWithContext(
            "Invalid size %d for sized uint at offset 0x%x", bytes, offset);
    }
    offset = o + bytes - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public int readSizedRightExtendedInt(int bytes) {
    int o = mappedDexBuf.baseOffset + offset;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    int result;
    switch (bytes) {
      case 4:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | (mappedByteBuf.get(o + 3) << 24);
        break;
      case 3:
        result =
            (mappedByteBuf.get(o) & 0xff) << 8
                | ((mappedByteBuf.get(o + 1) & 0xff) << 16)
                | (mappedByteBuf.get(o + 2) << 24);
        break;
      case 2:
        result = (mappedByteBuf.get(o) & 0xff) << 16 | (mappedByteBuf.get(o + 1) << 24);
        break;
      case 1:
        result = mappedByteBuf.get(o) << 24;
        break;
      default:
        throw new ExceptionWithContext(
            "Invalid size %d for sized, right extended int at offset 0x%x", bytes, offset);
    }
    offset = o + bytes - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public long readSizedRightExtendedLong(int bytes) {
    int o = mappedDexBuf.baseOffset + offset;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    long result;
    switch (bytes) {
      case 8:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 5) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 6) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 7)) << 56);
        break;
      case 7:
        result =
            (mappedByteBuf.get(o) & 0xff) << 8
                | ((mappedByteBuf.get(o + 1) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 2) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 5) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 6)) << 56);
        break;
      case 6:
        result =
            (mappedByteBuf.get(o) & 0xff) << 16
                | ((mappedByteBuf.get(o + 1) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 2) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 5)) << 56);
        break;
      case 5:
        result =
            (mappedByteBuf.get(o) & 0xffL) << 24
                | ((mappedByteBuf.get(o + 1) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 2) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 4)) << 56);
        break;
      case 4:
        result =
            (mappedByteBuf.get(o) & 0xffL) << 32
                | ((mappedByteBuf.get(o + 1) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 2) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 3)) << 56);
        break;
      case 3:
        result =
            (mappedByteBuf.get(o) & 0xffL) << 40
                | ((mappedByteBuf.get(o + 1) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 2)) << 56);
        break;
      case 2:
        result = (mappedByteBuf.get(o) & 0xffL) << 48 | (((long) mappedByteBuf.get(o + 1)) << 56);
        break;
      case 1:
        result = ((long) mappedByteBuf.get(o)) << 56;
        break;
      default:
        throw new ExceptionWithContext(
            "Invalid size %d for sized, right extended long at offset 0x%x", bytes, offset);
    }
    offset = o + bytes - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public long readSizedLong(int bytes) {
    int o = mappedDexBuf.baseOffset + offset;
    MappedByteBuffer mappedByteBuf = mappedDexBuf.mappedByteBuf;

    long result;
    switch (bytes) {
      case 8:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 5) & 0xffL) << 40)
                | ((mappedByteBuf.get(o + 6) & 0xffL) << 48)
                | (((long) mappedByteBuf.get(o + 7)) << 56);
        break;
      case 7:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 32)
                | ((mappedByteBuf.get(o + 5) & 0xffL) << 40)
                | ((long) mappedByteBuf.get(o + 6) << 48);
        break;
      case 6:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 24)
                | ((mappedByteBuf.get(o + 4) & 0xffL) << 32)
                | ((long) mappedByteBuf.get(o + 5) << 40);
        break;
      case 5:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | ((mappedByteBuf.get(o + 3) & 0xffL) << 24)
                | ((long) mappedByteBuf.get(o + 4) << 32);
        break;
      case 4:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | ((mappedByteBuf.get(o + 2) & 0xff) << 16)
                | (((long) mappedByteBuf.get(o + 3)) << 24);
        break;
      case 3:
        result =
            (mappedByteBuf.get(o) & 0xff)
                | ((mappedByteBuf.get(o + 1) & 0xff) << 8)
                | (mappedByteBuf.get(o + 2) << 16);
        break;
      case 2:
        result = (mappedByteBuf.get(o) & 0xff) | (mappedByteBuf.get(o + 1) << 8);
        break;
      case 1:
        result = mappedByteBuf.get(o);
        break;
      default:
        throw new ExceptionWithContext(
            "Invalid size %d for sized long at offset 0x%x", bytes, offset);
    }

    offset = o + bytes - mappedDexBuf.baseOffset;
    return result;
  }

  @Override
  public String readString(int utf16Length) {
    int[] ret = new int[1];
    String value =
        utf8BytesWithUtf16LengthToString(
            mappedDexBuf.mappedByteBuf, mappedDexBuf.baseOffset + offset, utf16Length, ret);
    offset += ret[0];
    return value;
  }

  @Override
  public int peekStringLength(int utf16Length) {
    int[] ret = new int[1];
    utf8BytesWithUtf16LengthToString(
        mappedDexBuf.mappedByteBuf, mappedDexBuf.baseOffset + offset, utf16Length, ret);
    return ret[0];
  }

  private static final ThreadLocal<char[]> localBuffer =
      new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
          // A reasonably sized initial value
          return new char[256];
        }
      };

  /**
   * Converts an array of UTF-8 bytes into a string.
   *
   * @param mappedByteBuffer; the MappedByteBuffer to convert
   * @param start the start index of the utf8 string to convert
   * @param utf16Length the number of utf16 characters in the string to decode
   * @param readLength If non-null, the first element will contain the number of bytes read after
   *     the method exits
   * @return non-null; the converted string
   */
  private static String utf8BytesWithUtf16LengthToString(
      MappedByteBuffer mappedByteBuffer, int start, int utf16Length, int[] readLength) {
    char[] chars = localBuffer.get();
    if (chars == null || chars.length < utf16Length) {
      chars = new char[utf16Length];
      localBuffer.set(chars);
    }
    int outAt = 0;

    int at = 0;
    for (at = start; utf16Length > 0; utf16Length--) {
      int v0 = mappedByteBuffer.get(at) & 0xFF;
      char out;
      switch (v0 >> 4) {
        case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
        case 0x04:
        case 0x05:
        case 0x06:
        case 0x07:
        {
          // 0XXXXXXX -- single-byte encoding
          if (v0 == 0) {
            // A single zero byte is illegal.
            return throwBadUtf8(v0, at);
          }
          out = (char) v0;
          at++;
          break;
        }
        case 0x0c:
        case 0x0d:
        {
          // 110XXXXX -- two-byte encoding
          int v1 = mappedByteBuffer.get(at + 1) & 0xFF;
          if ((v1 & 0xc0) != 0x80) {
            return throwBadUtf8(v1, at + 1);
          }
          int value = ((v0 & 0x1f) << 6) | (v1 & 0x3f);
          if ((value != 0) && (value < 0x80)) {
            /*
             * This should have been represented with
             * one-byte encoding.
             */
            return throwBadUtf8(v1, at + 1);
          }
          out = (char) value;
          at += 2;
          break;
        }
        case 0x0e:
        {
          // 1110XXXX -- three-byte encoding
          int v1 = mappedByteBuffer.get(at + 1) & 0xFF;
          if ((v1 & 0xc0) != 0x80) {
            return throwBadUtf8(v1, at + 1);
          }
          int v2 = mappedByteBuffer.get(at + 2) & 0xFF;
          if ((v2 & 0xc0) != 0x80) {
            return throwBadUtf8(v2, at + 2);
          }
          int value = ((v0 & 0x0f) << 12) | ((v1 & 0x3f) << 6) | (v2 & 0x3f);
          if (value < 0x800) {
            /*
             * This should have been represented with one- or
             * two-byte encoding.
             */
            return throwBadUtf8(v2, at + 2);
          }
          out = (char) value;
          at += 3;
          break;
        }
        default:
        {
          // 10XXXXXX, 1111XXXX -- illegal
          return throwBadUtf8(v0, at);
        }
      }
      chars[outAt] = out;
      outAt++;
    }

    if (readLength != null && readLength.length > 0) {
      readLength[0] = at - start;
      readLength[0] = at - start;
    }
    return new String(chars, 0, outAt);
  }

  /**
   * Helper for {@link #utf8BytesToString}, which throws the right exception for a bogus utf-8 byte.
   *
   * @param value the byte value
   * @param offset the file offset
   * @return never
   * @throws IllegalArgumentException always thrown
   */
  private static String throwBadUtf8(int value, int offset) {
    throw new IllegalArgumentException(
        "bad utf-8 byte " + Hex.u1(value) + " at offset " + Hex.u4(offset));
  }
}
