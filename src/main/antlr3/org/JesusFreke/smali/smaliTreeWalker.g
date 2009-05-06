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
 
tree grammar smaliTreeWalker;

options {
	tokenVocab=smaliParser;
	ASTLabelType=CommonTree;
}

@header {
package org.JesusFreke.smali;

import java.util.HashMap;

import org.JesusFreke.dexlib.*;
import org.JesusFreke.dexlib.EncodedValue.*;
import org.JesusFreke.dexlib.util.*;
import org.JesusFreke.dexlib.code.*;
import org.JesusFreke.dexlib.code.Format.*;
}

@members {
	public DexFile dexFile;
	public ClassDefItem classDefItem;
	public ClassDataItem classDataItem;
	
	private static byte[] intToBytes(int value) {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)value;
		bytes[1] = (byte)(value >> 8);
		bytes[2] = (byte)(value >> 16);
		bytes[3] = (byte)(value >> 24);
		return bytes;
	}
	
	private static byte parseIntLiteral_nibble(String intLiteral) {
		byte val = Byte.parseByte(intLiteral);
		if (val < -(1<<3) || val >= 1<<3) {
			//TODO: throw correct exception type
			throw new RuntimeException("The literal integer value must be between -8 and 7, inclusive");
		}
		return val;
	}
	
	private static byte parseIntLiteral_byte(String intLiteral) {
		return Byte.parseByte(intLiteral);
	}
	
	private static short parseIntLiteral_short(String intLiteral) {
		return Short.parseShort(intLiteral);
	}
	
	private static long parseLongLiteral(String longLiteral) {
		if (longLiteral.endsWith("L") || longLiteral.endsWith("l")) {
			longLiteral = longLiteral.substring(0, longLiteral.length()-1);
		}
		
		return Long.parseLong(longLiteral);
	}
	
	private static long parseDoubleLiteral(String doubleLiteral) {
		//TODO: implement this
		return 0;
	}
	
	private static byte parseRegister_nibble(String register) {
		//register should be in the format "v12"		
		byte val = Byte.parseByte(register.substring(1));
		if (val >= 2<<4) {
			//TODO: throw correct exception type
			throw new RuntimeException("The maximum allowed register in this context is list of registers is v15");
		}
		//the parser wouldn't accept a negative register, i.e. v-1, so we don't have to check for val<0;
		return val;
	}
	
	//return a short, because java's byte is signed
	private static short parseRegister_byte(String register) {
		//register should be in the format "v123"
		short val = Short.parseShort(register.substring(1));
		if (val >= 2<<8) {
			//TODO: throw correct exception type
			throw new RuntimeException("The maximum allowed register in this context is v255");
		}
		return val;
	}
	
	//return an int because java's short is signed
	private static int parseRegister_short(String register) {
		//register should be in the format "v12345"		
		int val = Integer.parseInt(register.substring(1));
		if (val >= 2<<16) {
			//TODO: throw correct exception type
			throw new RuntimeException("The maximum allowed register in this context is v65535");
		}
		//the parser wouldn't accept a negative register, i.e. v-1, so we don't have to check for val<0;
		return val;
	}	
}



smali_file returns[ClassDefItem classDefItem]
	:	^(I_CLASS_DEF header methods fields);

header	:	class_spec super_spec
	{
		classDataItem = new ClassDataItem(dexFile, 0);
		classDefItem = new ClassDefItem(dexFile, $class_spec.type, $class_spec.accessFlags, $super_spec.type, classDataItem);
	};

class_spec returns[TypeIdItem type, int accessFlags]
	:	class_type_descriptor access_list
	{
		$type = $class_type_descriptor.type;
		$accessFlags = $access_list.value;
	};

super_spec returns[TypeIdItem type]
	:	^(I_SUPER class_type_descriptor)
	{
		$type = $class_type_descriptor.type;
	};

access_list returns [int value]
	@init
	{
		$value = 0;
	}
	:	^(I_ACCESS_LIST
			(
				ACCESS_SPEC
				{
					$value |= AccessFlags.getValueForAccessFlag($ACCESS_SPEC.getText());
				}
			)+);

fields	:	^(I_FIELDS
			(field
			{
				classDefItem.addField($field.encodedField, $field.encodedValue);
			})*);

methods	:	^(I_METHODS
			(method
			{
				classDataItem.addMethod($method.encodedMethod);
			})*);

