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

parser grammar smaliParser;

options {
	tokenVocab=smaliLexer;
	output=AST;
	ASTLabelType=CommonTree;
}

tokens {
	//I_* tokens are imaginary tokens used as parent AST nodes
	I_CLASS_DEF;
	I_SUPER;
	I_ACCESS_LIST;
	I_METHODS;
	I_FIELDS;
	I_FIELD;
	I_FIELD_TYPE;
	I_FIELD_INITIAL_VALUE;
	I_METHOD;
	I_METHOD_PROTOTYPE;
	I_METHOD_RETURN_TYPE;
	I_REGISTERS;
	I_STATEMENTS;
	I_STATEMENT_FORMAT10x;
	I_STATEMENT_FORMAT11x;
	I_STATEMENT_FORMAT12x;
	I_STATEMENT_FORMAT21c_TYPE;
	I_STATEMENT_FORMAT21c_FIELD;
	I_STATEMENT_FORMAT22c_FIELD;
	I_STATEMENT_FORMAT21c_STRING;
	I_STATEMENT_FORMAT35c_METHOD;
	I_STATEMENT_FORMAT3rc_METHOD;
	I_REGISTER_RANGE;
	I_REGISTER_LIST;
	
	CLASS_NAME;
	MEMBER_NAME;
}

@header {
package org.JesusFreke.smali;
}


smali_file:	header methods_and_fields -> ^(I_CLASS_DEF header methods_and_fields);

header	:	class_spec super_spec;

class_spec
	:	CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR -> CLASS_DESCRIPTOR access_list;

super_spec
	:	SUPER_DIRECTIVE CLASS_DESCRIPTOR -> ^(I_SUPER[$start, "I_SUPER"] CLASS_DESCRIPTOR);

access_list
	:	ACCESS_SPEC+ -> ^(I_ACCESS_LIST[$start,"I_ACCESS_LIST"] ACCESS_SPEC+);

methods_and_fields
	:	(method | field)* -> ^(I_METHODS method*) ^(I_FIELDS field*);

field	:	FIELD_DIRECTIVE access_list MEMBER_NAME field_type_descriptor literal?
		-> ^(I_FIELD[$start, "I_FIELD"] MEMBER_NAME access_list ^(I_FIELD_TYPE field_type_descriptor) ^(I_FIELD_INITIAL_VALUE literal)?);
		
method	:	METHOD_DIRECTIVE access_list  MEMBER_NAME method_prototype 
		registers_directive
		statements 
		END_METHOD_DIRECTIVE
		-> ^(I_METHOD[$start, "I_METHOD"] MEMBER_NAME method_prototype access_list registers_directive statements);

method_prototype
	:	OPEN_PAREN field_type_descriptor* CLOSE_PAREN type_descriptor
		-> ^(I_METHOD_PROTOTYPE[$start, "I_METHOD_PROTOTYPE"] ^(I_METHOD_RETURN_TYPE type_descriptor) field_type_descriptor*);



registers_directive
	:	REGISTERS_DIRECTIVE INTEGER_LITERAL
		-> ^(I_REGISTERS[$start, "I_REGISTERS"] INTEGER_LITERAL);


fully_qualified_method
	:	CLASS_NAME MEMBER_NAME method_prototype;

fully_qualified_field
	:	CLASS_NAME MEMBER_NAME field_type_descriptor;

statements
	:	statement* -> ^(I_STATEMENTS statement*);

statement
	:	instruction;
	
instruction
		//e.g. return
	:	INSTRUCTION_FORMAT10x
		-> ^(I_STATEMENT_FORMAT10x[$start, "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x)
	|	//e.g. move-result-object v1
		INSTRUCTION_FORMAT11x REGISTER
		-> ^(I_STATEMENT_FORMAT11x[$start, "I_STATEMENT_FORMAT11x"] INSTRUCTION_FORMAT11x REGISTER)
	|	//e.g. move v1 v2
		INSTRUCTION_FORMAT12x REGISTER REGISTER
		-> ^(I_STATEMENT_FORMAT12x[$start, "I_STATEMENT_FORMAT12x"] INSTRUCTION_FORMAT12x REGISTER REGISTER)		
	|	//e.g. sget_object v0 java/lang/System/out LJava/io/PrintStream;
		INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field
		-> ^(I_STATEMENT_FORMAT21c_FIELD[$start, "I_STATEMENT_FORMAT21c_FIELD"] INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field)
	|	//e.g. const-string v1 "Hello World!"
		INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL
		-> ^(I_STATEMENT_FORMAT21c_STRING[$start, "I_STATEMENT_FORMAT21c_STRING"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL)
	|	//e.g. const-class v2 org/JesusFreke/HelloWorld2/HelloWorld2
		INSTRUCTION_FORMAT21c_TYPE REGISTER class_or_array_type_descriptor
		-> ^(I_STATEMENT_FORMAT21c_TYPE[$start, "I_STATEMENT_FORMAT21c"] INSTRUCTION_FORMAT21c_TYPE REGISTER class_or_array_type_descriptor)
	|	//e.g. iput-object v1 v0 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
		INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER fully_qualified_field
		-> ^(I_STATEMENT_FORMAT22c_FIELD[$start, "I_INSTANCE_FIELD_STATEMENT"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER fully_qualified_field)
	|	//e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
		INSTRUCTION_FORMAT35c_METHOD OPEN_BRACKET register_list CLOSE_BRACKET fully_qualified_method 
		-> ^(I_STATEMENT_FORMAT35c_METHOD[$start, "I_STATEMENT_FORMAT35c_METHOD"] INSTRUCTION_FORMAT35c_METHOD register_list fully_qualified_method)
	|	//e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
		INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACKET register_range CLOSE_BRACKET fully_qualified_method
		-> ^(I_STATEMENT_FORMAT3rc_METHOD[$start, "I_STATEMENT_FORMAT3rc_METHOD"] INSTRUCTION_FORMAT3rc_METHOD register_range fully_qualified_method)
	;


register_list
	:	REGISTER* -> ^(I_REGISTER_LIST[$start, "I_REGISTER_LIST"] REGISTER*);
	
register_range
	:	REGISTER REGISTER? -> ^(I_REGISTER_RANGE[$start, "I_REGISTER_RANGE"] REGISTER REGISTER?);


field_type_descriptor
	:	PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR
	;
	
class_or_array_type_descriptor
	:	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR;
	
type_descriptor
	:	VOID_TYPE
	|	PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR
	;	
	
literal	:	INTEGER_LITERAL
	|	LONG_LITERAL
	|	FLOAT_LITERAL
	|	DOUBLE_LITERAL
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	BOOL_LITERAL;
