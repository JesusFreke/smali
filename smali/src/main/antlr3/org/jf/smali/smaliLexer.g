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

tokens {
	ACCESS_SPEC;
}

@lexer::header {
package org.jf.smali;

import java.util.LinkedList;
}

@lexer::init {
	state.token = Token.INVALID_TOKEN;
}

@lexer::members {
	protected LinkedList<Token> tokens = new LinkedList<Token>();
	
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
    			}
    		}
    	}

	public void skip() {
		tokens.add(Token.SKIP_TOKEN);
	}

	 public void emit(Token token) {
	 	token.setLine(state.tokenStartLine);
	 	token.setCharPositionInLine(state.tokenStartCharPositionInLine);
    		tokens.add(token);
    	}
    	
    	public void emit(Token token, int type) {
    		token.setLine(state.tokenStartLine);
    		token.setCharPositionInLine(state.tokenStartCharPositionInLine);
    		token.setType(type);
    		tokens.add(token);
    	}
    	
    	public void emit(Token token, int type, int channel) {
    		token.setLine(state.tokenStartLine);
    		token.setCharPositionInLine(state.tokenStartCharPositionInLine);
    		token.setType(type);
    		token.setChannel(channel);
    		tokens.add(token);
    	}

	private int lexerErrors = 0;
	public String getErrorHeader(RecognitionException e) {
		lexerErrors++;
		return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
	}
	
	public int getNumberOfLexerErrors() {
		return lexerErrors;
	}
}



CLASS_PHRASE
	:	CLASS_DIRECTIVE_EMIT
		WS
		(CLASS_ACCESS_SPEC_EMIT WS)*
		CLASS_DESCRIPTOR_EMIT;
		
SUPER_PHRASE
	:	SUPER_DIRECTIVE_EMIT
		WS
		CLASS_DESCRIPTOR_EMIT;
		
IMPLEMENTS_PHRASE
	:	IMPLEMENTS_DIRECTIVE_EMIT
		WS
		CLASS_DESCRIPTOR_EMIT;
		
SOURCE_PHRASE
	:	SOURCE_DIRECTIVE_EMIT
		WS
		STRING_LITERAL_EMIT;			
		
FIELD_PHRASE
	:	FIELD_DIRECTIVE_EMIT
		WS
		(FIELD_ACCESS_SPEC_EMIT WS)*
		MEMBER_NAME_EMIT
		COLON_EMIT
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD
		WS?
		(EQUAL_EMIT WS? LITERAL_EMITCHILD)?;

END_FIELD_PHRASE
	:	END_FIELD_DIRECTIVE_EMIT;

METHOD_PHRASE
	:	METHOD_DIRECTIVE_EMIT
		WS
		(METHOD_ACCESS_SPEC_EMIT WS)*
		MEMBER_NAME_EMIT
		METHOD_PROTOTYPE_EMITCHILDREN;
		
END_METHOD_PHRASE
	:	END_METHOD_DIRECTIVE_EMIT;
		
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
		WS? COMMA_EMIT WS?
		INTEGRAL_LITERAL_EMITCHILD;
	
INSTRUCTION_FORMAT11x_PHRASE
	:	INSTRUCTION_FORMAT11x_EMIT
		WS
		REGISTER_EMIT;

INSTRUCTION_FORMAT12x_PHRASE
	:	INSTRUCTION_FORMAT12x_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT;
		
INSTRUCTION_FORMAT20t_PHRASE
	:	INSTRUCTION_FORMAT20t_EMIT
		WS
		(LABEL_EMIT | OFFSET_EMIT);

INSTRUCTION_FORMAT21c_FIELD_PHRASE
	:	INSTRUCTION_FORMAT21c_FIELD_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		FULLY_QUALIFIED_FIELD_EMITCHILDREN;
		
INSTRUCTION_FORMAT21c_STRING_PHRASE
	:	INSTRUCTION_FORMAT21c_STRING_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		STRING_LITERAL_EMIT;
		
INSTRUCTION_FORMAT21c_TYPE_PHRASE
	:	INSTRUCTION_FORMAT21c_TYPE_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REFERENCE_TYPE_DESCRIPTOR_EMITCHILD;
		
INSTRUCTION_FORMAT21h_PHRASE
	:	INSTRUCTION_FORMAT21h_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		INTEGRAL_LITERAL_EMITCHILD;

INSTRUCTION_FORMAT21s_PHRASE
	:	INSTRUCTION_FORMAT21s_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		INTEGRAL_LITERAL_EMITCHILD;
		
INSTRUCTION_FORMAT21t_PHRASE
	:	INSTRUCTION_FORMAT21t_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		(LABEL_EMIT | OFFSET_EMIT);
		
INSTRUCTION_FORMAT22b_PHRASE
	:	INSTRUCTION_FORMAT22b_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		INTEGRAL_LITERAL_EMITCHILD;
		
INSTRUCTION_FORMAT22c_FIELD_PHRASE
	:	INSTRUCTION_FORMAT22c_FIELD_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		FULLY_QUALIFIED_FIELD_EMITCHILDREN;

INSTRUCTION_FORMAT22c_TYPE_PHRASE
	:	INSTRUCTION_FORMAT22c_TYPE_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD;

INSTRUCTION_FORMAT22s_PHRASE
	:	INSTRUCTION_FORMAT22s_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		INTEGRAL_LITERAL_EMITCHILD;
		
