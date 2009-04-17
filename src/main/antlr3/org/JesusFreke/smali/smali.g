/* 
 * The comment lexical rule, and the number, string and character constant
 * lexical rules are derived from rules from the Java 1.6 grammar which can be
 * found here: http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
 * 
 * Specifically, these rules:
 * 
 * COMMENT, LONG_LITERAL, INT_LITERAL, Integer_number, Hex_prefix, Hex_digit,
 * Long_suffix, Non_integer_number_SIMPLE_NAME, Non_integer_number,
 * Decimal_exponent, Hex_exponent, Float_suffix, Double_suffix,
 * FLOAT_LITERAL_SIMPLE_NAME, FLOAT_LITERAL, DOUBLE_LITERAL_SIMPLE_NAME,
 * DOUBLE_LITERAL, CHAR_LITERAL, STRING_LITERAL, EscapeSequence
 * 
 * These rules were originally copyrighted by Terence Parr, and are used here in
 * accordance with the following license
 * 
 * [The "BSD licence"]
 * Copyright (c) 2007-2008 Terence Parr
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
 * 
 * 
 * The remainder of this grammar is released by me (Ben Gruver) under the
 * following license:	
 * 
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

grammar smali;

options {
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
	I_INVOKE_STATEMENT;
	I_INVOKE_RANGE_STATEMENT;
	I_BARE_STATEMENT;
	I_STATIC_FIELD_STATEMENT;
	I_INSTANCE_FIELD_STATEMENT;
	I_CONST_STRING_STATEMENT;
	I_CONST_CLASS_STATEMENT;
	I_NEW_INSTANCE_STATEMENT;
	I_SINGLE_REGISTER_STATEMENT;
	I_REGISTER_RANGE;
	I_REGISTER_LIST;
}

@parser::header {
package org.JesusFreke.smali;
}

@lexer::header {
package org.JesusFreke.smali;

import java.util.ArrayDeque;
}

@lexer::init {
	state.token = Token.INVALID_TOKEN;
}

@lexer::members {
       protected ArrayDeque<Token> tokens = new ArrayDeque<Token>();
 
    public void reset() {
        super.reset();
        state.token = Token.INVALID_TOKEN;
        tokens.clear();
    }

    	public Token nextToken() {
            while (true) {
                if (tokens.size() > 0) {
                    Token token = tokens.poll();
                    if (token == Token.SKIP_TOKEN) {
                        continue;
                    }

                    return token;
                }

    			state.channel = Token.DEFAULT_CHANNEL;
    			state.tokenStartCharIndex = input.index();
    			state.tokenStartCharPositionInLine = input.getCharPositionInLine();
    			state.tokenStartLine = input.getLine();
    			state.text = null;
    			if ( input.LA(1)==CharStream.EOF ) {
    				return Token.EOF_TOKEN;
    			}
    			try {
    				mTokens();

                    if (tokens.size() == 0) {
    					emit();
    				}
    			}
    			catch (NoViableAltException nva) {
    				reportError(nva);
    				recover(nva); // throw out current char and try again
    			}
    			catch (RecognitionException re) {
    				reportError(re);
    				// match() routine has already called recover()
    			}
    		}
    	}

	public void skip() {
		tokens.add(Token.SKIP_TOKEN);
	}

    public void emit(Token token) {
		tokens.add(token);
	}
}


smali_file:	header methods_and_fields -> ^(I_CLASS_DEF header methods_and_fields);

header	:	class_spec super_spec;

class_spec
	:	'.class' access_list class_name -> class_name access_list;

super_spec
	:	first_token='.super' class_name -> ^(I_SUPER[$first_token, "I_SUPER"] class_name);

access_list
	:	first_token=ACCESS_SPEC ACCESS_SPEC* -> ^(I_ACCESS_LIST[$first_token,"I_ACCESS_LIST"] ACCESS_SPEC+);

methods_and_fields
	:	(method | field)* -> ^(I_METHODS method*) ^(I_FIELDS field*);

field	:	first_token='.field' access_list member_name field_type_descriptor ('=' literal)?
		-> ^(I_FIELD[$first_token, "I_FIELD"] member_name access_list ^(I_FIELD_TYPE field_type_descriptor) ^(I_FIELD_INITIAL_VALUE literal)?);

method	:	first_token='.method' access_list  method_name_and_prototype locals_directive statements '.end method'
		-> ^(I_METHOD[$first_token, "I_METHOD"] method_name_and_prototype access_list locals_directive statements);

method_prototype
	:	first_token='(' field_type_list ')' type_descriptor
		-> ^(I_METHOD_PROTOTYPE[$first_token, "I_METHOD_PROTOTYPE"] ^(I_METHOD_RETURN_TYPE type_descriptor) field_type_list?);

method_name_and_prototype
	:	member_name method_prototype;

field_type_list
	:	field_type_descriptor*;
	
locals_directive
	:	first_token='.registers' INT_LITERAL
		-> ^(I_REGISTERS[$first_token, "I_REGISTERS"] INT_LITERAL);


full_method_name_and_prototype
	:	QUALIFIED_MEMBER__CLASS_NAME QUALIFIED_MEMBER__MEMBER_NAME method_prototype;

full_field_name_and_type
	:	QUALIFIED_MEMBER__CLASS_NAME QUALIFIED_MEMBER__MEMBER_NAME field_type_descriptor;

statements
	:	statement* -> ^(I_STATEMENTS statement*);

statement
	:	instruction;
	
instruction
		//e.g. return
	:	BARE_INSTRUCTION_NAME
		-> ^(I_BARE_STATEMENT[$start, "I_BARE_STATEMENT"] BARE_INSTRUCTION_NAME)
	|	//e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
		INVOKE_INSTRUCTION_NAME '{' register_list '}' full_method_name_and_prototype
		-> ^(I_INVOKE_STATEMENT[$start, "I_INVOKE_STATEMENT"] INVOKE_INSTRUCTION_NAME register_list full_method_name_and_prototype)
	|	//e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
		INVOKE_RANGE_INSTRUCTION_NAME '{' register_range '}' full_method_name_and_prototype
		-> ^(I_INVOKE_RANGE_STATEMENT[$start, "I_INVOKE_RANGE_STATEMENT"] INVOKE_RANGE_INSTRUCTION_NAME register_range full_method_name_and_prototype)
	|	//e.g. sget_object v0 java/lang/System/out LJava/io/PrintStream;
		STATIC_FIELD_INSTRUCTION_NAME REGISTER full_field_name_and_type
		-> ^(I_STATIC_FIELD_STATEMENT[$start, "I_STATIC_FIELD_STATEMENT"] STATIC_FIELD_INSTRUCTION_NAME REGISTER full_field_name_and_type)
	|	//e.g. iput-object v1 v0 org/JesusFreke/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
		INSTANCE_FIELD_INSTRUCTION_NAME REGISTER REGISTER full_field_name_and_type
		-> ^(I_INSTANCE_FIELD_STATEMENT[$start, "I_INSTANCE_FIELD_STATEMENT"] INSTANCE_FIELD_INSTRUCTION_NAME REGISTER REGISTER full_field_name_and_type)
	|	//e.g. const-string v1 "Hello World!"
		CONST_STRING_INSTRUCTION_NAME REGISTER STRING_LITERAL
		-> ^(I_CONST_STRING_STATEMENT[$start, "I_CONST_STRING_STATMENT"] CONST_STRING_INSTRUCTION_NAME REGISTER STRING_LITERAL)
	|	//e.g. const-class v2 org/JesusFreke/HelloWorld2/HelloWorld2
		CONST_CLASS_INSTRUCTION_NAME REGISTER class_or_array_type_descriptor
		-> ^(I_CONST_CLASS_STATEMENT[$start, "I_CONST_CLASS_STATEMENT"] CONST_CLASS_INSTRUCTION_NAME REGISTER class_or_array_type_descriptor)
	|	//e.g. new-instance v1 android/widget/TextView
		NEW_INSTANCE_INSTRUCTION_NAME REGISTER CLASS_DESCRIPTOR
		-> ^(I_NEW_INSTANCE_STATEMENT[$start, "I_NEW_INSTANCE_STATEMENT"] NEW_INSTANCE_INSTRUCTION_NAME REGISTER CLASS_DESCRIPTOR)
	|	//e.g. move-result-object v1
		SINGLE_REGISTER_INSTRUCTION_NAME REGISTER
		-> ^(I_SINGLE_REGISTER_STATEMENT[$start, "I_SINGLE_REGISTER_STATEMENT"] SINGLE_REGISTER_INSTRUCTION_NAME REGISTER)		
	;


register_list
	:	first_token=REGISTER? (',' REGISTER)* -> ^(I_REGISTER_LIST[$first_token, "I_REGISTER_LIST"] REGISTER*);
	
register_range
	:	first_token=REGISTER ('..' REGISTER)? -> ^(I_REGISTER_RANGE[$first_token, "I_REGISTER_RANGE"] REGISTER REGISTER?);

/*since there are no reserved words in the dex specification, there are a
number of tokens that can be a valid simple_name, in addition to just
SIMPLE_NAME. We need to match any token that could also be considered a valid
SIMPLE_NAME. In the case of floating point literals, some could be considered
a valid SIMPLE_NAME while others couldn't. The lexer will generate a separate
FLOAT_LITERAL_SIMPLE_NAME OR DOUBLE_LITERAL_SIMPLE_NAME token for literals
that can be considered a valid SIMPLE_NAME*/
simple_name
	:	SIMPLE_NAME
	|	ACCESS_SPEC
	|	instruction_name
	|	INT_LITERAL
	|	LONG_LITERAL
	|	FLOAT_LITERAL_SIMPLE_NAME
	|	DOUBLE_LITERAL_SIMPLE_NAME
	|	BOOL_LITERAL
	|	PRIMITIVE_TYPE
	;

