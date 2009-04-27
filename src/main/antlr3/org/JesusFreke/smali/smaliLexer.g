/* 
 * The comment, number, string and character constant lexical rules are
 derived from rules from the Java 1.6 grammar which can be found here:
 * http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
 * 
 * Specifically, these rules:
 * 
 * STRING_LITERAL, ESCAPE_SEQUENCE, HEX_DIGITS, HEX_DIGIT, INTEGER_LITERAL,
 * HEX_PREFIX, LONG_LITERAL, FLOATING_POINT_NUMBER, DECIMAL_EXPONENT,
 * HEX_EXPONENT, FLOAT_LITERAL, DOUBLE_LITERAL, CHAR_LITERAL, LINE_COMMENT
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



/*smali files are particularly hard to tokenize, because of dex's
identifiers, which are much more all-encompassing than languages'.
One reasonable possibility would be to limit the identifiers to what Java
supports. But I want the syntax to expose the full functionality of the dex
format, so that means supporting the wide range of identifiers that it
supports.

This makes tokenizing a much more context sensitive operation than usual. To
address this, I've added extended the base lexer class to support multiple
token emissions per rule. The top level *_PHRASE lexical rules generally 
match a "phrase". Each phrase has a specific format, and a unique starting
sequence - typically a directive or opcode. Each phrase rule doesn't generate
a token that represents itself, like a typical lexical rule, rather, it emits
all of its children tokens.

For example, a phrase may consist of ".field private helloWorld Ljava/lang/String;".

The corresponding rule (without the supporting emission code) would look something like

FIELD_PHRASE : '.field' ACCESS_SPEC+ MEMBER_NAME FIELD_TYPE_DESCRIPTOR

There would never be a "FIELD_PHRASE" token in the output token stream. Instead,
it would emit a token for each of its children tokens.*/


lexer grammar smaliLexer;

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
			
			System.out.println(token.toString());
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
	
	public void emit(Token token, int type) {
		token.setType(type);
		tokens.add(token);
	}
	
	public void emit(Token token, int type, int channel) {
		token.setType(type);
		token.setChannel(channel);
		tokens.add(token);
	}
	
/*protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException
{
	throw new MismatchedTokenException(ttype, input);
}

public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException
{
	throw e;
}*/

}

/*@rulecatch {
catch (RecognitionException e) {
throw e;
}
}*/


CLASS_PHRASE
	:	CLASS_DIRECTIVE_EMIT
		WS
		(ACCESS_SPEC_EMIT WS)+
		CLASS_DESCRIPTOR_EMIT;
		
SUPER_PHRASE
	:	SUPER_DIRECTIVE_EMIT
		WS
		CLASS_DESCRIPTOR_EMIT;
		
FIELD_PHRASE
	:	FIELD_DIRECTIVE_EMIT
		WS
		(ACCESS_SPEC_EMIT WS)+
		MEMBER_NAME_EMIT
		WS
		FIELD_TYPE_DESCRIPTOR_EMITCHILD
		WS?
		('=' WS? LITERAL_EMITCHILD)?;

METHOD_PHRASE
	:	METHOD_DIRECTIVE_EMIT
		WS
		(ACCESS_SPEC_EMIT WS)+
		MEMBER_NAME_EMIT
		METHOD_PROTOTYPE_EMITCHILDREN;
		
END_METHOD_PHRASE
	:	END_METHOD_DIRECTIVE_EMIT;
		
REGISTERS_PHRASE
	:	REGISTERS_DIRECTIVE_EMIT
		WS
		INTEGER_LITERAL_EMIT;
		
INSTRUCTION_FORMAT10t_PHRASE
	:	INSTRUCTION_FORMAT10t_EMIT
		WS
		(LABEL_EMIT | OFFSET_EMIT);

INSTRUCTION_FORMAT10x_PHRASE
	:	INSTRUCTION_FORMAT10x_EMIT;
	
INSTRUCTION_FORMAT11n_PHRASE
	:	INSTRUCTION_FORMAT11n_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		INTEGER_LITERAL_EMIT;
	
INSTRUCTION_FORMAT11x_PHRASE
	:	INSTRUCTION_FORMAT11x_EMIT
		WS
		REGISTER_EMIT;

INSTRUCTION_FORMAT12x_PHRASE
	:	INSTRUCTION_FORMAT12x_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		REGISTER_EMIT;
		
