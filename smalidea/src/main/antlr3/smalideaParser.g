/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

parser grammar smalideaParser;

options {
  tokenVocab=smaliParser;
}

@header {
package org.jf.smalidea;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import org.jf.smalidea.psi.SmaliElementTypes;
}


@members {
    private PsiBuilder psiBuilder;

    public void setPsiBuilder(PsiBuilder psiBuilder) {
        this.psiBuilder = psiBuilder;
    }

    public Marker mark() {
        return psiBuilder.mark();
    }

    protected void syncToFollows(boolean acceptEof) {
        BitSet follow = computeErrorRecoverySet();
        int mark = input.mark();
        Marker marker = null;
        try {
            int token = input.LA(1);
            while (!follow.member(token)) {
                if (token == Token.EOF) {
                    if (acceptEof) {
                        break;
                    }
                    input.rewind(mark);
                    mark = -1;
                    marker = null;
                    return;
                }
                if (marker == null) {
                    marker = mark();
                }
                input.consume();
                token = input.LA(1);
            }
        } finally {
            if  (mark != -1) {
                input.release(mark);
            }
            if (marker != null) {
                marker.error("Unexpected tokens");
            }
        }
    }

    @Override
    protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow)
            throws RecognitionException
    {
        RecognitionException e = null;
        // if next token is what we are looking for then "delete" this token
        if ( mismatchIsUnwantedToken(input, ttype) ) {
            e = new UnwantedTokenException(ttype, input);
            /*
               System.err.println("recoverFromMismatchedToken deleting "+
                                  ((TokenStream)input).LT(1)+
                                  " since "+((TokenStream)input).LT(2)+" is what we want");
                */
            beginResync();
            Marker mark = mark();
            input.consume(); // simply delete extra token
            mark.error(getErrorMessage(e, tokenNames));
            endResync();
            reportError(e, true);  // report after consuming so AW sees the token in the exception
            // we want to return the token we're actually matching
            Object matchedSymbol = getCurrentInputSymbol(input);
            input.consume(); // move past ttype token as if all were ok
            return matchedSymbol;
        }
        // can't recover with single token deletion, try insertion
        if ( mismatchIsMissingToken(input, follow) ) {
            Object inserted = getMissingSymbol(input, e, ttype, follow);
            Marker mark = mark();
            e = new MissingTokenException(ttype, input, inserted);
            mark.error(getErrorMessage(e, tokenNames));
            reportError(e, true);  // report after inserting so AW sees the token in the exception
            return inserted;
        }

        // even that didn't work; must throw the exception
        e = new MismatchedTokenException(ttype, input);
        throw e;
    }

    @Override
    public void reportError(RecognitionException e) {
        reportError(e, false);
    }

    public void reportError(RecognitionException e, boolean alreadyReported) {
        // if we've already reported an error and have not matched a token
        // yet successfully, don't report any errors.
        if ( state.errorRecovery ) {
            //System.err.print("[SPURIOUS] ");
            return;
        }
        state.syntaxErrors++; // don't count spurious
        state.errorRecovery = true;

        if (!alreadyReported) {
            displayRecognitionError(this.getTokenNames(), e);
        }
    }

    @Override
    public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
        Marker mark = psiBuilder.mark();
        mark.error(getErrorMessage(e, tokenNames));
    }
}

sync[boolean toEof]
  @init { syncToFollows($toEof); }
  : /*epsilon*/;

smali_file
  @init {
    mark().done(SmaliElementTypes.MODIFIER_LIST);
    mark().done(SmaliElementTypes.EXTENDS_LIST);
    mark().done(SmaliElementTypes.IMPLEMENTS_LIST);
  }
  :
  (
    ( class_spec
    | super_spec
    | implements_spec
    | source_spec
    | method
    | field
    | annotation
    )
    sync[true]
  )+
  EOF;

class_spec
  @init { Marker marker = mark(); }
  : CLASS_DIRECTIVE access_list class_descriptor;
  finally { marker.done(SmaliElementTypes.CLASS_STATEMENT); }

