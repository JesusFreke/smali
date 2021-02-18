package org.jf.smali;

import static java.lang.Math.toIntExact;

import java.io.*;
import java.util.Stack;
import org.antlr.runtime.*;
import org.jf.smali.util.*;
import org.jf.util.*;
import static org.jf.smali.smaliParser.*;

%%

%public
%class smaliFlexLexer
%implements TokenSource
%implements LexerErrorInterface
%type Token
%unicode
%line
%column
%char

%ctorarg int apiLevel
%init{
    this.apiLevel = apiLevel;
%init}

%{
    private StringBuffer sb = new StringBuffer();
    private String tokenError = null;
    private int tokenStartLine;
    private int tokenStartCol;
    private int tokenStartChar;

    private int lexerErrors = 0;

    private File sourceFile;

    private boolean suppressErrors;

    private int apiLevel;

    private Stack<Integer> stateStack = new Stack<>();

    public Token nextToken() {
        try {
            Token token = yylex();
            if (token instanceof InvalidToken) {
                InvalidToken invalidToken = (InvalidToken)token;
                if (!suppressErrors) {
                    System.err.println(getErrorHeader(invalidToken) + " Error for input '" +
                        invalidToken.getText() + "': " + invalidToken.getMessage());
                }
                lexerErrors++;
            }
            return token;
        }
        catch (java.io.IOException e) {
            System.err.println("shouldn't happen: " + e.getMessage());
            return newToken(EOF);
        }
    }

    public void setLine(int line) {
        this.yyline = line-1;
    }

    public void setColumn(int column) {
        this.yycolumn = column;
    }

    public int getLine() {
        return this.yyline+1;
    }

    public int getColumn() {
        return this.yycolumn;
    }

    public void setSuppressErrors(boolean suppressErrors) {
        this.suppressErrors = suppressErrors;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getSourceName() {
        if (sourceFile == null) {
            return "";
        }
        try {
            return  PathUtil.getRelativeFile(new File("."), sourceFile).getPath();
        } catch (IOException ex) {
            return sourceFile.getAbsolutePath();
        }
    }

    public int getNumberOfSyntaxErrors() {
        return lexerErrors;
    }

    private Token newToken(int type, String text, boolean hidden) {
        CommonToken token = new CommonToken(type, text);
        if (hidden) {
            token.setChannel(Token.HIDDEN_CHANNEL);
        }
        // yychar is long, but antlr CommonToken only takes an int.
        token.setStartIndex(toIntExact(yychar));
        token.setStopIndex(stopIndex());
        token.setLine(getLine());
        token.setCharPositionInLine(getColumn());
        return token;
    }

    private Token newToken(int type, String text) {
        return newToken(type, text, false);
    }

    private Token newToken(int type, boolean hidden) {
        return newToken(type, yytext(), hidden);
    }

    private Token newToken(int type) {
        return newToken(type, yytext(), false);
    }

    private Token invalidToken(String message, String text) {
        InvalidToken token = new InvalidToken(message, text);
        // yychar is long, but antlr CommonToken only takes an int.
        token.setStartIndex(toIntExact(yychar));
        token.setStopIndex(stopIndex());
        token.setLine(getLine());
        token.setCharPositionInLine(getColumn());

        return token;
    }

    private Token invalidToken(String message) {
        return invalidToken(message, yytext());
    }

    private void beginStateBasedToken(int state) {
        stateStack.push(yystate());
        yybegin(state);
        sb.setLength(0);
        tokenStartLine = getLine();
        tokenStartCol = getColumn();
        // yychar is long, but antlr CommonToken only takes an int.
        tokenStartChar = toIntExact(yychar);
        tokenError = null;
    }

    private Token endStateBasedToken(int type) {
        if (tokenError != null) {
            return invalidStateBasedToken(tokenError);
        }

        yybegin(stateStack.pop());

        CommonToken token = new CommonToken(type, sb.toString());
        token.setStartIndex(tokenStartChar);
        token.setStopIndex(stopIndex());
        token.setLine(tokenStartLine);
        token.setCharPositionInLine(tokenStartCol);
        return token;
    }

    private void setStateBasedTokenError(String message) {
        if (tokenError == null) {
            tokenError = message;
        }
    }

    private Token invalidStateBasedToken(String message) {
        yybegin(stateStack.pop());

        InvalidToken token = new InvalidToken(message, sb.toString());
        token.setStartIndex(tokenStartChar);
        token.setStopIndex(stopIndex());
        token.setLine(tokenStartLine);
        token.setCharPositionInLine(tokenStartCol);
        return token;
    }

    public String getErrorHeader(InvalidToken token) {
        return getSourceName()+"["+ token.getLine()+","+token.getCharPositionInLine()+"]";
    }

    public void reset(CharSequence charSequence, int start, int end, int initialState) {
        zzReader = BlankReader.INSTANCE;
        zzBuffer = new char[charSequence.length()];
        for (int i=0; i<charSequence.length(); i++) {
            zzBuffer[i] = charSequence.charAt(i);
        }

        yychar = zzCurrentPos = zzMarkedPos = zzStartRead = start;
        zzEndRead = end;
        zzAtBOL = true;
        zzAtEOF = false;
        yybegin(initialState);
    }

    private String processQuotedSimpleName(String text) {
        // strip backticks
        return text.substring(1, text.length() - 1);
    }

    private String processQuotedSimpleNameWithSpaces(String text) {
        if (apiLevel < 30) {
            setStateBasedTokenError("spaces in class descriptors and member names are not supported prior to API " +
                "level 30/dex version 040");
        }
        return processQuotedSimpleName(text);
    }

    private int stopIndex() {
      // jflex yychar is long, but antlr CommonToken only takes an int for
      // stopIndex.
      return toIntExact(yychar + yylength() - 1);
    }
%}

HexPrefix = 0 [xX]

HexDigit = [0-9a-fA-F]
HexDigits = [0-9a-fA-F]{4}
FewerHexDigits = [0-9a-fA-F]{0,3}

Integer1 = 0
Integer2 = [1-9] [0-9]*
Integer3 = 0 [0-7]+
Integer4 = {HexPrefix} {HexDigit}+
Integer = {Integer1} | {Integer2} | {Integer3} | {Integer4}

DecimalExponent = [eE] -? [0-9]+

BinaryExponent = [pP] -? [0-9]+

/*This can either be a floating point number or an identifier*/
FloatOrID1 = -? [0-9]+ {DecimalExponent}
FloatOrID2 = -? {HexPrefix} {HexDigit}+ {BinaryExponent}
FloatOrID3 = -? [iI][nN][fF][iI][nN][iI][tT][yY]
FloatOrID4 = [nN][aA][nN]
FloatOrID =  {FloatOrID1} | {FloatOrID2} | {FloatOrID3} | {FloatOrID4}


/*This can only be a float and not an identifier, due to the decimal point*/
Float1 = -? [0-9]+ "." [0-9]* {DecimalExponent}?
Float2 = -? "." [0-9]+ {DecimalExponent}?
Float3 = -? {HexPrefix} {HexDigit}+ "." {HexDigit}* {BinaryExponent}
Float4 = -? {HexPrefix} "." {HexDigit}+ {BinaryExponent}
Float =  {Float1} | {Float2} | {Float3} | {Float4}

HighSurrogate = [\ud800-\udbff]

LowSurrogate = [\udc00-\udfff]

SimpleNameCharacter = ({HighSurrogate} {LowSurrogate}) | [A-Za-z0-9$\-_\u00a1-\u1fff\u2010-\u2027\u2030-\ud7ff\ue000-\uffef]
UnicodeSpace = [\u0020\u00A0\u1680\u2000-\u200A\u202F\u205F\u3000] /* Zs category */

SimpleNameRaw = {SimpleNameCharacter}+
SimpleNameQuoted = [`] {SimpleNameCharacter}+ [`]
SimpleNameQuotedWithSpaces = [`] ({SimpleNameCharacter} | {UnicodeSpace})+ [`]
SimpleName = {SimpleNameRaw} | {SimpleNameQuoted} | {SimpleNameQuotedWithSpaces}

PrimitiveType = [ZBSCIJFD]

ClassDescriptor = L ({SimpleName} "/")* {SimpleName} ;

ArrayPrefix = "["+

Type = {PrimitiveType} | {ClassDescriptor} | {ArrayPrefix} ({ClassDescriptor} | {PrimitiveType})


%state PARAM_LIST_OR_ID
%state PARAM_LIST
%state ARRAY_DESCRIPTOR
%state STRING
%state CHAR
%state CLASS_DESCRIPTOR_BEGINNING
%state CLASS_DESCRIPTOR_REMAINING

%%

/*Directives*/
<YYINITIAL>
{
    ".class" { return newToken(CLASS_DIRECTIVE); }
    ".super" { return newToken(SUPER_DIRECTIVE); }
    ".implements" { return newToken(IMPLEMENTS_DIRECTIVE); }
    ".source" { return newToken(SOURCE_DIRECTIVE); }
    ".field" { return newToken(FIELD_DIRECTIVE); }
    ".end field" { return newToken(END_FIELD_DIRECTIVE); }
    ".subannotation" { return newToken(SUBANNOTATION_DIRECTIVE); }
    ".end subannotation" { return newToken(END_SUBANNOTATION_DIRECTIVE); }
    ".annotation" { return newToken(ANNOTATION_DIRECTIVE); }
    ".end annotation" { return newToken(END_ANNOTATION_DIRECTIVE); }
    ".enum" { return newToken(ENUM_DIRECTIVE); }
    ".method" { return newToken(METHOD_DIRECTIVE); }
    ".end method" { return newToken(END_METHOD_DIRECTIVE); }
    ".registers" { return newToken(REGISTERS_DIRECTIVE); }
    ".locals" { return newToken(LOCALS_DIRECTIVE); }
    ".array-data" { return newToken(ARRAY_DATA_DIRECTIVE); }
    ".end array-data" { return newToken(END_ARRAY_DATA_DIRECTIVE); }
    ".packed-switch" { return newToken(PACKED_SWITCH_DIRECTIVE); }
    ".end packed-switch" { return newToken(END_PACKED_SWITCH_DIRECTIVE); }
    ".sparse-switch" { return newToken(SPARSE_SWITCH_DIRECTIVE); }
    ".end sparse-switch" { return newToken(END_SPARSE_SWITCH_DIRECTIVE); }
    ".catch" { return newToken(CATCH_DIRECTIVE); }
    ".catchall" { return newToken(CATCHALL_DIRECTIVE); }
    ".line" { return newToken(LINE_DIRECTIVE); }
    ".param" { return newToken(PARAMETER_DIRECTIVE); }
    ".end param" { return newToken(END_PARAMETER_DIRECTIVE); }
    ".local" { return newToken(LOCAL_DIRECTIVE); }
    ".end local" { return newToken(END_LOCAL_DIRECTIVE); }
    ".restart local" { return newToken(RESTART_LOCAL_DIRECTIVE); }
    ".prologue" { return newToken(PROLOGUE_DIRECTIVE); }
    ".epilogue" { return newToken(EPILOGUE_DIRECTIVE); }

    ".end" { return invalidToken("Invalid directive"); }
    ".end " [a-zA-z0-9\-_]+ { return invalidToken("Invalid directive"); }
    ".restart" { return invalidToken("Invalid directive"); }
    ".restart " [a-zA-z0-9\-_]+ { return invalidToken("Invalid directive"); }
}

/*Literals*/
<YYINITIAL> {
    {Integer} { return newToken(POSITIVE_INTEGER_LITERAL); }
    - {Integer} { return newToken(NEGATIVE_INTEGER_LITERAL); }
    -? {Integer} [lL] { return newToken(LONG_LITERAL); }
    -? {Integer} [sS] { return newToken(SHORT_LITERAL); }
    -? {Integer} [tT] { return newToken(BYTE_LITERAL); }

    {FloatOrID} [fF] | -? [0-9]+ [fF] { return newToken(FLOAT_LITERAL_OR_ID); }
    {FloatOrID} [dD]? | -? [0-9]+ [dD] { return newToken(DOUBLE_LITERAL_OR_ID); }
    {Float} [fF] { return newToken(FLOAT_LITERAL); }
    {Float} [dD]? { return newToken(DOUBLE_LITERAL); }

    "true"|"false" { return newToken(BOOL_LITERAL); }
    "null" { return newToken(NULL_LITERAL); }

    "\"" { beginStateBasedToken(STRING); sb.append('"'); }

    ' { beginStateBasedToken(CHAR); sb.append('\''); }
}