field returns[ClassDataItem.EncodedField encodedField, EncodedValue encodedValue]
	:^(I_FIELD MEMBER_NAME access_list ^(I_FIELD_TYPE field_type_descriptor) field_initial_value)
	{
		TypeIdItem classType = classDefItem.getClassType();
		StringIdItem memberName = new StringIdItem(dexFile, $MEMBER_NAME.text);
		TypeIdItem fieldType = $field_type_descriptor.type;

		FieldIdItem fieldIdItem = new FieldIdItem(dexFile, classType, memberName, fieldType);
		$encodedField = new ClassDataItem.EncodedField(dexFile, fieldIdItem, $access_list.value);
		
		if ($field_initial_value.encodedValue != null) {
			if (($access_list.value & AccessFlags.STATIC) == 0) {
				//TODO: change to an appropriate exception type?
				throw new RuntimeException("Initial field values can only be specified for static fields.");
			}
			
			$encodedValue = $field_initial_value.encodedValue;
		} else {
			$encodedValue = null;			
		}
	};

field_initial_value returns[EncodedValue encodedValue]
	:	^(I_FIELD_INITIAL_VALUE 
			(	integer_literal { $encodedValue = new EncodedValue(dexFile, new IntEncodedValueSubField($integer_literal.value)); }
			|	long_literal { $encodedValue = new EncodedValue(dexFile, new LongEncodedValueSubField($long_literal.value)); }
			|	float_literal { $encodedValue = new EncodedValue(dexFile, new FloatEncodedValueSubField($float_literal.value)); }
			|	double_literal { $encodedValue = new EncodedValue(dexFile, new DoubleEncodedValueSubField($double_literal.value)); }
			|	char_literal { $encodedValue = new EncodedValue(dexFile, new CharEncodedValueSubField($char_literal.value)); }
			|	string_literal { $encodedValue = new EncodedValue(dexFile, new EncodedIndexedItemReference(dexFile, new StringIdItem(dexFile, $string_literal.value))); }
			|	bool_literal { $encodedValue = new EncodedValue(dexFile, new BoolEncodedValueSubField($bool_literal.value)); }
			))
	| ;
	
fixed_literal returns[byte[\] value]
	:	integer_literal { value = intToBytes($integer_literal.value); };
	
array_elements returns[List<byte[\]> values]
	:	{$values = new ArrayList<byte[]>();}
		^(I_ARRAY_ELEMENTS
			(fixed_literal
			{
				$values.add($fixed_literal.value);				
			})*);	
	
method returns[ClassDataItem.EncodedMethod encodedMethod]
	scope
	{
		HashMap<String, Integer> labels;
		int currentAddress;
	}
	:	{
			$method::labels = new HashMap<String, Integer>();
			$method::currentAddress = 0;
		}
		^(I_METHOD method_name_and_prototype access_list registers_directive labels statements)
	{
		MethodIdItem methodIdItem = $method_name_and_prototype.methodIdItem;
		int registers = $registers_directive.registers;
		int access = $access_list.value;
		boolean isStatic = (access & AccessFlags.STATIC) != 0; 
		ArrayList<Instruction> instructions = $statements.instructions;
		
		CodeItem codeItem = new CodeItem(dexFile, registers, methodIdItem.getParameterWordCount(isStatic), instructions);
		
		$encodedMethod = new ClassDataItem.EncodedMethod(dexFile, methodIdItem, access, codeItem);
	};
	
method_prototype returns[ProtoIdItem protoIdItem]
	:	^(I_METHOD_PROTOTYPE ^(I_METHOD_RETURN_TYPE type_descriptor) field_type_list)
	{
		TypeIdItem returnType = $type_descriptor.type;
		ArrayList<TypeIdItem> parameterTypes = $field_type_list.types;

		$protoIdItem = new ProtoIdItem(dexFile, returnType, parameterTypes);
	};

method_name_and_prototype returns[MethodIdItem methodIdItem]
	:	MEMBER_NAME method_prototype
	{
		TypeIdItem classType = classDefItem.getClassType();
		String methodNameString = $MEMBER_NAME.text;
		StringIdItem methodName = new StringIdItem(dexFile, methodNameString);
		ProtoIdItem protoIdItem = $method_prototype.protoIdItem;

		$methodIdItem = new MethodIdItem(dexFile, classType, methodName, protoIdItem);
	};

field_type_list returns[ArrayList<TypeIdItem> types]
	@init
	{
		$types = new ArrayList<TypeIdItem>();
	}
	:	(
			field_type_descriptor
			{
				$types.add($field_type_descriptor.type);
			}
		)*;
	
