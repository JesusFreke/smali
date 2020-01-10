package org.jf.dexlib2.dexbacked;

import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import org.jf.util.ExceptionWithContext;

public class MappedDexBuffer extends DexBuffer {
  final MappedByteBuffer mappedByteBuf;
  final int baseOffset;

  public MappedDexBuffer(MappedByteBuffer mappedByteBuf) {
    this(mappedByteBuf, 0);
  }

  public MappedDexBuffer(MappedByteBuffer mappedByteBuf, int offset) {
    super(new byte[] {});
    this.mappedByteBuf = mappedByteBuf;
    this.mappedByteBuf.order(ByteOrder.LITTLE_ENDIAN);
    this.baseOffset = offset;
  }

  @Override
  public int readSmallUint(int offset) {
    offset += baseOffset;
    int result = mappedByteBuf.getInt(offset);
    if (result < 0) {
      throw new ExceptionWithContext(
          "Encountered small uint that is out of range at offset 0x%x", offset);
    }
    return result;
  }

  @Override
  public int readOptionalUint(int offset) {
    offset += baseOffset;
    int result = mappedByteBuf.getInt(offset);
    if (result < -1) {
      throw new ExceptionWithContext(
          "Encountered optional uint that is out of range at offset 0x%x", offset);
    }
    return result;
  }

  @Override
  public int readUshort(int offset) {
    offset += baseOffset;
    return mappedByteBuf.getShort(offset) & 0xffff;
  }

  @Override
  public int readUbyte(int offset) {
    return mappedByteBuf.get(offset + baseOffset) & 0xff;
  }

  @Override
  public long readLong(int offset) {
    offset += baseOffset;
    return mappedByteBuf.getLong(offset);
  }

  @Override
  public int readLongAsSmallUint(int offset) {
    offset += baseOffset;
    long result = mappedByteBuf.getLong(offset);
    if (result < 0 || result > Integer.MAX_VALUE) {
      throw new ExceptionWithContext("Encountered out-of-range ulong at offset 0x%x", offset);
    }
    return (int) result;
  }

  @Override
  public int readInt(int offset) {
    offset += baseOffset;
    return mappedByteBuf.getInt(offset);
  }

  @Override
  public int readShort(int offset) {
    offset += baseOffset;
    return mappedByteBuf.getShort(offset);
  }

  @Override
  public int readByte(int offset) {
    return mappedByteBuf.get(baseOffset + offset);
  }

  @Override
  public byte[] readByteRange(int start, int length) {
    byte[] buf = new byte[length];
    mappedByteBuf.get(buf, baseOffset + start, length);
    return buf;
  }

  @Override
  public DexReader<DexBuffer> readerAt(int offset) {
    return new MappedDexReader(this, offset);
  }

  @Override
  public int getBaseOffset() {
    return baseOffset;
  }
}