INSTRUCTION_FORMAT20t_PHRASE
	:	INSTRUCTION_FORMAT20t_EMIT
		WS
		(LABEL_EMIT | OFFSET_EMIT);

INSTRUCTION_FORMAT21c_FIELD_PHRASE
	:	INSTRUCTION_FORMAT21c_FIELD_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		FULLY_QUALIFIED_MEMBER_NAME_EMITCHILDREN
		WS
		FIELD_TYPE_DESCRIPTOR_EMITCHILD;
		
INSTRUCTION_FORMAT21c_STRING_PHRASE
	:	INSTRUCTION_FORMAT21c_STRING_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		STRING_LITERAL_EMIT;
		
INSTRUCTION_FORMAT21c_TYPE_PHRASE
	:	INSTRUCTION_FORMAT21c_TYPE_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		CLASS_OR_ARRAY_TYPE_DESCRIPTOR_EMITCHILD;
		
INSTRUCTION_FORMAT21t_PHRASE
	:	INSTRUCTION_FORMAT21t_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		(LABEL_EMIT | OFFSET_EMIT);
		
INSTRUCTION_FORMAT22c_FIELD_PHRASE
	:	INSTRUCTION_FORMAT22c_FIELD_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		REGISTER_EMIT
		WS? ',' WS?
		FULLY_QUALIFIED_MEMBER_NAME_EMITCHILDREN
		WS
		FIELD_TYPE_DESCRIPTOR_EMITCHILD;
		
INSTRUCTION_FORMAT22x_PHRASE
	:	INSTRUCTION_FORMAT22x_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		REGISTER_EMIT;		
		
INSTRUCTION_FORMAT30t_PHRASE
	:	INSTRUCTION_FORMAT30t_EMIT
		WS
		(LABEL_EMIT | OFFSET_EMIT);
		
INSTRUCTION_FORMAT32x_PHRASE
	:	INSTRUCTION_FORMAT32x_EMIT
		WS
		REGISTER_EMIT
		WS? ',' WS?
		REGISTER_EMIT;		
		
INSTRUCTION_FORMAT35c_METHOD_PHRASE
	:	INSTRUCTION_FORMAT35c_METHOD_EMIT
		WS
		REGISTER_LIST_EMITCHILDREN
		WS? ',' WS?
		FULLY_QUALIFIED_MEMBER_NAME_EMITCHILDREN
		METHOD_PROTOTYPE_EMITCHILDREN;
		
INSTRUCTION_FORMAT3rc_METHOD_PHRASE
	:	INSTRUCTION_FORMAT3rc_METHOD_EMIT
		WS
		REGISTER_RANGE_EMITCHILDREN
		WS? ',' WS?
		FULLY_QUALIFIED_MEMBER_NAME_EMITCHILDREN
		METHOD_PROTOTYPE_EMITCHILDREN;


fragment OFFSET_EMIT
	:	OFFSET {emit($OFFSET, OFFSET);};
fragment OFFSET
	:	('+' | '-') INTEGER_LITERAL;

fragment LABEL_EMIT
	:	LABEL {emit($LABEL, LABEL);};
LABEL
	:	SIMPLE_NAME ':';

fragment CLASS_DIRECTIVE_EMIT
	:	CLASS_DIRECTIVE {emit($CLASS_DIRECTIVE, CLASS_DIRECTIVE);};
fragment CLASS_DIRECTIVE
	:	'.class';
	
fragment SUPER_DIRECTIVE_EMIT
	:	SUPER_DIRECTIVE {emit($SUPER_DIRECTIVE, SUPER_DIRECTIVE);};
fragment SUPER_DIRECTIVE
	:	'.super';
	
fragment FIELD_DIRECTIVE_EMIT
	:	FIELD_DIRECTIVE {emit($FIELD_DIRECTIVE, FIELD_DIRECTIVE);};
fragment FIELD_DIRECTIVE
	:	'.field';

fragment METHOD_DIRECTIVE_EMIT
	:	METHOD_DIRECTIVE {emit($METHOD_DIRECTIVE, METHOD_DIRECTIVE);};
fragment METHOD_DIRECTIVE
	:	'.method';
	
fragment END_METHOD_DIRECTIVE_EMIT
	:	END_METHOD_DIRECTIVE {emit($END_METHOD_DIRECTIVE, END_METHOD_DIRECTIVE);};