registers_directive returns[int registers]
	:	^(I_REGISTERS INTEGER_LITERAL) {$registers = Integer.parseInt($INTEGER_LITERAL.text);};



fully_qualified_method returns[MethodIdItem methodIdItem]
	:	CLASS_NAME MEMBER_NAME method_prototype
	{
		TypeIdItem classType = new TypeIdItem(dexFile, "L" + $CLASS_NAME.text + ";");
		StringIdItem methodName = new StringIdItem(dexFile, $MEMBER_NAME.text);
		ProtoIdItem prototype = $method_prototype.protoIdItem;
		$methodIdItem = new MethodIdItem(dexFile, classType, methodName, prototype);		
	};

fully_qualified_field returns[FieldIdItem fieldIdItem]
	:	CLASS_NAME MEMBER_NAME field_type_descriptor
	{
		TypeIdItem classType = new TypeIdItem(dexFile, "L" + $CLASS_NAME.text + ";");
		StringIdItem fieldName = new StringIdItem(dexFile, $MEMBER_NAME.text);
		TypeIdItem fieldType = $field_type_descriptor.type;
		$fieldIdItem = new FieldIdItem(dexFile, classType, fieldName, fieldType);
	};
	
labels
	:	^(I_LABELS label_def*);
	
label_def
	:	^(I_LABEL label integer_literal)
		{
			String labelName = $label.labelName;
			if ($method::labels.containsKey(labelName)) {
				//TODO: use appropriate exception type
				throw new RuntimeException("Label " + labelName + " has multiple defintions.");
			}
				
			
			$method::labels.put(labelName, $integer_literal.value);
		};

statements returns[ArrayList<Instruction> instructions]
	@init
	{
		$instructions = new ArrayList<Instruction>();
	}
	:	^(I_STATEMENTS	(instruction
				{
					$instructions.add($instruction.instruction);
					$method::currentAddress += $instruction.instruction.getBytes().length/2;
				})*);
			
label_ref returns[int labelAddress]
	:	label
		{
			String labelName = $label.labelName;
			
			Integer labelAdd = $method::labels.get(labelName);
			
			if (labelAdd == null) {
				//TODO: throw correct exception type
				throw new RuntimeException("Label \"" + labelName + "\" is not defined.");
			}
			
			$labelAddress = labelAdd;
		};
	
	
label returns[String labelName]
	:	LABEL
		{
			String label = $LABEL.text;
			return label.substring(0, label.length()-1);
		};
		
offset	returns[int offsetValue]
	:	OFFSET
		{
			String offsetText = $OFFSET.text;
			if (offsetText.startsWith("+")) {
				offsetText = offsetText.substring(1);
			}
			$offsetValue = Integer.parseInt(offsetText);
		};
		
offset_or_label returns[int offsetValue]
	:	offset {$offsetValue = $offset.offsetValue;}
	|	label_ref
		{
			int labelAddress = $label_ref.labelAddress;
			int currentAddress = $method::currentAddress;
			
			$offsetValue = labelAddress-currentAddress;
		};
	
