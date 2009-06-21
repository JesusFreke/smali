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
	I_IMPLEMENTS;
	I_SOURCE;
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
	I_LABELS;
	I_LABEL;
	I_ANNOTATIONS;
	I_ANNOTATION;
	I_ANNOTATION_ELEMENT;
	I_SUBANNOTATION;
	I_ENCODED_FIELD;
	I_ENCODED_METHOD;
	I_ENCODED_ENUM;
	I_ENCODED_ARRAY;
	I_ARRAY_ELEMENT_SIZE;
	I_ARRAY_ELEMENTS;	
	I_PACKED_SWITCH_START_KEY;
	I_PACKED_SWITCH_TARGET_COUNT;
	I_PACKED_SWITCH_TARGETS;
	I_PACKED_SWITCH_DECLARATION;
	I_PACKED_SWITCH_DECLARATIONS;
	I_SPARSE_SWITCH_KEYS;
	I_SPARSE_SWITCH_TARGET_COUNT;
	I_SPARSE_SWITCH_TARGETS;
	I_SPARSE_SWITCH_DECLARATION;
	I_SPARSE_SWITCH_DECLARATIONS;
	I_ADDRESS;
	I_CATCH;
	I_CATCHALL;
	I_CATCHES;
	I_PARAMETER;
	I_PARAMETERS;
	I_PARAMETER_NOT_SPECIFIED;
	I_ORDERED_DEBUG_DIRECTIVES;
	I_LINE;
	I_LOCAL;
	I_END_LOCAL;
	I_RESTART_LOCAL;
	I_PROLOGUE;
	I_EPILOGUE;
	I_STATEMENTS;
	I_STATEMENT_FORMAT10t;
	I_STATEMENT_FORMAT10x;
	I_STATEMENT_FORMAT11n;
	I_STATEMENT_FORMAT11x;
	I_STATEMENT_FORMAT12x;
	I_STATEMENT_FORMAT20t;
	I_STATEMENT_FORMAT21c_TYPE;
	I_STATEMENT_FORMAT21c_FIELD;
	I_STATEMENT_FORMAT21c_STRING;
	I_STATEMENT_FORMAT21h;
	I_STATEMENT_FORMAT21s;
	I_STATEMENT_FORMAT21t;
	I_STATEMENT_FORMAT22b;
	I_STATEMENT_FORMAT22c_FIELD;
	I_STATEMENT_FORMAT22c_TYPE;
	I_STATEMENT_FORMAT22s;
	I_STATEMENT_FORMAT22t;
	I_STATEMENT_FORMAT22x;
	I_STATEMENT_FORMAT23x;
	I_STATEMENT_FORMAT30t;
	I_STATEMENT_FORMAT31c;
	I_STATEMENT_FORMAT31i;
	I_STATEMENT_FORMAT31t;	
	I_STATEMENT_FORMAT32x;
	I_STATEMENT_FORMAT35c_METHOD;
	I_STATEMENT_FORMAT35c_TYPE;
	I_STATEMENT_FORMAT3rc_METHOD;
	I_STATEMENT_FORMAT3rc_TYPE;
	I_STATEMENT_FORMAT51l;
	I_STATEMENT_ARRAY_DATA;
	I_STATEMENT_PACKED_SWITCH;
	I_STATEMENT_SPARSE_SWITCH;
	I_REGISTER_RANGE;
	I_REGISTER_LIST;
}

@header {
package org.jf.smali;

import org.jf.dexlib.Code.Format.*;
}