super_spec
  @init { Marker marker = mark(); }
  : SUPER_DIRECTIVE class_descriptor;
  finally { marker.done(SmaliElementTypes.SUPER_STATEMENT); }

implements_spec
  @init { Marker marker = mark(); }
  : IMPLEMENTS_DIRECTIVE class_descriptor;
  finally { marker.done(SmaliElementTypes.IMPLEMENTS_STATEMENT); }

source_spec
  @init { Marker marker = mark(); }
  : SOURCE_DIRECTIVE string_literal;
  finally { marker.done(SmaliElementTypes.SOURCE_STATEMENT); }

access_list
  @init { Marker marker = mark(); }
  : ACCESS_SPEC*;
  finally { marker.done(SmaliElementTypes.ACCESS_LIST); }


/*When there are annotations immediately after a field definition, we don't know whether they are field annotations
or class annotations until we determine if there is an .end field directive. In either case, we still "consume" and parse
the annotations. If it turns out that they are field annotations, we include them in the I_FIELD AST. Otherwise, we
add them to the $smali_file::classAnnotations list*/
field
  @init {
    Marker marker = mark();
    mark().done(SmaliElementTypes.MODIFIER_LIST);
    Marker annotationsMarker = null;
    Marker fieldInitializerMarker = null;
    boolean classAnnotations = true;
  }
  : FIELD_DIRECTIVE
    access_list
    member_name COLON nonvoid_type_descriptor
    ( {fieldInitializerMarker = mark();}
      EQUAL literal
      {fieldInitializerMarker.done(SmaliElementTypes.FIELD_INITIALIZER);}
    )?
    ( END_FIELD_DIRECTIVE
    | (ANNOTATION_DIRECTIVE)=> ( {annotationsMarker = mark();}
                                 ((ANNOTATION_DIRECTIVE)=> annotation)+
                                 (END_FIELD_DIRECTIVE {classAnnotations = false;})?
                               )
    | /*epsilon*/
    );
  finally {
    if (annotationsMarker != null) {
      if (classAnnotations) {
        marker.doneBefore(SmaliElementTypes.FIELD, annotationsMarker);
        annotationsMarker.drop();
      } else {
        annotationsMarker.drop();
        marker.done(SmaliElementTypes.FIELD);
      }
    } else {
      marker.done(SmaliElementTypes.FIELD);
    }
  }

method
  @init {
    Marker marker = mark();
    mark().done(SmaliElementTypes.MODIFIER_LIST);
  }
  : METHOD_DIRECTIVE access_list member_name method_prototype statements_and_directives
    END_METHOD_DIRECTIVE;
  finally { marker.done(SmaliElementTypes.METHOD); }

statements_and_directives
  : (
      ( ordered_method_item
      | registers_directive
      | catch_directive
      | catchall_directive
      | parameter_directive
      | annotation
      )
      sync[false]
    )*;

/* Method items whose order/location is important */
ordered_method_item
  : label
  | instruction
  | debug_directive;

registers_directive
  @init { Marker marker = mark(); }
  : (
      REGISTERS_DIRECTIVE integral_literal
    | LOCALS_DIRECTIVE integral_literal
    );
  finally { marker.done(SmaliElementTypes.REGISTERS_STATEMENT); }

param_list_or_id
  : PARAM_LIST_OR_ID_START primitive_type+ PARAM_LIST_OR_ID_END;