INSTRUCTION_FORMAT22t_PHRASE
	:	INSTRUCTION_FORMAT22t_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		(LABEL_EMIT | OFFSET_EMIT);		
		
INSTRUCTION_FORMAT22x_PHRASE
	:	INSTRUCTION_FORMAT22x_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT;

INSTRUCTION_FORMAT23x_PHRASE
	:	INSTRUCTION_FORMAT23x_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT;
		
INSTRUCTION_FORMAT30t_PHRASE
	:	INSTRUCTION_FORMAT30t_EMIT
		WS
		(LABEL_EMIT | OFFSET_EMIT);
		
INSTRUCTION_FORMAT31c_PHRASE
	:	INSTRUCTION_FORMAT31c_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		STRING_LITERAL_EMIT;		

INSTRUCTION_FORMAT31i_PHRASE
	:	INSTRUCTION_FORMAT31i_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		(FIXED_32BIT_LITERAL_EMITCHILD);
		
INSTRUCTION_FORMAT31t_PHRASE
	:	INSTRUCTION_FORMAT31t_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		(LABEL_EMIT | OFFSET_EMIT);
		
INSTRUCTION_FORMAT32x_PHRASE
	:	INSTRUCTION_FORMAT32x_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		REGISTER_EMIT;		
		
INSTRUCTION_FORMAT35c_METHOD_PHRASE
	:	INSTRUCTION_FORMAT35c_METHOD_EMIT
		WS
		REGISTER_LIST_EMITCHILDREN
		WS? COMMA_EMIT WS?
		FULLY_QUALIFIED_METHOD_EMITCHILDREN;

INSTRUCTION_FORMAT35c_TYPE_PHRASE
	:	INSTRUCTION_FORMAT35c_TYPE_EMIT
		WS
		REGISTER_LIST_EMITCHILDREN
		WS? COMMA_EMIT WS?
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD;
		
INSTRUCTION_FORMAT35ms_METHOD_PHRASE
	:	INSTRUCTION_FORMAT35ms_METHOD_EMIT
		WS
		REGISTER_LIST_EMITCHILDREN
		WS? COMMA_EMIT WS?
		VTABLE_OFFSET_EMIT;

INSTRUCTION_FORMAT3rc_METHOD_PHRASE
	:	INSTRUCTION_FORMAT3rc_METHOD_EMIT
		WS
		REGISTER_RANGE_EMITCHILDREN
		WS? COMMA_EMIT WS?
		FULLY_QUALIFIED_METHOD_EMITCHILDREN;

INSTRUCTION_FORMAT3rc_TYPE_PHRASE
	:	INSTRUCTION_FORMAT3rc_TYPE_EMIT
		WS
		REGISTER_RANGE_EMITCHILDREN
		WS? COMMA_EMIT WS?
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD;	

INSTRUCTION_FORMAT51l_PHRASE
	:	INSTRUCTION_FORMAT51l_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		(FIXED_LITERAL_EMITCHILD);

ARRAY_DATA_PHRASE
	:	ARRAY_DATA_DIRECTIVE_EMIT
		WS
		INTEGRAL_LITERAL_EMITCHILD
		(WSC FIXED_LITERAL_EMITCHILD)*
		WSC
		END_ARRAY_DATA_DIRECTIVE_EMIT;

PACKED_SWITCH_PHRASE
	:	PACKED_SWITCH_DIRECTIVE_EMIT
		WS
		FIXED_32BIT_LITERAL_EMITCHILD
		(WSC (LABEL_EMIT | OFFSET_EMIT))*
		WSC
		END_PACKED_SWITCH_DIRECTIVE_EMIT;

SPARSE_SWITCH_PHRASE
	:	SPARSE_SWITCH_DIRECTIVE_EMIT
		(WSC FIXED_32BIT_LITERAL_EMITCHILD WS? ARROW_EMIT[true] WS? (LABEL_EMIT | OFFSET_EMIT))*
		WSC?
		END_SPARSE_SWITCH_DIRECTIVE_EMIT;
		
REGISTERS_PHRASE
	:	(REGISTERS_DIRECTIVE_EMIT | LOCALS_DIRECTIVE_EMIT)
		WS
		INTEGRAL_LITERAL_EMITCHILD;

CATCHALL_PHRASE
	:	CATCHALL_DIRECTIVE_EMIT
		WS
		OPEN_BRACE_EMIT[true] WS?
		(LABEL_EMIT | OFFSET_EMIT)
		WS DOTDOT_EMIT[true] WS
		(LABEL_EMIT | OFFSET_EMIT)
		WS? CLOSE_BRACE_EMIT[true] WS?
		(LABEL_EMIT | OFFSET_EMIT);

CATCH_PHRASE
	:	CATCH_DIRECTIVE_EMIT
		WS
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD
		WS? OPEN_BRACE_EMIT[true] WS?
		(LABEL_EMIT | OFFSET_EMIT)
		WS DOTDOT_EMIT[true] WS
		(LABEL_EMIT | OFFSET_EMIT)
		WS? CLOSE_BRACE_EMIT[true] WS?
		(LABEL_EMIT | OFFSET_EMIT);
		
LINE_PHRASE
	:	LINE_DIRECTIVE_EMIT
		WS
		INTEGRAL_LITERAL_EMITCHILD;

PARAMETER_PHRASE
	:	PARAMETER_DIRECTIVE_EMIT
		(WS STRING_LITERAL_EMIT?)?;