instruction_name
	:	INVOKE_INSTRUCTION_NAME
	|	INVOKE_RANGE_INSTRUCTION_NAME
	|	STATIC_FIELD_INSTRUCTION_NAME
	|	INSTANCE_FIELD_INSTRUCTION_NAME	
	|	BARE_INSTRUCTION_NAME
	|	CONST_STRING_INSTRUCTION_NAME
	|	CONST_CLASS_INSTRUCTION_NAME
/*	|	CHECK_CAST_INSTRUCTION_NAME*/
	|	NEW_INSTANCE_INSTRUCTION_NAME
	|	SINGLE_REGISTER_INSTRUCTION_NAME
	;

member_name
	:	simple_name
	|	MEMBER_NAME
	;
	
class_name
	:	SIMPLE_NAME | CLASS_WITH_PACKAGE_NAME;
	
field_type_descriptor
	:	PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR
	|	ARRAY_TYPE
	;
	
class_or_array_type_descriptor
	:	CLASS_DESCRIPTOR
	|	ARRAY_TYPE;

type_descriptor
	:	VOID_TYPE
	|	field_type_descriptor
	;	
	
literal	:	INT_LITERAL
	|	LONG_LITERAL
	|	float_literal
	|	double_literal
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	BOOL_LITERAL;
	