/*identifiers are much more general than most languages. Any of the below can either be
the indicated type OR an identifier, depending on the context*/
simple_name
  : SIMPLE_NAME
  | ACCESS_SPEC
  | VERIFICATION_ERROR_TYPE
  | POSITIVE_INTEGER_LITERAL
  | NEGATIVE_INTEGER_LITERAL
  | FLOAT_LITERAL_OR_ID
  | DOUBLE_LITERAL_OR_ID
  | BOOL_LITERAL
  | NULL_LITERAL
  | register
  | param_list_or_id
  | PRIMITIVE_TYPE
  | VOID_TYPE
  | ANNOTATION_VISIBILITY
  | INSTRUCTION_FORMAT10t
  | INSTRUCTION_FORMAT10x
  | INSTRUCTION_FORMAT10x_ODEX
  | INSTRUCTION_FORMAT11x
  | INSTRUCTION_FORMAT12x_OR_ID
  | INSTRUCTION_FORMAT21c_FIELD
  | INSTRUCTION_FORMAT21c_FIELD_ODEX
  | INSTRUCTION_FORMAT21c_STRING
  | INSTRUCTION_FORMAT21c_TYPE
  | INSTRUCTION_FORMAT21t
  | INSTRUCTION_FORMAT22c_FIELD
  | INSTRUCTION_FORMAT22c_FIELD_ODEX
  | INSTRUCTION_FORMAT22c_TYPE
  | INSTRUCTION_FORMAT22cs_FIELD
  | INSTRUCTION_FORMAT22s_OR_ID
  | INSTRUCTION_FORMAT22t
  | INSTRUCTION_FORMAT23x
  | INSTRUCTION_FORMAT31i_OR_ID
  | INSTRUCTION_FORMAT31t
  | INSTRUCTION_FORMAT35c_METHOD
  | INSTRUCTION_FORMAT35c_METHOD_ODEX
  | INSTRUCTION_FORMAT35c_TYPE
  | INSTRUCTION_FORMAT35mi_METHOD
  | INSTRUCTION_FORMAT35ms_METHOD
  | INSTRUCTION_FORMAT51l;

member_name
  @init { Marker marker = mark(); }
  : simple_name
  | MEMBER_NAME;
  finally { marker.done(SmaliElementTypes.MEMBER_NAME); }

method_prototype
  @init { Marker marker = mark(); }
  : OPEN_PAREN param_list CLOSE_PAREN type_descriptor;
  finally { marker.done(SmaliElementTypes.METHOD_PROTOTYPE); }

param_list
  @init { Marker marker = mark(); }
  : (PARAM_LIST_START param* PARAM_LIST_END)
  | (PARAM_LIST_OR_ID_START param* PARAM_LIST_OR_ID_END)
  | (param*);
  finally { marker.done(SmaliElementTypes.METHOD_PARAM_LIST); }

param
  @init {
    Marker marker = mark();
    mark().done(SmaliElementTypes.MODIFIER_LIST);
  }
  : nonvoid_type_descriptor;
  finally { marker.done(SmaliElementTypes.METHOD_PARAMETER); }

method_prototype_reference
  : OPEN_PAREN param_list_reference CLOSE_PAREN type_descriptor;

param_list_reference
  @init {
    Marker marker = mark();
  }
  : (PARAM_LIST_START nonvoid_type_descriptor* PARAM_LIST_END)
  | (PARAM_LIST_OR_ID_START nonvoid_type_descriptor* PARAM_LIST_OR_ID_END)
  | (nonvoid_type_descriptor*);
  finally { marker.done(SmaliElementTypes.METHOD_REFERENCE_PARAM_LIST); }

primitive_type
  @init { Marker marker = mark(); }
  : PRIMITIVE_TYPE;
  finally { marker.done(SmaliElementTypes.PRIMITIVE_TYPE); }

class_descriptor
  @init { Marker marker = mark(); }
  : CLASS_DESCRIPTOR;
  finally { marker.done(SmaliElementTypes.CLASS_TYPE); }

array_descriptor
  @init { Marker marker = mark(); }
  : ARRAY_TYPE_PREFIX (primitive_type | class_descriptor);
  finally { marker.done(SmaliElementTypes.ARRAY_TYPE); }

void_type
  @init { Marker marker = mark(); }
  : VOID_TYPE;
  finally { marker.done(SmaliElementTypes.VOID_TYPE); }

type_descriptor
  : void_type
  | primitive_type
  | class_descriptor
  | array_descriptor;

nonvoid_type_descriptor
  : primitive_type
  | class_descriptor
  | array_descriptor;

reference_type_descriptor
  : class_descriptor
  | array_descriptor;

null_literal
  @init { Marker marker = mark(); }
  : NULL_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

bool_literal
  @init { Marker marker = mark(); }
  : BOOL_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

byte_literal
  @init { Marker marker = mark(); }
  : BYTE_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

