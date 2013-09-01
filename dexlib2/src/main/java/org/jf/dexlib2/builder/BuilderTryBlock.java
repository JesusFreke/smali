package org.jf.dexlib2.builder;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

class BuilderTryBlock extends BaseTryBlock<ExceptionHandler> {
    // We only ever have one exception handler per try block. They are later merged as needed in TryListBuilder
    @Nonnull public final ExceptionHandler exceptionHandler;
    @Nonnull public final Label start;
    // The end location is exclusive, it should point to the codeAddress of the instruction immediately after the last
    // covered instruction.
    @Nonnull public final Label end;

    public BuilderTryBlock(@Nonnull Label start, @Nonnull Label end, @Nullable String exceptionType,
                           @Nonnull Label handler) {
        this.start = start;
        this.end = end;
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(exceptionType, handler);
    }

    public BuilderTryBlock(@Nonnull Label start, @Nonnull Label end, @Nullable TypeReference exceptionType,
                           @Nonnull Label handler) {
        this.start = start;
        this.end = end;
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(exceptionType, handler);
    }

    public BuilderTryBlock(@Nonnull Label start, @Nonnull Label end, @Nonnull Label handler) {
        this.start = start;
        this.end = end;
        this.exceptionHandler = BuilderExceptionHandler.newExceptionHandler(handler);
    }

    @Override public int getStartCodeAddress() {
        return start.getCodeAddress();
    }

    @Override public int getCodeUnitCount() {
        return end.getCodeAddress() - start.getCodeAddress();
    }

    @Nonnull @Override public List<? extends ExceptionHandler> getExceptionHandlers() {
        return ImmutableList.of(exceptionHandler);
    }
}
