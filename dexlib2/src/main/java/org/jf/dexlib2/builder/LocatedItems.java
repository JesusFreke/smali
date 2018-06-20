package org.jf.dexlib2.builder;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public abstract class LocatedItems<T extends ItemWithLocation> {
    // We end up creating and keeping around a *lot* of MethodLocation objects
    // when building a new dex file, so it's worth the trouble of lazily creating
    // the labels and debugItems lists only when they are needed
    @Nullable
    private List<T> items = null;

    @Nonnull
    private List<T> getMutableItems() {
        if (items == null) {
            items = new ArrayList<>(1);
        }
        return items;
    }

    @Nonnull
    private List<T> getImmutableItems() {
        if (items == null) {
            return ImmutableList.of();
        }
        return items;
    }

    public Set<T> getModifiableItems(MethodLocation newItemsLocation) {
        return new AbstractSet<T>() {
            @Nonnull
            @Override public Iterator<T> iterator() {
                final Iterator<T> it = getImmutableItems().iterator();

                return new Iterator<T>() {
                    private @Nullable
                    T currentItem = null;

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public T next() {
                        currentItem = it.next();
                        return currentItem;
                    }

                    @Override public void remove() {
                        if (currentItem != null) {
                            currentItem.setLocation(null);
                        }
                        it.remove();
                    }
                };
            }

            @Override public int size() {
                return getImmutableItems().size();
            }

            @Override public boolean add(@Nonnull T item) {
                if (item.isPlaced()) {
                    throw new IllegalArgumentException(addLocatedItemError());
                }
                item.setLocation(newItemsLocation);
                getMutableItems().add(item);
                return true;
            }
        };
    }

    protected abstract String addLocatedItemError();

    public void mergeItemsInto(@Nonnull MethodLocation newLocation, LocatedItems<T> otherLocatedItems) {
        if (items != null || otherLocatedItems.items != null) {
            List<T> otherItems = otherLocatedItems.getMutableItems();
            for (T item: getImmutableItems()) {
                item.setLocation(newLocation);
                otherItems.add(item);
            }
            items = null;
        }
    }
}
