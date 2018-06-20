package org.jf.dexlib2.builder;

public class LocatedDebugItems extends LocatedItems<BuilderDebugItem> {

    @Override
    protected String addLocatedItemError() {
        return "Cannot add a debug item that has already been added to a method." +
                "You must remove it from its current location first.";
    }
}