<PARAM_LIST_OR_ID> {
    {PrimitiveType} { return newToken(PARAM_LIST_OR_ID_PRIMITIVE_TYPE); }
    [^] { yypushback(1); yybegin(YYINITIAL); }
    <<EOF>> { yybegin(YYINITIAL); }
}

<PARAM_LIST> {
    {PrimitiveType} { return newToken(PRIMITIVE_TYPE); }
    {ClassDescriptor} {
        yypushback(yylength());
        beginStateBasedToken(CLASS_DESCRIPTOR_BEGINNING);
        sb.append(yytext());
    }
    {ArrayPrefix} { return newToken(ARRAY_TYPE_PREFIX); }
    [^] { yypushback(1); yybegin(YYINITIAL);}
    <<EOF>> { yybegin(YYINITIAL);}
}

<CLASS_DESCRIPTOR_BEGINNING> {
    "L" {SimpleNameRaw} {
        sb.append(yytext());
        yybegin(CLASS_DESCRIPTOR_REMAINING);
    }
    "L" {SimpleNameQuoted} {
        sb.append("L");
        sb.append(processQuotedSimpleName(yytext().substring(1)));
        yybegin(CLASS_DESCRIPTOR_REMAINING);
    }
    "L" {SimpleNameQuotedWithSpaces} {
        sb.append("L");
        sb.append(processQuotedSimpleNameWithSpaces(yytext().substring(1)));
        yybegin(CLASS_DESCRIPTOR_REMAINING);
    }
}

