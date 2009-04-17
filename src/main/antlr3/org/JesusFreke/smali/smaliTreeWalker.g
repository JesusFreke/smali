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
	tokenVocab=smali;
	ASTLabelType=CommonTree;
}

@header {
package org.JesusFreke.smali;

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
	:	class_name access_list
	{
		$type = $class_name.type;
		$accessFlags = $access_list.value;
	};

super_spec returns[TypeIdItem type]
	:	^(I_SUPER class_name)
	{
		$type = $class_name.type;
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
	:^(I_FIELD member_name access_list ^(I_FIELD_TYPE field_type_descriptor) field_initial_value)
	{
		TypeIdItem classType = classDefItem.getClassType();
		StringIdItem memberName = new StringIdItem(dexFile, $member_name.memberName);
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
			(	int_literal { $encodedValue = new EncodedValue(dexFile, new IntEncodedValueSubField($int_literal.value)); }
			|	long_literal { $encodedValue = new EncodedValue(dexFile, new LongEncodedValueSubField($long_literal.value)); }
			|	float_literal { $encodedValue = new EncodedValue(dexFile, new FloatEncodedValueSubField($float_literal.value)); }
			|	double_literal { $encodedValue = new EncodedValue(dexFile, new DoubleEncodedValueSubField($double_literal.value)); }
			|	char_literal { $encodedValue = new EncodedValue(dexFile, new CharEncodedValueSubField($char_literal.value)); }
			|	string_literal { $encodedValue = new EncodedValue(dexFile, new EncodedIndexedItemReference(dexFile, new StringIdItem(dexFile, $string_literal.value))); }
			|	bool_literal { $encodedValue = new EncodedValue(dexFile, new BoolEncodedValueSubField($bool_literal.value)); }
			))
	| ;

	
method returns[ClassDataItem.EncodedMethod encodedMethod]
	:	^(I_METHOD method_name_and_prototype access_list locals_directive statements)
	{
		MethodIdItem methodIdItem = $method_name_and_prototype.methodIdItem;
		int registers = $locals_directive.registers;
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
	:	member_name method_prototype
	{
		TypeIdItem classType = classDefItem.getClassType();
		String methodNameString = $member_name.memberName;
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
	
locals_directive returns[int registers]
	:	^(I_REGISTERS INT_LITERAL) {$registers = Integer.parseInt($INT_LITERAL.text);};

full_method_name_and_prototype returns[MethodIdItem methodIdItem]
	:	QUALIFIED_MEMBER__CLASS_NAME QUALIFIED_MEMBER__MEMBER_NAME method_prototype
	{
		TypeIdItem classType = new TypeIdItem(dexFile, "L" + $QUALIFIED_MEMBER__CLASS_NAME.text + ";");
		StringIdItem methodName = new StringIdItem(dexFile, $QUALIFIED_MEMBER__MEMBER_NAME.text);
		ProtoIdItem prototype = $method_prototype.protoIdItem;
		$methodIdItem = new MethodIdItem(dexFile, classType, methodName, prototype);		
	};

full_field_name_and_type returns[FieldIdItem fieldIdItem]
	:	QUALIFIED_MEMBER__CLASS_NAME QUALIFIED_MEMBER__MEMBER_NAME field_type_descriptor
	{
		TypeIdItem classType = new TypeIdItem(dexFile, "L" + $QUALIFIED_MEMBER__CLASS_NAME.text + ";");
		StringIdItem fieldName = new StringIdItem(dexFile, $QUALIFIED_MEMBER__MEMBER_NAME.text);
		TypeIdItem fieldType = $field_type_descriptor.type;
		$fieldIdItem = new FieldIdItem(dexFile, classType, fieldName, fieldType);
	};

statements returns[ArrayList<Instruction> instructions]
	@init
	{
		$instructions = new ArrayList<Instruction>();
	}
	:	^(I_STATEMENTS
			(instruction
			{
				$instructions.add($instruction.instruction);			
			})*);

	
instruction returns[Instruction instruction]
		//e.g. return
	:	^(I_STATEMENT_FORMAT10x INSTRUCTION_NAME_FORMAT10x)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT10x.text);
			$instruction = Format10x.Format.make(dexFile, opcode.value);
		}
	|	//e.g. move-result-object v1
		^(I_STATEMENT_FORMAT11x INSTRUCTION_NAME_FORMAT11x REGISTER)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT11x.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			$instruction = Format11x.Format.make(dexFile, opcode.value, regA);
		}	
	|	//e.g. sget_object v0 java/lang/System/out LJava/io/PrintStream;
		^(I_STATEMENT_FORMAT21c_FIELD INSTRUCTION_NAME_FORMAT21c_FIELD REGISTER full_field_name_and_type)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT21c_FIELD.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			FieldIdItem fieldIdItem = $full_field_name_and_type.fieldIdItem;

			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, fieldIdItem);
		}
	|	//e.g. const-string v1 "Hello World!"
		^(I_STATEMENT_FORMAT21c_STRING INSTRUCTION_NAME_FORMAT21c_STRING REGISTER string_literal)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT21c_STRING.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			StringIdItem stringIdItem = new StringIdItem(dexFile, $string_literal.value);

			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, stringIdItem);
		}
	|	//e.g. const-class v2 org/JesusFreke/HelloWorld2/HelloWorld2
		^(I_STATEMENT_FORMAT21c_TYPE INSTRUCTION_NAME_FORMAT21c_TYPE REGISTER class_or_array_type_descriptor)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT21c_TYPE.text);
			short regA = parseRegister_byte($REGISTER.text);
			
			TypeIdItem typeIdItem = $class_or_array_type_descriptor.type;
			
			$instruction = Format21c.Format.make(dexFile, opcode.value, regA, typeIdItem);
		}	
	|	//e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
		^(I_STATEMENT_FORMAT35c_METHOD INSTRUCTION_NAME_FORMAT35c_METHOD register_list full_method_name_and_prototype)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT35c_METHOD.text);

			//this depends on the fact that register_list returns a byte[5]
			byte[] registers = $register_list.registers;
			byte registerCount = $register_list.registerCount;
			
			MethodIdItem methodIdItem = $full_method_name_and_prototype.methodIdItem;
			
			$instruction = Format35c.Format.make(dexFile, opcode.value, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], methodIdItem);
		}
	|	//e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
		^(I_STATEMENT_FORMAT3rc_METHOD INSTRUCTION_NAME_FORMAT3rc_METHOD register_range full_method_name_and_prototype)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT3rc_METHOD.text);
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
			
			MethodIdItem methodIdItem = $full_method_name_and_prototype.methodIdItem;

			//not supported yet
			$instruction = Format3rc.Format.make(dexFile, opcode.value, (short)registerCount, startRegister, methodIdItem);
		}
	|	//e.g. iput-object v1 v0 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
		^(I_STATEMENT_FORMAT22c_FIELD INSTRUCTION_NAME_FORMAT22c_FIELD registerA=REGISTER registerB=REGISTER full_field_name_and_type)
		{
			Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_NAME_FORMAT22c_FIELD.text);
			byte regA = parseRegister_nibble($registerA.text);
			byte regB = parseRegister_nibble($registerB.text);
			
			FieldIdItem fieldIdItem = $full_field_name_and_type.fieldIdItem;
			
			$instruction = Format22c.Format.make(dexFile, opcode.value, regA, regB, fieldIdItem);			
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