END_PARAMETER_PHRASE
	:	END_PARAMETER_DIRECTIVE_EMIT;

LOCAL_PHRASE
	:	LOCAL_DIRECTIVE_EMIT
		WS
		REGISTER_EMIT
		WS? COMMA_EMIT WS?
		SIMPLE_NAME_EMIT
		COLON_EMIT
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD
		WS?
		( COMMA_EMIT WS? STRING_LITERAL_EMIT)?;
		
END_LOCAL_PHRASE
	:	END_LOCAL_DIRECTIVE_EMIT
		WS
		REGISTER_EMIT;

RESTART_LOCAL_PHRASE
	:	RESTART_LOCAL_DIRECTIVE_EMIT
		WS
		REGISTER_EMIT;

PROLOGUE_PHRASE
	:	PROLOGUE_DIRECTIVE_EMIT;

EPILOGUE_PHRASE
	:	EPILOGUE_DIRECTIVE_EMIT;

ANNOTATION_PHRASE
	:	ANNOTATION_START_EMIT
		WS
		ANNOTATION_VISIBILITY_EMIT
		WS
		CLASS_DESCRIPTOR_EMIT
		WS
		(ANNOTATION_ELEMENT_EMITCHILDREN WS)*
		ANNOTATION_END_EMIT;

//TODO: add support for both relative and ahbsolute offsets?
fragment OFFSET_EMIT
	:	OFFSET {emit($OFFSET, OFFSET);};
fragment OFFSET
	:	INTEGER_LITERAL;

fragment LABEL_EMIT
	:	LABEL {emit($LABEL, LABEL);};
LABEL
	:	':' SIMPLE_NAME;

fragment CLASS_DIRECTIVE_EMIT
	:	CLASS_DIRECTIVE {emit($CLASS_DIRECTIVE, CLASS_DIRECTIVE);};
fragment CLASS_DIRECTIVE
	:	'.class';
	
fragment SUPER_DIRECTIVE_EMIT
	:	SUPER_DIRECTIVE {emit($SUPER_DIRECTIVE, SUPER_DIRECTIVE);};
fragment SUPER_DIRECTIVE
	:	'.super';

fragment IMPLEMENTS_DIRECTIVE_EMIT
	:	IMPLEMENTS_DIRECTIVE {emit($IMPLEMENTS_DIRECTIVE, IMPLEMENTS_DIRECTIVE);};
fragment IMPLEMENTS_DIRECTIVE
	:	'.implements';
	
fragment SOURCE_DIRECTIVE_EMIT
	:	SOURCE_DIRECTIVE {emit($SOURCE_DIRECTIVE, SOURCE_DIRECTIVE);};
fragment SOURCE_DIRECTIVE
	:	'.source';
	
fragment FIELD_DIRECTIVE_EMIT
	:	FIELD_DIRECTIVE {emit($FIELD_DIRECTIVE, FIELD_DIRECTIVE);};
fragment FIELD_DIRECTIVE
	:	'.field';

fragment END_FIELD_DIRECTIVE_EMIT
	:	END_FIELD_DIRECTIVE {emit($END_FIELD_DIRECTIVE, END_FIELD_DIRECTIVE);};
fragment END_FIELD_DIRECTIVE
	:	'.end field';

fragment METHOD_DIRECTIVE_EMIT
	:	METHOD_DIRECTIVE {emit($METHOD_DIRECTIVE, METHOD_DIRECTIVE);};
fragment METHOD_DIRECTIVE
	:	'.method';
	
fragment END_METHOD_DIRECTIVE_EMIT
	:	END_METHOD_DIRECTIVE {emit($END_METHOD_DIRECTIVE, END_METHOD_DIRECTIVE);};
fragment END_METHOD_DIRECTIVE
	:	'.end method';

fragment ARRAY_DATA_DIRECTIVE_EMIT
	:	ARRAY_DATA_DIRECTIVE {emit($ARRAY_DATA_DIRECTIVE, ARRAY_DATA_DIRECTIVE);};
fragment ARRAY_DATA_DIRECTIVE
	:	'.array-data';

fragment END_ARRAY_DATA_DIRECTIVE_EMIT
	:	END_ARRAY_DATA_DIRECTIVE {emit($END_ARRAY_DATA_DIRECTIVE, END_ARRAY_DATA_DIRECTIVE);};
fragment END_ARRAY_DATA_DIRECTIVE
	:	'.end array-data';
	
fragment PACKED_SWITCH_DIRECTIVE_EMIT
	:	PACKED_SWITCH_DIRECTIVE {emit($PACKED_SWITCH_DIRECTIVE, PACKED_SWITCH_DIRECTIVE);};
fragment PACKED_SWITCH_DIRECTIVE
	:	'.packed-switch';
	
fragment END_PACKED_SWITCH_DIRECTIVE_EMIT
	:	END_PACKED_SWITCH_DIRECTIVE {emit($END_PACKED_SWITCH_DIRECTIVE, END_PACKED_SWITCH_DIRECTIVE);};
fragment END_PACKED_SWITCH_DIRECTIVE
	:	'.end packed-switch';

fragment SPARSE_SWITCH_DIRECTIVE_EMIT
	:	SPARSE_SWITCH_DIRECTIVE {emit($SPARSE_SWITCH_DIRECTIVE, SPARSE_SWITCH_DIRECTIVE);};
