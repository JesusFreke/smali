/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public class CollectionUtils {
    public static <T extends Comparable<? super T>> int compareAsList(@Nonnull Collection<? extends T> list1,
                                                                      @Nonnull Collection<? extends T> list2) {
        int res = Ints.compare(list1.size(), list2.size());
        if (res != 0) return res;
        Iterator<? extends T> elements2 = list2.iterator();
        for (T element1: list1) {
            res = element1.compareTo(elements2.next());
            if (res != 0) return res;
        }
        return 0;
    }

    public static <T> int compareAsList(@Nonnull Comparator<? super T> elementComparator,
                                        @Nonnull Collection<? extends T> list1,
                                        @Nonnull Collection<? extends T> list2) {
        int res = Ints.compare(list1.size(), list2.size());
        if (res != 0) return res;
        Iterator<? extends T> elements2 = list2.iterator();
        for (T element1: list1) {
            res = elementComparator.compare(element1, elements2.next());
            if (res != 0) return res;
        }
        return 0;
    }

    public static <T> Comparator<Collection<? extends T>> listComparator(
            @Nonnull final Comparator<? super T> elementComparator) {
        return new Comparator<Collection<? extends T>>() {
            @Override
            public int compare(Collection<? extends T> list1, Collection<? extends T> list2) {
                return compareAsList(elementComparator, list1, list2);
            }
        };
    }

    @Nonnull
    private static <T> SortedSet<? extends T> toNaturalSortedSet(@Nonnull Collection<? extends T> collection) {
        if (collection instanceof SortedSet) {
            SortedSet<? extends T> sortedSet = (SortedSet<? extends T>)collection;
            Comparator<?> comparator = sortedSet.comparator();
            if (comparator == null || comparator.equals(Ordering.natural())) {
                return sortedSet;
            }
        }
        return ImmutableSortedSet.copyOf(collection);
    }

    @Nonnull
    private static <T> SortedSet<? extends T> toSortedSet(@Nonnull Comparator<? super T> elementComparator,
                                                          @Nonnull Collection<? extends T> collection) {
        if (collection instanceof SortedSet) {
            SortedSet<? extends T> sortedSet = (SortedSet<? extends T>)collection;
            Comparator<?> comparator = sortedSet.comparator();
            if (comparator != null && comparator.equals(elementComparator)) {
                return sortedSet;
            }
        }
        return ImmutableSortedSet.copyOf(elementComparator, collection);
    }

    public static <T extends Comparable<T>> int compareAsSet(@Nonnull Collection<? extends T> set1,
                                                             @Nonnull Collection<? extends T> set2) {
        int res = Ints.compare(set1.size(), set2.size());
        if (res != 0) return res;

        SortedSet<? extends T> sortedSet1 = toNaturalSortedSet(set1);
        SortedSet<? extends T> sortedSet2 = toNaturalSortedSet(set2);

        Iterator<? extends T> elements2 = set2.iterator();
        for (T element1: set1) {
            res = element1.compareTo(elements2.next());
            if (res != 0) return res;
        }
        return 0;
    }

    public static <T> int compareAsSet(@Nonnull Comparator<? super T> elementComparator,
                                       @Nonnull Collection<? extends T> list1,
                                       @Nonnull Collection<? extends T> list2) {
        int res = Ints.compare(list1.size(), list2.size());
        if (res != 0) return res;

        SortedSet<? extends T> set1 = toSortedSet(elementComparator, list1);
        SortedSet<? extends T> set2 = toSortedSet(elementComparator, list2);

        Iterator<? extends T> elements2 = set2.iterator();
        for (T element1: set1) {
            res = elementComparator.compare(element1, elements2.next());
            if (res != 0) return res;
        }
        return 0;
    }
}
