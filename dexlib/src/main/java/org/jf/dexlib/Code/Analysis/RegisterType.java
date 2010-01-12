package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.TypeIdItem;

public class RegisterType {
    public final Category category;
    public final TypeIdItem type;

    protected RegisterType(Category category, TypeIdItem type) {
        this.category = category;
        this.type = type;
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


    /*private static RegisterType[][] mergeTable  =
        {
               //Unknown        Null            Nonreference    Reference   Conflicted
                {Unknown,       Null,           NonReference,   Reference,  Conflicted}, //Unknown
                {Null,          Null,           NonReference,   Reference,  Conflicted}, //Null
                {NonReference,  NonReference,   NonReference,   Conflicted, Conflicted}, //NonReference
                {Reference,     Reference,      Conflicted,     Reference,  Conflicted}, //Referenced
                {Conflicted,    Conflicted,     Conflicted,     Conflicted, Conflicted}, //Conflicted
        };*/

    /*public static RegisterType mergeRegisterTypes(RegisterType type1, RegisterType type2) {
        return mergeTable[type1.ordinal()][type2.ordinal()];
    }*/

    public static enum Category {
        Unknown,
        Null,
        NonReference,
        Reference,
        Conflicted
    }
}