fragment SPARSE_SWITCH_DIRECTIVE
	:	'.sparse-switch';

fragment END_SPARSE_SWITCH_DIRECTIVE_EMIT
	:	END_SPARSE_SWITCH_DIRECTIVE {emit($END_SPARSE_SWITCH_DIRECTIVE, END_SPARSE_SWITCH_DIRECTIVE);};
fragment END_SPARSE_SWITCH_DIRECTIVE
	:	'.end sparse-switch';

fragment REGISTERS_DIRECTIVE_EMIT
	:	REGISTERS_DIRECTIVE {emit($REGISTERS_DIRECTIVE, REGISTERS_DIRECTIVE);};
fragment REGISTERS_DIRECTIVE
	:	'.registers';

fragment LOCALS_DIRECTIVE_EMIT
	:	LOCALS_DIRECTIVE {emit($LOCALS_DIRECTIVE, LOCALS_DIRECTIVE);};
fragment LOCALS_DIRECTIVE
	:	'.locals';

fragment CATCH_DIRECTIVE_EMIT
	:	CATCH_DIRECTIVE {emit($CATCH_DIRECTIVE, CATCH_DIRECTIVE);};
fragment CATCH_DIRECTIVE
	:	'.catch';

fragment CATCHALL_DIRECTIVE_EMIT
	:	CATCHALL_DIRECTIVE {emit($CATCHALL_DIRECTIVE, CATCHALL_DIRECTIVE);};
fragment CATCHALL_DIRECTIVE
	:	'.catchall';
	
fragment LINE_DIRECTIVE_EMIT
	:	LINE_DIRECTIVE {emit($LINE_DIRECTIVE, LINE_DIRECTIVE);};
fragment LINE_DIRECTIVE
	:	'.line';
	
fragment PARAMETER_DIRECTIVE_EMIT
	:	PARAMETER_DIRECTIVE {emit($PARAMETER_DIRECTIVE, PARAMETER_DIRECTIVE);};
fragment PARAMETER_DIRECTIVE
	:	'.parameter';

fragment END_PARAMETER_DIRECTIVE_EMIT
	:	END_PARAMETER_DIRECTIVE {emit($END_PARAMETER_DIRECTIVE, END_PARAMETER_DIRECTIVE);};
fragment END_PARAMETER_DIRECTIVE
	:	'.end parameter';

fragment LOCAL_DIRECTIVE_EMIT
	:	LOCAL_DIRECTIVE {emit($LOCAL_DIRECTIVE, LOCAL_DIRECTIVE);};
fragment LOCAL_DIRECTIVE
	:	'.local';

fragment END_LOCAL_DIRECTIVE_EMIT
	:	END_LOCAL_DIRECTIVE {emit($END_LOCAL_DIRECTIVE, END_LOCAL_DIRECTIVE);};
fragment END_LOCAL_DIRECTIVE
	:	'.end local';

fragment RESTART_LOCAL_DIRECTIVE_EMIT
	:	RESTART_LOCAL_DIRECTIVE {emit($RESTART_LOCAL_DIRECTIVE, RESTART_LOCAL_DIRECTIVE);};
fragment RESTART_LOCAL_DIRECTIVE
	:	'.restart local';

fragment PROLOGUE_DIRECTIVE_EMIT
	:	PROLOGUE_DIRECTIVE {emit($PROLOGUE_DIRECTIVE, PROLOGUE_DIRECTIVE);};
fragment PROLOGUE_DIRECTIVE
	:	'.prologue';

fragment EPILOGUE_DIRECTIVE_EMIT
	:	EPILOGUE_DIRECTIVE {emit($EPILOGUE_DIRECTIVE, EPILOGUE_DIRECTIVE);};
fragment EPILOGUE_DIRECTIVE
	:	'.epilogue';
	
fragment REGISTER_EMIT
	:	REGISTER {emit($REGISTER, REGISTER);};
fragment REGISTER
	:	('v' | 'p') ('0'..'9')+;

fragment REGISTER_LIST_EMITCHILDREN
	:	OPEN_BRACE_EMIT[false]
		(	WS?
			REGISTER_EMIT (WS? COMMA_EMIT WS? REGISTER_EMIT)*
			WS? 
		|	WS?)
		CLOSE_BRACE_EMIT[false];


fragment REGISTER_RANGE_EMITCHILDREN
	:	OPEN_BRACE_EMIT[false]
		WS?
		REGISTER_EMIT
		WS?
		(DOTDOT_EMIT[true] WS?
			REGISTER_EMIT)?
		CLOSE_BRACE_EMIT[false];
				

fragment METHOD_PROTOTYPE_EMITCHILDREN
	:	OPEN_PAREN_EMIT
		(NONVOID_TYPE_DESCRIPTOR_EMITCHILD+)?
		CLOSE_PAREN_EMIT
		TYPE_DESCRIPTOR_EMITCHILD;

fragment FULLY_QUALIFIED_FIELD_EMITCHILDREN
	:	REFERENCE_TYPE_DESCRIPTOR_EMITCHILD
		ARROW_EMIT[false]
		MEMBER_NAME_EMIT
		COLON_EMIT
		NONVOID_TYPE_DESCRIPTOR_EMITCHILD;
		
fragment FULLY_QUALIFIED_METHOD_EMITCHILDREN
	:	REFERENCE_TYPE_DESCRIPTOR_EMITCHILD
		ARROW_EMIT[false]
		MEMBER_NAME_EMIT
		METHOD_PROTOTYPE_EMITCHILDREN;
	