char_literal
  @init { Marker marker = mark(); }
  : CHAR_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

short_literal
  @init { Marker marker = mark(); }
  : SHORT_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

integer_literal
  @init { Marker marker = mark(); }
  : POSITIVE_INTEGER_LITERAL
  | NEGATIVE_INTEGER_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

long_literal
  @init { Marker marker = mark(); }
  : LONG_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

float_literal
  @init { Marker marker = mark(); }
  : FLOAT_LITERAL_OR_ID
  | FLOAT_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

double_literal
  @init { Marker marker = mark(); }
  : DOUBLE_LITERAL_OR_ID
  | DOUBLE_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

string_literal
  @init { Marker marker = mark(); }
  : STRING_LITERAL;
  finally { marker.done(SmaliElementTypes.LITERAL); }

array_literal
  @init { Marker marker = mark(); }
  : OPEN_BRACE (literal (COMMA literal)* | ) CLOSE_BRACE;
  finally { marker.done(SmaliElementTypes.LITERAL); }

enum_literal
  @init { Marker marker = mark(); }
  : ENUM_DIRECTIVE reference_type_descriptor ARROW simple_name COLON reference_type_descriptor;
  finally { marker.done(SmaliElementTypes.LITERAL); }

type_field_method_literal
  @init { Marker marker = mark(); }
  : reference_type_descriptor
    ( ARROW
      ( member_name COLON nonvoid_type_descriptor
      | member_name method_prototype_reference
      )
    | /* epsilon */
    )
  | primitive_type
  | void_type;
  finally { marker.done(SmaliElementTypes.LITERAL); }

subannotation
  @init { Marker marker = mark(); }
  : SUBANNOTATION_DIRECTIVE class_descriptor annotation_element* END_SUBANNOTATION_DIRECTIVE;
  finally { marker.done(SmaliElementTypes.LITERAL); }

literal
  : long_literal
  | integer_literal
  | short_literal
  | byte_literal
  | float_literal
  | double_literal
  | char_literal
  | string_literal
  | bool_literal
  | null_literal
  | array_literal
  | subannotation
  | type_field_method_literal
  | enum_literal;

integral_literal
  : long_literal
  | integer_literal
  | short_literal
  | char_literal
  | byte_literal;

fixed_32bit_literal
  : long_literal
  | integer_literal
  | short_literal
  | byte_literal
  | float_literal
  | char_literal
  | bool_literal;

fixed_literal
  : integer_literal
  | long_literal
  | short_literal
  | byte_literal
  | float_literal
  | double_literal
  | char_literal
  | bool_literal;

annotation_element
  @init {
    Marker marker = mark();
    Marker nameMarker = null;
  }
  : { nameMarker = mark(); } simple_name { nameMarker.done(SmaliElementTypes.ANNOTATION_ELEMENT_NAME); }
    EQUAL literal;
  finally { marker.done(SmaliElementTypes.ANNOTATION_ELEMENT); }

annotation
  @init {
    Marker marker = mark();
    Marker paramListMarker = null;
  }
  : ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY class_descriptor
    { paramListMarker = mark(); }
    annotation_element*
    { paramListMarker.done(SmaliElementTypes.ANNOTATION_PARAMETER_LIST); }
    END_ANNOTATION_DIRECTIVE;
  finally { marker.done(SmaliElementTypes.ANNOTATION); }

fully_qualified_method
  @init { Marker marker = mark(); }
  : reference_type_descriptor ARROW member_name method_prototype_reference;
  finally { marker.done(SmaliElementTypes.METHOD_REFERENCE); }

fully_qualified_field
  @init { Marker marker = mark(); }
  : reference_type_descriptor ARROW member_name COLON nonvoid_type_descriptor;
  finally { marker.done(SmaliElementTypes.FIELD_REFERENCE); }

label
  @init { Marker marker = mark(); }
  : COLON simple_name;
  finally { marker.done(SmaliElementTypes.LABEL); }

label_ref
  @init { Marker marker = mark(); }
  : COLON simple_name;
  finally { marker.done(SmaliElementTypes.LABEL_REFERENCE); }

register_list
  : register (COMMA register)*
  | /* epsilon */;