instruction returns[Instruction instruction]
	:	//e.g. goto endloop:
		^(I_STATEMENT_FORMAT10t INSTRUCTION_FORMAT10t offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10t.text);
			
			int addressOffset = $offset_or_label.offsetValue;

			if (addressOffset < Byte.MIN_VALUE || addressOffset > Byte.MAX_VALUE) {
				//TODO: throw correct exception type
				throw new RuntimeException("The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-128, 127].");
			}
			
			$instruction = Format10t.Format.make(dexFile, opcode.value, (byte)addressOffset);
		}
	|	//e.g. return
		^(I_STATEMENT_FORMAT10x INSTRUCTION_FORMAT10x)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
			$instruction = Format10x.Format.make(dexFile, opcode.value);
		}
	|	//e.g. const/4 v0, 5
		^(I_STATEMENT_FORMAT11n INSTRUCTION_FORMAT11n REGISTER INTEGER_LITERAL)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
			byte regA = parseRegister_nibble($REGISTER.text);
			byte litB = parseIntLiteral_nibble($INTEGER_LITERAL.text);
			
			$instruction = Format11n.Format.make(dexFile, opcode.value, regA, litB);
		}				
	|	//e.g. move-result-object v1
		^(I_STATEMENT_FORMAT11x INSTRUCTION_FORMAT11x REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			$instruction = Format11x.Format.make(dexFile, opcode.value, regA);
		}
	|	//e.g. move v1 v2
		^(I_STATEMENT_FORMAT12x INSTRUCTION_FORMAT12x registerA=REGISTER registerB=REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT12x.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			$instruction = Format12x.Format.make(dexFile, opcode.value, regA, regB);
		}
	|	//e.g. goto/16 endloop:
		^(I_STATEMENT_FORMAT20t INSTRUCTION_FORMAT20t offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT20t.text);
			
			int addressOffset = $offset_or_label.offsetValue;

			if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
				//TODO: throw correct exception type
				throw new RuntimeException("The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
			}
			
			$instruction = Format20t.Format.make(dexFile, opcode.value, (short)addressOffset);
		}
	|	//e.g. sget_object v0 java/lang/System/out LJava/io/PrintStream;
		^(I_STATEMENT_FORMAT21c_FIELD INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_FIELD.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;

			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, fieldIdItem);
		}
	|	//e.g. const-string v1 "Hello World!"
		^(I_STATEMENT_FORMAT21c_STRING INSTRUCTION_FORMAT21c_STRING REGISTER string_literal)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			StringIdItem stringIdItem = new StringIdItem(dexFile, $string_literal.value);

			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, stringIdItem);
		}
	|	//e.g. const-class v2 org/JesusFreke/HelloWorld2/HelloWorld2
		^(I_STATEMENT_FORMAT21c_TYPE INSTRUCTION_FORMAT21c_TYPE REGISTER class_or_array_type_descriptor)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			TypeIdItem typeIdItem = $class_or_array_type_descriptor.type;
			
			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, typeIdItem);
		}
	|	//e.g. const/high16 v1, 1234
		^(I_STATEMENT_FORMAT21h INSTRUCTION_FORMAT21h REGISTER INTEGER_LITERAL)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21h.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			short litB = parseIntLiteral_short($INTEGER_LITERAL.text);
			
			$instruction = Format21h.Format.make(dexFile, opcode.value, regA, litB);
		}
	|	//e.g. const/16 v1, 1234
		^(I_STATEMENT_FORMAT21s INSTRUCTION_FORMAT21s REGISTER INTEGER_LITERAL)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			short litB = parseIntLiteral_short($INTEGER_LITERAL.text);
			
			$instruction = Format21s.Format.make(dexFile, opcode.value, regA, litB);
		}
	|	//e.g. if-eqz v0, endloop:
		^(I_STATEMENT_FORMAT21t INSTRUCTION_FORMAT21t REGISTER offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			int addressOffset = $offset_or_label.offsetValue;

			if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
				//TODO: throw correct exception type
				throw new RuntimeException("The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
			}
			
			$instruction = Format21t.Format.make(dexFile, opcode.value, regA, (short)addressOffset);
		}
	|	//e.g. add-int v0, v1, 123
		^(I_STATEMENT_FORMAT22b INSTRUCTION_FORMAT22b registerA=REGISTER registerB=REGISTER INTEGER_LITERAL)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
			short regA = parseRegister_byte($registerA.text);
			short regB = parseRegister_byte($registerB.text);
			
			byte litC = parseIntLiteral_byte($INTEGER_LITERAL.text);
			
			$instruction = Format22b.Format.make(dexFile, opcode.value, regA, regB, litC);
		}
	|	//e.g. iput-object v1 v0 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
		^(I_STATEMENT_FORMAT22c_FIELD INSTRUCTION_FORMAT22c_FIELD registerA=REGISTER registerB=REGISTER fully_qualified_field)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22c_FIELD.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			FieldIdItem fieldIdItem = $fully_qualified_field.fieldIdItem;
			
			$instruction = Format22c.Format.make(dexFile, opcode.value, regA, regB, fieldIdItem);			
		}
	|	//e.g. instance-of v0, v1, Ljava/lang/String;
		^(I_STATEMENT_FORMAT22c_TYPE INSTRUCTION_FORMAT22c_TYPE registerA=REGISTER registerB=REGISTER field_type_descriptor)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			TypeIdItem typeIdItem = $field_type_descriptor.type;
			
			$instruction = Format22c.Format.make(dexFile, opcode.value, regA, regB, typeIdItem);
		}
	|	//e.g. add-int/lit16 v0, v1, 12345
		^(I_STATEMENT_FORMAT22s INSTRUCTION_FORMAT22s registerA=REGISTER registerB=REGISTER INTEGER_LITERAL)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22s.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			short litC = parseIntLiteral_short($INTEGER_LITERAL.text);
			
			$instruction = Format22s.Format.make(dexFile, opcode.value, regA, regB, litC);
		}
	|	//e.g. if-eq v0, v1, endloop:
		^(I_STATEMENT_FORMAT22t INSTRUCTION_FORMAT22t registerA=REGISTER registerB=REGISTER offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			int addressOffset = $offset_or_label.offsetValue;

			if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
				//TODO: throw correct exception type
				throw new RuntimeException("The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
			}
			
			$instruction = Format22t.Format.make(dexFile, opcode.value, regA, regB, (short)addressOffset);
		}
	|	//e.g. move/from16 v1, v1234
		^(I_STATEMENT_FORMAT22x INSTRUCTION_FORMAT22x registerA=REGISTER registerB=REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
			short regA = parseRegister_byte($registerA.text);
			int regB = parseRegister_short($registerB.text);
			
			$instruction = Format22x.Format.make(dexFile, opcode.value, regA, regB);
		}
	|	//e.g. add-int v1, v2, v3
		^(I_STATEMENT_FORMAT23x INSTRUCTION_FORMAT23x registerA=REGISTER registerB=REGISTER registerC=REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
			short regA = parseRegister_byte($registerA.text);
			short regB = parseRegister_byte($registerB.text);
			short regC = parseRegister_byte($registerC.text);			
			
			$instruction = Format23x.Format.make(dexFile, opcode.value, regA, regB, regC);
		}
	|	//e.g. goto/32 endloop:
		^(I_STATEMENT_FORMAT30t INSTRUCTION_FORMAT30t offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT30t.text);
			
			int addressOffset = $offset_or_label.offsetValue;
	
			$instruction = Format30t.Format.make(dexFile, opcode.value, addressOffset);
		}
	|	//e.g. const-string/jumbo v1 "Hello World!"
		^(I_STATEMENT_FORMAT31c INSTRUCTION_FORMAT31c REGISTER string_literal)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
			short regA = parseRegister_byte($REGISTER.text);
					
			StringIdItem stringIdItem = new StringIdItem(dexFile, $string_literal.value);
			
			$instruction = Format31c.Format.make(dexFile, opcode.value, regA, stringIdItem);
		}
	|	//e.g. const v0, 123456
		^(I_STATEMENT_FORMAT31i INSTRUCTION_FORMAT31i REGISTER integer_or_float_literal)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31i.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			int litB = $integer_or_float_literal.value;
			
			$instruction = Format31i.Format.make(dexFile, opcode.value, regA, litB);
		}
	|	//e.g. fill-array-data v0, ArrayData:
		^(I_STATEMENT_FORMAT31t INSTRUCTION_FORMAT31t REGISTER offset_or_label)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31t.text);
			
			short regA = parseRegister_byte($REGISTER.text);
			
			int addressOffset = $offset_or_label.offsetValue;
			if (($method::currentAddress + addressOffset) \% 2 != 0) {
				addressOffset++;
			}
			
			$instruction = Format31t.Format.make(dexFile, opcode.value, regA, addressOffset);
		}
	|	//e.g. move/16 v5678, v1234
		^(I_STATEMENT_FORMAT32x INSTRUCTION_FORMAT32x registerA=REGISTER registerB=REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
			int regA = parseRegister_short($registerA.text);
			int regB = parseRegister_short($registerB.text);
			
			$instruction = Format32x.Format.make(dexFile, opcode.value, regA, regB);
		}
	|	//e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
		^(I_STATEMENT_FORMAT35c_METHOD INSTRUCTION_FORMAT35c_METHOD register_list fully_qualified_method)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT35c_METHOD.text);

			//this depends on the fact that register_list returns a byte[5]
			byte[] registers = $register_list.registers;
			byte registerCount = $register_list.registerCount;
			
			MethodIdItem methodIdItem = $fully_qualified_method.methodIdItem;
			
			$instruction = Format35c.Format.make(dexFile, opcode.value, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], methodIdItem);
		}
	|	//e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
		^(I_STATEMENT_FORMAT3rc_METHOD INSTRUCTION_FORMAT3rc_METHOD register_range fully_qualified_method)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
			int startRegister = $register_range.startRegister;
			int endRegister = $register_range.endRegister;
			
			int registerCount = endRegister-startRegister+1;
			if (registerCount > 256) {
				//TODO: throw appropriate exception type
				throw new RuntimeException("A register range can span a maximum of 256 registers");
			}
			if (registerCount < 1) {
				//TODO: throw appropriate exception type
				throw new RuntimeException("A register range must have the lower register listed first");
			}
			
			MethodIdItem methodIdItem = $fully_qualified_method.methodIdItem;

			//not supported yet
			$instruction = Format3rc.Format.make(dexFile, opcode.value, (short)registerCount, startRegister, methodIdItem);
		}
	|	//e.g. const-wide v0, 5000000000L
		^(I_STATEMENT_FORMAT51l INSTRUCTION_FORMAT51l REGISTER long_or_double_literal)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			long litB = $long_or_double_literal.value;
			
			$instruction = Format51l.Format.make(dexFile, opcode.value, regA, litB);		
		}
	|	//e.g. .array-data 4 1000000 .end array-data
		^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE INTEGER_LITERAL) array_elements)
		{
			int elementWidth = parseIntLiteral_short($INTEGER_LITERAL.text);
			List<byte[]> byteValues = $array_elements.values;
			
			$instruction = ArrayData.make(dexFile, elementWidth, byteValues);
		}
	;