fragment TYPE_DESCRIPTOR_EMITCHILD
	:	PRIMITIVE_TYPE_EMIT
	|	VOID_TYPE_EMIT
	|	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;	
	
fragment NONVOID_TYPE_DESCRIPTOR_EMITCHILD
	:	PRIMITIVE_TYPE_EMIT
	|	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;
	
fragment REFERENCE_TYPE_DESCRIPTOR_EMITCHILD
	:	CLASS_DESCRIPTOR_EMIT
	|	ARRAY_DESCRIPTOR_EMIT;

fragment VTABLE_OFFSET_EMIT
	:	VTABLE_OFFSET {emit($VTABLE_OFFSET, VTABLE_OFFSET);};

fragment VTABLE_OFFSET
	:	'vtable@0x'
		(
			'0'..'9'
		|	'a'..'f'
		|	'A'..'F'
		)+
		;

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
	:	'L' (SIMPLE_NAME '/')* SIMPLE_NAME ';';


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


fragment CLASS_ACCESS_SPEC_EMIT
	:	CLASS_ACCESS_SPEC {emit($CLASS_ACCESS_SPEC, ACCESS_SPEC);};

fragment CLASS_ACCESS_SPEC
	:	'public'
	|	'final'
	|	'interface'
	|	'abstract'
	|	'synthetic'
	|	'enum'
	|	'annotation';
	
fragment FIELD_ACCESS_SPEC_EMIT
	:	FIELD_ACCESS_SPEC {emit($FIELD_ACCESS_SPEC, ACCESS_SPEC);};
	
fragment FIELD_ACCESS_SPEC
	:	'public'
	|	'private'
	|	'protected'
	|	'static'
	|	'final'
	|	'volatile'
	|	'transient'
	|	'synthetic'
	|	'enum';
	
fragment METHOD_ACCESS_SPEC_EMIT
	:	METHOD_ACCESS_SPEC {emit($METHOD_ACCESS_SPEC, ACCESS_SPEC);};
	
fragment METHOD_ACCESS_SPEC
	:	'public'
	|	'private'
	|	'protected'
	|	'static'
	|	'final'
	|	'synchronized'
	|	'bridge'
	|	'varargs'
	|	'native'
	|	'abstract'
	|	'strictfp'
	|	'synthetic'
	|	'constructor'
	|	'declared-synchronized';


fragment MEMBER_NAME_EMIT
	:	MEMBER_NAME {emit($MEMBER_NAME, MEMBER_NAME);};

fragment MEMBER_NAME
	:	'<'? SIMPLE_NAME '>'?;

fragment SIMPLE_NAME_EMIT
	:	SIMPLE_NAME {emit($SIMPLE_NAME, SIMPLE_NAME);};
	
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
	
fragment INTEGRAL_LITERAL_EMITCHILD
	:	LONG_LITERAL_EMIT
	|	INTEGER_LITERAL_EMIT
	|	SHORT_LITERAL_EMIT
	|	CHAR_LITERAL_EMIT
	|	BYTE_LITERAL_EMIT;

fragment FIXED_LITERAL_EMITCHILD
	:	INTEGER_LITERAL_EMIT
	|	LONG_LITERAL_EMIT
	|	SHORT_LITERAL_EMIT
	|	BYTE_LITERAL_EMIT
	|	FLOAT_LITERAL_EMIT
	|	DOUBLE_LITERAL_EMIT
	|	CHAR_LITERAL_EMIT
	|	BOOL_LITERAL_EMIT;
	
fragment FIXED_32BIT_LITERAL_EMITCHILD
	:	INTEGER_LITERAL_EMIT
	|	LONG_LITERAL_EMIT
	|	SHORT_LITERAL_EMIT
	|	BYTE_LITERAL_EMIT
	|	FLOAT_LITERAL_EMIT
	|	CHAR_LITERAL_EMIT
	|	BOOL_LITERAL_EMIT;

fragment LITERAL_EMITCHILD
	:	STRING_LITERAL_EMIT
	|	INTEGER_LITERAL_EMIT
	|	LONG_LITERAL_EMIT
	|	SHORT_LITERAL_EMIT
	|	BYTE_LITERAL_EMIT
	|	FLOAT_LITERAL_EMIT
	|	DOUBLE_LITERAL_EMIT
	|	CHAR_LITERAL_EMIT
	|	BOOL_LITERAL_EMIT
	|	NULL_LITERAL_EMIT
	|	ARRAY_LITERAL_EMITCHILDREN
	|	SUBANNOTATION_EMITCHILDREN
	|	TYPE_FIELD_METHOD_LITERAL_EMITCHILDREN
	|	ENUM_LITERAL_EMITCHILDREN;

fragment SUBANNOTATION_EMITCHILDREN
	:	SUBANNOTATION_START_EMIT
		WS
		CLASS_DESCRIPTOR_EMIT
		WS
		(ANNOTATION_ELEMENT_EMITCHILDREN WS)*
		SUBANNOTATION_END_EMIT		
		;

fragment SUBANNOTATION_START_EMIT
	:	SUBANNOTATION_START {emit($SUBANNOTATION_START, SUBANNOTATION_START);};
fragment SUBANNOTATION_START
	:	'.subannotation';