register_range
  : (register (DOTDOT register)?)?;

verification_error_reference
  : class_descriptor | fully_qualified_field | fully_qualified_method;

catch_directive
  @init { Marker marker = mark(); }
  : CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE label_ref DOTDOT label_ref CLOSE_BRACE label_ref;
  finally { marker.done(SmaliElementTypes.CATCH_STATEMENT); }

catchall_directive
  @init { Marker marker = mark(); }
  : CATCHALL_DIRECTIVE OPEN_BRACE label_ref DOTDOT label_ref CLOSE_BRACE label_ref;
  finally { marker.done(SmaliElementTypes.CATCH_ALL_STATEMENT); }

/*When there are annotations immediately after a parameter definition, we don't know whether they are parameter annotations
or method annotations until we determine if there is an .end parameter directive. In either case, we still "consume" and parse
the annotations. If it turns out that they are parameter annotations, we include them in the I_PARAMETER AST. Otherwise, we
add them to the $statements_and_directives::methodAnnotations list*/
parameter_directive
  @init {
    Marker marker = mark();
    Marker preAnnotationMarker = null;
    Marker nameMarker = null;
  }
  : PARAMETER_DIRECTIVE register
    (COMMA { nameMarker = mark(); } string_literal { nameMarker.done(SmaliElementTypes.LOCAL_NAME); })?
    { preAnnotationMarker = mark(); }
    ({input.LA(1) == ANNOTATION_DIRECTIVE}? annotation)*
    ( END_PARAMETER_DIRECTIVE {
        preAnnotationMarker.drop();
        marker.done(SmaliElementTypes.PARAMETER_STATEMENT);
      }
    | /*epsilon*/ {
        marker.doneBefore(SmaliElementTypes.PARAMETER_STATEMENT, preAnnotationMarker);
        preAnnotationMarker.drop();
      }
    );

register
  @init { Marker marker = mark(); }
  : REGISTER;
  finally { marker.done(SmaliElementTypes.REGISTER_REFERENCE); }

debug_directive
  : line_directive
  | local_directive
  | end_local_directive
  | restart_local_directive
  | prologue_directive
  | epilogue_directive
  | source_directive;

line_directive
  @init { Marker marker = mark(); }
  : LINE_DIRECTIVE integral_literal;
  finally { marker.done(SmaliElementTypes.LINE_DEBUG_STATEMENT); }

local_directive
  @init { Marker marker = mark(); }
  : LOCAL_DIRECTIVE register (COMMA (null_literal | string_literal) COLON (void_type | nonvoid_type_descriptor)
                              (COMMA string_literal)? )?;
  finally { marker.done(SmaliElementTypes.LOCAL_DEBUG_STATEMENT); }

end_local_directive
  @init { Marker marker = mark(); }
  : END_LOCAL_DIRECTIVE register;
  finally { marker.done(SmaliElementTypes.END_LOCAL_DEBUG_STATEMENT); }

restart_local_directive
  @init { Marker marker = mark(); }
  : RESTART_LOCAL_DIRECTIVE register;
  finally { marker.done(SmaliElementTypes.RESTART_LOCAL_DEBUG_STATEMENT); }

prologue_directive
  @init { Marker marker = mark(); }
  : PROLOGUE_DIRECTIVE;
  finally { marker.done(SmaliElementTypes.PROLOGUE_DEBUG_STATEMENT); }

epilogue_directive
  @init { Marker marker = mark(); }
  : EPILOGUE_DIRECTIVE;
  finally { marker.done(SmaliElementTypes.EPILOGUE_DEBUG_STATEMENT); }

source_directive
  @init { Marker marker = mark(); }
  : SOURCE_DIRECTIVE string_literal?;
  finally { marker.done(SmaliElementTypes.SOURCE_DEBUG_STATEMENT); }

instruction_format12x
  : INSTRUCTION_FORMAT12x
  | INSTRUCTION_FORMAT12x_OR_ID;

instruction_format22s
  : INSTRUCTION_FORMAT22s
  | INSTRUCTION_FORMAT22s_OR_ID;