fragment END_METHOD_DIRECTIVE
	:	'.end method';

fragment REGISTERS_DIRECTIVE_EMIT
	:	REGISTERS_DIRECTIVE {emit($REGISTERS_DIRECTIVE, REGISTERS_DIRECTIVE);};
fragment REGISTERS_DIRECTIVE
	:	'.registers';
	
fragment REGISTER_EMIT
	:	REGISTER {emit($REGISTER, REGISTER);};
fragment REGISTER
	:	'v' ('0'..'9')+;
	

fragment REGISTER_LIST_EMITCHILDREN
	:	OPEN_BRACKET_EMIT
		(	WS?
			REGISTER_EMIT (WS? ',' WS? REGISTER_EMIT)*
			WS? 
		|	WS?)
		CLOSE_BRACKET_EMIT;


fragment REGISTER_RANGE_EMITCHILDREN
	:	OPEN_BRACKET_EMIT
		WS?
		REGISTER_EMIT
		WS?
		('..' WS?
			REGISTER_EMIT)?
		CLOSE_BRACKET_EMIT;
				

fragment METHOD_PROTOTYPE_EMITCHILDREN
	:	OPEN_PAREN_EMIT
		(FIELD_TYPE_DESCRIPTOR_EMITCHILD+)?
		CLOSE_PAREN_EMIT
		TYPE_DESCRIPTOR_EMITCHILD;

fragment FULLY_QUALIFIED_MEMBER_NAME_EMITCHILDREN
@init {int startPos;}
	:	{startPos = getCharIndex();} (SIMPLE_NAME '/')* token=SIMPLE_NAME {((CommonToken)$token).setStartIndex(startPos); emit($token, CLASS_NAME);}
		'/'
		MEMBER_NAME_EMIT;
		
fragment TYPE_DESCRIPTOR_EMITCHILD
	:	PRIMITIVE_TYPE_EMIT
	|	VOID_TYPE_EMIT
	|	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;
	
	
fragment FIELD_TYPE_DESCRIPTOR_EMITCHILD
	:	PRIMITIVE_TYPE_EMIT
	|	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;
	
fragment CLASS_OR_ARRAY_TYPE_DESCRIPTOR_EMITCHILD
	:	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;

fragment PRIMITIVE_TYPE_EMIT
	:	PRIMITIVE_TYPE {emit($PRIMITIVE_TYPE, PRIMITIVE_TYPE);};

fragment PRIMITIVE_TYPE
	:	'Z'
	|	'B'
	|	'S'
	|	'C'
	|	'I'
	|	'J'
	|	'F'
	|	'D'
	;


fragment VOID_TYPE_EMIT
	:	VOID_TYPE {emit($VOID_TYPE, VOID_TYPE);};
fragment VOID_TYPE
	:	'V';


fragment CLASS_DESCRIPTOR_EMIT
	:	CLASS_DESCRIPTOR {emit($CLASS_DESCRIPTOR, CLASS_DESCRIPTOR);};

fragment CLASS_DESCRIPTOR
	:	'L' CLASS_NAME ';';

fragment CLASS_NAME
	:	(SIMPLE_NAME '/')* SIMPLE_NAME;	


fragment ARRAY_DESCRIPTOR_EMIT
	:	ARRAY_DESCRIPTOR {emit($ARRAY_DESCRIPTOR, ARRAY_DESCRIPTOR);};

fragment ARRAY_DESCRIPTOR
	:	ARRAY_TYPE_PREFIX (PRIMITIVE_TYPE | CLASS_DESCRIPTOR);
	
fragment ARRAY_TYPE_PREFIX
	:	ARRAY_CHAR_LIST[255];
	
fragment ARRAY_CHAR_LIST[int maxCount]
	:	{$maxCount > 1}?=> '[' ARRAY_CHAR_LIST[$maxCount - 1]
	|	'['
	;


fragment ACCESS_SPEC_EMIT
	:	ACCESS_SPEC {emit($ACCESS_SPEC, ACCESS_SPEC);};

fragment ACCESS_SPEC
	:	'public'
	|	'private'
	|	'static'
	|	'constructor'
	|	'final';

		

fragment MEMBER_NAME_EMIT
	:	MEMBER_NAME {emit($MEMBER_NAME, MEMBER_NAME);};