fragment SUBANNOTATION_END_EMIT
	:	SUBANNOTATION_END {emit($SUBANNOTATION_END, SUBANNOTATION_END);};
fragment SUBANNOTATION_END
	:	'.end subannotation';

fragment ANNOTATION_START_EMIT
	:	ANNOTATION_START {emit($ANNOTATION_START, ANNOTATION_START);};
fragment ANNOTATION_START
	:	'.annotation';

fragment ANNOTATION_END_EMIT
	:	ANNOTATION_END {emit($ANNOTATION_END, ANNOTATION_END);};
fragment ANNOTATION_END
	:	'.end annotation';

fragment ANNOTATION_VISIBILITY_EMIT
	:	ANNOTATION_VISIBILITY {emit($ANNOTATION_VISIBILITY, ANNOTATION_VISIBILITY);};
fragment ANNOTATION_VISIBILITY
	:	'build'
	|	'runtime'
	|	'system';

fragment ANNOTATION_ELEMENT_EMITCHILDREN
	:	MEMBER_NAME_EMIT
		WS?
 		EQUAL_EMIT
		WS?
		LITERAL_EMITCHILD;

fragment TYPE_FIELD_METHOD_LITERAL_EMITCHILDREN
	:	REFERENCE_TYPE_DESCRIPTOR_EMITCHILD
		(	ARROW_EMIT[false]
			MEMBER_NAME_EMIT
			(	METHOD_PROTOTYPE_EMITCHILDREN
			|	COLON_EMIT NONVOID_TYPE_DESCRIPTOR_EMITCHILD))?
	|	PRIMITIVE_TYPE_EMIT
	|	VOID_TYPE_EMIT;
	
fragment ENUM_EMIT
	:	ENUM {emit($ENUM, ENUM);};
fragment ENUM
	:	'.enum';
	
fragment ENUM_LITERAL_EMITCHILDREN
	:	ENUM_EMIT
		WS
		REFERENCE_TYPE_DESCRIPTOR_EMITCHILD
		ARROW_EMIT[false]
		MEMBER_NAME_EMIT
		COLON_EMIT
		REFERENCE_TYPE_DESCRIPTOR_EMITCHILD;		

fragment ARRAY_LITERAL_EMITCHILDREN
	:	OPEN_BRACE_EMIT[false]
		WS?
		(LITERAL_EMITCHILD WS? (COMMA_EMIT WS? LITERAL_EMITCHILD WS?)*)?
		CLOSE_BRACE_EMIT[false];
				
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
	|	'-'? '0' ('0'..'7')+         
	|	'-'? HEX_PREFIX HEX_DIGIT+        
	;

fragment HEX_PREFIX
	:	'0x'|'0X';
	

fragment LONG_LITERAL_EMIT
	:	LONG_LITERAL {emit($LONG_LITERAL, LONG_LITERAL);};
fragment LONG_LITERAL
	:	INTEGER_LITERAL ('l' | 'L');
	
fragment SHORT_LITERAL_EMIT
	:	SHORT_LITERAL {emit($SHORT_LITERAL, SHORT_LITERAL);};
fragment SHORT_LITERAL
	:	INTEGER_LITERAL ('s' | 'S');

fragment BYTE_LITERAL_EMIT
	:	BYTE_LITERAL {emit($BYTE_LITERAL, BYTE_LITERAL);};
fragment BYTE_LITERAL
	:	INTEGER_LITERAL ('t' | 'T');
	
	
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
	:	'-'? (	FLOATING_POINT_NUMBER ('f' | 'F')
	 	     |  ('0' .. '9')+ ('f' | 'F')
	 	     |  ('i' | 'I') ('n' | 'N') ('f' | 'F') ('i' | 'I') ('n' | 'N') ('i' | 'I') ('t' | 'T') ('y' | 'Y') ('f' | 'F'))
	|	('n' | 'N') ('a' | 'A') ('n' | 'N') ('f' | 'F');
       
fragment DOUBLE_LITERAL_EMIT
	:	DOUBLE_LITERAL {emit($DOUBLE_LITERAL, DOUBLE_LITERAL);};
fragment DOUBLE_LITERAL
	:	'-'? (	FLOATING_POINT_NUMBER ('d' | 'D')?
	 	     |  ('0' .. '9')+ ('d' | 'D')
	 	     |  ('i' | 'I') ('n' | 'N') ('f' | 'F') ('i' | 'I') ('n' | 'N') ('i' | 'I') ('t' | 'T') ('y' | 'Y') ('d' | 'D')?)
	|	('n' | 'N') ('a' | 'A') ('n' | 'N') ('d' | 'D')?;


fragment CHAR_LITERAL_EMIT
	@init {StringBuilder sb = new StringBuilder();}
	:	CHAR_LITERAL[sb]
		{
			$CHAR_LITERAL.setText(sb.toString());
			emit($CHAR_LITERAL, CHAR_LITERAL);
		};
fragment CHAR_LITERAL[StringBuilder sb]
    :   '\'' {sb.append("'");}
        (   ESCAPE_SEQUENCE[sb]
        |   ~( '\'' | '\\' | '\r' | '\n' )  {sb.append((char)input.LA(-1));}
        ) 
        '\''  {sb.append("'");}
    ;

fragment NULL_LITERAL_EMIT
	:	NULL_LITERAL {emit($NULL_LITERAL, NULL_LITERAL);};