@members {
	public String getErrorMessage(RecognitionException e,
		String[] tokenNames)
	{
		List stack = getRuleInvocationStack(e, this.getClass().getName());
		String msg = null;
		if ( e instanceof NoViableAltException ) {
			NoViableAltException nvae = (NoViableAltException)e;
			msg = " no viable alt; token="+e.token+
			" (decision="+nvae.decisionNumber+
			" state "+nvae.stateNumber+")"+
			" decision=<<"+nvae.grammarDecisionDescription+">>";
		}
		else {
			msg = super.getErrorMessage(e, tokenNames);
		}
		return stack+" "+msg;
	}
	
	public String getTokenErrorDisplay(Token t) {
		return t.toString();
	}
	
	public String getErrorHeader(RecognitionException e) {
		return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
	}
	
	private CommonTree buildTree(int type, String text, List<CommonTree> children) {
		CommonTree root = new CommonTree(new CommonToken(type, text));
		for (CommonTree child: children) {
			root.addChild(child);
		}	
		return root;
	}
}


smali_file
	scope
	{
		boolean hasClassSpec;
		boolean hasSuperSpec;
		boolean hasSourceSpec;
	}
	@init { $smali_file::hasClassSpec = $smali_file::hasSuperSpec = $smali_file::hasSourceSpec = false; }
	:
	(	{!$smali_file::hasClassSpec}?=> class_spec {$smali_file::hasClassSpec = true;}
	|	{!$smali_file::hasSuperSpec}?=> super_spec {$smali_file::hasSuperSpec = true;}
	|	implements_spec
	|	{!$smali_file::hasSourceSpec}?=> source_spec {$smali_file::hasSourceSpec = true;}
	|	method
	|	field
	|	annotation
	)+
	EOF
	{
		if (!$smali_file::hasClassSpec) {
			throw new SemanticException(input, "The file must contain a .class directive");
		}
		
		if (!$smali_file::hasSuperSpec) {
			throw new SemanticException(input, "The file must contain a .super directive");
		}
	}
	->	^(I_CLASS_DEF
			class_spec
			super_spec
			implements_spec*
			source_spec?
			^(I_METHODS method*) ^(I_FIELDS field*) ^(I_ANNOTATIONS annotation*));
		
class_spec
	:	CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR -> CLASS_DESCRIPTOR access_list;

super_spec
	:	SUPER_DIRECTIVE CLASS_DESCRIPTOR -> ^(I_SUPER[$start, "I_SUPER"] CLASS_DESCRIPTOR);

implements_spec
	:	IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR -> ^(I_IMPLEMENTS[$start, "I_IMPLEMENTS"] CLASS_DESCRIPTOR);
	
source_spec
	:	SOURCE_DIRECTIVE STRING_LITERAL -> ^(I_SOURCE[$start, "I_SOURCE"] STRING_LITERAL);

access_list
	:	ACCESS_SPEC* -> ^(I_ACCESS_LIST[$start,"I_ACCESS_LIST"] ACCESS_SPEC*);


field	:	FIELD_DIRECTIVE access_list MEMBER_NAME nonvoid_type_descriptor literal? 
		//TODO: get rid of this predicate
		(	(annotation+ END_FIELD_DIRECTIVE)=> annotation+ END_FIELD_DIRECTIVE
		| 	END_FIELD_DIRECTIVE?
		)
		-> ^(I_FIELD[$start, "I_FIELD"] MEMBER_NAME access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) ^(I_FIELD_INITIAL_VALUE literal)? ^(I_ANNOTATIONS annotation*));
		
method	
	scope {int currentAddress;}
	:	{$method::currentAddress = 0;}
		METHOD_DIRECTIVE access_list  MEMBER_NAME method_prototype 
		statements_and_directives
		END_METHOD_DIRECTIVE
		-> ^(I_METHOD[$start, "I_METHOD"] MEMBER_NAME method_prototype access_list statements_and_directives);

method_prototype
	:	OPEN_PAREN nonvoid_type_descriptor* CLOSE_PAREN type_descriptor
		-> ^(I_METHOD_PROTOTYPE[$start, "I_METHOD_PROTOTYPE"] ^(I_METHOD_RETURN_TYPE type_descriptor) nonvoid_type_descriptor*);


fully_qualified_method
	:	reference_type_descriptor ARROW MEMBER_NAME method_prototype
	->	reference_type_descriptor MEMBER_NAME method_prototype;