instruction_format31i
  : INSTRUCTION_FORMAT31i
  | INSTRUCTION_FORMAT31i_OR_ID;

instruction
  @init { Marker marker = mark(); }
  : insn_format10t
  | insn_format10x
  | insn_format10x_odex
  | insn_format11n
  | insn_format11x
  | insn_format12x
  | insn_format20bc
  | insn_format20t
  | insn_format21c_field
  | insn_format21c_field_odex
  | insn_format21c_string
  | insn_format21c_type
  | insn_format21ih
  | insn_format21lh
  | insn_format21s
  | insn_format21t
  | insn_format22b
  | insn_format22c_field
  | insn_format22c_field_odex
  | insn_format22c_type
  | insn_format22cs_field
  | insn_format22s
  | insn_format22t
  | insn_format22x
  | insn_format23x
  | insn_format30t
  | insn_format31c
  | insn_format31i
  | insn_format31t
  | insn_format32x
  | insn_format35c_method
  | insn_format35c_type
  | insn_format35c_method_odex
  | insn_format35mi_method
  | insn_format35ms_method
  | insn_format3rc_method
  | insn_format3rc_method_odex
  | insn_format3rc_type
  | insn_format3rmi_method
  | insn_format3rms_method
  | insn_format51l
  | insn_array_data_directive
  | insn_packed_switch_directive
  | insn_sparse_switch_directive;
  finally { marker.done(SmaliElementTypes.INSTRUCTION); }

insn_format10t
  : //e.g. goto endloop:
    //e.g. goto +3
    INSTRUCTION_FORMAT10t label_ref;

insn_format10x
  : //e.g. return-void
    INSTRUCTION_FORMAT10x;

insn_format10x_odex
  : //e.g. return-void-barrier
    INSTRUCTION_FORMAT10x_ODEX;

insn_format11n
  : //e.g. const/4 v0, 5
    INSTRUCTION_FORMAT11n register COMMA integral_literal;

insn_format11x
  : //e.g. move-result-object v1
    INSTRUCTION_FORMAT11x register;

insn_format12x
  : //e.g. move v1 v2
    instruction_format12x register COMMA register;

insn_format20bc
  : //e.g. throw-verification-error generic-error, Lsome/class;
    INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference;

insn_format20t
  : //e.g. goto/16 endloop:
    INSTRUCTION_FORMAT20t label_ref;

insn_format21c_field
  : //e.g. sget-object v0, java/lang/System/out LJava/io/PrintStream;
    INSTRUCTION_FORMAT21c_FIELD register COMMA fully_qualified_field;

insn_format21c_field_odex
  : //e.g. sget-object-volatile v0, java/lang/System/out LJava/io/PrintStream;
    INSTRUCTION_FORMAT21c_FIELD_ODEX register COMMA fully_qualified_field;

insn_format21c_string
  : //e.g. const-string v1, "Hello World!"
    INSTRUCTION_FORMAT21c_STRING register COMMA string_literal;

insn_format21c_type
  : //e.g. const-class v2, Lorg/jf/HelloWorld2/HelloWorld2;
    INSTRUCTION_FORMAT21c_TYPE register COMMA nonvoid_type_descriptor;

insn_format21ih
  : //e.g. const/high16 v1, 1234
    INSTRUCTION_FORMAT21ih register COMMA fixed_32bit_literal;

insn_format21lh
  : //e.g. const-wide/high16 v1, 1234
    INSTRUCTION_FORMAT21lh register COMMA fixed_32bit_literal;

insn_format21s
  : //e.g. const/16 v1, 1234
    INSTRUCTION_FORMAT21s register COMMA integral_literal;

insn_format21t
  : //e.g. if-eqz v0, endloop:
    INSTRUCTION_FORMAT21t register COMMA label_ref;

insn_format22b
  : //e.g. add-int v0, v1, 123
    INSTRUCTION_FORMAT22b register COMMA register COMMA integral_literal;

insn_format22c_field
  : //e.g. iput-object v1, v0 org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    INSTRUCTION_FORMAT22c_FIELD register COMMA register COMMA fully_qualified_field;

