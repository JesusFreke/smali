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

tree grammar smaliTreeWalker;

options {
  tokenVocab=smaliParser;
  ASTLabelType=CommonTree;
}

@header {
package org.jf.smali;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.*;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.VerificationError;
import org.jf.dexlib2.dexbacked.raw.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.debug.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.immutable.value.*;
import org.jf.dexlib2.util.MethodUtil;


}

@members {
  public String classType;
  private boolean verboseErrors = false;

  public void setVerboseErrors(boolean verboseErrors) {
    this.verboseErrors = verboseErrors;
  }

  private byte parseRegister_nibble(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;

    //register should be in the format "v12"
    int val = Byte.parseByte(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<4) {
      throw new SemanticException(input, "The maximum allowed register in this context is list of registers is v15");
    }
    //the parser wouldn't have accepted a negative register, i.e. v-1, so we don't have to check for val<0;
    return (byte)val;
  }

  //return a short, because java's byte is signed
  private short parseRegister_byte(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;
    //register should be in the format "v123"
    int val = Short.parseShort(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<8) {
      throw new SemanticException(input, "The maximum allowed register in this context is v255");
    }
    return (short)val;
  }

  //return an int because java's short is signed
  private int parseRegister_short(String register)
      throws SemanticException {
    int totalMethodRegisters = method_stack.peek().totalMethodRegisters;
    int methodParameterRegisters = method_stack.peek().methodParameterRegisters;
    //register should be in the format "v12345"
    int val = Integer.parseInt(register.substring(1));
    if (register.charAt(0) == 'p') {
      val = totalMethodRegisters - methodParameterRegisters + val;
    }
    if (val >= 2<<16) {
      throw new SemanticException(input, "The maximum allowed register in this context is v65535");
    }
    //the parser wouldn't accept a negative register, i.e. v-1, so we don't have to check for val<0;
    return val;
  }

  public String getErrorMessage(RecognitionException e, String[] tokenNames) {
    if ( e instanceof SemanticException ) {
      return e.getMessage();
    } else {
      return super.getErrorMessage(e, tokenNames);
    }
  }

  public String getErrorHeader(RecognitionException e) {
    return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
  }
}

smali_file returns[ClassDef classDef]
  : ^(I_CLASS_DEF header methods fields annotations)
  {
    $classDef = new ImmutableClassDef($header.classType, $header.accessFlags, $header.superType,
            $header.implementsList, $header.sourceSpec, $annotations.annotations, $fields.fields, $methods.methods);
  };
  catch [Exception ex] {
    if (verboseErrors) {
      ex.printStackTrace(System.err);
    }
    reportError(new SemanticException(input, ex));
  }


header returns[String classType, int accessFlags, String superType, List<String> implementsList, String sourceSpec]
: class_spec super_spec? implements_list source_spec
  {
    classType = $class_spec.type;
    $classType = classType;
    $accessFlags = $class_spec.accessFlags;
    $superType = $super_spec.type;
    $implementsList = $implements_list.implementsList;
    $sourceSpec = $source_spec.source;
  };


class_spec returns[String type, int accessFlags]
  : CLASS_DESCRIPTOR access_list
  {
    $type = $CLASS_DESCRIPTOR.text;
    $accessFlags = $access_list.value;
  };

super_spec returns[String type]
  : ^(I_SUPER CLASS_DESCRIPTOR)
  {
    $type = $CLASS_DESCRIPTOR.text;
  };


implements_spec returns[String type]
  : ^(I_IMPLEMENTS CLASS_DESCRIPTOR)
  {
    $type = $CLASS_DESCRIPTOR.text;
  };

implements_list returns[List<String> implementsList]
@init { List<String> typeList; }
  : {typeList = Lists.newArrayList();}
    (implements_spec {typeList.add($implements_spec.type);} )*
  {
    if (typeList.size() > 0) {
      $implementsList = typeList;
    } else {
      $implementsList = null;
    }
  };

source_spec returns[String source]
  : {$source = null;}
    ^(I_SOURCE string_literal {$source = $string_literal.value;})
  | /*epsilon*/;

access_list returns [int value]
  @init
  {
    $value = 0;
  }
  : ^(I_ACCESS_LIST
      (
        ACCESS_SPEC
        {
          $value |= AccessFlags.getAccessFlag($ACCESS_SPEC.getText()).getValue();
        }
      )*);


fields returns[List<Field> fields]
  @init {$fields = Lists.newArrayList();}
  : ^(I_FIELDS
      (field
      {
        $fields.add($field.field);
      })*);

methods returns[List<Method> methods]
  @init {$methods = Lists.newArrayList();}
  : ^(I_METHODS
      (method
      {
        $methods.add($method.ret);
      })*);

field returns [Field field]
  :^(I_FIELD SIMPLE_NAME access_list ^(I_FIELD_TYPE nonvoid_type_descriptor) field_initial_value annotations?)
  {
    int accessFlags = $access_list.value;


    if (!AccessFlags.STATIC.isSet(accessFlags) && $field_initial_value.encodedValue != null) {
        throw new SemanticException(input, "Initial field values can only be specified for static fields.");
    }

    $field = new ImmutableField(classType, $SIMPLE_NAME.text, $nonvoid_type_descriptor.type, $access_list.value,
            $field_initial_value.encodedValue, $annotations.annotations);
  };


field_initial_value returns[EncodedValue encodedValue]
  : ^(I_FIELD_INITIAL_VALUE literal) {$encodedValue = $literal.encodedValue;}
  | /*epsilon*/;

