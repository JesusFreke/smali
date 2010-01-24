package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.TypeIdItem;
import static org.jf.dexlib.Code.Analysis.ClassPath.ClassDef;
import static org.jf.dexlib.Code.Analysis.ClassPath.PrimitiveClassDef;
import static org.jf.dexlib.Code.Analysis.ClassPath.ArrayClassDef;
import java.util.HashMap;

public class RegisterType {
    private final static HashMap<RegisterType, RegisterType> internedRegisterTypes =
            new HashMap<RegisterType, RegisterType>();

    public final Category category;
    public final ClassDef type;

    private RegisterType(Category category, ClassDef type) {
        assert ((category == Category.Reference || category == Category.UninitRef) && type != null) ||
               ((category != Category.Reference && category != Category.UninitRef) && type == null);

        this.category = category;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + category.name() + (type==null?"":("," + type.getClassType())) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterType that = (RegisterType) o;

        if (category != that.category) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = category.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public static enum Category {
        Unknown,
        Null,
        One,
        Boolean,
        Byte,
        PosByte,
        Short,
        PosShort,
        Char,
        Integer,
        Float,
        LongLo,
        LongHi,
        DoubleLo,
        DoubleHi,
        UninitRef,
        Reference,
        Conflicted;

        protected static Category[][] mergeTable  =
        {
                /*             Unknown      Null        One,        Boolean     Byte        PosByte     Short       PosShort    Char        Integer,    Float,      LongLo      LongHi      DoubleLo    DoubleHi    UninitRef   Reference   Conflicted*/
                /*Unknown*/    {Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown,    Unknown},
                /*Null*/       {Unknown,    Null,       Conflicted, Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Reference,  Conflicted},
                /*One*/        {Unknown,    Conflicted, One,        Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Boolean*/    {Unknown,    Boolean,    Boolean,    Boolean,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Byte*/       {Unknown,    Byte,       Byte,       Byte,       Byte,       Byte,       Short,      Short,      Integer,    Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*PosByte*/    {Unknown,    PosByte,    PosByte,    PosByte,    Byte,       PosByte,    Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Short*/      {Unknown,    Short,      Short,      Short,      Short,      Short,      Short,      Short,      Integer,    Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*PosShort*/   {Unknown,    PosShort,   PosShort,   PosShort,   Short,      PosShort,   Short,      PosShort,   Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Char*/       {Unknown,    Char,       Char,       Char,       Integer,    Char,       Integer,    Char,       Char,       Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Integer*/    {Unknown,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Integer,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*Float*/      {Unknown,    Float,      Float,      Float,      Float,      Float,      Float,      Float,      Float,      Integer,    Float,      Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted},
                /*LongLo*/     {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongLo,     Conflicted, LongLo,     Conflicted, Conflicted, Conflicted, Conflicted},
                /*LongHi*/     {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongHi,     Conflicted, LongHi,     Conflicted, Conflicted, Conflicted},
                /*DoubleLo*/   {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongLo,     Conflicted, DoubleLo,   Conflicted, Conflicted, Conflicted, Conflicted},
                /*DoubleHi*/   {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, LongHi,     Conflicted, DoubleHi,   Conflicted, Conflicted, Conflicted},
                /*UninitRef*/  {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, UninitRef,  Conflicted, Conflicted},
                /*Reference*/  {Unknown,    Reference,  Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Reference,  Conflicted},
                /*Conflicted*/ {Unknown,    Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted, Conflicted}
        };
    }

    public static RegisterType getRegisterTypeForTypeIdItem(TypeIdItem typeIdItem) {
        switch (typeIdItem.getTypeDescriptor().charAt(0)) {
            case 'V':
                throw new ValidationException("The V type can only be used as a method return type");
            case 'Z':
                return getRegisterType(Category.Boolean, null);
            case 'B':
                return getRegisterType(Category.Byte, null);
            case 'S':
                return getRegisterType(Category.Short, null);
            case 'C':
                return getRegisterType(Category.Char, null);
            case 'I':
                return getRegisterType(Category.Integer, null);
            case 'F':
                return getRegisterType(Category.Float, null);
            case 'J':
                return getRegisterType(Category.LongLo, null);
            case 'D':
                return getRegisterType(Category.DoubleLo, null);
            case 'L':
            case '[':
                return getRegisterType(Category.Reference, ClassPath.getClassDef(typeIdItem));
            default:
                throw new RuntimeException("Invalid type: " + typeIdItem.getTypeDescriptor());
        }
    }

    public static RegisterType getWideRegisterTypeForTypeIdItem(TypeIdItem typeIdItem, boolean firstRegister) {
        if (typeIdItem.getRegisterCount() == 1) {
            throw new RuntimeException("Cannot use this method for non-wide register type: " +
                    typeIdItem.getTypeDescriptor());
        }

        switch (typeIdItem.getTypeDescriptor().charAt(0)) {
            case 'J':
                if (firstRegister) {
                    return getRegisterType(Category.LongLo, null);
                } else {
                    return getRegisterType(Category.LongHi, null);
                }
            case 'D':
                if (firstRegister) {
                    return getRegisterType(Category.DoubleLo, null);
                } else {
                    return getRegisterType(Category.DoubleHi, null);
                }
            default:
                throw new RuntimeException("Invalid type: " + typeIdItem.getTypeDescriptor());
        }
    }

    public static RegisterType getRegisterTypeForLiteral(long literalValue) {
        if (literalValue < -32768) {
            return getRegisterType(Category.Integer, null);
        }
        if (literalValue < -128) {
            return getRegisterType(Category.Short, null);
        }
        if (literalValue < 0) {
            return getRegisterType(Category.Byte, null);
        }
        if (literalValue == 0) {
            return getRegisterType(Category.Null, null);
        }
        if (literalValue == 1) {
            return getRegisterType(Category.One, null);
        }
        if (literalValue < 128) {
            return getRegisterType(Category.PosByte, null);
        }
        if (literalValue < 32768) {
            return getRegisterType(Category.PosShort, null);
        }
        if (literalValue < 65536) {
            return getRegisterType(Category.Char, null);
        }
        return getRegisterType(Category.Integer, null);
    }

    public RegisterType merge(RegisterType type) {
        if (type == null || type == this) {
            return this;
        }

        Category mergedCategory = Category.mergeTable[this.category.ordinal()][type.category.ordinal()];
        if (mergedCategory == Category.Conflicted) {
            throw new ValidationException("Incompatible register types." +
                    " Category1: " + this.category.name() +
                    (this.type==null?"":" Type1: " + this.type.getClassType()) +
                    " Category2: " + type.category.name() +
                    (type.type==null?"":" Type2: " + type.type.getClassType()));
        }

        ClassDef mergedType = null;
        if (mergedCategory == Category.Reference) {
            mergedType = ClassPath.getCommonSuperclass(this.type, type.type);
        }
        return RegisterType.getRegisterType(mergedCategory, mergedType);
    }

    public static RegisterType getRegisterType(Category category, ClassDef classType) {
        RegisterType newRegisterType = new RegisterType(category, classType);
        RegisterType internedRegisterType = internedRegisterTypes.get(newRegisterType);
        if (internedRegisterType == null) {
            internedRegisterTypes.put(newRegisterType, newRegisterType);
            return newRegisterType;
        }
        return internedRegisterType;
    }
}