insn_format22c_field_odex
  : //e.g. iput-object-volatile v1, v0 org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    INSTRUCTION_FORMAT22c_FIELD_ODEX register COMMA register COMMA fully_qualified_field;

insn_format22c_type
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    INSTRUCTION_FORMAT22c_TYPE register COMMA register COMMA nonvoid_type_descriptor;

insn_format22cs_field
  : //e.g. iget-quick v0, v1, field@0xc
    INSTRUCTION_FORMAT22cs_FIELD register COMMA register COMMA FIELD_OFFSET;

insn_format22s
  : //e.g. add-int/lit16 v0, v1, 12345
    instruction_format22s register COMMA register COMMA integral_literal;

insn_format22t
  : //e.g. if-eq v0, v1, endloop:
    INSTRUCTION_FORMAT22t register COMMA register COMMA label_ref;

insn_format22x
  : //e.g. move/from16 v1, v1234
    INSTRUCTION_FORMAT22x register COMMA register;

insn_format23x
  : //e.g. add-int v1, v2, v3
    INSTRUCTION_FORMAT23x register COMMA register COMMA register;

insn_format30t
  : //e.g. goto/32 endloop:
    INSTRUCTION_FORMAT30t label_ref;

insn_format31c
  : //e.g. const-string/jumbo v1 "Hello World!"
    INSTRUCTION_FORMAT31c register COMMA string_literal;

insn_format31i
  : //e.g. const v0, 123456
    instruction_format31i register COMMA fixed_32bit_literal;

insn_format31t
  : //e.g. fill-array-data v0, ArrayData:
    INSTRUCTION_FORMAT31t register COMMA label_ref;

insn_format32x
  : //e.g. move/16 v4567, v1234
    INSTRUCTION_FORMAT32x register COMMA register;

insn_format35c_method
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method;

insn_format35c_type
  : //e.g. filled-new-array {v0,v1}, I
    INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor;

insn_format35c_method_odex
  : //e.g. invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method;

insn_format35mi_method
  : //e.g. execute-inline {v0, v1}, inline@0x4
    INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX;

insn_format35ms_method
  : //e.g. invoke-virtual-quick {v0, v1}, vtable@0x4
    INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX;

insn_format3rc_method
  : //e.g. invoke-virtual/range {v25..v26}, java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA fully_qualified_method;

insn_format3rc_method_odex
  : //e.g. invoke-object-init/range {p0}, Ljava/lang/Object;-><init>()V
    INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA fully_qualified_method;

insn_format3rc_type
  : //e.g. filled-new-array/range {v0..v6}, I
    INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor;

insn_format3rmi_method
  : //e.g. execute-inline/range {v0 .. v10}, inline@0x14
    INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX;

insn_format3rms_method
  : //e.g. invoke-virtual-quick/range {v0 .. v10}, vtable@0x14
    INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX;

insn_format51l
  : //e.g. const-wide v0, 5000000000L
    INSTRUCTION_FORMAT51l register COMMA fixed_literal;

insn_array_data_directive
  : ARRAY_DATA_DIRECTIVE
    integer_literal
    array_data_element* END_ARRAY_DATA_DIRECTIVE;

array_data_element
  @init { Marker marker = mark(); }
  : fixed_literal;
  finally { marker.done(SmaliElementTypes.ARRAY_DATA_ELEMENT); }

insn_packed_switch_directive
  : PACKED_SWITCH_DIRECTIVE
    fixed_32bit_literal
    packed_switch_element*
    END_PACKED_SWITCH_DIRECTIVE;

packed_switch_element
  @init { Marker marker = mark(); }
  : label_ref;
  finally { marker.done(SmaliElementTypes.PACKED_SWITCH_ELEMENT); }

insn_sparse_switch_directive
  : SPARSE_SWITCH_DIRECTIVE
    sparse_switch_element*
    END_SPARSE_SWITCH_DIRECTIVE;

sparse_switch_element
  @init { Marker marker = mark(); }
  : fixed_32bit_literal ARROW label_ref;
  finally { marker.done(SmaliElementTypes.SPARSE_SWITCH_ELEMENT); }