literal returns[EncodedValue encodedValue]
  : integer_literal { $encodedValue = new ImmutableIntEncodedValue($integer_literal.value); }
  | long_literal { $encodedValue = new ImmutableLongEncodedValue($long_literal.value); }
  | short_literal { $encodedValue = new ImmutableShortEncodedValue($short_literal.value); }
  | byte_literal { $encodedValue = new ImmutableByteEncodedValue($byte_literal.value); }
  | float_literal { $encodedValue = new ImmutableFloatEncodedValue($float_literal.value); }
  | double_literal { $encodedValue = new ImmutableDoubleEncodedValue($double_literal.value); }
  | char_literal { $encodedValue = new ImmutableCharEncodedValue($char_literal.value); }
  | string_literal { $encodedValue = new ImmutableStringEncodedValue($string_literal.value); }
  | bool_literal { $encodedValue = ImmutableBooleanEncodedValue.forBoolean($bool_literal.value); }
  | NULL_LITERAL { $encodedValue = ImmutableNullEncodedValue.INSTANCE; }
  | type_descriptor { $encodedValue = new ImmutableTypeEncodedValue($type_descriptor.type); }
  | array_literal { $encodedValue = new ImmutableArrayEncodedValue($array_literal.elements); }
  | subannotation { $encodedValue = new ImmutableAnnotationEncodedValue($subannotation.annotationType, $subannotation.elements); }
  | field_literal { $encodedValue = new ImmutableFieldEncodedValue($field_literal.value); }
  | method_literal { $encodedValue = new ImmutableMethodEncodedValue($method_literal.value); }
  | enum_literal { $encodedValue = new ImmutableEnumEncodedValue($enum_literal.value); };


//everything but string
fixed_size_literal returns[byte[\] value]
  : integer_literal { $value = LiteralTools.intToBytes($integer_literal.value); }
  | long_literal { $value = LiteralTools.longToBytes($long_literal.value); }
  | short_literal { $value = LiteralTools.shortToBytes($short_literal.value); }
  | byte_literal { $value = new byte[] { $byte_literal.value }; }
  | float_literal { $value = LiteralTools.floatToBytes($float_literal.value); }
  | double_literal { $value = LiteralTools.doubleToBytes($double_literal.value); }
  | char_literal { $value = LiteralTools.charToBytes($char_literal.value); }
  | bool_literal { $value = LiteralTools.boolToBytes($bool_literal.value); };