register_list returns[byte[\] registers, byte registerCount]
	@init
	{
		$registers = new byte[5];
		$registerCount = 0;
	}
	:	^(I_REGISTER_LIST 
			(REGISTER
			{
				if ($registerCount == 5) {
					//TODO: throw the correct type of exception
					throw new RuntimeException("A list of registers can only have a maximum of 5 registers. Use the <op>/range alternate opcode instead.");
				}
				$registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
			})*);
	
register_range returns[int startRegister, int endRegister]
	:	^(I_REGISTER_RANGE startReg=REGISTER endReg=REGISTER?)
		{
			$startRegister  = parseRegister_short($startReg.text);
			if ($endReg == null) {
				$endRegister = $startRegister;
			} else {
				$endRegister = parseRegister_short($endReg.text);
			}
		}
	;

field_type_descriptor returns [TypeIdItem type]
	:	(PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR	
	|	ARRAY_DESCRIPTOR)
	{
		$type = new TypeIdItem(dexFile, $start.getText());
	};
	
class_or_array_type_descriptor returns [TypeIdItem type]
	:	(CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR)
	{
		$type = new TypeIdItem(dexFile, $start.getText());
	};

class_type_descriptor returns [TypeIdItem type]
	:	CLASS_DESCRIPTOR
	{
		$type = new TypeIdItem(dexFile, $CLASS_DESCRIPTOR.text);
	};