fragment MEMBER_NAME
	:	'<'? SIMPLE_NAME '>'?;

	
fragment SIMPLE_NAME:
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


fragment LITERAL_EMITCHILD
	:	STRING_LITERAL_EMIT
	|	INTEGER_LITERAL_EMIT
	|	LONG_LITERAL_EMIT
	|	FLOAT_LITERAL_EMIT
	|	DOUBLE_LITERAL_EMIT
	|	CHAR_LITERAL_EMIT
	|	BOOL_LITERAL_EMIT;


fragment STRING_LITERAL_EMIT
	@init {StringBuilder sb = new StringBuilder();}
	:	STRING_LITERAL[sb]
	{
		$STRING_LITERAL.setText(sb.toString());
		emit($STRING_LITERAL, STRING_LITERAL);
	};

fragment STRING_LITERAL [StringBuilder sb]
    :   '"' {sb.append('"');}
        (   ESCAPE_SEQUENCE[sb]
        |   ~( '\\' | '"' | '\r' | '\n' ) {sb.append((char)input.LA(-1));}
        )* 
        '"' {sb.append('"');}
        ;
        
fragment
ESCAPE_SEQUENCE[StringBuilder sb]
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
		|	'u' HEX_DIGITS {sb.append((char)Integer.parseInt($HEX_DIGITS.text, 16));}
		);
		
fragment HEX_DIGITS
	:	HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
	
fragment HEX_DIGIT
	:	('0'..'9'|'a'..'f'|'A'..'F');
	
	
fragment INTEGER_LITERAL_EMIT
	:	INTEGER_LITERAL {emit($INTEGER_LITERAL, INTEGER_LITERAL);};

fragment INTEGER_LITERAL
	:	'-'? '0' 
	|	'-'? ('1'..'9') ('0'..'9')*    
	|	'0' ('0'..'7')+         
	|	HEX_PREFIX HEX_DIGIT+        
	;

fragment HEX_PREFIX
	:	'0x'|'0X';
	

fragment LONG_LITERAL_EMIT
	:	LONG_LITERAL {emit($LONG_LITERAL, LONG_LITERAL);};
fragment LONG_LITERAL
	:	INTEGER_LITERAL ('l' | 'L');
	
	
fragment FLOATING_POINT_NUMBER
    :   ('0' .. '9')+ '.' ('0' .. '9')* DECIMAL_EXPONENT?  
    |   '.' ( '0' .. '9' )+ DECIMAL_EXPONENT?  
    |   ('0' .. '9')+ DECIMAL_EXPONENT  
    |   HEX_PREFIX
        (	HEX_DIGIT+ ('.' HEX_DIGIT*)? 
        |	'.' HEX_DIGIT+
        )
        BINARY_EXPONENT
        ;
     
fragment DECIMAL_EXPONENT
	:	('e'|'E') '-'? ('0'..'9')+;

fragment BINARY_EXPONENT
	:	('p'|'P') '-'? ('0'..'9')+;
    
       
fragment FLOAT_LITERAL_EMIT
	:	FLOAT_LITERAL {emit($FLOAT_LITERAL, FLOAT_LITERAL);};
fragment FLOAT_LITERAL
	:	(FLOATING_POINT_NUMBER | ('0' .. '9')+) ('f' | 'F');
       
fragment DOUBLE_LITERAL_EMIT
	:	DOUBLE_LITERAL {emit($DOUBLE_LITERAL, DOUBLE_LITERAL);};
fragment DOUBLE_LITERAL
	:	FLOATING_POINT_NUMBER ('d' | 'D')?
	|	('0' .. '9')+ ('d' | 'D');


fragment CHAR_LITERAL_EMIT
	:	CHAR_LITERAL {emit($CHAR_LITERAL, CHAR_LITERAL);};
fragment CHAR_LITERAL
    :   '\'' {StringBuilder sb = new StringBuilder("'");}
        (   ESCAPE_SEQUENCE[sb] {sb.append("'"); setText(sb.toString());}
        |   ~( '\'' | '\\' | '\r' | '\n' )
        ) 
        '\''
    ;

fragment BOOL_LITERAL_EMIT
	:	BOOL_LITERAL {emit($BOOL_LITERAL, BOOL_LITERAL);};
fragment BOOL_LITERAL
	:	'true'|'false';
	
