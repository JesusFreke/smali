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

package org.JesusFreke.dexlib.code;

import java.util.ArrayList;
import java.util.HashMap;

public enum Opcode
{        
    NOP((byte)0x00, "NOP", (byte)2, ReferenceType.none, "10x"),
    MOVE((byte)0x01, "MOVE", (byte)2, ReferenceType.none, "12x"),
    MOVE_FROM16((byte)0x02, "MOVE/FROM16", (byte)4, ReferenceType.none, "22x"),
    MOVE_16((byte)0x03, "MOVE/16", (byte)6, ReferenceType.none, "32x"),
    MOVE_WIDE((byte)0x04, "MOVE-WIDE", (byte)2, ReferenceType.none, "12x"),
    MOVE_WIDE_FROM16((byte)0x05, "MOVE-WIDE/FROM16", (byte)4, ReferenceType.none, "22x"),
    MOVE_WIDE_16((byte)0x06, "MOVE-WIDE/16", (byte)6, ReferenceType.none, "32x"),
    MOVE_OBJECT((byte)0x07, "MOVE-OBJECT", (byte)2, ReferenceType.none, "12x"),
    MOVE_OBJECT_FROM16((byte)0x08, "MOVE-OBJECT/FROM16", (byte)4, ReferenceType.none, "22x"),
    MOVE_OBJECT_16((byte)0x09, "MOVE-OBJECT/16", (byte)6, ReferenceType.none, "32x"),
    MOVE_RESULT((byte)0x0a, "MOVE-RESULT", (byte)2, ReferenceType.none, "11x"),
    MOVE_RESULT_WIDE((byte)0x0b, "MOVE-RESULT-WIDE", (byte)2, ReferenceType.none, "11x"),
    MOVE_RESULT_OBJECT((byte)0x0c, "MOVE-RESULT-OBJECT", (byte)2, ReferenceType.none, "11x"),
    MOVE_EXCEPTION((byte)0x0d, "MOVE-EXCEPTION", (byte)2, ReferenceType.none, "11x"),
    RETURN_VOID((byte)0x0e, "RETURN-VOID", (byte)2, ReferenceType.none, "10x"),
    RETURN((byte)0x0f, "RETURN", (byte)2, ReferenceType.none, "11x"),
    RETURN_WIDE((byte)0x10, "RETURN-WIDE", (byte)2, ReferenceType.none, "11x"),
    RETURN_OBJECT((byte)0x11, "RETURN-OBJECT", (byte)2, ReferenceType.none, "11x"),
    CONST_4((byte)0x12, "CONST-4", (byte)2, ReferenceType.none, "11n"),
    CONST_16((byte)0x13, "CONST/16", (byte)4, ReferenceType.none, "21s"),
    CONST((byte)0x14, "CONST", (byte)6, ReferenceType.none, "31i"),
    CONST_HIGH16((byte)0x15, "CONST/HIGH16", (byte)4, ReferenceType.none, "21h"),
    CONST_WIDE_16((byte)0x16, "CONST-WIDE/16", (byte)4, ReferenceType.none, "21s"),
    CONST_WIDE_32((byte)0x17, "CONST-WIDE/32", (byte)6, ReferenceType.none, "31i"),
    CONST_WIDE((byte)0x18, "CONST-WIDE", (byte)10, ReferenceType.none, "51l"),
    CONST_WIDE_HIGH16((byte)0x19, "CONST-WIDE/HIGH16", (byte)4, ReferenceType.none, "21h"),
    CONST_STRING((byte)0x1a, "CONST-STRING", (byte)4, ReferenceType.string, "21c"),
    CONST_STRING_JUMBO((byte)0x1b, "CONST-STRING/JUMBO", (byte)6, ReferenceType.string, "31c"),
    CONST_CLASS((byte)0x1c, "CONST-CLASS", (byte)4, ReferenceType.type, "21c"),
    MONITOR_ENTER((byte)0x1d, "MONITOR-ENTER", (byte)2, ReferenceType.none, "11x"),
    MONITOR_EXIT((byte)0x1e, "MONITOR-EXIT", (byte)2, ReferenceType.none, "11x"),
    CHECK_CAST((byte)0x1f, "CHECK-CAST", (byte)4, ReferenceType.type, "21c"),
    INSTANCE_OF((byte)0x20, "INSTANCE-OF", (byte)4, ReferenceType.type, "22c"),
    ARRAY_LENGTH((byte)0x21, "ARRAY-LENGTH", (byte)2, ReferenceType.none, "12x"),
    NEW_INSTANCE((byte)0x22, "NEW-INSTANCE", (byte)4, ReferenceType.type, "21c"),
    NEW_ARRAY((byte)0x23, "NEW-ARRAY", (byte)4, ReferenceType.type, "22c"),
    FILLED_NEW_ARRAY((byte)0x24, "FILLED-NEW-ARRAY", (byte)6, ReferenceType.type, "35c"),
    FILLED_NEW_ARRAY_RANGE((byte)0x25, "FILLED-NEW-ARRAY-RANGE", (byte)6, ReferenceType.type, "3rc"),
    FILL_ARRAY_DATA((byte)0x26, "FILL-ARRAY-DATA", (byte)6, ReferenceType.none, "31t"),
    THROW((byte)0x27, "THROW", (byte)2, ReferenceType.none, "11x"),
    GOTO((byte)0x28, "GOTO", (byte)2, ReferenceType.none, "10t"),
    GOTO_16((byte)0x29, "GOTO/16", (byte)4, ReferenceType.none, "20t"),
    GOTO_32((byte)0x2a, "GOTO/32", (byte)6, ReferenceType.none, "30t"),
    PACKED_SWITCH((byte)0x2b, "PACKED-SWITCH", (byte)6, ReferenceType.none, "31t"),
    SPARSE_SWITCH((byte)0x2c, "SPARSE-SWITCH", (byte)6, ReferenceType.none, "31t"),
    CMPL_FLOAT((byte)0x2d, "CMPL-FLOAT", (byte)4, ReferenceType.none, "23x"),
    CMPG_FLOAT((byte)0x2e, "CMPG-FLOAT", (byte)4, ReferenceType.none, "23x"),
    CMPL_DOUBLE((byte)0x2f, "CMPL-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    CMPG_DOUBLE((byte)0x30, "CMPG-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    CMP_LONG((byte)0x31, "CMP-LONG", (byte)4, ReferenceType.none, "23x"),
    IF_EQ((byte)0x32, "IF-EQ", (byte)4, ReferenceType.none, "22t"),
    IF_NE((byte)0x33, "IF-NE", (byte)4, ReferenceType.none, "22t"),
    IF_LT((byte)0x34, "IF-LT", (byte)4, ReferenceType.none, "22t"),
    IF_GE((byte)0x35, "IF-GE", (byte)4, ReferenceType.none, "22t"),
    IF_GT((byte)0x36, "IF-GT", (byte)4, ReferenceType.none, "22t"),
    IF_LE((byte)0x37, "IF-LE", (byte)4, ReferenceType.none, "22t"),
    IF_EQZ((byte)0x38, "IF-EQZ", (byte)4, ReferenceType.none, "21t"),
    IF_NEZ((byte)0x39, "IF-NEZ", (byte)4, ReferenceType.none, "21t"),
    IF_LTZ((byte)0x3a, "IF-LTZ", (byte)4, ReferenceType.none, "21t"),
    IF_GEZ((byte)0x3b, "IF-GEZ", (byte)4, ReferenceType.none, "21t"),
    IF_GTZ((byte)0x3c, "IF-GTZ", (byte)4, ReferenceType.none, "21t"),
    IF_LEZ((byte)0x3d, "IF-LEZ", (byte)4, ReferenceType.none, "21t"),
    AGET((byte)0x44, "AGET", (byte)4, ReferenceType.none, "23x"),
    AGET_WIDE((byte)0x45, "AGET-WIDE", (byte)4, ReferenceType.none, "23x"),
    AGET_OBJECT((byte)0x46, "AGET-OBJECT", (byte)4, ReferenceType.none, "23x"),
    AGET_BOOLEAN((byte)0x47, "AGET-BOOLEAN", (byte)4, ReferenceType.none, "23x"),
    AGET_BYTE((byte)0x48, "AGET-BYTE", (byte)4, ReferenceType.none, "23x"),
    AGET_CHAR((byte)0x49, "AGET-CHAR", (byte)4, ReferenceType.none, "23x"),
    AGET_SHORT((byte)0x4a, "AGET-SHORT", (byte)4, ReferenceType.none, "23x"),
    APUT((byte)0x4b, "APUT", (byte)4, ReferenceType.none, "23x"),
    APUT_WIDE((byte)0x4c, "APUT-WIDE", (byte)4, ReferenceType.none, "23x"),
    APUT_OBJECT((byte)0x4d, "APUT-OBJECT", (byte)4, ReferenceType.none, "23x"),
    APUT_BOOLEAN((byte)0x4e, "APUT-BOOLEAN", (byte)4, ReferenceType.none, "23x"),
    APUT_BYTE((byte)0x4f, "APUT-BYTE", (byte)4, ReferenceType.none, "23x"),
    APUT_CHAR((byte)0x50, "APUT-CHAR", (byte)4, ReferenceType.none, "23x"),
    APUT_SHORT((byte)0x51, "APUT-SHORT", (byte)4, ReferenceType.none, "23x"),
    IGET((byte)0x52, "IGET", (byte)4, ReferenceType.field, "22c"),
    IGET_WIDE((byte)0x53, "IGET-WIDE", (byte)4, ReferenceType.field, "22c"),
    IGET_OBJECT((byte)0x54, "IGET-OBJECT", (byte)4, ReferenceType.field, "22c"),
    IGET_BOOLEAN((byte)0x55, "IGET-BOOLEAN", (byte)4, ReferenceType.field, "22c"),
    IGET_BYTE((byte)0x56, "IGET-BYTE", (byte)4, ReferenceType.field, "22c"),
    IGET_CHAR((byte)0x57, "IGET-CHAR", (byte)4, ReferenceType.field, "22c"),
    IGET_SHORT((byte)0x58, "IGET-SHORT", (byte)4, ReferenceType.field, "22c"),
    IPUT((byte)0x59, "IPUT", (byte)4, ReferenceType.field, "22c"),
    IPUT_WIDE((byte)0x5a, "IPUT-WIDE", (byte)4, ReferenceType.field, "22c"),
    IPUT_OBJECT((byte)0x5b, "IPUT-OBJECT", (byte)4, ReferenceType.field, "22c"),
    IPUT_BOOLEAN((byte)0x5c, "IPUT-BOOLEAN", (byte)4, ReferenceType.field, "22c"),
    IPUT_BYTE((byte)0x5d, "IPUT-BYTE", (byte)4, ReferenceType.field, "22c"),
    IPUT_CHAR((byte)0x5e, "IPUT-CHAR", (byte)4, ReferenceType.field, "22c"),
    IPUT_SHORT((byte)0x5f, "IPUT-SHORT", (byte)4, ReferenceType.field, "22c"),
    SGET((byte)0x60, "SGET", (byte)4, ReferenceType.field, "21c"),
    SGET_WIDE((byte)0x61, "SGET-WIDE", (byte)4, ReferenceType.field, "21c"),
    SGET_OBJECT((byte)0x62, "SGET-OBJECT", (byte)4, ReferenceType.field, "21c"),
    SGET_BOOLEAN((byte)0x63, "SGET-BOOLEAN", (byte)4, ReferenceType.field, "21c"),
    SGET_BYTE((byte)0x64, "SGET-BYTE", (byte)4, ReferenceType.field, "21c"),
    SGET_CHAR((byte)0x65, "SGET-CHAR", (byte)4, ReferenceType.field, "21c"),
    SGET_SHORT((byte)0x66, "SGET-SHORT", (byte)4, ReferenceType.field, "21c"),
    SPUT((byte)0x67, "SPUT", (byte)4, ReferenceType.field, "21c"),
    SPUT_WIDE((byte)0x68, "SPUT-WIDE", (byte)4, ReferenceType.field, "21c"),
    SPUT_OBJECT((byte)0x69, "SPUT-OBJECT", (byte)4, ReferenceType.field, "21c"),
    SPUT_BOOLEAN((byte)0x6a, "SPUT-BOOLEAN", (byte)4, ReferenceType.field, "21c"),
    SPUT_BYTE((byte)0x6b, "SPUT-BYTE", (byte)4, ReferenceType.field, "21c"),
    SPUT_CHAR((byte)0x6c, "SPUT-CHAR", (byte)4, ReferenceType.field, "21c"),
    SPUT_SHORT((byte)0x6d, "SPUT-SHORT", (byte)4, ReferenceType.field, "21c"),
    INVOKE_VIRTUAL((byte)0x6e, "INVOKE-VIRTUAL", (byte)6, ReferenceType.method, "35c"),
    INVOKE_SUPER((byte)0x6f, "INVOKE-SUPER", (byte)6, ReferenceType.method, "35c"),
    INVOKE_DIRECT((byte)0x70, "INVOKE-DIRECT", (byte)6, ReferenceType.method, "35c"),
    INVOKE_STATIC((byte)0x71, "INVOKE-STATIC", (byte)6, ReferenceType.method, "35c"),
    INVOKE_INTERFACE((byte)0x72, "INVOKE-INTERFACE", (byte)6, ReferenceType.method, "35c"),
    INVOKE_VIRTUAL_RANGE((byte)0x74, "INVOKE-VIRTUAL/RANGE", (byte)6, ReferenceType.method, "3rc"),
    INVOKE_SUPER_RANGE((byte)0x75, "INVOKE-SUPER/RANGE", (byte)6, ReferenceType.method, "3rc"),
    INVOKE_DIRECT_RANGE((byte)0x76, "INVOKE-DIRECT/RANGE", (byte)6, ReferenceType.method, "3rc"),
    INVOKE_STATIC_RANGE((byte)0x77, "INVOKE-STATIC/RANGE", (byte)6, ReferenceType.method, "3rc"),
    INVOKE_INTERFACE_RANGE((byte)0x78, "INVOKE-INTERFACE/RANGE", (byte)6, ReferenceType.method, "3rc"),
    NEG_INT((byte)0x7b, "NEG-INT", (byte)2, ReferenceType.none, "12x"),
    NOT_INT((byte)0x7c, "NOT-INT", (byte)2, ReferenceType.none, "12x"),
    NEG_LONG((byte)0x7d, "NEG-LONG", (byte)2, ReferenceType.none, "12x"),
    NOT_LONG((byte)0x7e, "NOT-LONG", (byte)2, ReferenceType.none, "12x"),
    NEG_FLOAT((byte)0x7f, "NEG-FLOAT", (byte)2, ReferenceType.none, "12x"),
    NEG_DOUBLE((byte)0x80, "NEG-DOUBLE", (byte)2, ReferenceType.none, "12x"),
    INT_TO_LONG((byte)0x81, "INT-TO-LONG", (byte)2, ReferenceType.none, "12x"),
    INT_TO_FLOAT((byte)0x82, "INT-TO-FLOAT", (byte)2, ReferenceType.none, "12x"),
    INT_TO_DOUBLE((byte)0x83, "INT-TO-DOUBLE", (byte)2, ReferenceType.none, "12x"),
    LONG_TO_INT((byte)0x84, "LONG-TO-INT", (byte)2, ReferenceType.none, "12x"),
    LONG_TO_FLOAT((byte)0x85, "LONG-TO-FLOAT", (byte)2, ReferenceType.none, "12x"),
    LONG_TO_DOUBLE((byte)0x86, "LONG-TO-DOUBLE", (byte)2, ReferenceType.none, "12x"),
    FLOAT_TO_INT((byte)0x87, "FLOAT-TO-INT", (byte)2, ReferenceType.none, "12x"),
    FLOAT_TO_LONG((byte)0x88, "FLOAT-TO-LONG", (byte)2, ReferenceType.none, "12x"),
    FLOAT_TO_DOUBLE((byte)0x89, "FLOAT-TO-DOUBLE", (byte)2, ReferenceType.none, "12x"),
    DOUBLE_TO_INT((byte)0x8a, "DOUBLE-TO-INT", (byte)2, ReferenceType.none, "12x"),
    DOUBLE_TO_LONG((byte)0x8b, "DOUBLE-TO-LONG", (byte)2, ReferenceType.none, "12x"),
    DOUBLE_TO_FLOAT((byte)0x8c, "DOUBLE-TO-FLOAT", (byte)2, ReferenceType.none, "12x"),
    INT_TO_BYTE((byte)0x8d, "INT-TO-BYTE", (byte)2, ReferenceType.none, "12x"),
    INT_TO_CHAR((byte)0x8e, "INT-TO-CHAR", (byte)2, ReferenceType.none, "12x"),
    INT_TO_SHORT((byte)0x8f, "INT-TO-SHORT", (byte)2, ReferenceType.none, "12x"),
    ADD_INT((byte)0x90, "ADD-INT", (byte)4, ReferenceType.none, "23x"),
    SUB_INT((byte)0x91, "SUB-INT", (byte)4, ReferenceType.none, "23x"),
    MUL_INT((byte)0x92, "MUL-INT", (byte)4, ReferenceType.none, "23x"),
    DIV_INT((byte)0x93, "DIV-INT", (byte)4, ReferenceType.none, "23x"),
    REM_INT((byte)0x94, "REM-INT", (byte)4, ReferenceType.none, "23x"),
    AND_INT((byte)0x95, "AND-INT", (byte)4, ReferenceType.none, "23x"),
    OR_INT((byte)0x96, "OR-INT", (byte)4, ReferenceType.none, "23x"),
    XOR_INT((byte)0x97, "XOR-INT", (byte)4, ReferenceType.none, "23x"),
    SHL_INT((byte)0x98, "SHL-INT", (byte)4, ReferenceType.none, "23x"),
    SHR_INT((byte)0x99, "SHR-INT", (byte)4, ReferenceType.none, "23x"),
    USHR_INT((byte)0x9a, "USHR-INT", (byte)4, ReferenceType.none, "23x"),
    ADD_LONG((byte)0x9b, "ADD-LONG", (byte)4, ReferenceType.none, "23x"),
    SUB_LONG((byte)0x9c, "SUB-LONG", (byte)4, ReferenceType.none, "23x"),
    MUL_LONG((byte)0x9d, "MUL-LONG", (byte)4, ReferenceType.none, "23x"),
    DIV_LONG((byte)0x9e, "DIV-LONG", (byte)4, ReferenceType.none, "23x"),
    REM_LONG((byte)0x9f, "REM-LONG", (byte)4, ReferenceType.none, "23x"),
    AND_LONG((byte)0xa0, "AND-LONG", (byte)4, ReferenceType.none, "23x"),
    OR_LONG((byte)0xa1, "OR-LONG", (byte)4, ReferenceType.none, "23x"),
    XOR_LONG((byte)0xa2, "XOR-LONG", (byte)4, ReferenceType.none, "23x"),
    SHL_LONG((byte)0xa3, "SHL-LONG", (byte)4, ReferenceType.none, "23x"),
    SHR_LONG((byte)0xa4, "SHR-LONG", (byte)4, ReferenceType.none, "23x"),
    USHR_LONG((byte)0xa5, "USHR-LONG", (byte)4, ReferenceType.none, "23x"),
    ADD_FLOAT((byte)0xa6, "ADD-FLOAT", (byte)4, ReferenceType.none, "23x"),
    SUB_FLOAT((byte)0xa7, "SUB-FLOAT", (byte)4, ReferenceType.none, "23x"),
    MUL_FLOAT((byte)0xa8, "MUL-FLOAT", (byte)4, ReferenceType.none, "23x"),
    DIV_FLOAT((byte)0xa9, "DIV-FLOAT", (byte)4, ReferenceType.none, "23x"),
    REM_FLOAT((byte)0xaa, "REM-FLOAT", (byte)4, ReferenceType.none, "23x"),
    ADD_DOUBLE((byte)0xab, "ADD-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    SUB_DOUBLE((byte)0xac, "SUB-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    MUL_DOUBLE((byte)0xad, "MUL-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    DIV_DOUBLE((byte)0xae, "DIV-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    REM_DOUBLE((byte)0xaf, "REM-DOUBLE", (byte)4, ReferenceType.none, "23x"),
    ADD_INT_2ADDR((byte)0xb0, "ADD-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SUB_INT_2ADDR((byte)0xb1, "SUB-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    MUL_INT_2ADDR((byte)0xb2, "MUL-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    DIV_INT_2ADDR((byte)0xb3, "DIV-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    REM_INT_2ADDR((byte)0xb4, "REM-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    AND_INT_2ADDR((byte)0xb5, "AND-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    OR_INT_2ADDR((byte)0xb6, "OR-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    XOR_INT_2ADDR((byte)0xb7, "XOR-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SHL_INT_2ADDR((byte)0xb8, "SHL-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SHR_INT_2ADDR((byte)0xb9, "SHR-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    USHR_INT_2ADDR((byte)0xba, "USHR-INT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    ADD_LONG_2ADDR((byte)0xbb, "ADD-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SUB_LONG_2ADDR((byte)0xbc, "SUB-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    MUL_LONG_2ADDR((byte)0xbd, "MUL-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    DIV_LONG_2ADDR((byte)0xbe, "DIV-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    REM_LONG_2ADDR((byte)0xbf, "REM-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    AND_LONG_2ADDR((byte)0xc0, "AND-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    OR_LONG_2ADDR((byte)0xc1, "OR-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    XOR_LONG_2ADDR((byte)0xc2, "XOR-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SHL_LONG_2ADDR((byte)0xc3, "SHL-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SHR_LONG_2ADDR((byte)0xc4, "SHR-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    USHR_LONG_2ADDR((byte)0xc5, "USHR-LONG/2ADDR", (byte)2, ReferenceType.none, "12x"),
    ADD_FLOAT_2ADDR((byte)0xc6, "ADD-FLOAT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SUB_FLOAT_2ADDR((byte)0xc7, "SUB-FLOAT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    MUL_FLOAT_2ADDR((byte)0xc8, "MUL-FLOAT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    DIV_FLOAT_2ADDR((byte)0xc9, "DIV-FLOAT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    REM_FLOAT_2ADDR((byte)0xca, "REM-FLOAT/2ADDR", (byte)2, ReferenceType.none, "12x"),
    ADD_DOUBLE_2ADDR((byte)0xcb, "ADD-DOUBLE/2ADDR", (byte)2, ReferenceType.none, "12x"),
    SUB_DOUBLE_2ADDR((byte)0xcc, "SUB-DOUBLE/2ADDR", (byte)2, ReferenceType.none, "12x"),
    MUL_DOUBLE_2ADDR((byte)0xcd, "MUL-DOUBLE/2ADDR", (byte)2, ReferenceType.none, "12x"),
    DIV_DOUBLE_2ADDR((byte)0xce, "DIV-DOUBLE/2ADDR", (byte)2, ReferenceType.none, "12x"),
    REM_DOUBLE_2ADDR((byte)0xcf, "REM-DOUBLE/2ADDR", (byte)2, ReferenceType.none, "12x"),
    ADD_INT_LIT16((byte)0xd0, "ADD-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    RSUB_INT_LIT16((byte)0xd1, "RSUB-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    MUL_INT_LIT16((byte)0xd2, "MUL-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    DIV_INT_LIT16((byte)0xd3, "DIV-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    REM_INT_LIT16((byte)0xd4, "REM-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    AND_INT_LIT16((byte)0xd5, "AND-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    OR_INT_LIT16((byte)0xd6, "OR-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    XOR_INT_LIT16((byte)0xd7, "XOR-INT/LIT16", (byte)4, ReferenceType.none, "22s"),
    ADD_INT_LIT8((byte)0xd8, "ADD-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    RSUB_INT_LIT8((byte)0xd9, "RSUB-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    MUL_INT_LIT8((byte)0xda, "MUL-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    DIV_INT_LIT8((byte)0xdb, "DIV-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    REM_INT_LIT8((byte)0xdc, "REM-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    AND_INT_LIT8((byte)0xdd, "AND-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    OR_INT_LIT8((byte)0xde, "OR-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    XOR_INT_LIT8((byte)0xdf, "XOR-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    SHL_INT_LIT8((byte)0xe0, "SHL-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    SHR_INT_LIT8((byte)0xe1, "SHR-INT/LIT8", (byte)4, ReferenceType.none, "22b"),
    USHR_INT_LIT8((byte)0xe2, "USHR-INT/LIT8", (byte)4, ReferenceType.none, "22b");



    private static ArrayList<Opcode> opcodesByValue;
    private static HashMap<Integer, Opcode> opcodesByName;

    static {
        try
        {
        opcodesByValue = new ArrayList<Opcode>();
        opcodesByName = new HashMap<Integer, Opcode>();

        for (int i=0; i<0x100; i++) {
            opcodesByValue.add(null);
        }                 

        for (Opcode opcode: Opcode.values()) {
            opcodesByValue.set((opcode.value & 0xFF), opcode);
            opcodesByName.put(opcode.name.toLowerCase().hashCode(), opcode);
        }
        }catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    public static Opcode getOpcodeByName(String opcodeName) {
        return opcodesByName.get(opcodeName.toLowerCase().hashCode());
    }

    public static Opcode getOpcodeByValue(byte opcodeValue) {
        return opcodesByValue.get(opcodeValue);
    }

    public final byte value;
    public final String name;
    public final byte numBytes;
    public final ReferenceType referenceType;
    public final String format;
    
    Opcode(byte opcodeValue, String opcodeName, byte numBytes, ReferenceType referenceType, String format) {
        this.value = opcodeValue;
        this.name = opcodeName;
        this.numBytes = numBytes;
        this.referenceType = referenceType;
        this.format = format;
    }
}