type_descriptor returns [TypeIdItem type]
	:	VOID_TYPE {$type = new TypeIdItem(dexFile, "V");}
	|	field_type_descriptor {$type = $field_type_descriptor.type;}
	;
	
integer_or_float_literal returns[int value]
	:	INTEGER_LITERAL { $value = Integer.parseInt($INTEGER_LITERAL.text); }
	|	FLOAT_LITERAL { $value = Float.floatToIntBits(Float.parseFloat($FLOAT_LITERAL.text)); };
	
long_or_double_literal returns[long value]
	:	LONG_LITERAL { $value = parseLongLiteral($LONG_LITERAL.text); }
	|	DOUBLE_LITERAL { $value = parseDoubleLiteral($DOUBLE_LITERAL.text); };
	
integer_literal returns[int value]
	:	INTEGER_LITERAL { $value = Integer.parseInt($INTEGER_LITERAL.text); };

long_literal returns[long value]
	:	LONG_LITERAL { $value = Long.parseLong($LONG_LITERAL.text); };
	
float_literal returns[float value]
	:	FLOAT_LITERAL { $value = Float.parseFloat($FLOAT_LITERAL.text); };
	
double_literal returns[double value]
	:	DOUBLE_LITERAL { $value = Double.parseDouble($DOUBLE_LITERAL.text); };

char_literal returns[char value]
	:	CHAR_LITERAL { $value = $CHAR_LITERAL.text.charAt(0); };

string_literal returns[String value]
	:	STRING_LITERAL
		{
			$value = $STRING_LITERAL.text;
			$value = $value.substring(1,$value.length()-1);
		};

bool_literal returns[boolean value]
	:	BOOL_LITERAL { $value = Boolean.parseBoolean($BOOL_LITERAL.text); };