float_literal
	:	FLOAT_LITERAL -> FLOAT_LITERAL
	|	FLOAT_LITERAL_SIMPLE_NAME -> FLOAT_LITERAL[$FLOAT_LITERAL_SIMPLE_NAME, $FLOAT_LITERAL_SIMPLE_NAME.text];

double_literal
	:	DOUBLE_LITERAL -> DOUBLE_LITERAL
	|	DOUBLE_LITERAL_SIMPLE_NAME -> DOUBLE_LITERAL[$DOUBLE_LITERAL_SIMPLE_NAME, $DOUBLE_LITERAL_SIMPLE_NAME.text];
	
ACCESS_SPEC
	:	'public' | 'private' | 'static' | 'constructor' | 'final';

INVOKE_INSTRUCTION_NAME
	:	'invoke-virtual'
	|	'invoke-super'
	|	'invoke-direct'
	|	'invoke-static'
	|	'invoke-interface'
	;
	
INVOKE_RANGE_INSTRUCTION_NAME
	:	'invoke-virtual/range'
	|	'invoke-super/range'
	|	'invoke-direct/range'
	|	'invoke-static/range'
	|	'invoke-interface/range'
	;

STATIC_FIELD_INSTRUCTION_NAME
	:	'sget'
	|	'sget-wide'
	|	'sget-object'
	|	'sget-boolean'
	|	'sget-byte'
	|	'sget-char'
	|	'sget-short'
	|	'sput'
	|	'sput-wide'
	|	'sput-object'
	|	'sput-boolean'
	|	'sput-byte'
	|	'sput-char'
	|	'sput-short'
	;
	
