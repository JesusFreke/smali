/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code;

import org.jf.dexlib.Code.Format.Format;

import java.util.HashMap;

public enum Opcode
{
    NOP((byte)0x00, "nop", ReferenceType.none, Format.Format10x, Opcode.CAN_CONTINUE),
    MOVE((byte)0x01, "move", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_FROM16((byte)0x02, "move/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_16((byte)0x03, "move/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_WIDE((byte)0x04, "move-wide", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_FROM16((byte)0x05, "move-wide/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_16((byte)0x06, "move-wide/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_OBJECT((byte)0x07, "move-object", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_FROM16((byte)0x08, "move-object/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_16((byte)0x09, "move-object/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT((byte)0x0a, "move-result", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT_WIDE((byte)0x0b, "move-result-wide", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_RESULT_OBJECT((byte)0x0c, "move-result-object", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_EXCEPTION((byte)0x0d, "move-exception", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RETURN_VOID((byte)0x0e, "return-void", ReferenceType.none, Format.Format10x),
    RETURN((byte)0x0f, "return", ReferenceType.none, Format.Format11x),
    RETURN_WIDE((byte)0x10, "return-wide", ReferenceType.none, Format.Format11x),
    RETURN_OBJECT((byte)0x11, "return-object", ReferenceType.none, Format.Format11x),
    CONST_4((byte)0x12, "const/4", ReferenceType.none, Format.Format11n, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_16((byte)0x13, "const/16", ReferenceType.none, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST((byte)0x14, "const", ReferenceType.none, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_HIGH16((byte)0x15, "const/high16", ReferenceType.none, Format.Format21h, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_WIDE_16((byte)0x16, "const-wide/16", ReferenceType.none, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_32((byte)0x17, "const-wide/32", ReferenceType.none, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE((byte)0x18, "const-wide", ReferenceType.none, Format.Format51l, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_HIGH16((byte)0x19, "const-wide/high16", ReferenceType.none, Format.Format21h, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_STRING((byte)0x1a, "const-string", ReferenceType.string, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_STRING_JUMBO((byte)0x1b, "const-string/jumbo", ReferenceType.string, Format.Format31c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_CLASS((byte)0x1c, "const-class", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MONITOR_ENTER((byte)0x1d, "monitor-enter", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    MONITOR_EXIT((byte)0x1e, "monitor-exit", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    CHECK_CAST((byte)0x1f, "check-cast", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INSTANCE_OF((byte)0x20, "instance-of", ReferenceType.type, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ARRAY_LENGTH((byte)0x21, "array-length", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEW_INSTANCE((byte)0x22, "new-instance", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEW_ARRAY((byte)0x23, "new-array", ReferenceType.type, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    FILLED_NEW_ARRAY((byte)0x24, "filled-new-array", ReferenceType.type, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    FILLED_NEW_ARRAY_RANGE((byte)0x25, "filled-new-array/range", ReferenceType.type, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    FILL_ARRAY_DATA((byte)0x26, "fill-array-data", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    THROW((byte)0x27, "throw", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    GOTO((byte)0x28, "goto", ReferenceType.none, Format.Format10t),
    GOTO_16((byte)0x29, "goto/16", ReferenceType.none, Format.Format20t),
    GOTO_32((byte)0x2a, "goto/32", ReferenceType.none, Format.Format30t),
    PACKED_SWITCH((byte)0x2b, "packed-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    SPARSE_SWITCH((byte)0x2c, "sparse-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    CMPL_FLOAT((byte)0x2d, "cmpl-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_FLOAT((byte)0x2e, "cmpg-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPL_DOUBLE((byte)0x2f, "cmpl-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_DOUBLE((byte)0x30, "cmpg-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMP_LONG((byte)0x31, "cmp-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IF_EQ((byte)0x32, "if-eq", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_NE((byte)0x33, "if-ne", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LT((byte)0x34, "if-lt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GE((byte)0x35, "if-ge", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GT((byte)0x36, "if-gt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LE((byte)0x37, "if-le", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_EQZ((byte)0x38, "if-eqz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_NEZ((byte)0x39, "if-nez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LTZ((byte)0x3a, "if-ltz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GEZ((byte)0x3b, "if-gez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GTZ((byte)0x3c, "if-gtz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LEZ((byte)0x3d, "if-lez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    AGET((byte)0x44, "aget", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_WIDE((byte)0x45, "aget-wide", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AGET_OBJECT((byte)0x46, "aget-object", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BOOLEAN((byte)0x47, "aget-boolean", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BYTE((byte)0x48, "aget-byte", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_CHAR((byte)0x49, "aget-char", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_SHORT((byte)0x4a, "aget-short", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    APUT((byte)0x4b, "aput", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_WIDE((byte)0x4c, "aput-wide", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_OBJECT((byte)0x4d, "aput-object", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BOOLEAN((byte)0x4e, "aput-boolean", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BYTE((byte)0x4f, "aput-byte", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_CHAR((byte)0x50, "aput-char", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_SHORT((byte)0x51, "aput-short", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET((byte)0x52, "iget", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE((byte)0x53, "iget-wide", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IGET_OBJECT((byte)0x54, "iget-object", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_BOOLEAN((byte)0x55, "iget-boolean", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_BYTE((byte)0x56, "iget-byte", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_CHAR((byte)0x57, "iget-char", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_SHORT((byte)0x58, "iget-short", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT((byte)0x59, "iput", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_WIDE((byte)0x5a, "iput-wide", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_OBJECT((byte)0x5b, "iput-object", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_BOOLEAN((byte)0x5c, "iput-boolean", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_BYTE((byte)0x5d, "iput-byte", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_CHAR((byte)0x5e, "iput-char", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_SHORT((byte)0x5f, "iput-short", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET((byte)0x60, "sget", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_WIDE((byte)0x61, "sget-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SGET_OBJECT((byte)0x62, "sget-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_BOOLEAN((byte)0x63, "sget-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_BYTE((byte)0x64, "sget-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_CHAR((byte)0x65, "sget-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_SHORT((byte)0x66, "sget-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT((byte)0x67, "sput", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_WIDE((byte)0x68, "sput-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_OBJECT((byte)0x69, "sput-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_BOOLEAN((byte)0x6a, "sput-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_BYTE((byte)0x6b, "sput-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_CHAR((byte)0x6c, "sput-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_SHORT((byte)0x6d, "sput-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    INVOKE_VIRTUAL((byte)0x6e, "invoke-virtual", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER((byte)0x6f, "invoke-super", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT((byte)0x70, "invoke-direct", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_STATIC((byte)0x71, "invoke-static", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_INTERFACE((byte)0x72, "invoke-interface", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_RANGE((byte)0x74, "invoke-virtual/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_RANGE((byte)0x75, "invoke-super/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT_RANGE((byte)0x76, "invoke-direct/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_STATIC_RANGE((byte)0x77, "invoke-static/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_INTERFACE_RANGE((byte)0x78, "invoke-interface/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    NEG_INT((byte)0x7b, "neg-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NOT_INT((byte)0x7c, "not-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_LONG((byte)0x7d, "neg-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NOT_LONG((byte)0x7e, "not-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NEG_FLOAT((byte)0x7f, "neg-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_DOUBLE((byte)0x80, "neg-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_LONG((byte)0x81, "int-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_FLOAT((byte)0x82, "int-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_DOUBLE((byte)0x83, "int-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    LONG_TO_INT((byte)0x84, "long-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_FLOAT((byte)0x85, "long-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_DOUBLE((byte)0x86, "long-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_INT((byte)0x87, "float-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    FLOAT_TO_LONG((byte)0x88, "float-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_DOUBLE((byte)0x89, "float-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_INT((byte)0x8a, "double-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DOUBLE_TO_LONG((byte)0x8b, "double-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_FLOAT((byte)0x8c, "double-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_BYTE((byte)0x8d, "int-to-byte", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_CHAR((byte)0x8e, "int-to-char", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_SHORT((byte)0x8f, "int-to-short", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT((byte)0x90, "add-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT((byte)0x91, "sub-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT((byte)0x92, "mul-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT((byte)0x93, "div-int", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT((byte)0x94, "rem-int", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT((byte)0x95, "and-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT((byte)0x96, "or-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT((byte)0x97, "xor-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT((byte)0x98, "shl-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT((byte)0x99, "shr-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT((byte)0x9a, "ushr-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG((byte)0x9b, "add-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG((byte)0x9c, "sub-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG((byte)0x9d, "mul-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG((byte)0x9e, "div-long", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG((byte)0x9f, "rem-long", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG((byte)0xa0, "and-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG((byte)0xa1, "or-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG((byte)0xa2, "xor-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG((byte)0xa3, "shl-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG((byte)0xa4, "shr-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG((byte)0xa5, "ushr-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT((byte)0xa6, "add-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_FLOAT((byte)0xa7, "sub-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_FLOAT((byte)0xa8, "mul-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_FLOAT((byte)0xa9, "div-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_FLOAT((byte)0xaa, "rem-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_DOUBLE((byte)0xab, "add-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE((byte)0xac, "sub-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE((byte)0xad, "mul-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE((byte)0xae, "div-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE((byte)0xaf, "rem-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_2ADDR((byte)0xb0, "add-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT_2ADDR((byte)0xb1, "sub-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_2ADDR((byte)0xb2, "mul-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_2ADDR((byte)0xb3, "div-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_2ADDR((byte)0xb4, "rem-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_2ADDR((byte)0xb5, "and-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_2ADDR((byte)0xb6, "or-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_2ADDR((byte)0xb7, "xor-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_2ADDR((byte)0xb8, "shl-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_2ADDR((byte)0xb9, "shr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_2ADDR((byte)0xba, "ushr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG_2ADDR((byte)0xbb, "add-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG_2ADDR((byte)0xbc, "sub-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG_2ADDR((byte)0xbd, "mul-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG_2ADDR((byte)0xbe, "div-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG_2ADDR((byte)0xbf, "rem-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG_2ADDR((byte)0xc0, "and-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG_2ADDR((byte)0xc1, "or-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG_2ADDR((byte)0xc2, "xor-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG_2ADDR((byte)0xc3, "shl-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG_2ADDR((byte)0xc4, "shr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG_2ADDR((byte)0xc5, "ushr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT_2ADDR((byte)0xc6, "add-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_FLOAT_2ADDR((byte)0xc7, "sub-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_FLOAT_2ADDR((byte)0xc8, "mul-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_FLOAT_2ADDR((byte)0xc9, "div-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_FLOAT_2ADDR((byte)0xca, "rem-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_DOUBLE_2ADDR((byte)0xcb, "add-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE_2ADDR((byte)0xcc, "sub-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE_2ADDR((byte)0xcd, "mul-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE_2ADDR((byte)0xce, "div-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE_2ADDR((byte)0xcf, "rem-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_LIT16((byte)0xd0, "add-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT((byte)0xd1, "rsub-int", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT16((byte)0xd2, "mul-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT16((byte)0xd3, "div-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT16((byte)0xd4, "rem-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT16((byte)0xd5, "and-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT16((byte)0xd6, "or-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT16((byte)0xd7, "xor-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT_LIT8((byte)0xd8, "add-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT_LIT8((byte)0xd9, "rsub-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT8((byte)0xda, "mul-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT8((byte)0xdb, "div-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT8((byte)0xdc, "rem-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT8((byte)0xdd, "and-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT8((byte)0xde, "or-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT8((byte)0xdf, "xor-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_LIT8((byte)0xe0, "shl-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_LIT8((byte)0xe1, "shr-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_LIT8((byte)0xe2, "ushr-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),


    EXECUTE_INLINE((byte)0xee, "execute-inline", ReferenceType.none,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    EXECUTE_INLINE_RANGE((byte)0xef, "execute-inline/range", ReferenceType.none,  Format.Format3rms,  Opcode.ODEX_ONLY | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT_EMPTY((byte)0xf0, "invoke-direct-empty", ReferenceType.method,  Format.Format35s, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    IGET_QUICK((byte)0xf2, "iget-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET_WIDE_QUICK((byte)0xf3, "iget-wide-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_WIDE_REGISTER),
    IGET_OBJECT_QUICK((byte)0xf4, "iget-object-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_QUICK((byte)0xf5, "iput-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_WIDE_QUICK((byte)0xf6, "iput-wide-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_OBJECT_QUICK((byte)0xf7, "iput-object-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    INVOKE_VIRTUAL_QUICK((byte)0xf8, "invoke-virtual-quick", ReferenceType.none,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_QUICK_RANGE((byte)0xf9, "invoke-virtual-quick/range", ReferenceType.none,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK((byte)0xfa, "invoke-super-quick", ReferenceType.none,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK_RANGE((byte)0xfb, "invoke-super-quick/range", ReferenceType.none,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT);


    private static Opcode[] opcodesByValue;
    private static HashMap<Integer, Opcode> opcodesByName;

    //if the instruction can throw an exception
    public static final int CAN_THROW = 0x1;
    //if the instruction is an odex only instruction
    public static final int ODEX_ONLY = 0x2;
    //if execution can continue to the next instruction
    public static final int CAN_CONTINUE = 0x4;
    //if the instruction sets the "hidden" result register
    public static final int SETS_RESULT = 0x8;
    //if the instruction sets the value of it's first register
    public static final int SETS_REGISTER = 0x16;
    //if the instruction sets the value of it's first register to a wide type
    public static final int SETS_WIDE_REGISTER = 0x32;

    static {
        opcodesByValue = new Opcode[256];
        opcodesByName = new HashMap<Integer, Opcode>();

        for (Opcode opcode: Opcode.values()) {
            opcodesByValue[opcode.value & 0xFF] = opcode;
            opcodesByName.put(opcode.name.hashCode(), opcode);
        }
    }

    public static Opcode getOpcodeByName(String opcodeName) {
        return opcodesByName.get(opcodeName.toLowerCase().hashCode());
    }

    public static Opcode getOpcodeByValue(byte opcodeValue) {
        return opcodesByValue[opcodeValue & 0xFF];
    }

    public final byte value;
    public final String name;
    public final ReferenceType referenceType;
    public final Format format;
    public final int flags;

    Opcode(byte opcodeValue, String opcodeName, ReferenceType referenceType, Format format) {
        this(opcodeValue, opcodeName, referenceType, format, 0);
    }

    Opcode(byte opcodeValue, String opcodeName, ReferenceType referenceType, Format format, int flags) {
        this.value = opcodeValue;
        this.name = opcodeName;
        this.referenceType = referenceType;
        this.format = format;
        this.flags = flags;
    }

    public final boolean canThrow() {
        return (flags & CAN_THROW) != 0;
    }

    public final boolean odexOnly() {
        return (flags & ODEX_ONLY) != 0;
    }

    public final boolean canContinue() {
        return (flags & CAN_CONTINUE) != 0;
    }

    public final boolean setsResult() {
        return (flags & SETS_RESULT) != 0;
    }

    public final boolean setsRegister() {
        return (flags & SETS_REGISTER) != 0;
    }

    public final boolean setsWideRegister() {
        return (flags & SETS_WIDE_REGISTER) != 0;
    }
}
