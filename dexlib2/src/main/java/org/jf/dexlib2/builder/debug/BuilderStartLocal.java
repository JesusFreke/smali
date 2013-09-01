package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.StartLocal;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderStartLocal extends BuilderDebugItem implements StartLocal {
    private final int register;
    @Nullable private final StringReference name;
    @Nullable private final TypeReference type;
    @Nullable private final StringReference signature;

    public BuilderStartLocal(@Nonnull MethodLocation location,
                             int register,
                             @Nullable StringReference name,
                             @Nullable TypeReference type,
                             @Nullable StringReference signature) {
        super(location);
        this.register = register;
        this.name = name;
        this.type = type;
        this.signature = signature;
    }

    @Override public int getRegister() { return register; }

    @Nullable @Override public StringReference getNameReference() { return name; }
    @Nullable @Override public TypeReference getTypeReference() { return type; }
    @Nullable @Override public StringReference getSignatureReference() { return signature; }

    @Nullable @Override public String getName() {
        return name==null?null:name.getString();
    }

    @Nullable @Override public String getType() {
        return type==null?null:type.getType();
    }

    @Nullable @Override public String getSignature() {
        return signature==null?null:signature.getString();
    }

    @Override public int getDebugItemType() { return DebugItemType.START_LOCAL; }
}