fragment NULL_LITERAL
	:	'null';

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
	|	'const-class'
	;
	
fragment INSTRUCTION_FORMAT21h_EMIT
	:	INSTRUCTION_FORMAT21h {emit($INSTRUCTION_FORMAT21h, INSTRUCTION_FORMAT21h);};
fragment INSTRUCTION_FORMAT21h
	:	'const/high16'
	|	'const-wide/high16'
	;

fragment INSTRUCTION_FORMAT21s_EMIT
	:	INSTRUCTION_FORMAT21s {emit($INSTRUCTION_FORMAT21s, INSTRUCTION_FORMAT21s);};
fragment INSTRUCTION_FORMAT21s
	:	'const/16'
	|	'const-wide/16'
	;
	
fragment INSTRUCTION_FORMAT21t_EMIT
	:	INSTRUCTION_FORMAT21t {emit($INSTRUCTION_FORMAT21t, INSTRUCTION_FORMAT21t);};
fragment INSTRUCTION_FORMAT21t
	:	'if-eqz'
	|	'if-nez'
	|	'if-ltz'
	|	'if-gez'
	|	'if-gtz'
	|	'if-lez'
	;
	
fragment INSTRUCTION_FORMAT22b_EMIT
	:	INSTRUCTION_FORMAT22b {emit($INSTRUCTION_FORMAT22b, INSTRUCTION_FORMAT22b);};
fragment INSTRUCTION_FORMAT22b
	:	'add-int/lit8'
	|	'rsub-int/lit8'
	|	'mul-int/lit8'
	|	'div-int/lit8'
	|	'rem-int/lit8'
	|	'and-int/lit8'
	|	'or-int/lit8'
	|	'xor-int/lit8'
	|	'shl-int/lit8'
	|	'shr-int/lit8'
	|	'ushr-int/lit8'
	;
	
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
	
fragment INSTRUCTION_FORMAT22c_TYPE_EMIT
	:	INSTRUCTION_FORMAT22c_TYPE {emit($INSTRUCTION_FORMAT22c_TYPE, INSTRUCTION_FORMAT22c_TYPE);};
fragment INSTRUCTION_FORMAT22c_TYPE
	:	'instance-of'
	|	'new-array';
	
fragment INSTRUCTION_FORMAT22s_EMIT
	:	INSTRUCTION_FORMAT22s {emit($INSTRUCTION_FORMAT22s, INSTRUCTION_FORMAT22s);};
fragment INSTRUCTION_FORMAT22s
	:	'add-int/lit16'
	|	'rsub-int'
	|	'mul-int/lit16'
	|	'div-int/lit16'
	|	'rem-int/lit16'
	|	'and-int/lit16'
	|	'or-int/lit16'
	|	'xor-int/lit16'
	;
	
fragment INSTRUCTION_FORMAT22t_EMIT
	:	INSTRUCTION_FORMAT22t {emit($INSTRUCTION_FORMAT22t, INSTRUCTION_FORMAT22t);};
fragment INSTRUCTION_FORMAT22t
	:	'if-eq'
	|	'if-ne'
	|	'if-lt'
	|	'if-ge'
	|	'if-gt'
	|	'if-le'
	;
	
fragment INSTRUCTION_FORMAT22x_EMIT
	:	INSTRUCTION_FORMAT22x {emit($INSTRUCTION_FORMAT22x, INSTRUCTION_FORMAT22x);};
fragment INSTRUCTION_FORMAT22x
	:	'move/from16'
	|	'move-wide/from16'
	|	'move-object/from16'
	;
	
fragment INSTRUCTION_FORMAT23x_EMIT
	:	INSTRUCTION_FORMAT23x {emit($INSTRUCTION_FORMAT23x, INSTRUCTION_FORMAT23x);};
fragment INSTRUCTION_FORMAT23x
	:	'cmpl-float'
	|	'cmpg-float'
	|	'cmpl-double'
	|	'cmpg-double'
	|	'cmp-long'
	|	'aget'
	|	'aget-wide'
	|	'aget-object'
	|	'aget-boolean'
	|	'aget-byte'
	|	'aget-char'
	|	'aget-short'
	|	'aput'
	|	'aput-wide'
	|	'aput-object'
	|	'aput-boolean'
	|	'aput-byte'
	|	'aput-char'
	|	'aput-short'
	|	'add-int'
	|	'sub-int'
	|	'mul-int'
	|	'div-int'
	|	'rem-int'
	|	'and-int'
	|	'or-int'
	|	'xor-int'
	|	'shl-int'
	|	'shr-int'
	|	'ushr-int'
	|	'add-long'
	|	'sub-long'
	|	'mul-long'
	|	'div-long'
	|	'rem-long'
	|	'and-long'
	|	'or-long'
	|	'xor-long'
	|	'shl-long'
	|	'shr-long'
	|	'ushr-long'
	|	'add-float'
	|	'sub-float'
	|	'mul-float'
	|	'div-float'
	|	'rem-float'
	|	'add-double'
	|	'sub-double'
	|	'mul-double'
	|	'div-double'
	|	'rem-double'
	;	
	
fragment INSTRUCTION_FORMAT30t_EMIT
	:	INSTRUCTION_FORMAT30t {emit($INSTRUCTION_FORMAT30t, INSTRUCTION_FORMAT30t);};
fragment INSTRUCTION_FORMAT30t
	:	'goto/32';
	