//everything but string
fixed_64bit_literal returns[long value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { $value = $long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | double_literal { $value = Double.doubleToRawLongBits($double_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

//everything but string and double
//long is allowed, but it must fit into an int
fixed_32bit_literal returns[int value]
  : integer_literal { $value = $integer_literal.value; }
  | long_literal { LiteralTools.checkInt($long_literal.value); $value = (int)$long_literal.value; }
  | short_literal { $value = $short_literal.value; }
  | byte_literal { $value = $byte_literal.value; }
  | float_literal { $value = Float.floatToRawIntBits($float_literal.value); }
  | char_literal { $value = $char_literal.value; }
  | bool_literal { $value = $bool_literal.value?1:0; };

array_elements returns[List<byte[\]> elements]
  : {$elements = Lists.newArrayList();}
    ^(I_ARRAY_ELEMENTS
      (fixed_size_literal
      {
        $elements.add($fixed_size_literal.value);
      })*);

packed_switch_elements[int baseAddress, int firstKey] returns[List<SwitchElement> elements]
  @init {$elements = Lists.newArrayList();}
  :
    ^(I_PACKED_SWITCH_ELEMENTS

      (offset_or_label
      {
        $elements.add(new ImmutableSwitchElement(firstKey++,
                ($method::currentAddress + $offset_or_label.offsetValue) - $baseAddress));
      })*
    );

sparse_switch_elements[int baseAddress] returns[List<SwitchElement> elements]
  @init {$elements = Lists.newArrayList();}
  :
    ^(I_SPARSE_SWITCH_ELEMENTS
       (fixed_32bit_literal offset_or_label
       {
         $elements.add(new ImmutableSwitchElement($fixed_32bit_literal.value,
                       ($method::currentAddress + $offset_or_label.offsetValue) - $baseAddress));

       })*
    );

method returns[Method ret]
  scope
  {
    HashMap<String, Integer> labels;
    int currentAddress;
    HashMap<Integer, Integer> packedSwitchDeclarations;
    HashMap<Integer, Integer> sparseSwitchDeclarations;
    int totalMethodRegisters;
    int methodParameterRegisters;
  }
  @init
  {
    $method::totalMethodRegisters = 0;
    $method::methodParameterRegisters = 0;
    int accessFlags = 0;
    boolean isStatic = false;
    $method::labels = new HashMap<String, Integer>();
    $method::currentAddress = 0;
    $method::packedSwitchDeclarations = new HashMap<Integer, Integer>();
    $method::sparseSwitchDeclarations = new HashMap<Integer, Integer>();
  }
  :
    ^(I_METHOD
      method_name_and_prototype
      access_list
      {
        accessFlags = $access_list.value;
        isStatic = AccessFlags.STATIC.isSet(accessFlags);
        $method::methodParameterRegisters = MethodUtil.getParameterRegisterCount($method_name_and_prototype.parameters, isStatic);
      }
      (registers_directive
       {
         if ($registers_directive.isLocalsDirective) {
           $method::totalMethodRegisters = $registers_directive.registers + $method::methodParameterRegisters;
         } else {
           $method::totalMethodRegisters = $registers_directive.registers;
         }
       }
      )?
      labels
      packed_switch_declarations
      sparse_switch_declarations
      statements
      catches
      parameters[$method_name_and_prototype.parameters]
      ordered_debug_directives
      annotations
    )
  {
    List<TryBlock> tryBlocks = $catches.tryBlocks;
    List<DebugItem> debugItems = $ordered_debug_directives.debugItems;

    MethodImplementation methodImplementation = null;

    boolean isAbstract = false;
    boolean isNative = false;

    if ((accessFlags & AccessFlags.ABSTRACT.getValue()) != 0) {
      isAbstract = true;
    } else if ((accessFlags & AccessFlags.NATIVE.getValue()) != 0) {
      isNative = true;
    }

    if ($statements.instructions.size() == 0) {
      if (!isAbstract && !isNative) {
        throw new SemanticException(input, $I_METHOD, "A non-abstract/non-native method must have at least 1 instruction");
      }

      String methodType;
      if (isAbstract) {
        methodType = "an abstract";
      } else {
        methodType = "a native";
      }

      if ($registers_directive.start != null) {
        if ($registers_directive.isLocalsDirective) {
          throw new SemanticException(input, $registers_directive.start, "A .locals directive is not valid in \%s method", methodType);
        } else {
          throw new SemanticException(input, $registers_directive.start, "A .registers directive is not valid in \%s method", methodType);
        }
      }

      if ($method::labels.size() > 0) {
        throw new SemanticException(input, $I_METHOD, "Labels cannot be present in \%s method", methodType);
      }

      if ((tryBlocks != null && tryBlocks.size() > 0)) {
        throw new SemanticException(input, $I_METHOD, "try/catch blocks cannot be present in \%s method", methodType);
      }

      if (debugItems != null && debugItems.size() > 0) {
        throw new SemanticException(input, $I_METHOD, "debug directives cannot be present in \%s method", methodType);
      }
    } else {
      if (isAbstract) {
        throw new SemanticException(input, $I_METHOD, "An abstract method cannot have any instructions");
      }
      if (isNative) {
        throw new SemanticException(input, $I_METHOD, "A native method cannot have any instructions");
      }

      if ($registers_directive.start == null) {
        throw new SemanticException(input, $I_METHOD, "A .registers or .locals directive must be present for a non-abstract/non-final method");
      }

      if ($method::totalMethodRegisters < $method::methodParameterRegisters) {
        throw new SemanticException(input, $registers_directive.start, "This method requires at least " +
                Integer.toString($method::methodParameterRegisters) +
                " registers, for the method parameters");
      }

      methodImplementation = new ImmutableMethodImplementation(
              $method::totalMethodRegisters,
              $statements.instructions,
              tryBlocks,
              debugItems);
    }

    $ret = new ImmutableMethod(
            classType,
            $method_name_and_prototype.name,
            $parameters.parameters,
            $method_name_and_prototype.returnType,
            accessFlags,
            $annotations.annotations,
            methodImplementation);
  };

method_prototype returns[List<String> parameters, String returnType]
  : ^(I_METHOD_PROTOTYPE ^(I_METHOD_RETURN_TYPE type_descriptor) field_type_list)
  {
    $returnType = $type_descriptor.type;
    $parameters = $field_type_list.types;
  };

method_name_and_prototype returns[String name, List<String> parameters, String returnType]
  : SIMPLE_NAME method_prototype
  {
    $name = $SIMPLE_NAME.text;
    $parameters = $method_prototype.parameters;
    $returnType = $method_prototype.returnType;
  };

field_type_list returns[List<String> types]
  @init
  {
    $types = Lists.newArrayList();
  }
  : (
      nonvoid_type_descriptor
      {
        $types.add($nonvoid_type_descriptor.type);
      }
    )*;


fully_qualified_method returns[ImmutableMethodReference methodReference]
  : reference_type_descriptor SIMPLE_NAME method_prototype
  {
    $methodReference = new ImmutableMethodReference($reference_type_descriptor.type, $SIMPLE_NAME.text,
             $method_prototype.parameters, $method_prototype.returnType);
  };

fully_qualified_field returns[ImmutableFieldReference fieldReference]
  : reference_type_descriptor SIMPLE_NAME nonvoid_type_descriptor
  {
    $fieldReference = new ImmutableFieldReference($reference_type_descriptor.type, $SIMPLE_NAME.text,
            $nonvoid_type_descriptor.type);
  };

registers_directive returns[boolean isLocalsDirective, int registers]
  : {$registers = 0;}
    ^(( I_REGISTERS {$isLocalsDirective = false;}
      | I_LOCALS {$isLocalsDirective = true;}
      )
      short_integral_literal {$registers = $short_integral_literal.value;}
     );

labels
  : ^(I_LABELS label_def*);

label_def
  : ^(I_LABEL SIMPLE_NAME address)
    {
      if ($method::labels.containsKey($SIMPLE_NAME.text)) {
        throw new SemanticException(input, $I_LABEL, "Label " + $SIMPLE_NAME.text + " has multiple defintions.");
      }

      $method::labels.put($SIMPLE_NAME.text, $address.address);
    };

packed_switch_declarations
  : ^(I_PACKED_SWITCH_DECLARATIONS packed_switch_declaration*);
packed_switch_declaration
  : ^(I_PACKED_SWITCH_DECLARATION address offset_or_label_absolute[$address.address])
    {
      int switchDataAddress = $offset_or_label_absolute.address;
      if ((switchDataAddress \% 2) != 0) {
        switchDataAddress++;
      }
      if (!$method::packedSwitchDeclarations.containsKey(switchDataAddress)) {
        $method::packedSwitchDeclarations.put(switchDataAddress, $address.address);
      }
    };

sparse_switch_declarations
  : ^(I_SPARSE_SWITCH_DECLARATIONS sparse_switch_declaration*);
sparse_switch_declaration
  : ^(I_SPARSE_SWITCH_DECLARATION address offset_or_label_absolute[$address.address])
    {
      int switchDataAddress = $offset_or_label_absolute.address;
      if ((switchDataAddress \% 2) != 0) {
        switchDataAddress++;
      }
      if (!$method::sparseSwitchDeclarations.containsKey(switchDataAddress)) {
        $method::sparseSwitchDeclarations.put(switchDataAddress, $address.address);
      }
    };

catches returns[List<TryBlock> tryBlocks]
  @init {tryBlocks = Lists.newArrayList();}
  : ^(I_CATCHES (catch_directive { tryBlocks.add($catch_directive.tryBlock); })*
                (catchall_directive { tryBlocks.add($catchall_directive.tryBlock); })*);

catch_directive returns[TryBlock tryBlock]
  : ^(I_CATCH address nonvoid_type_descriptor from=offset_or_label_absolute[$address.address] to=offset_or_label_absolute[$address.address]
        using=offset_or_label_absolute[$address.address])
    {
      String type = $nonvoid_type_descriptor.type;
      int startAddress = $from.address;
      int endAddress = $to.address;
      int handlerAddress = $using.address;

      // We always create try blocks with a single exception handler. These will be merged appropriately when written
      // to a dex file
      $tryBlock = new ImmutableTryBlock(startAddress, endAddress-startAddress,
              ImmutableList.of(new ImmutableExceptionHandler(type, handlerAddress)));
    };

catchall_directive returns[TryBlock tryBlock]
  : ^(I_CATCHALL address from=offset_or_label_absolute[$address.address] to=offset_or_label_absolute[$address.address]
        using=offset_or_label_absolute[$address.address])
    {
      int startAddress = $from.address;
      int endAddress = $to.address;
      int handlerAddress = $using.address;

      // We always create try blocks with a single exception handler. These will be merged appropriately when written
      // to a dex file
      $tryBlock = new ImmutableTryBlock(startAddress, endAddress-startAddress,
              ImmutableList.of(new ImmutableExceptionHandler(null, handlerAddress)));
    };

address returns[int address]
  : I_ADDRESS
    {
      $address = Integer.parseInt($I_ADDRESS.text);
    };

parameters[List<String> parameterTypes] returns[List<MethodParameter> parameters]
  @init
  {
    Iterator<String> parameterIter = parameterTypes.iterator();
    $parameters = Lists.newArrayList();
  }
  : ^(I_PARAMETERS (parameter[parameterIter] { $parameters.add($parameter.parameter); })*)
  {
    String paramType;
    while (parameterIter.hasNext()) {
      paramType = parameterIter.next();
      $parameters.add(new ImmutableMethodParameter(paramType, null, null));
    }
  };

parameter[Iterator<String> parameterTypes] returns[MethodParameter parameter]
  : ^(I_PARAMETER string_literal? annotations
        {
            if (!$parameterTypes.hasNext()) {
                throw new SemanticException(input, $I_PARAMETER, "Too many .parameter directives specified.");
            }
            String type = $parameterTypes.next();
            String name = $string_literal.value;
            Set<Annotation> annotations = $annotations.annotations;

            $parameter = new ImmutableMethodParameter(type, annotations, name);
        }
    );

ordered_debug_directives returns[List<DebugItem> debugItems]
  @init {debugItems = Lists.newArrayList();}
  : ^(I_ORDERED_DEBUG_DIRECTIVES
       ( line { $debugItems.add($line.debugItem); }
       | local { $debugItems.add($local.debugItem); }
       | end_local { $debugItems.add($end_local.debugItem); }
       | restart_local { $debugItems.add($restart_local.debugItem); }
       | prologue { $debugItems.add($prologue.debugItem); }
       | epilogue { $debugItems.add($epilogue.debugItem); }
       | source { $debugItems.add($source.debugItem); }
       )*
     );

line returns[DebugItem debugItem]
  : ^(I_LINE integral_literal address)
    {
        $debugItem = new ImmutableLineNumber($address.address, $integral_literal.value);
    };

local returns[DebugItem debugItem]
  : ^(I_LOCAL REGISTER SIMPLE_NAME nonvoid_type_descriptor string_literal? address)
    {
      int registerNumber = parseRegister_short($REGISTER.text);

      $debugItem = new ImmutableStartLocal($address.address, registerNumber, $SIMPLE_NAME.text,
              $nonvoid_type_descriptor.type, $string_literal.value);
    };

end_local returns[DebugItem debugItem]
  : ^(I_END_LOCAL REGISTER address)
    {
      int registerNumber = parseRegister_short($REGISTER.text);

      $debugItem = new ImmutableEndLocal($address.address, registerNumber);
    };

restart_local returns[DebugItem debugItem]
  : ^(I_RESTART_LOCAL REGISTER address)
    {
      int registerNumber = parseRegister_short($REGISTER.text);

      $debugItem = new ImmutableRestartLocal($address.address, registerNumber);
    };

prologue returns[DebugItem debugItem]
  : ^(I_PROLOGUE address)
    {
      $debugItem = new ImmutablePrologueEnd($address.address);
    };

epilogue returns[DebugItem debugItem]
  : ^(I_EPILOGUE address)
    {
      $debugItem = new ImmutableEpilogueBegin($address.address);
    };

source returns[DebugItem debugItem]
  : ^(I_SOURCE string_literal address)
    {
      $debugItem = new ImmutableSetSourceFile($address.address, $string_literal.value);
    };

statements returns[List<Instruction> instructions, int maxOutRegisters]
  @init
  {
    $instructions = Lists.newArrayList();
    $maxOutRegisters = 0;
  }
  : ^(I_STATEMENTS (instruction[$instructions]
        {
          $method::currentAddress += $instructions.get($instructions.size() - 1).getCodeUnits();
          if ($maxOutRegisters < $instruction.outRegisters) {
            $maxOutRegisters = $instruction.outRegisters;
          }
        })*);

label_ref returns[int labelAddress]
  : SIMPLE_NAME
    {
      Integer labelAdd = $method::labels.get($SIMPLE_NAME.text);

      if (labelAdd == null) {
        throw new SemanticException(input, $SIMPLE_NAME, "Label \"" + $SIMPLE_NAME.text + "\" is not defined.");
      }

      $labelAddress = labelAdd;
    };

offset returns[int offsetValue]
  : OFFSET
    {
      String offsetText = $OFFSET.text;
      if (offsetText.charAt(0) == '+') {
        offsetText = offsetText.substring(1);
      }
      $offsetValue = LiteralTools.parseInt(offsetText);
    };

offset_or_label_absolute[int baseAddress] returns[int address]
  : offset {$address = $offset.offsetValue + $baseAddress;}
  | label_ref {$address = $label_ref.labelAddress;};

offset_or_label returns[int offsetValue]
  : offset {$offsetValue = $offset.offsetValue;}
  | label_ref {$offsetValue = $label_ref.labelAddress-$method::currentAddress;};


register_list returns[byte[\] registers, byte registerCount]
  @init
  {
    $registers = new byte[5];
    $registerCount = 0;
  }
  : ^(I_REGISTER_LIST
      (REGISTER
      {
        if ($registerCount == 5) {
          throw new SemanticException(input, $I_REGISTER_LIST, "A list of registers can only have a maximum of 5 " +
                  "registers. Use the <op>/range alternate opcode instead.");
        }
        $registers[$registerCount++] = parseRegister_nibble($REGISTER.text);
      })*);

register_range returns[int startRegister, int endRegister]
  : ^(I_REGISTER_RANGE (startReg=REGISTER endReg=REGISTER?)?)
    {
        if ($startReg == null) {
            $startRegister = 0;
            $endRegister = -1;
        } else {
                $startRegister  = parseRegister_short($startReg.text);
                if ($endReg == null) {
                    $endRegister = $startRegister;
                } else {
                    $endRegister = parseRegister_short($endReg.text);
                }

                int registerCount = $endRegister-$startRegister+1;
                if (registerCount < 1) {
                    throw new SemanticException(input, $I_REGISTER_RANGE, "A register range must have the lower register listed first");
                }
            }
    };

verification_error_reference returns[ImmutableReference reference]
  : CLASS_DESCRIPTOR
  {
    $reference = new ImmutableTypeReference($CLASS_DESCRIPTOR.text);
  }
  | fully_qualified_field
  {
    $reference = $fully_qualified_field.fieldReference;
  }
  | fully_qualified_method
  {
    $reference = $fully_qualified_method.methodReference;
  };

verification_error_type returns[int verificationError]
  : VERIFICATION_ERROR_TYPE
  {
    $verificationError = VerificationError.getVerificationError($VERIFICATION_ERROR_TYPE.text);
  };

instruction[List<Instruction> instructions] returns[int outRegisters]
  : insn_format10t[$instructions] { $outRegisters = $insn_format10t.outRegisters; }
  | insn_format10x[$instructions] { $outRegisters = $insn_format10x.outRegisters; }
  | insn_format11n[$instructions] { $outRegisters = $insn_format11n.outRegisters; }
  | insn_format11x[$instructions] { $outRegisters = $insn_format11x.outRegisters; }
  | insn_format12x[$instructions] { $outRegisters = $insn_format12x.outRegisters; }
  | insn_format20bc[$instructions] { $outRegisters = $insn_format20bc.outRegisters; }
  | insn_format20t[$instructions] { $outRegisters = $insn_format20t.outRegisters; }
  | insn_format21c_field[$instructions] { $outRegisters = $insn_format21c_field.outRegisters; }
  | insn_format21c_string[$instructions] { $outRegisters = $insn_format21c_string.outRegisters; }
  | insn_format21c_type[$instructions] { $outRegisters = $insn_format21c_type.outRegisters; }
  | insn_format21ih[$instructions] { $outRegisters = $insn_format21ih.outRegisters; }
  | insn_format21lh[$instructions] { $outRegisters = $insn_format21lh.outRegisters; }
  | insn_format21s[$instructions] { $outRegisters = $insn_format21s.outRegisters; }
  | insn_format21t[$instructions] { $outRegisters = $insn_format21t.outRegisters; }
  | insn_format22b[$instructions] { $outRegisters = $insn_format22b.outRegisters; }
  | insn_format22c_field[$instructions] { $outRegisters = $insn_format22c_field.outRegisters; }
  | insn_format22c_type[$instructions] { $outRegisters = $insn_format22c_type.outRegisters; }
  | insn_format22s[$instructions] { $outRegisters = $insn_format22s.outRegisters; }
  | insn_format22t[$instructions] { $outRegisters = $insn_format22t.outRegisters; }
  | insn_format22x[$instructions] { $outRegisters = $insn_format22x.outRegisters; }
  | insn_format23x[$instructions] { $outRegisters = $insn_format23x.outRegisters; }
  | insn_format30t[$instructions] { $outRegisters = $insn_format30t.outRegisters; }
  | insn_format31c[$instructions] { $outRegisters = $insn_format31c.outRegisters; }
  | insn_format31i[$instructions] { $outRegisters = $insn_format31i.outRegisters; }
  | insn_format31t[$instructions] { $outRegisters = $insn_format31t.outRegisters; }
  | insn_format32x[$instructions] { $outRegisters = $insn_format32x.outRegisters; }
  | insn_format35c_method[$instructions] { $outRegisters = $insn_format35c_method.outRegisters; }
  | insn_format35c_type[$instructions] { $outRegisters = $insn_format35c_type.outRegisters; }
  | insn_format3rc_method[$instructions] { $outRegisters = $insn_format3rc_method.outRegisters; }
  | insn_format3rc_type[$instructions] { $outRegisters = $insn_format3rc_type.outRegisters; }
  | insn_format51l_type[$instructions] { $outRegisters = $insn_format51l_type.outRegisters; }
  | insn_array_data_directive[$instructions] { $outRegisters = $insn_array_data_directive.outRegisters; }
  | insn_packed_switch_directive[$instructions] { $outRegisters = $insn_packed_switch_directive.outRegisters; }
  | insn_sparse_switch_directive[$instructions] { $outRegisters = $insn_sparse_switch_directive.outRegisters; };
    catch [Exception ex] {
      reportError(new SemanticException(input, $start, ex.getMessage()));
      recover(input, null);
    }


insn_format10t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto endloop:
    {$outRegisters = 0;}
    ^(I_STATEMENT_FORMAT10t INSTRUCTION_FORMAT10t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new ImmutableInstruction10t(opcode, addressOffset));
    };

insn_format10x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. return
    ^(I_STATEMENT_FORMAT10x INSTRUCTION_FORMAT10x)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT10x.text);
      $instructions.add(new ImmutableInstruction10x(opcode));
    };

insn_format11n[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/4 v0, 5
    ^(I_STATEMENT_FORMAT11n INSTRUCTION_FORMAT11n REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11n.text);
      byte regA = parseRegister_nibble($REGISTER.text);

      short litB = $short_integral_literal.value;
      LiteralTools.checkNibble(litB);

      $instructions.add(new ImmutableInstruction11n(opcode, regA, litB));
    };

insn_format11x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move-result-object v1
    ^(I_STATEMENT_FORMAT11x INSTRUCTION_FORMAT11x REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT11x.text);
      short regA = parseRegister_byte($REGISTER.text);

      $instructions.add(new ImmutableInstruction11x(opcode, regA));
    };