simple_name
	:	SIMPLE_NAME 
	|	ACCESS_SPEC
	|	INT_LITERAL
	|	LONG_LITERAL
	|	FLOAT_LITERAL_SIMPLE_NAME
	|	DOUBLE_LITERAL_SIMPLE_NAME
	|	BOOL_LITERAL
	|	PRIMITIVE_TYPE
	|	instruction_name
	;

instruction_name returns[String value]
	:	INSTRUCTION_NAME_FORMAT10x
	|	INSTRUCTION_NAME_FORMAT11x
	|	INSTRUCTION_NAME_FORMAT21c_FIELD
	|	INSTRUCTION_NAME_FORMAT21c_STRING
	|	INSTRUCTION_NAME_FORMAT21c_TYPE
	|	INSTRUCTION_NAME_FORMAT22c_FIELD
	|	INSTRUCTION_NAME_FORMAT35c_METHOD
	|	INSTRUCTION_NAME_FORMAT3rc_METHOD
	;

member_name returns[String memberName]
	:	(simple_name
	|	MEMBER_NAME) {$memberName = $start.getText();}
	; 
	
class_name returns [TypeIdItem type]
	:	token=(SIMPLE_NAME | CLASS_WITH_PACKAGE_NAME)
		{
			$type = new TypeIdItem(dexFile, 'L'+$token.text+';');
		};
	
field_type_descriptor returns [TypeIdItem type]
	:	token=(PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR	
	|	ARRAY_TYPE)
	{
		$type = new TypeIdItem(dexFile, $token.text);
	};
	
class_or_array_type_descriptor returns [TypeIdItem type]
	:	token=(CLASS_DESCRIPTOR
	|	ARRAY_TYPE)
	{
		$type = new TypeIdItem(dexFile, $token.text);
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
	
int_literal returns[int value]
	:	INT_LITERAL { $value = Integer.parseInt($INT_LITERAL.text); };

long_literal returns[long value]
	:	LONG_LITERAL { $value = Long.parseLong($LONG_LITERAL.text); };
	
float_literal returns[float value]
	:	FLOAT_LITERAL { $value = Float.parseFloat($FLOAT_LITERAL.text); };
	
double_literal returns[double value]
	:	DOUBLE_LITERAL { $value = Double.parseDouble($DOUBLE_LITERAL.text); };

char_literal returns[char value]
	:	CHAR_LITERAL { $value = $CHAR_LITERAL.text.charAt(0); };

string_literal returns[String value]
	:	STRING_LITERAL { $value = $STRING_LITERAL.text; };

bool_literal returns[boolean value]
	:	BOOL_LITERAL { $value = Boolean.parseBoolean($BOOL_LITERAL.text); };