fragment INSTRUCTION_FORMAT31c_EMIT
	:	INSTRUCTION_FORMAT31c {emit($INSTRUCTION_FORMAT31c, INSTRUCTION_FORMAT31c);};
fragment INSTRUCTION_FORMAT31c
	:	'const-string/jumbo';

fragment INSTRUCTION_FORMAT31i_EMIT
	:	INSTRUCTION_FORMAT31i {emit($INSTRUCTION_FORMAT31i, INSTRUCTION_FORMAT31i);};
fragment INSTRUCTION_FORMAT31i
	:	'const'
	|	'const-wide/32'
	;

fragment INSTRUCTION_FORMAT31t_EMIT
	:	INSTRUCTION_FORMAT31t {emit($INSTRUCTION_FORMAT31t, INSTRUCTION_FORMAT31t);};
fragment INSTRUCTION_FORMAT31t
	:	'fill-array-data'
	|	'packed-switch'
	|	'sparse-switch'
	;
	
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

fragment INSTRUCTION_FORMAT35c_TYPE_EMIT
	:	INSTRUCTION_FORMAT35c_TYPE {emit($INSTRUCTION_FORMAT35c_TYPE, INSTRUCTION_FORMAT35c_TYPE);};
fragment INSTRUCTION_FORMAT35c_TYPE
	:	'filled-new-array';
	
fragment INSTRUCTION_FORMAT35ms_METHOD_EMIT
	:	INSTRUCTION_FORMAT35ms_METHOD {emit($INSTRUCTION_FORMAT35ms_METHOD, INSTRUCTION_FORMAT35ms_METHOD);};
fragment INSTRUCTION_FORMAT35ms_METHOD
	:	'execute-inline'
	|	'invoke-virtual-quick'
	|	'invoke-super-quick'
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

fragment INSTRUCTION_FORMAT3rc_TYPE_EMIT
	:	INSTRUCTION_FORMAT3rc_TYPE {emit($INSTRUCTION_FORMAT3rc_TYPE, INSTRUCTION_FORMAT3rc_TYPE);};
fragment INSTRUCTION_FORMAT3rc_TYPE
	:	'filled-new-array/range';

fragment INSTRUCTION_FORMAT51l_EMIT
	:	INSTRUCTION_FORMAT51l {emit($INSTRUCTION_FORMAT51l, INSTRUCTION_FORMAT51l);};
INSTRUCTION_FORMAT51l
	:	'const-wide';

fragment OPEN_PAREN_EMIT
	:	OPEN_PAREN {emit($OPEN_PAREN, OPEN_PAREN);};
fragment OPEN_PAREN
	:	'(';

fragment CLOSE_PAREN_EMIT
	:	CLOSE_PAREN {emit($CLOSE_PAREN, CLOSE_PAREN);};
fragment CLOSE_PAREN
	:	')';
	
fragment WSC
	:	(WS | LINE_COMMENT_EMIT)+;

fragment WS
	:	WHITE_SPACE {emit($WHITE_SPACE, WHITE_SPACE, Token.HIDDEN_CHANNEL);};	
	
WHITE_SPACE
	:	(' '|'\t'|'\n'|'\r')+ {$channel = HIDDEN;};
	
fragment LINE_COMMENT_EMIT
	:	LINE_COMMENT2 {emit($LINE_COMMENT2, LINE_COMMENT, Token.HIDDEN_CHANNEL);};
fragment LINE_COMMENT2
	:	'#' ~('\n'|'\r')*  ('\r\n' | '\r' | '\n');
	
	
LINE_COMMENT
	:	('#' ~('\n'|'\r')*  ('\r\n' | '\r' | '\n') 
	|	'#' ~('\n'|'\r')*)
		{$channel = HIDDEN;};   
		
fragment EQUAL_EMIT
	:	EQUAL {emit($EQUAL, EQUAL, Token.HIDDEN_CHANNEL);};
fragment EQUAL
	:	'=';

fragment COMMA_EMIT 
	:	COMMA {emit($COMMA, COMMA, Token.HIDDEN_CHANNEL);};
fragment COMMA
	:	',';
	
fragment COLON_EMIT
	:	COLON {emit($COLON, COLON, Token.HIDDEN_CHANNEL);};
fragment COLON
	:	':';

fragment ARROW_EMIT[boolean hidden]
	:	ARROW {emit($ARROW, ARROW, $hidden?Token.HIDDEN_CHANNEL:Token.DEFAULT_CHANNEL);};
fragment ARROW
	:	'->';

fragment OPEN_BRACE_EMIT[boolean hidden]
	:	OPEN_BRACE {emit($OPEN_BRACE, OPEN_BRACE, $hidden?Token.HIDDEN_CHANNEL:Token.DEFAULT_CHANNEL);};
fragment OPEN_BRACE
	:	'{';

fragment CLOSE_BRACE_EMIT[boolean hidden]
	:	CLOSE_BRACE {emit($CLOSE_BRACE, CLOSE_BRACE, $hidden?Token.HIDDEN_CHANNEL:Token.DEFAULT_CHANNEL);};
fragment CLOSE_BRACE
	:	'}';
	
fragment DOTDOT_EMIT[boolean hidden]
	:	DOTDOT {emit($DOTDOT, DOTDOT, $hidden?Token.HIDDEN_CHANNEL:Token.DEFAULT_CHANNEL);};
fragment DOTDOT
	:	'..';		