fragment INSTRUCTION_FORMAT10t_EMIT
	:	INSTRUCTION_FORMAT10t {emit($INSTRUCTION_FORMAT10t, INSTRUCTION_FORMAT10t);};
fragment INSTRUCTION_FORMAT10t
	:	'goto';

fragment INSTRUCTION_FORMAT10x_EMIT
	:	INSTRUCTION_FORMAT10x {emit($INSTRUCTION_FORMAT10x, INSTRUCTION_FORMAT10x);};
fragment INSTRUCTION_FORMAT10x
	:	'return-void'
	|	'nop';
	
fragment INSTRUCTION_FORMAT11n_EMIT
	:	INSTRUCTION_FORMAT11n {emit($INSTRUCTION_FORMAT11n, INSTRUCTION_FORMAT11n);};
fragment INSTRUCTION_FORMAT11n
	:	'const/4';
	
fragment INSTRUCTION_FORMAT11x_EMIT
	:	INSTRUCTION_FORMAT11x {emit($INSTRUCTION_FORMAT11x, INSTRUCTION_FORMAT11x);};
fragment INSTRUCTION_FORMAT11x
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
	
fragment INSTRUCTION_FORMAT12x_EMIT
	:	INSTRUCTION_FORMAT12x {emit($INSTRUCTION_FORMAT12x, INSTRUCTION_FORMAT12x);};
fragment INSTRUCTION_FORMAT12x
	:	'move'
	|	'move-wide'
	|	'move-object'
	|	'array-length'
	|	'neg-int'
	|	'not-int'
	|	'neg-long'
	|	'not-long'
	|	'neg-float'
	|	'neg-double'
	|	'int-to-long'
	|	'int-to-float'
	|	'int-to-double'
	|	'long-to-int'
	|	'long-to-float'
	|	'long-to-double'
	|	'float-to-int'
	|	'float-to-long'
	|	'float-to-double'
	|	'double-to-int'
	|	'double-to-long'
	|	'double-to-float'
	|	'int-to-byte'
	|	'int-to-char'
	|	'int-to-short'
	|	'add-int/2addr'
	|	'sub-int/2addr'
	|	'mul-int/2addr'
	|	'div-int/2addr'
	|	'rem-int/2addr'
	|	'and-int/2addr'
	|	'or-int/2addr'
	|	'xor-int/2addr'
	|	'shl-int/2addr'
	|	'shr-int/2addr'
	|	'ushr-int/2addr'
	|	'add-long/2addr'
	|	'sub-long/2addr'
	|	'mul-long/2addr'
	|	'div-long/2addr'
	|	'rem-long/2addr'
	|	'and-long/2addr'
	|	'or-long/2addr'
	|	'xor-long/2addr'
	|	'shl-long/2addr'
	|	'shr-long/2addr'
	|	'ushr-long/2addr'
	|	'add-float/2addr'
	|	'sub-float/2addr'
	|	'mul-float/2addr'
	|	'div-float/2addr'
	|	'rem-float/2addr'
	|	'add-double/2addr'
	|	'sub-double/2addr'
	|	'mul-double/2addr'
	|	'div-double/2addr'
	|	'rem-double/2addr';

fragment INSTRUCTION_FORMAT20t_EMIT
	:	INSTRUCTION_FORMAT20t {emit($INSTRUCTION_FORMAT20t, INSTRUCTION_FORMAT20t);};
fragment INSTRUCTION_FORMAT20t
	:	'goto/16';
	
fragment INSTRUCTION_FORMAT21c_FIELD_EMIT
	:	INSTRUCTION_FORMAT21c_FIELD {emit($INSTRUCTION_FORMAT21c_FIELD, INSTRUCTION_FORMAT21c_FIELD);};
fragment INSTRUCTION_FORMAT21c_FIELD
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
	
fragment INSTRUCTION_FORMAT21c_STRING_EMIT
	:	INSTRUCTION_FORMAT21c_STRING {emit($INSTRUCTION_FORMAT21c_STRING, INSTRUCTION_FORMAT21c_STRING);};
fragment INSTRUCTION_FORMAT21c_STRING
	:	'const-string';
	
fragment INSTRUCTION_FORMAT21c_TYPE_EMIT
	:	INSTRUCTION_FORMAT21c_TYPE {emit($INSTRUCTION_FORMAT21c_TYPE, INSTRUCTION_FORMAT21c_TYPE);};