INSTANCE_FIELD_INSTRUCTION_NAME
	:	'iget'
	|	'iget-wide'
	|	'iget-object'
	|	'iget-boolean'
	|	'iget-byte'
	|	'iget-char'
	|	'iget-short'
	|	'iput'
	|	'iput-wide'
	|	'iput-object'
	|	'iput-boolean'
	|	'iput-byte'
	|	'iput-char'
	|	'iput-short'
	;
	
BARE_INSTRUCTION_NAME
	:	'return-void'
	|	'nop';
	
	
CONST_STRING_INSTRUCTION_NAME
	:	'const-string';

CONST_CLASS_INSTRUCTION_NAME
	:	'const-class';
	
/*CHECK_CAST_INSTRUCTION_NAME
	:	'check-cast';*/
	
NEW_INSTANCE_INSTRUCTION_NAME
	:	'new-instance';
	
SINGLE_REGISTER_INSTRUCTION_NAME
	:	'move-result'
	|	'move-result-wide'
	|	'move-result-object'
	|	'move-exception'
	|	'return'
	|	'return-wide'
	|	'return-object'
	|	'monitor-enter'
	|	'monitor-exit'
	|	'throw';


/*since SIMPLE_NAME is so all-encompassing, it includes all integer literals
and a subset of the possible floating point literals. For floating point
literals, we need to generate a separate token depending on whether the token
could also be considered a SIMPLE_NAME or not.

The floating point related tokens with a _SIMPLE_NAME suffix could also be
considered valid SIMPLE_NAME tokens, while the plain version of the token
(without the suffix) could not be considered a valid SIMPLE_NAME token*/

LONG_LITERAL
	:	Integer_number Long_suffix;
    
INT_LITERAL
	:	Integer_number;

fragment Integer_number
	:	'-'? '0' 
	|	'-'? ('1'..'9') ('0'..'9')*    
	|	'0' ('0'..'7')+         
	|	Hex_prefix Hex_digit+        
	;

fragment Hex_prefix
	:	'0x'|'0X';
        
fragment Hex_digit
	:	('0'..'9'|'a'..'f'|'A'..'F');

fragment Long_suffix
	:	'l'|'L';

fragment Non_integer_number_SIMPLE_NAME
	:	('0'..'9')+ Decimal_exponent  
	|	('0'..'9')+ 
	|	Hex_prefix (Hex_digit)* Hex_exponent
        ;
        

fragment Non_integer_number
	:	('0'..'9')+ '.' ('0'..'9')* Decimal_exponent?  
	|	'.' ('0'..'9')+ Decimal_exponent? 
	|	Hex_prefix (Hex_digit)* '.' (Hex_digit)* Hex_exponent
        ;
        
fragment Decimal_exponent
	:	('e'|'E') '-'? ('0'..'9')+;
   
fragment Hex_exponent 
	:	('p'|'P') '-'? ('0'..'9')+;
    
fragment Float_suffix
	:	'f'|'F';     

fragment Double_suffix
	:	'd'|'D';
        
FLOAT_LITERAL_SIMPLE_NAME
	:	Non_integer_number_SIMPLE_NAME Float_suffix;
    
FLOAT_LITERAL
	:	Non_integer_number Float_suffix;
    
DOUBLE_LITERAL_SIMPLE_NAME
	:	Non_integer_number_SIMPLE_NAME Double_suffix?;
    
DOUBLE_LITERAL
	:	Non_integer_number Double_suffix?;

CHAR_LITERAL
	
    :   '\'' {StringBuilder sb = new StringBuilder();}
        (   Escape_sequence[sb] {setText(sb.toString());}
        |   ~( '\'' | '\\' | '\r' | '\n' )
        ) 
        '\''
    ; 