fully_qualified_field
	:	reference_type_descriptor ARROW MEMBER_NAME nonvoid_type_descriptor
	->	reference_type_descriptor MEMBER_NAME nonvoid_type_descriptor;

statements_and_directives
	scope
	{
		boolean hasRegistersDirective;
		List<CommonTree> packedSwitchDeclarations;
		List<CommonTree> sparseSwitchDeclarations;
	}
	:	{
			$method::currentAddress = 0;
			$statements_and_directives::hasRegistersDirective = false;
			$statements_and_directives::packedSwitchDeclarations = new ArrayList<CommonTree>();
			$statements_and_directives::sparseSwitchDeclarations = new ArrayList<CommonTree>();
		}
		(	instruction {$method::currentAddress += $instruction.size/2;}
		|	{!$statements_and_directives::hasRegistersDirective}?=> registers_directive {$statements_and_directives::hasRegistersDirective = true;}
		|	label
		|	catch_directive
		|	catchall_directive
		|	parameter_directive
		|	ordered_debug_directive
		|	annotation
		)*		
		->	^(I_REGISTERS registers_directive?)
			^(I_LABELS label*)
			{buildTree(I_PACKED_SWITCH_DECLARATIONS, "I_PACKED_SWITCH_DECLARATIONS", $statements_and_directives::packedSwitchDeclarations)}
			{buildTree(I_SPARSE_SWITCH_DECLARATIONS, "I_SPARSE_SWITCH_DECLARATIONS", $statements_and_directives::sparseSwitchDeclarations)}
			^(I_STATEMENTS instruction*)
			^(I_CATCHES catch_directive* catchall_directive*)
			^(I_PARAMETERS parameter_directive*)
			^(I_ORDERED_DEBUG_DIRECTIVES ordered_debug_directive*)
			^(I_ANNOTATIONS annotation*);

registers_directive
	:	REGISTERS_DIRECTIVE integral_literal
	-> 	integral_literal;

catch_directive
	:	CATCH_DIRECTIVE nonvoid_type_descriptor from=offset_or_label to=offset_or_label using=offset_or_label
		-> ^(I_CATCH[$start, "I_CATCH"] I_ADDRESS[$start, Integer.toString($method::currentAddress)] nonvoid_type_descriptor $from $to $using)
	;
	
catchall_directive
	:	CATCHALL_DIRECTIVE from=offset_or_label to=offset_or_label using=offset_or_label
		-> ^(I_CATCHALL[$start, "I_CATCHALL"] I_ADDRESS[$start, Integer.toString($method::currentAddress)] $from $to $using)
	;


parameter_directive
	:	PARAMETER_DIRECTIVE 
		(	STRING_LITERAL
			(	(annotation+ END_PARAMETER_DIRECTIVE)=> annotation+ END_PARAMETER_DIRECTIVE
			|	END_PARAMETER_DIRECTIVE?
			)
			-> ^(I_PARAMETER STRING_LITERAL ^(I_ANNOTATIONS annotation*))
		|	(	(annotation+ END_PARAMETER_DIRECTIVE)=> annotation+ END_PARAMETER_DIRECTIVE
			|	END_PARAMETER_DIRECTIVE?
			)
			-> ^(I_PARAMETER I_PARAMETER_NOT_SPECIFIED ^(I_ANNOTATIONS annotation*))
		);

ordered_debug_directive
	:	line_directive
	|	local_directive
	|	end_local_directive
	|	restart_local_directive
	|	prologue_directive
	|	epilogue_directive
	|	source_directive;

line_directive
	:	LINE_DIRECTIVE integral_literal
		-> ^(I_LINE integral_literal I_ADDRESS[$start, Integer.toString($method::currentAddress)]);
					