insn_format12x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move v1 v2
    ^(I_STATEMENT_FORMAT12x INSTRUCTION_FORMAT12x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT12x.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      $instructions.add(new ImmutableInstruction12x(opcode, regA, regB));
    };

insn_format20bc[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. throw-verification-error generic-error, Lsome/class;
    ^(I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc verification_error_type verification_error_reference)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT20bc.text);

      int verificationError = $verification_error_type.verificationError;
      ImmutableReference referencedItem = $verification_error_reference.reference;

      $instructions.add(new ImmutableInstruction20bc(opcode, verificationError, referencedItem));
    };

insn_format20t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto/16 endloop:
    ^(I_STATEMENT_FORMAT20t INSTRUCTION_FORMAT20t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT20t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new ImmutableInstruction20t(opcode, addressOffset));
    };

insn_format21c_field[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. sget_object v0, java/lang/System/out LJava/io/PrintStream;
    ^(I_STATEMENT_FORMAT21c_FIELD inst=(INSTRUCTION_FORMAT21c_FIELD | INSTRUCTION_FORMAT21c_FIELD_ODEX) REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($inst.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableFieldReference fieldReference = $fully_qualified_field.fieldReference;

      $instructions.add(new ImmutableInstruction21c(opcode, regA, fieldReference));
    };

insn_format21c_string[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-string v1, "Hello World!"
    ^(I_STATEMENT_FORMAT21c_STRING INSTRUCTION_FORMAT21c_STRING REGISTER string_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_STRING.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableStringReference stringReference = new ImmutableStringReference($string_literal.value);

      instructions.add(new ImmutableInstruction21c(opcode, regA, stringReference));
    };

insn_format21c_type[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-class v2, org/jf/HelloWorld2/HelloWorld2
    ^(I_STATEMENT_FORMAT21c_TYPE INSTRUCTION_FORMAT21c_TYPE REGISTER reference_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21c_TYPE.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableTypeReference typeReference = new ImmutableTypeReference($reference_type_descriptor.type);

      $instructions.add(new ImmutableInstruction21c(opcode, regA, typeReference));
    };

insn_format21ih[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/high16 v1, 1234
    ^(I_STATEMENT_FORMAT21ih INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21ih.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      instructions.add(new ImmutableInstruction21ih(opcode, regA, litB));
    };

insn_format21lh[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-wide/high16 v1, 1234
    ^(I_STATEMENT_FORMAT21lh INSTRUCTION_FORMAT21lh REGISTER fixed_64bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21lh.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      instructions.add(new ImmutableInstruction21lh(opcode, regA, litB));
    };

insn_format21s[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const/16 v1, 1234
    ^(I_STATEMENT_FORMAT21s INSTRUCTION_FORMAT21s REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21s.text);
      short regA = parseRegister_byte($REGISTER.text);

      short litB = $short_integral_literal.value;

      $instructions.add(new ImmutableInstruction21s(opcode, regA, litB));
    };

insn_format21t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. if-eqz v0, endloop:
    ^(I_STATEMENT_FORMAT21t INSTRUCTION_FORMAT21t REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT21t.text);
      short regA = parseRegister_byte($REGISTER.text);

      int addressOffset = $offset_or_label.offsetValue;

      if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
        throw new SemanticException(input, $offset_or_label.start, "The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
      }

      $instructions.add(new ImmutableInstruction21t(opcode, regA, addressOffset));
    };

insn_format22b[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int v0, v1, 123
    ^(I_STATEMENT_FORMAT22b INSTRUCTION_FORMAT22b registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22b.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);

      short litC = $short_integral_literal.value;
      LiteralTools.checkByte(litC);

      $instructions.add(new ImmutableInstruction22b(opcode, regA, regB, litC));
    };

insn_format22c_field[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. iput-object v1, v0, org/jf/HelloWorld2/HelloWorld2.helloWorld Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_FIELD inst=(INSTRUCTION_FORMAT22c_FIELD | INSTRUCTION_FORMAT22c_FIELD_ODEX) registerA=REGISTER registerB=REGISTER fully_qualified_field)
    {
      Opcode opcode = Opcode.getOpcodeByName($inst.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      ImmutableFieldReference fieldReference = $fully_qualified_field.fieldReference;

      $instructions.add(new ImmutableInstruction22c(opcode, regA, regB, fieldReference));
    };

insn_format22c_type[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. instance-of v0, v1, Ljava/lang/String;
    ^(I_STATEMENT_FORMAT22c_TYPE INSTRUCTION_FORMAT22c_TYPE registerA=REGISTER registerB=REGISTER nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22c_TYPE.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      ImmutableTypeReference typeReference = new ImmutableTypeReference($nonvoid_type_descriptor.type);

      $instructions.add(new ImmutableInstruction22c(opcode, regA, regB, typeReference));
    };

insn_format22s[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int/lit16 v0, v1, 12345
    ^(I_STATEMENT_FORMAT22s INSTRUCTION_FORMAT22s registerA=REGISTER registerB=REGISTER short_integral_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22s.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      short litC = $short_integral_literal.value;

      $instructions.add(new ImmutableInstruction22s(opcode, regA, regB, litC));
    };

insn_format22t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. if-eq v0, v1, endloop:
    ^(I_STATEMENT_FORMAT22t INSTRUCTION_FORMAT22t registerA=REGISTER registerB=REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22t.text);
      byte regA = parseRegister_nibble($registerA.text);
      byte regB = parseRegister_nibble($registerB.text);

      int addressOffset = $offset_or_label.offsetValue;

      if (addressOffset < Short.MIN_VALUE || addressOffset > Short.MAX_VALUE) {
        throw new SemanticException(input, $offset_or_label.start, "The offset/label is out of range. The offset is " + Integer.toString(addressOffset) + " and the range for this opcode is [-32768, 32767].");
      }

      $instructions.add(new ImmutableInstruction22t(opcode, regA, regB, addressOffset));
    };

insn_format22x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move/from16 v1, v1234
    ^(I_STATEMENT_FORMAT22x INSTRUCTION_FORMAT22x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT22x.text);
      short regA = parseRegister_byte($registerA.text);
      int regB = parseRegister_short($registerB.text);

      $instructions.add(new ImmutableInstruction22x(opcode, regA, regB));
    };

insn_format23x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. add-int v1, v2, v3
    ^(I_STATEMENT_FORMAT23x INSTRUCTION_FORMAT23x registerA=REGISTER registerB=REGISTER registerC=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT23x.text);
      short regA = parseRegister_byte($registerA.text);
      short regB = parseRegister_byte($registerB.text);
      short regC = parseRegister_byte($registerC.text);

      $instructions.add(new ImmutableInstruction23x(opcode, regA, regB, regC));
    };

insn_format30t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. goto/32 endloop:
    ^(I_STATEMENT_FORMAT30t INSTRUCTION_FORMAT30t offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT30t.text);

      int addressOffset = $offset_or_label.offsetValue;

      $instructions.add(new ImmutableInstruction30t(opcode, addressOffset));
    };

insn_format31c[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-string/jumbo v1 "Hello World!"
    ^(I_STATEMENT_FORMAT31c INSTRUCTION_FORMAT31c REGISTER string_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31c.text);
      short regA = parseRegister_byte($REGISTER.text);

      ImmutableStringReference stringReference = new ImmutableStringReference($string_literal.value);

      $instructions.add(new ImmutableInstruction31c(opcode, regA, stringReference));
    };

insn_format31i[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const v0, 123456
    ^(I_STATEMENT_FORMAT31i INSTRUCTION_FORMAT31i REGISTER fixed_32bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31i.text);
      short regA = parseRegister_byte($REGISTER.text);

      int litB = $fixed_32bit_literal.value;

      $instructions.add(new ImmutableInstruction31i(opcode, regA, litB));
    };

insn_format31t[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. fill-array-data v0, ArrayData:
    ^(I_STATEMENT_FORMAT31t INSTRUCTION_FORMAT31t REGISTER offset_or_label)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT31t.text);

      short regA = parseRegister_byte($REGISTER.text);

      int addressOffset = $offset_or_label.offsetValue;
      if (($method::currentAddress + addressOffset) \% 2 != 0) {
        addressOffset++;
      }

      $instructions.add(new ImmutableInstruction31t(opcode, regA, addressOffset));
    };

insn_format32x[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. move/16 v5678, v1234
    ^(I_STATEMENT_FORMAT32x INSTRUCTION_FORMAT32x registerA=REGISTER registerB=REGISTER)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT32x.text);
      int regA = parseRegister_short($registerA.text);
      int regB = parseRegister_short($registerB.text);

      $instructions.add(new ImmutableInstruction32x(opcode, regA, regB));
    };

insn_format35c_method[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. invoke-virtual {v0,v1} java/io/PrintStream/print(Ljava/lang/Stream;)V
    ^(I_STATEMENT_FORMAT35c_METHOD INSTRUCTION_FORMAT35c_METHOD register_list fully_qualified_method)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT35c_METHOD.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;
      $outRegisters = registerCount;

      ImmutableMethodReference methodReference = $fully_qualified_method.methodReference;

      $instructions.add(new ImmutableInstruction35c(opcode, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], methodReference));
    };

insn_format35c_type[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. filled-new-array {v0,v1}, I
    ^(I_STATEMENT_FORMAT35c_TYPE INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT35c_TYPE.text);

      //this depends on the fact that register_list returns a byte[5]
      byte[] registers = $register_list.registers;
      byte registerCount = $register_list.registerCount;
      $outRegisters = registerCount;

      ImmutableTypeReference typeReference = new ImmutableTypeReference($nonvoid_type_descriptor.type);

      $instructions.add(new ImmutableInstruction35c(opcode, registerCount, registers[0], registers[1], registers[2], registers[3], registers[4], typeReference));
    };

insn_format3rc_method[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. invoke-virtual/range {v25..v26} java/lang/StringBuilder/append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    ^(I_STATEMENT_FORMAT3rc_METHOD INSTRUCTION_FORMAT3rc_METHOD register_range fully_qualified_method)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT3rc_METHOD.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      ImmutableMethodReference methodReference = $fully_qualified_method.methodReference;

      $instructions.add(new ImmutableInstruction3rc(opcode, startRegister, registerCount, methodReference));
    };

insn_format3rc_type[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. filled-new-array/range {v0..v6} I
    ^(I_STATEMENT_FORMAT3rc_TYPE INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT3rc_TYPE.text);
      int startRegister = $register_range.startRegister;
      int endRegister = $register_range.endRegister;

      int registerCount = endRegister-startRegister+1;
      $outRegisters = registerCount;

      ImmutableTypeReference typeReference = new ImmutableTypeReference($nonvoid_type_descriptor.type);

      $instructions.add(new ImmutableInstruction3rc(opcode, startRegister, registerCount, typeReference));
    };

insn_format51l_type[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. const-wide v0, 5000000000L
    ^(I_STATEMENT_FORMAT51l INSTRUCTION_FORMAT51l REGISTER fixed_64bit_literal)
    {
      Opcode opcode = Opcode.getOpcodeByName($INSTRUCTION_FORMAT51l.text);
      short regA = parseRegister_byte($REGISTER.text);

      long litB = $fixed_64bit_literal.value;

      $instructions.add(new ImmutableInstruction51l(opcode, regA, litB));
    };

insn_array_data_directive[List<Instruction> instructions] returns[int outRegisters]
  : //e.g. .array-data 4 1000000 .end array-data
    ^(I_STATEMENT_ARRAY_DATA ^(I_ARRAY_ELEMENT_SIZE short_integral_literal) array_elements)
    {
      // TODO: reimplement, after changing how it's parsed
      /*
      int elementWidth = $short_integral_literal.value;
      List<byte[]> byteValues = $array_elements.elements;

      int length = 0;
      for (byte[] byteValue: byteValues) {
        length+=byteValue.length;
      }

      byte[] encodedValues = new byte[length];
      int index = 0;
      for (byte[] byteValue: byteValues) {
        System.arraycopy(byteValue, 0, encodedValues, index, byteValue.length);
        index+=byteValue.length;
      }

      $instructions.add(new ImmutableArrayPayload(elementWidth, null));
      */
    };

insn_packed_switch_directive[List<Instruction> instructions] returns[int outRegisters]
  :
    ^(I_STATEMENT_PACKED_SWITCH ^(I_PACKED_SWITCH_START_KEY fixed_32bit_literal)
      {
        int startKey = $fixed_32bit_literal.value;
        Integer baseAddress = $method::packedSwitchDeclarations.get($method::currentAddress);
        if (baseAddress == null) {
          baseAddress = 0;
        }
      }
      packed_switch_elements[baseAddress, startKey])
      {
        $instructions.add(new ImmutablePackedSwitchPayload($packed_switch_elements.elements));
      };

insn_sparse_switch_directive[List<Instruction> instructions] returns[int outRegisters]
  :
    {
      Integer baseAddress = $method::sparseSwitchDeclarations.get($method::currentAddress);
      if (baseAddress == null) {
        baseAddress = 0;
      }
    }
    ^(I_STATEMENT_SPARSE_SWITCH sparse_switch_elements[baseAddress])
    {
      $instructions.add(new ImmutableSparseSwitchPayload($sparse_switch_elements.elements));
    };

nonvoid_type_descriptor returns [String type]
  : (PRIMITIVE_TYPE
  | CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = $start.getText();
  };


reference_type_descriptor returns [String type]
  : (CLASS_DESCRIPTOR
  | ARRAY_DESCRIPTOR)
  {
    $type = $start.getText();
  };

type_descriptor returns [String type]
  : VOID_TYPE {$type = "V";}
  | nonvoid_type_descriptor {$type = $nonvoid_type_descriptor.type;}
  ;

short_integral_literal returns[short value]
  : long_literal
    {
      LiteralTools.checkShort($long_literal.value);
      $value = (short)$long_literal.value;
    }
  | integer_literal
    {
      LiteralTools.checkShort($integer_literal.value);
      $value = (short)$integer_literal.value;
    }
  | short_literal {$value = $short_literal.value;}
  | char_literal {$value = (short)$char_literal.value;}
  | byte_literal {$value = $byte_literal.value;};

integral_literal returns[int value]
  : long_literal
    {
      LiteralTools.checkInt($long_literal.value);
      $value = (int)$long_literal.value;
    }
  | integer_literal {$value = $integer_literal.value;}
  | short_literal {$value = $short_literal.value;}
  | byte_literal {$value = $byte_literal.value;};


integer_literal returns[int value]
  : INTEGER_LITERAL { $value = LiteralTools.parseInt($INTEGER_LITERAL.text); };

long_literal returns[long value]
  : LONG_LITERAL { $value = LiteralTools.parseLong($LONG_LITERAL.text); };

short_literal returns[short value]
  : SHORT_LITERAL { $value = LiteralTools.parseShort($SHORT_LITERAL.text); };

byte_literal returns[byte value]
  : BYTE_LITERAL { $value = LiteralTools.parseByte($BYTE_LITERAL.text); };

float_literal returns[float value]
  : FLOAT_LITERAL { $value = LiteralTools.parseFloat($FLOAT_LITERAL.text); };

double_literal returns[double value]
  : DOUBLE_LITERAL { $value = LiteralTools.parseDouble($DOUBLE_LITERAL.text); };

char_literal returns[char value]
  : CHAR_LITERAL { $value = $CHAR_LITERAL.text.charAt(1); };

string_literal returns[String value]
  : STRING_LITERAL
    {
      $value = $STRING_LITERAL.text;
      $value = $value.substring(1,$value.length()-1);
    };

bool_literal returns[boolean value]
  : BOOL_LITERAL { $value = Boolean.parseBoolean($BOOL_LITERAL.text); };

array_literal returns[List<EncodedValue> elements]
  : {$elements = Lists.newArrayList();}
    ^(I_ENCODED_ARRAY (literal {$elements.add($literal.encodedValue);})*);

annotations returns[Set<Annotation> annotations]
  : {HashMap<String, Annotation> annotationMap = Maps.newHashMap();}
    ^(I_ANNOTATIONS (annotation
    {
        Annotation anno = $annotation.annotation;
        Annotation old = annotationMap.put(anno.getType(), anno);
        if (old != null) {
            throw new SemanticException(input, "Multiple annotations of type \%s", anno.getType());
        }
    })*)
    {
      if (annotationMap.size() > 0) {
        $annotations = ImmutableSet.copyOf(annotationMap.values());
      }
    };

annotation returns[Annotation annotation]
  : ^(I_ANNOTATION ANNOTATION_VISIBILITY subannotation)
    {
      int visibility = AnnotationVisibility.getVisibility($ANNOTATION_VISIBILITY.text);
      $annotation = new ImmutableAnnotation(visibility, $subannotation.annotationType, $subannotation.elements);
    };

annotation_element returns[AnnotationElement element]
  : ^(I_ANNOTATION_ELEMENT SIMPLE_NAME literal)
    {
      $element = new ImmutableAnnotationElement($SIMPLE_NAME.text, $literal.encodedValue);
    };

subannotation returns[String annotationType, List<AnnotationElement> elements]
  : {ArrayList<AnnotationElement> elements = Lists.newArrayList();}
    ^(I_SUBANNOTATION
        CLASS_DESCRIPTOR
        (annotation_element
        {
           elements.add($annotation_element.element);
        })*
     )
    {
      $annotationType = $CLASS_DESCRIPTOR.text;
      $elements = elements;
    };

field_literal returns[FieldReference value]
  : ^(I_ENCODED_FIELD fully_qualified_field)
    {
      $value = $fully_qualified_field.fieldReference;
    };

method_literal returns[MethodReference value]
  : ^(I_ENCODED_METHOD fully_qualified_method)
    {
      $value = $fully_qualified_method.methodReference;
    };

enum_literal returns[FieldReference value]
  : ^(I_ENCODED_ENUM fully_qualified_field)
    {
      $value = $fully_qualified_field.fieldReference;
    };