STRING_LITERAL
    :   '"' {StringBuilder sb = new StringBuilder();}
        (   Escape_sequence[sb]
        |   ~( '\\' | '"' | '\r' | '\n' ) {sb.append((char)input.LA(-1));}
        )* 
        '"' {setText(sb.toString());}
    ;


Hex_digits
	:	Hex_digit Hex_digit Hex_digit Hex_digit;

fragment
Escape_sequence[StringBuilder sb]
	:	'\\'
		(
			'b' {sb.append("\b");}
		|	't' {sb.append("\t");}
		|	'n' {sb.append("\n");}
		|	'f' {sb.append("\f");}
		|	'r' {sb.append("\r");}
		|	'\"' {sb.append("\"");}
		|	'\'' {sb.append("'");}
		|	'\\' {sb.append("\\");}
		|	'u' Hex_digits {sb.append((char)Integer.parseInt($Hex_digits.text, 16));}
/*		|	octdigits=(('0'..'3') ('0'..'7') ('0'..'7')) {$value = (char)Integer.parseInt("0" + $octdigits.text);}
		|	octdigits=(('0'..'7') ('0'..'7')) {$value = (char)Integer.parseInt("0" + $octdigits.text);}
		|	octdigits=(('0'..'7')) {$value = (char)Integer.parseInt("0" + $octdigits.text);}*/
		);
             
BOOL_LITERAL
	:	'true'|'false';



WHITESPACE
	:	(' '|'\t'|'\n'|'\r')+ {$channel = HIDDEN;};
	
REGISTER:	'v' ('0'..'9')+;
	

/*a token of type QUALIFIED_MEMBER is never generated. This rule emits 2 sub-tokens
that represent the class name and the member name, so that they don't have to be
parsed out later*/
QUALIFIED_MEMBER
	:	class_name=QUALIFIED_MEMBER__CLASS_NAME '.' member_name=QUALIFIED_MEMBER__MEMBER_NAME
	{
		$class_name.setType(QUALIFIED_MEMBER__CLASS_NAME);
		$member_name.setType(QUALIFIED_MEMBER__MEMBER_NAME);
		emit($class_name);
		emit($member_name);
	};
	
fragment QUALIFIED_MEMBER__CLASS_NAME
	:	(SIMPLE_NAME '/')* SIMPLE_NAME;
	
fragment QUALIFIED_MEMBER__MEMBER_NAME
	:	MEMBER_NAME | SIMPLE_NAME;

	
ARRAY_TYPE
	:	
	ARRAY_CHAR_LIST[255] (PRIMITIVE_TYPE | CLASS_DESCRIPTOR);
	

//match from 1 to maxCount '[' characters
fragment
ARRAY_CHAR_LIST[int maxCount]
	:	{$maxCount > 1}?=> '[' ARRAY_CHAR_LIST[$maxCount - 1]
	|	'['
	;

MEMBER_NAME
	:	'<' SIMPLE_NAME '>';

VOID_TYPE
	:	'V';
	
PRIMITIVE_TYPE
	:	'Z'
	|	'B'
	|	'S'
	|	'C'
	|	'I'
	|	'J'
	|	'F'
	|	'D'
	;
	
CLASS_WITH_PACKAGE_NAME
	:	(SIMPLE_NAME '/')+ SIMPLE_NAME;
	
CLASS_DESCRIPTOR
	:	'L' (SIMPLE_NAME | CLASS_WITH_PACKAGE_NAME) ';';	

SIMPLE_NAME:
	(	'A'..'Z'
	|	'a'..'z'
	|	'0'..'9'
	|	'$'
	|	'-'
	|	'_'
	|	'\u00a1'..'\u1fff'
	|	'\u2010'..'\u2027'
	|	'\u2030'..'\ud7ff'
	|	'\ue000'..'\uffef'
	)+;

COMMENT
    :   (';' ~('\n'|'\r')*  ('\r\n' | '\r' | '\n')
    |   ';' ~('\n'|'\r')*)
 	{
 		$channel = HIDDEN;
 	}
    ; 