local_directive
	:	LOCAL_DIRECTIVE	REGISTER SIMPLE_NAME nonvoid_type_descriptor STRING_LITERAL?
		-> ^(I_LOCAL[$start, "I_LOCAL"] REGISTER SIMPLE_NAME nonvoid_type_descriptor STRING_LITERAL? I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

end_local_directive
	:	END_LOCAL_DIRECTIVE REGISTER
		-> ^(I_END_LOCAL[$start, "I_END_LOCAL"] REGISTER I_ADDRESS[$start, Integer.toString($method::currentAddress)]);
		
restart_local_directive
	:	RESTART_LOCAL_DIRECTIVE REGISTER
		-> ^(I_RESTART_LOCAL[$start, "I_RESTART_LOCAL"] REGISTER I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

prologue_directive
	:	PROLOGUE_DIRECTIVE
		-> ^(I_PROLOGUE[$start, "I_PROLOGUE"] I_ADDRESS[$start, Integer.toString($method::currentAddress)]);

epilogue_directive
	:	EPILOGUE_DIRECTIVE
		-> ^(I_EPILOGUE[$start, "I_EPILOGUE"] I_ADDRESS[$start, Integer.toString($method::currentAddress)]);
		
source_directive
	:	SOURCE_DIRECTIVE STRING_LITERAL
		-> ^(I_SOURCE[$start, "I_SOURCE"] STRING_LITERAL I_ADDRESS[$start, Integer.toString($method::currentAddress)]);
		
label
	:	LABEL -> ^(I_LABEL LABEL I_ADDRESS[$start, Integer.toString($method::currentAddress)]);
	
instruction returns [int size]
	@init {boolean needsNop = false; int targetCount = 0;}
	:	//e.g. goto endloop:
		//e.g. goto +3
		INSTRUCTION_FORMAT10t (LABEL | OFFSET) {$size = Format.Format10t.size;}
		-> ^(I_STATEMENT_FORMAT10t[$start, "I_STATEMENT_FORMAT10t"] INSTRUCTION_FORMAT10t LABEL? OFFSET?)
	|	//e.g. return
		INSTRUCTION_FORMAT10x {$size = Format.Format10x.size;}
		-> ^(I_STATEMENT_FORMAT10x[$start, "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x)
	|	//e.g. const/4 v0, 5
		INSTRUCTION_FORMAT11n REGISTER integral_literal {$size = Format.Format11n.size;}
		-> ^(I_STATEMENT_FORMAT11n[$start, "I_STARTMENT_FORMAT11n"] INSTRUCTION_FORMAT11n REGISTER integral_literal)
	|	//e.g. move-result-object v1
		INSTRUCTION_FORMAT11x REGISTER {$size = Format.Format11x.size;}
		-> ^(I_STATEMENT_FORMAT11x[$start, "I_STATEMENT_FORMAT11x"] INSTRUCTION_FORMAT11x REGISTER)
	|	//e.g. move v1 v2
		INSTRUCTION_FORMAT12x REGISTER REGISTER {$size = Format.Format12x.size;}
		-> ^(I_STATEMENT_FORMAT12x[$start, "I_STATEMENT_FORMAT12x"] INSTRUCTION_FORMAT12x REGISTER REGISTER)		
	|	//e.g. goto/16 endloop:
		INSTRUCTION_FORMAT20t (LABEL | OFFSET) {$size = Format.Format20t.size;}
		-> ^(I_STATEMENT_FORMAT20t[$start, "I_STATEMENT_FORMAT20t"] INSTRUCTION_FORMAT20t LABEL? OFFSET?)
	|	//e.g. sget_object v0 java/lang/System/out LJava/io/PrintStream;
		INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field {$size = Format.Format21c.size;}
		-> ^(I_STATEMENT_FORMAT21c_FIELD[$start, "I_STATEMENT_FORMAT21c_FIELD"] INSTRUCTION_FORMAT21c_FIELD REGISTER fully_qualified_field)
	|	//e.g. const-string v1 "Hello World!"
		INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL {$size = Format.Format21c.size;}
		-> ^(I_STATEMENT_FORMAT21c_STRING[$start, "I_STATEMENT_FORMAT21c_STRING"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL)
	|	//e.g. const-class v2 org/jf/HelloWorld2/HelloWorld2
		INSTRUCTION_FORMAT21c_TYPE REGISTER reference_type_descriptor {$size = Format.Format21c.size;}
		-> ^(I_STATEMENT_FORMAT21c_TYPE[$start, "I_STATEMENT_FORMAT21c"] INSTRUCTION_FORMAT21c_TYPE REGISTER reference_type_descriptor)
	|	//e.g. const/high16 v1, 1234
		INSTRUCTION_FORMAT21h REGISTER integral_literal {$size = Format.Format21h.size;}
		-> ^(I_STATEMENT_FORMAT21h[$start, "I_STATEMENT_FORMAT21h"] INSTRUCTION_FORMAT21h REGISTER integral_literal)
	|	//e.g. const/16 v1, 1234
		INSTRUCTION_FORMAT21s REGISTER integral_literal {$size = Format.Format21s.size;}
		-> ^(I_STATEMENT_FORMAT21s[$start, "I_STATEMENT_FORMAT21s"] INSTRUCTION_FORMAT21s REGISTER integral_literal)
	|	//e.g. if-eqz v0, endloop:
		INSTRUCTION_FORMAT21t REGISTER (LABEL | OFFSET) {$size = Format.Format21t.size;}	
		-> ^(I_STATEMENT_FORMAT21t[$start, "I_STATEMENT_FORMAT21t"] INSTRUCTION_FORMAT21t REGISTER LABEL? OFFSET?)
	|	//e.g. add-int v0, v1, 123
		INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal {$size = Format.Format22b.size;}
		-> ^(I_STATEMENT_FORMAT22b[$start, "I_STATEMENT_FORMAT22b"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal)
	|	//e.g. iput-object v1, v0 org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
		INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER fully_qualified_field {$size = Format.Format22c.size;}
		-> ^(I_STATEMENT_FORMAT22c_FIELD[$start, "I_STATEMENT_FORMAT22c_FIELD"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER fully_qualified_field)
	|	//e.g. instance-of v0, v1, Ljava/lang/String;
		INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor {$size = Format.Format22c.size;}
		-> ^(I_STATEMENT_FORMAT22c_TYPE[$start, "I_STATEMENT_FORMAT22c_TYPE"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor)
	|	//e.g. add-int/lit16 v0, v1, 12345
		INSTRUCTION_FORMAT22s REGISTER REGISTER integral_literal {$size = Format.Format22s.size;}
		-> ^(I_STATEMENT_FORMAT22s[$start, "I_STATEMENT_FORMAT22s"] INSTRUCTION_FORMAT22s REGISTER REGISTER integral_literal)
	|	//e.g. if-eq v0, v1, endloop:
		INSTRUCTION_FORMAT22t REGISTER REGISTER (LABEL | OFFSET) {$size = Format.Format22t.size;}
		-> ^(I_STATEMENT_FORMAT22t[$start, "I_STATEMENT_FFORMAT22t"] INSTRUCTION_FORMAT22t REGISTER REGISTER LABEL? OFFSET?)
	|	//e.g. move/from16 v1, v1234
		INSTRUCTION_FORMAT22x REGISTER REGISTER {$size = Format.Format22x.size;}
		-> ^(I_STATEMENT_FORMAT22x[$start, "I_STATEMENT_FORMAT22x"] INSTRUCTION_FORMAT22x REGISTER REGISTER)
	|	//e.g. add-int v1, v2, v3
		INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER {$size = Format.Format23x.size;}
		-> ^(I_STATEMENT_FORMAT23x[$start, "I_STATEMENT_FORMAT23x"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER)
	|	//e.g. goto/32 endloop:
		INSTRUCTION_FORMAT30t (LABEL | OFFSET) {$size = Format.Format30t.size;}
		-> ^(I_STATEMENT_FORMAT30t[$start, "I_STATEMENT_FORMAT30t"] INSTRUCTION_FORMAT30t LABEL? OFFSET?)
	|	//e.g. const-string/jumbo v1 "Hello World!"
		INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL {$size = Format.Format31c.size;}
		->^(I_STATEMENT_FORMAT31c[$start, "I_STATEMENT_FORMAT31c"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL)	
	|	//e.g. const v0, 123456
		INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal {$size = Format.Format31i.size;}
		-> ^(I_STATEMENT_FORMAT31i[$start, "I_STATEMENT_FORMAT31i"] INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal)
	|	//e.g. fill-array-data v0, ArrayData:
		INSTRUCTION_FORMAT31t REGISTER offset_or_label {$size = Format.Format31t.size;}
		{
			if ($INSTRUCTION_FORMAT31t.text.equals("packed-switch")) {
				CommonTree root = new CommonTree(new CommonToken(I_PACKED_SWITCH_DECLARATION, "I_PACKED_SWITCH_DECLARATION"));
				CommonTree address = new CommonTree(new CommonToken(I_ADDRESS, Integer.toString($method::currentAddress)));
				root.addChild(address);
				root.addChild($offset_or_label.tree.dupNode());
				$statements_and_directives::packedSwitchDeclarations.add(root);
			} else if ($INSTRUCTION_FORMAT31t.text.equals("sparse-switch")) {
				CommonTree root = new CommonTree(new CommonToken(I_SPARSE_SWITCH_DECLARATION, "I_SPARSE_SWITCH_DECLARATION"));
				CommonTree address = new CommonTree(new CommonToken(I_ADDRESS, Integer.toString($method::currentAddress)));
				root.addChild(address);
				root.addChild($offset_or_label.tree.dupNode());
				$statements_and_directives::sparseSwitchDeclarations.add(root);
			}			
		}
		-> ^(I_STATEMENT_FORMAT31t[$start, "I_STATEMENT_FORMAT31t"] INSTRUCTION_FORMAT31t REGISTER offset_or_label)
	|	//e.g. move/16 v4567, v1234
		INSTRUCTION_FORMAT32x REGISTER REGISTER {$size = Format.Format32x.size;}
		-> ^(I_STATEMENT_FORMAT32x[$start, "I_STATEMENT_FORMAT32x"] INSTRUCTION_FORMAT32x REGISTER REGISTER)		
	|	//e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
		INSTRUCTION_FORMAT35c_METHOD OPEN_BRACKET register_list CLOSE_BRACKET fully_qualified_method {$size = Format.Format35c.size;}
		-> ^(I_STATEMENT_FORMAT35c_METHOD[$start, "I_STATEMENT_FORMAT35c_METHOD"] INSTRUCTION_FORMAT35c_METHOD register_list fully_qualified_method)
	|	//e.g. filled-new-array {v0,v1}, I
		INSTRUCTION_FORMAT35c_TYPE OPEN_BRACKET register_list CLOSE_BRACKET nonvoid_type_descriptor {$size = Format.Format35c.size;}
		-> ^(I_STATEMENT_FORMAT35c_TYPE[$start, "I_STATEMENT_FORMAT35c_TYPE"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor)
	|	//e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
		INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACKET register_range CLOSE_BRACKET fully_qualified_method {$size = Format.Format3rc.size;}
		-> ^(I_STATEMENT_FORMAT3rc_METHOD[$start, "I_STATEMENT_FORMAT3rc_METHOD"] INSTRUCTION_FORMAT3rc_METHOD register_range fully_qualified_method)
	|	//e.g. filled-new-array/range {v0..v6} I
		INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACKET register_range CLOSE_BRACKET nonvoid_type_descriptor {$size = Format.Format3rc.size;}
		-> ^(I_STATEMENT_FORMAT3rc_TYPE[$start, "I_STATEMENT_FORMAT3rc_TYPE"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor)
	|	//e.g. const-wide v0, 5000000000L
		INSTRUCTION_FORMAT51l REGISTER fixed_literal {$size = Format.Format51l.size;}
		-> ^(I_STATEMENT_FORMAT51l[$start, "I_STATEMENT_FORMAT51l"] INSTRUCTION_FORMAT51l REGISTER fixed_literal)
	|	
		ARRAY_DATA_DIRECTIVE
		{	
			if (($method::currentAddress \% 2) != 0) {
				needsNop = true;
				$size = 2;
			} else {
				$size = 0;
			}
		}
		
		integral_literal (fixed_literal {$size+=$fixed_literal.size;})* END_ARRAY_DATA_DIRECTIVE
		{$size = (($size + 1)/2)*2 + 8;}
		
		/*add a nop statement before this if needed to force the correct alignment*/
 		->	{needsNop}?	^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"]) 
 					^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE integral_literal) ^(I_ARRAY_ELEMENTS fixed_literal*))

 		->	^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE integral_literal) ^(I_ARRAY_ELEMENTS fixed_literal*))
 	|	
 		PACKED_SWITCH_DIRECTIVE
 		{
 			targetCount = 0;
 			if (($method::currentAddress \% 2) != 0) {
 				needsNop = true;
 				$size = 2;
 			} else {
 				$size = 0;
 			}
 		}
 			
 		fixed_32bit_literal 
 		
 		(switch_target += offset_or_label {$size+=4; targetCount++;})*
 		
 		END_PACKED_SWITCH_DIRECTIVE {$size = $size + 8;}
 		
		/*add a nop statement before this if needed to force the correct alignment*/
 		->	{needsNop}?	^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"]) 
 					^(I_STATEMENT_PACKED_SWITCH[$start, "I_STATEMENT_PACKED_SWITCH"] 
 						^(I_PACKED_SWITCH_START_KEY[$start, "I_PACKED_SWITCH_START_KEY"] fixed_32bit_literal) 
 						^(I_PACKED_SWITCH_TARGETS[$start, "I_PACKED_SWITCH_TARGETS"] I_PACKED_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)] $switch_target*)
 					)
 					
		->	^(I_STATEMENT_PACKED_SWITCH[$start, "I_STATEMENT_PACKED_SWITCH"] 
				^(I_PACKED_SWITCH_START_KEY[$start, "I_PACKED_SWITCH_START_KEY"] fixed_32bit_literal) 
				^(I_PACKED_SWITCH_TARGETS[$start, "I_PACKED_SWITCH_TARGETS"] I_PACKED_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)] $switch_target*)
			)
 		
 	|
 		SPARSE_SWITCH_DIRECTIVE
 		{
 			targetCount = 0;
 			if (($method::currentAddress \% 2) != 0) {
 				needsNop = true;
 				$size = 2;
 			} else {
 				$size = 0;
 			}
 		}
 			
 		(fixed_32bit_literal switch_target += offset_or_label {$size += 8; targetCount++;})*
 		
 		END_SPARSE_SWITCH_DIRECTIVE {$size = $size + 4;}
 		
		/*add a nop statement before this if needed to force the correct alignment*/
 		->	{needsNop}?	^(I_STATEMENT_FORMAT10x[$start,  "I_STATEMENT_FORMAT10x"] INSTRUCTION_FORMAT10x[$start, "nop"]) 
 					^(I_STATEMENT_SPARSE_SWITCH[$start, "I_STATEMENT_SPARSE_SWITCH"]
 						I_SPARSE_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)]
 						^(I_SPARSE_SWITCH_KEYS[$start, "I_SPARSE_SWITCH_KEYS"] fixed_32bit_literal*)
 						^(I_SPARSE_SWITCH_TARGETS $switch_target*))
		->	^(I_STATEMENT_SPARSE_SWITCH[$start, "I_STATEMENT_SPARSE_SWITCH"]
				I_SPARSE_SWITCH_TARGET_COUNT[$start, Integer.toString(targetCount)]
				^(I_SPARSE_SWITCH_KEYS[$start, "I_SPARSE_SWITCH_KEYS"] fixed_32bit_literal*)
				^(I_SPARSE_SWITCH_TARGETS $switch_target*)) 					
	;
	
offset_or_label
	:	OFFSET | LABEL;	


register_list
	:	REGISTER* -> ^(I_REGISTER_LIST[$start, "I_REGISTER_LIST"] REGISTER*);
	
register_range
	:	REGISTER REGISTER? -> ^(I_REGISTER_RANGE[$start, "I_REGISTER_RANGE"] REGISTER REGISTER?);


nonvoid_type_descriptor
	:	PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR
	;
	
reference_type_descriptor
	:	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR;
	
type_descriptor
	:	VOID_TYPE
	|	PRIMITIVE_TYPE
	|	CLASS_DESCRIPTOR
	|	ARRAY_DESCRIPTOR
	;	
	
integral_literal
	:	LONG_LITERAL
	|	INTEGER_LITERAL
	|	SHORT_LITERAL
	|	CHAR_LITERAL
	|	BYTE_LITERAL;	
	
fixed_32bit_literal
	:	INTEGER_LITERAL
	|	LONG_LITERAL
	|	SHORT_LITERAL
	|	BYTE_LITERAL
	|	FLOAT_LITERAL
	|	CHAR_LITERAL
	|	BOOL_LITERAL;
	
fixed_literal returns[int size]
	:	INTEGER_LITERAL {$size = 4;}
	|	LONG_LITERAL {$size = 8;}
	|	SHORT_LITERAL {$size = 2;}
	|	BYTE_LITERAL {$size = 1;}
	|	FLOAT_LITERAL {$size = 4;}
	|	DOUBLE_LITERAL {$size = 8;}
	|	CHAR_LITERAL {$size = 2;}
	|	BOOL_LITERAL {$size = 1;};

literal
	:	INTEGER_LITERAL
	|	LONG_LITERAL
	|	SHORT_LITERAL
	|	BYTE_LITERAL
	|	FLOAT_LITERAL
	|	DOUBLE_LITERAL
	|	CHAR_LITERAL
	|	STRING_LITERAL
	|	BOOL_LITERAL
	|	NULL_LITERAL
	|	array_literal
	|	subannotation
	|	type_field_method_literal
	|	enum_literal;
	
array_literal
	:	ARRAY_START literal* ARRAY_END
		-> ^(I_ENCODED_ARRAY[$start, "I_ENCODED_ARRAY"] literal*);
		
annotation
	:	ANNOTATION_START ANNOTATION_VISIBILITY CLASS_DESCRIPTOR 
		annotation_element* ANNOTATION_END
		-> ^(I_ANNOTATION[$start, "I_ANNOTATION"] ANNOTATION_VISIBILITY ^(I_SUBANNOTATION[$start, "I_SUBANNOTATION"] CLASS_DESCRIPTOR annotation_element*));
	
annotation_element
	:	MEMBER_NAME literal
		-> ^(I_ANNOTATION_ELEMENT[$start, "I_ANNOTATION_ELEMENT"] MEMBER_NAME literal);

subannotation
	:	SUBANNOTATION_START CLASS_DESCRIPTOR annotation_element* SUBANNOTATION_END
		-> ^(I_SUBANNOTATION[$start, "I_SUBANNOTATION"] CLASS_DESCRIPTOR annotation_element*);
		
type_field_method_literal
	:	reference_type_descriptor 
		(	ARROW MEMBER_NAME
			(	nonvoid_type_descriptor -> ^(I_ENCODED_FIELD reference_type_descriptor MEMBER_NAME nonvoid_type_descriptor)
			|	method_prototype -> ^(I_ENCODED_METHOD reference_type_descriptor MEMBER_NAME method_prototype)
			)
		|	-> reference_type_descriptor
		)
	|	PRIMITIVE_TYPE
	|	VOID_TYPE;

enum_literal
	:	ENUM	
		reference_type_descriptor
		ARROW
		MEMBER_NAME
		reference_type_descriptor
	-> 	^(I_ENCODED_ENUM reference_type_descriptor MEMBER_NAME reference_type_descriptor);