fragment INSTRUCTION_FORMAT21c_TYPE
	:	'check-cast'
	|	'new-instance'
	|	'const-class';
	
fragment INSTRUCTION_FORMAT21t_EMIT
	:	INSTRUCTION_FORMAT21t {emit($INSTRUCTION_FORMAT21t, INSTRUCTION_FORMAT21t);};
fragment INSTRUCTION_FORMAT21t
	:	'if-eqz'
	|	'if-nez'
	|	'if-ltz'
	|	'if-gez'
	|	'if-gtz'
	|	'if-lez';
	
fragment INSTRUCTION_FORMAT22c_FIELD_EMIT
	:	INSTRUCTION_FORMAT22c_FIELD {emit($INSTRUCTION_FORMAT22c_FIELD, INSTRUCTION_FORMAT22c_FIELD);};
fragment INSTRUCTION_FORMAT22c_FIELD
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
	
fragment INSTRUCTION_FORMAT22x_EMIT
	:	INSTRUCTION_FORMAT22x {emit($INSTRUCTION_FORMAT22x, INSTRUCTION_FORMAT22x);};
fragment INSTRUCTION_FORMAT22x
	:	'move/from16'
	|	'move-wide/from16'
	|	'move-object/from16'
	;
	
fragment INSTRUCTION_FORMAT30t_EMIT
	:	INSTRUCTION_FORMAT30t {emit($INSTRUCTION_FORMAT30t, INSTRUCTION_FORMAT30t);};
fragment INSTRUCTION_FORMAT30t
	:	'goto/32';
	
fragment INSTRUCTION_FORMAT32x_EMIT
	:	INSTRUCTION_FORMAT32x {emit($INSTRUCTION_FORMAT32x, INSTRUCTION_FORMAT32x);};
fragment INSTRUCTION_FORMAT32x
	:	'move/16'
	|	'move-wide/16'
	|	'move-object/16'
	;	
	
fragment INSTRUCTION_FORMAT35c_METHOD_EMIT
	:	INSTRUCTION_FORMAT35c_METHOD {emit($INSTRUCTION_FORMAT35c_METHOD, INSTRUCTION_FORMAT35c_METHOD);};
fragment INSTRUCTION_FORMAT35c_METHOD
	:	'invoke-virtual'
	|	'invoke-super'
	|	'invoke-direct'
	|	'invoke-static'
	|	'invoke-interface'
	;
	
fragment INSTRUCTION_FORMAT3rc_METHOD_EMIT
	:	INSTRUCTION_FORMAT3rc_METHOD {emit($INSTRUCTION_FORMAT3rc_METHOD, INSTRUCTION_FORMAT3rc_METHOD);};
fragment INSTRUCTION_FORMAT3rc_METHOD
	:	'invoke-virtual/range'
	|	'invoke-super/range'
	|	'invoke-direct/range'
	|	'invoke-static/range'
	|	'invoke-interface/range'
	;


fragment OPEN_PAREN_EMIT
	:	OPEN_PAREN {emit($OPEN_PAREN, OPEN_PAREN);};
fragment OPEN_PAREN
	:	'(';

fragment CLOSE_PAREN_EMIT
	:	CLOSE_PAREN {emit($CLOSE_PAREN, CLOSE_PAREN);};
fragment CLOSE_PAREN
	:	')';
	
fragment OPEN_BRACKET_EMIT
	:	OPEN_BRACKET {emit($OPEN_BRACKET, OPEN_BRACKET);};
fragment OPEN_BRACKET
	:	'{';
	
fragment CLOSE_BRACKET_EMIT
	:	CLOSE_BRACKET {emit($CLOSE_BRACKET, CLOSE_BRACKET);};
fragment CLOSE_BRACKET
	:	'}';

fragment WS
	:	WHITE_SPACE {emit($WHITE_SPACE, WHITE_SPACE, Token.HIDDEN_CHANNEL);};

WHITE_SPACE
	:	(' '|'\t'|'\n'|'\r')+ {$channel = HIDDEN;};


LINE_COMMENT
	:	(';' ~('\n'|'\r')*  ('\r\n' | '\r' | '\n') 
	|	';' ~('\n'|'\r')*)
		{$channel = HIDDEN;};   