<CLASS_DESCRIPTOR_REMAINING> {
    "/" {SimpleNameRaw} {
        sb.append(yytext());
    }
    "/" {SimpleNameQuoted} {
        sb.append("/");
        sb.append(processQuotedSimpleName(yytext().substring(1)));
    }
    "/" {SimpleNameQuotedWithSpaces} {
        sb.append("/");
        sb.append(processQuotedSimpleNameWithSpaces(yytext().substring(1)));
    }

    ";" {
        sb.append(yytext());
        return endStateBasedToken(CLASS_DESCRIPTOR);
    }
}

<STRING> {
    "\""  { sb.append('"'); return endStateBasedToken(STRING_LITERAL); }

    [^\r\n\"\\]+ { sb.append(yytext()); }
    "\\b" { sb.append('\b'); }
    "\\t" { sb.append('\t'); }
    "\\n" { sb.append('\n'); }
    "\\f" { sb.append('\f'); }
    "\\r" { sb.append('\r'); }
    "\\'" { sb.append('\''); }
    "\\\"" { sb.append('"'); }
    "\\\\" { sb.append('\\'); }
    "\\u" {HexDigits} { sb.append((char)Integer.parseInt(yytext().substring(2,6), 16)); }

    "\\u" {FewerHexDigits} {
        sb.append(yytext());
        setStateBasedTokenError("Invalid \\u sequence. \\u must be followed by 4 hex digits");
    }

    "\\" [^btnfr'\"\\u] {
        sb.append(yytext());
        setStateBasedTokenError("Invalid escape sequence " + yytext());
    }

    [\r\n] { return invalidStateBasedToken("Unterminated string literal"); }
    <<EOF>> { return invalidStateBasedToken("Unterminated string literal"); }
}

<CHAR> {
    ' {
        sb.append('\'');
        if (sb.length() == 2) {
            return invalidStateBasedToken("Empty character literal");
        } else if (sb.length() > 3) {
            return invalidStateBasedToken("Character literal with multiple chars");
        }

        return endStateBasedToken(CHAR_LITERAL);
    }

    [^\r\n'\\]+ { sb.append(yytext()); }
    "\\b" { sb.append('\b'); }
    "\\t" { sb.append('\t'); }
    "\\n" { sb.append('\n'); }
    "\\f" { sb.append('\f'); }
    "\\r" { sb.append('\r'); }
    "\\'" { sb.append('\''); }
    "\\\"" { sb.append('"'); }
    "\\\\" { sb.append('\\'); }
    "\\u" {HexDigits} { sb.append((char)Integer.parseInt(yytext().substring(2,6), 16)); }

    "\\u" {HexDigit}* {
        sb.append(yytext());
        setStateBasedTokenError("Invalid \\u sequence. \\u must be followed by exactly 4 hex digits");
    }

    "\\" [^btnfr'\"\\u] {
        sb.append(yytext());
        setStateBasedTokenError("Invalid escape sequence " + yytext());
    }

    [\r\n] { return invalidStateBasedToken("Unterminated character literal"); }
    <<EOF>> { return invalidStateBasedToken("Unterminated character literal"); }
}

/*Misc*/
<YYINITIAL> {
    [vp] [0-9]+ { return newToken(REGISTER); }

    "build" | "runtime" | "system" {
        return newToken(ANNOTATION_VISIBILITY);
    }

    "public" | "private" | "protected" | "static" | "final" | "synchronized" | "bridge" | "varargs" | "native" |
    "abstract" | "strictfp" | "synthetic" | "constructor" | "declared-synchronized" | "interface" | "enum" |
    "annotation" | "volatile" | "transient" {
        return newToken(ACCESS_SPEC);
    }

    "whitelist" | "greylist" | "blacklist" | "greylist-max-o" | "greylist-max-p" | "greylist-max-q" |
    "core-platform-api" | "test-api" {
        return newToken(HIDDENAPI_RESTRICTION);
    }

    "no-error" | "generic-error" | "no-such-class" | "no-such-field" | "no-such-method" | "illegal-class-access" |
    "illegal-field-access" | "illegal-method-access" | "class-change-error" | "instantiation-error" {
        return newToken(VERIFICATION_ERROR_TYPE);
    }

    "inline@0x" {HexDigit}+ { return newToken(INLINE_INDEX); }
    "vtable@0x" {HexDigit}+ { return newToken(VTABLE_INDEX); }
    "field@0x" {HexDigit}+ { return newToken(FIELD_OFFSET); }

    "static-put" | "static-get" | "instance-put" | "instance-get" {
        return newToken(METHOD_HANDLE_TYPE_FIELD);
    }

    "invoke-instance" | "invoke-constructor" {
        return newToken(METHOD_HANDLE_TYPE_METHOD);
    }

    # [^\r\n]* { return newToken(LINE_COMMENT, true); }
}

/*Instructions*/
<YYINITIAL> {
    "goto" {
        return newToken(INSTRUCTION_FORMAT10t);
    }

    "return-void" | "nop" {
        return newToken(INSTRUCTION_FORMAT10x);
    }

    "return-void-barrier" | "return-void-no-barrier" {
        return newToken(INSTRUCTION_FORMAT10x_ODEX);
    }

    "const/4" {
        return newToken(INSTRUCTION_FORMAT11n);
    }

    "move-result" | "move-result-wide" | "move-result-object" | "move-exception" | "return" | "return-wide" |
    "return-object" | "monitor-enter" | "monitor-exit" | "throw" {
        return newToken(INSTRUCTION_FORMAT11x);
    }

    "move" | "move-wide" | "move-object" | "array-length" | "neg-int" | "not-int" | "neg-long" | "not-long" |
    "neg-float" | "neg-double" | "int-to-long" | "int-to-float" | "int-to-double" | "long-to-int" | "long-to-float" |
    "long-to-double" | "float-to-int" | "float-to-long" | "float-to-double" | "double-to-int" | "double-to-long" |
    "double-to-float" | "int-to-byte" | "int-to-char" | "int-to-short" {
        return newToken(INSTRUCTION_FORMAT12x_OR_ID);
    }

    "add-int/2addr" | "sub-int/2addr" | "mul-int/2addr" | "div-int/2addr" | "rem-int/2addr" | "and-int/2addr" |
    "or-int/2addr" | "xor-int/2addr" | "shl-int/2addr" | "shr-int/2addr" | "ushr-int/2addr" | "add-long/2addr" |
    "sub-long/2addr" | "mul-long/2addr" | "div-long/2addr" | "rem-long/2addr" | "and-long/2addr" | "or-long/2addr" |
    "xor-long/2addr" | "shl-long/2addr" | "shr-long/2addr" | "ushr-long/2addr" | "add-float/2addr" |
    "sub-float/2addr" | "mul-float/2addr" | "div-float/2addr" | "rem-float/2addr" | "add-double/2addr" |
    "sub-double/2addr" | "mul-double/2addr" | "div-double/2addr" | "rem-double/2addr" {
        return newToken(INSTRUCTION_FORMAT12x);
    }

    "throw-verification-error" {
        return newToken(INSTRUCTION_FORMAT20bc);
    }

    "goto/16" {
        return newToken(INSTRUCTION_FORMAT20t);
    }

    "sget" | "sget-wide" | "sget-object" | "sget-boolean" | "sget-byte" | "sget-char" | "sget-short" | "sput" |
    "sput-wide" | "sput-object" | "sput-boolean" | "sput-byte" | "sput-char" | "sput-short" {
        return newToken(INSTRUCTION_FORMAT21c_FIELD);
    }

    "sget-volatile" | "sget-wide-volatile" | "sget-object-volatile" | "sput-volatile" | "sput-wide-volatile" |
    "sput-object-volatile" {
        return newToken(INSTRUCTION_FORMAT21c_FIELD_ODEX);
    }

    "const-string" {
        return newToken(INSTRUCTION_FORMAT21c_STRING);
    }

    "check-cast" | "new-instance" | "const-class" {
        return newToken(INSTRUCTION_FORMAT21c_TYPE);
    }

    "const-method-handle" {
        return newToken(INSTRUCTION_FORMAT21c_METHOD_HANDLE);
    }

    "const-method-type" {
        return newToken(INSTRUCTION_FORMAT21c_METHOD_TYPE);
    }

    "const/high16" {
        return newToken(INSTRUCTION_FORMAT21ih);
    }

    "const-wide/high16" {
        return newToken(INSTRUCTION_FORMAT21lh);
    }

    "const/16" | "const-wide/16" {
        return newToken(INSTRUCTION_FORMAT21s);
    }

    "if-eqz" | "if-nez" | "if-ltz" | "if-gez" | "if-gtz" | "if-lez" {
        return newToken(INSTRUCTION_FORMAT21t);
    }

    "add-int/lit8" | "rsub-int/lit8" | "mul-int/lit8" | "div-int/lit8" | "rem-int/lit8" | "and-int/lit8" |
    "or-int/lit8" | "xor-int/lit8" | "shl-int/lit8" | "shr-int/lit8" | "ushr-int/lit8" {
        return newToken(INSTRUCTION_FORMAT22b);
    }

    "iget" | "iget-wide" | "iget-object" | "iget-boolean" | "iget-byte" | "iget-char" | "iget-short" | "iput" |
    "iput-wide" | "iput-object" | "iput-boolean" | "iput-byte" | "iput-char" | "iput-short" {
        return newToken(INSTRUCTION_FORMAT22c_FIELD);
    }

    "iget-volatile" | "iget-wide-volatile" | "iget-object-volatile" | "iput-volatile" | "iput-wide-volatile" |
    "iput-object-volatile" {
        return newToken(INSTRUCTION_FORMAT22c_FIELD_ODEX);
    }

    "instance-of" | "new-array" {
        return newToken(INSTRUCTION_FORMAT22c_TYPE);
    }

    "iget-quick" | "iget-wide-quick" | "iget-object-quick" | "iput-quick" | "iput-wide-quick" | "iput-object-quick" |
    "iput-boolean-quick" | "iput-byte-quick" | "iput-char-quick" | "iput-short-quick" {
        return newToken(INSTRUCTION_FORMAT22cs_FIELD);
    }

    "rsub-int" {
        return newToken(INSTRUCTION_FORMAT22s_OR_ID);
    }

    "add-int/lit16" | "mul-int/lit16" | "div-int/lit16" | "rem-int/lit16" | "and-int/lit16" | "or-int/lit16" |
    "xor-int/lit16" {
        return newToken(INSTRUCTION_FORMAT22s);
    }

    "if-eq" | "if-ne" | "if-lt" | "if-ge" | "if-gt" | "if-le" {
        return newToken(INSTRUCTION_FORMAT22t);
    }

    "move/from16" | "move-wide/from16" | "move-object/from16" {
        return newToken(INSTRUCTION_FORMAT22x);
    }

    "cmpl-float" | "cmpg-float" | "cmpl-double" | "cmpg-double" | "cmp-long" | "aget" | "aget-wide" | "aget-object" |
    "aget-boolean" | "aget-byte" | "aget-char" | "aget-short" | "aput" | "aput-wide" | "aput-object" | "aput-boolean" |
    "aput-byte" | "aput-char" | "aput-short" | "add-int" | "sub-int" | "mul-int" | "div-int" | "rem-int" | "and-int" |
    "or-int" | "xor-int" | "shl-int" | "shr-int" | "ushr-int" | "add-long" | "sub-long" | "mul-long" | "div-long" |
    "rem-long" | "and-long" | "or-long" | "xor-long" | "shl-long" | "shr-long" | "ushr-long" | "add-float" |
    "sub-float" | "mul-float" | "div-float" | "rem-float" | "add-double" | "sub-double" | "mul-double" | "div-double" |
    "rem-double" {
        return newToken(INSTRUCTION_FORMAT23x);
    }

    "goto/32" {
        return newToken(INSTRUCTION_FORMAT30t);
    }

    "const-string/jumbo" {
        return newToken(INSTRUCTION_FORMAT31c);
    }

    "const" {
        return newToken(INSTRUCTION_FORMAT31i_OR_ID);
    }

    "const-wide/32" {
        return newToken(INSTRUCTION_FORMAT31i);
    }

    "fill-array-data" | "packed-switch" | "sparse-switch" {
        return newToken(INSTRUCTION_FORMAT31t);
    }

    "move/16" | "move-wide/16" | "move-object/16" {
        return newToken(INSTRUCTION_FORMAT32x);
    }

    "invoke-custom" {
        return newToken(INSTRUCTION_FORMAT35c_CALL_SITE);
    }

    "invoke-virtual" | "invoke-super" {
        return newToken(INSTRUCTION_FORMAT35c_METHOD);
    }
    
    "invoke-direct" | "invoke-static" | "invoke-interface" {
        return newToken(INSTRUCTION_FORMAT35c_METHOD_OR_METHOD_HANDLE_TYPE);
    }

    "invoke-direct-empty" {
        return newToken(INSTRUCTION_FORMAT35c_METHOD_ODEX);
    }

    "filled-new-array" {
        return newToken(INSTRUCTION_FORMAT35c_TYPE);
    }

    "execute-inline" {
        return newToken(INSTRUCTION_FORMAT35mi_METHOD);
    }

    "invoke-virtual-quick" | "invoke-super-quick" {
        return newToken(INSTRUCTION_FORMAT35ms_METHOD);
    }

    "invoke-custom/range" {
        return newToken(INSTRUCTION_FORMAT3rc_CALL_SITE);
    }

    "invoke-virtual/range" | "invoke-super/range" | "invoke-direct/range" | "invoke-static/range" |
    "invoke-interface/range" {
        return newToken(INSTRUCTION_FORMAT3rc_METHOD);
    }

    "invoke-object-init/range" {
        return newToken(INSTRUCTION_FORMAT3rc_METHOD_ODEX);
    }

    "filled-new-array/range" {
        return newToken(INSTRUCTION_FORMAT3rc_TYPE);
    }

    "execute-inline/range" {
        return newToken(INSTRUCTION_FORMAT3rmi_METHOD);
    }

    "invoke-virtual-quick/range" | "invoke-super-quick/range" {
        return newToken(INSTRUCTION_FORMAT3rms_METHOD);
    }

    "invoke-polymorphic" {
        return newToken(INSTRUCTION_FORMAT45cc_METHOD);
    }

    "invoke-polymorphic/range" {
        return newToken(INSTRUCTION_FORMAT4rcc_METHOD);
    }

    "const-wide" {
        return newToken(INSTRUCTION_FORMAT51l);
    }
}

<ARRAY_DESCRIPTOR> {
    {PrimitiveType} { yybegin(YYINITIAL); return newToken(PRIMITIVE_TYPE); }
    {ClassDescriptor} {
        yypushback(yylength());
        beginStateBasedToken(CLASS_DESCRIPTOR_BEGINNING);
        sb.append(yytext());
    }
    [^] { yypushback(1); yybegin(YYINITIAL); }
    <<EOF>> { yybegin(YYINITIAL); }
}

/*Types*/
<YYINITIAL> {
    {PrimitiveType} { return newToken(PRIMITIVE_TYPE); }
    V { return newToken(VOID_TYPE); }
    {ClassDescriptor} {
        yypushback(yylength());
        beginStateBasedToken(CLASS_DESCRIPTOR_BEGINNING);
    }

    // we have to drop into a separate state so that we don't parse something like
    // "[I->" as "[" followed by "I-" as a SIMPLE_NAME
    {ArrayPrefix} {
      yybegin(ARRAY_DESCRIPTOR);
      return newToken(ARRAY_TYPE_PREFIX);
    }

    {PrimitiveType} {PrimitiveType}+ {
        // go back and re-lex it as a PARAM_LIST_OR_ID
        yypushback(yylength());
        yybegin(PARAM_LIST_OR_ID);
    }

    {Type} {Type}+ {
        // go back and re-lex it as a PARAM_LIST
        yypushback(yylength());
        yybegin(PARAM_LIST);
    }

    {SimpleNameRaw} { return newToken(SIMPLE_NAME, yytext()); }
    {SimpleNameQuoted} { return newToken(SIMPLE_NAME, processQuotedSimpleName(yytext())); }
    {SimpleNameQuotedWithSpaces} { return newToken(SIMPLE_NAME, processQuotedSimpleNameWithSpaces(yytext())); }
    "<" {SimpleNameRaw} ">" { return newToken(MEMBER_NAME); }
}

/*Symbols/Whitespace/EOF*/
<YYINITIAL> {
    ".." { return newToken(DOTDOT); }
    "->" { return newToken(ARROW); }
    "=" { return newToken(EQUAL); }
    ":" { return newToken(COLON); }
    "," { return newToken(COMMA); }
    "{" { return newToken(OPEN_BRACE); }
    "}" { return newToken(CLOSE_BRACE); }
    "(" { return newToken(OPEN_PAREN); }
    ")" { return newToken(CLOSE_PAREN); }
    "@" { return newToken(AT); }
    [\r\n\t ]+ { return newToken(WHITE_SPACE, true); }
    <<EOF>> { return newToken(EOF); }
}

/*catch all*/
<YYINITIAL> {
    "." { return invalidToken("Invalid directive"); }
    "." [a-zA-z\-_] { return invalidToken("Invalid directive"); }
    "." [a-zA-z\-_] [a-zA-z0-9\-_]* { return invalidToken("Invalid directive"); }
    [^] { return invalidToken("Invalid text"); }
}
