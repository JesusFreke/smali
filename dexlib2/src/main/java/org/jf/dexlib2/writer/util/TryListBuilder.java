/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.util;

import com.google.common.collect.Lists;
import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.immutable.ImmutableExceptionHandler;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TryListBuilder
{
    /*TODO: add logic to merge adjacent, identical try blocks, and remove superflous handlers
      Also provide a "strict" mode, where the above isn't performed, which will be useful to be able to
      exactly reproduce the original .dex file (for testing/verification purposes)*/

    // Linked list sentinels that don't represent an actual try block
    // Their values are never modified, only their links
    private final MutableTryBlock listStart;
    private final MutableTryBlock listEnd;

    public TryListBuilder() {
        listStart = new MutableTryBlock(0, 0);
        listEnd = new MutableTryBlock(0, 0);
        listStart.next = listEnd;
        listEnd.prev = listStart;
    }

    public static List<TryBlock> massageTryBlocks(List<? extends TryBlock> tryBlocks) {
        TryListBuilder tlb = new TryListBuilder();
        for (TryBlock tryBlock: tryBlocks) {
            int startAddress = tryBlock.getStartCodeAddress();
            int endAddress = startAddress + tryBlock.getCodeUnitCount();

            for (ExceptionHandler exceptionHandler: tryBlock.getExceptionHandlers()) {
                tlb.addHandler(exceptionHandler.getExceptionType(), startAddress, endAddress,
                        exceptionHandler.getHandlerCodeAddress());
            }
        }
        return tlb.getTryBlocks();
    }

    private static class TryBounds {
        @Nonnull public final MutableTryBlock start;
        @Nonnull public final MutableTryBlock end;

        public TryBounds(@Nonnull MutableTryBlock start, @Nonnull MutableTryBlock end) {
            this.start = start;
            this.end = end;
        }
    }

    public static class InvalidTryException extends ExceptionWithContext {
        public InvalidTryException(Throwable cause) {
            super(cause);
        }

        public InvalidTryException(Throwable cause, String message, Object... formatArgs) {
            super(cause, message, formatArgs);
        }

        public InvalidTryException(String message, Object... formatArgs) {
            super(message, formatArgs);
        }
    }

    private static class MutableTryBlock extends BaseTryBlock implements TryBlock {
        public MutableTryBlock prev = null;
        public MutableTryBlock next = null;

        public int startCodeAddress;
        public int endCodeAddress;
        @Nonnull public List<ExceptionHandler> exceptionHandlers = Lists.newArrayList();

        public MutableTryBlock(int startCodeAddress, int endCodeAddress) {
            this.startCodeAddress = startCodeAddress;
            this.endCodeAddress = endCodeAddress;
        }

        public MutableTryBlock(int startCodeAddress, int endCodeAddress,
                               @Nonnull List<? extends ExceptionHandler> exceptionHandlers) {
            this.startCodeAddress = startCodeAddress;
            this.endCodeAddress = endCodeAddress;
            this.exceptionHandlers = Lists.newArrayList(exceptionHandlers);
        }

        @Override public int getStartCodeAddress() {
            return startCodeAddress;
        }

        @Override public int getCodeUnitCount() {
            return endCodeAddress - startCodeAddress;
        }

        @Nonnull @Override public List<? extends ExceptionHandler> getExceptionHandlers() {
            return exceptionHandlers;
        }

        @Nonnull
        public MutableTryBlock split(int splitAddress) {
            MutableTryBlock newTryBlock = new MutableTryBlock(splitAddress, endCodeAddress, exceptionHandlers);
            endCodeAddress = splitAddress;
            append(newTryBlock);
            return newTryBlock;
        }

        public void delete() {
            next.prev = prev;
            prev.next = next;
        }

        public void mergeNext() {
            //assert next.startCodeAddress == this.endCodeAddress;
            this.endCodeAddress = next.endCodeAddress;
            next.delete();
        }

        public void append(@Nonnull MutableTryBlock tryBlock) {
            next.prev = tryBlock;
            tryBlock.next = next;
            tryBlock.prev = this;
            next = tryBlock;
        }

        public void prepend(@Nonnull MutableTryBlock tryBlock) {
            prev.next = tryBlock;
            tryBlock.prev = prev;
            tryBlock.next = this;
            prev = tryBlock;
        }

        public void addHandler(@Nonnull ExceptionHandler handler) {
            for (ExceptionHandler existingHandler: exceptionHandlers) {
                String existingType = existingHandler.getExceptionType();
                String newType = handler.getExceptionType();

                // Don't add it if we already have a handler of the same type
                if (existingType == null) {
                    if (newType == null) {
                        if (existingHandler.getHandlerCodeAddress() != handler.getHandlerCodeAddress()) {
                            throw new InvalidTryException(
                                    "Multiple overlapping catch all handlers with different handlers");
                        }
                        return;
                    }
                } else if (existingType.equals(newType)) {
                    if (existingHandler.getHandlerCodeAddress() != handler.getHandlerCodeAddress()) {
                        throw new InvalidTryException(
                                "Multiple overlapping catches for %s with different handlers", existingType);
                    }
                    return;
                }
            }

            exceptionHandlers.add(handler);
        }
    }

    private TryBounds getBoundingRanges(int startAddress, int endAddress) {
        MutableTryBlock startBlock = null;

        MutableTryBlock tryBlock = listStart.next;
        while (tryBlock != listEnd) {
            int currentStartAddress = tryBlock.startCodeAddress;
            int currentEndAddress = tryBlock.endCodeAddress;

            if (startAddress == currentStartAddress) {
                //|-----|
                //^------
                /*Bam. We hit the start of the range right on the head*/
                startBlock = tryBlock;
                break;
            } else if (startAddress > currentStartAddress && startAddress < currentEndAddress) {
                //|-----|
                //  ^----
                /*Almost. The start of the range being added is in the middle
                of an existing try range. We need to split the existing range
                at the start address of the range being added*/
                startBlock = tryBlock.split(startAddress);
                break;
            }else if (startAddress < currentStartAddress) {
                if (endAddress <= currentStartAddress) {
                    //      |-----|
                    //^--^
                    /*Oops, totally too far! The new range doesn't overlap any existing
                    ones, so we just add it and return*/
                    startBlock = new MutableTryBlock(startAddress, endAddress);
                    tryBlock.prepend(startBlock);
                    return new TryBounds(startBlock, startBlock);
                } else {
                    //   |-----|
                    //^---------
                    /*Oops, too far! We've passed the start of the range being added, but
                     the new range does overlap this one. We need to add a new range just
                     before this one*/
                    startBlock = new MutableTryBlock(startAddress, currentStartAddress);
                    tryBlock.prepend(startBlock);
                    break;
                }
            }

            tryBlock = tryBlock.next;
        }

        //|-----|
        //        ^-----
        /*Either the list of tries is blank, or all the tries in the list
        end before the range being added starts. In either case, we just need
        to add a new range at the end of the list*/
        if (startBlock == null) {
            startBlock = new MutableTryBlock(startAddress, endAddress);
            listEnd.prepend(startBlock);
            return new TryBounds(startBlock, startBlock);
        }

        tryBlock = startBlock;
        while (tryBlock != listEnd) {
            int currentStartAddress = tryBlock.startCodeAddress;
            int currentEndAddress = tryBlock.endCodeAddress;

            if (endAddress == currentEndAddress) {
                //|-----|
                //------^
                /*Bam! We hit the end right on the head... err, tail.*/
                return new TryBounds(startBlock, tryBlock);
            } else if (endAddress > currentStartAddress && endAddress < currentEndAddress) {
                //|-----|
                //--^
                /*Almost. The range being added ends in the middle of an
                existing range. We need to split the existing range
                at the end of the range being added.*/
                tryBlock.split(endAddress);
                return new TryBounds(startBlock, tryBlock);
            } else if (endAddress <= currentStartAddress) {
                //|-----|       |-----|
                //-----------^
                /*Oops, too far! The current range starts after the range being added
                ends. We need to create a new range that starts at the end of the
                previous range, and ends at the end of the range being added*/
                MutableTryBlock endBlock = new MutableTryBlock(tryBlock.prev.endCodeAddress, endAddress);
                tryBlock.prepend(endBlock);
                return new TryBounds(startBlock, endBlock);
            }
            tryBlock = tryBlock.next;
        }

        //|-----|
        //--------^
        /*The last range in the list ended before the end of the range being added.
        We need to add a new range that starts at the end of the last range in the
        list, and ends at the end of the range being added.*/
        MutableTryBlock endBlock = new MutableTryBlock(listEnd.prev.endCodeAddress, endAddress);
        listEnd.prepend(endBlock);
        return new TryBounds(startBlock, endBlock);
    }

    public void addHandler(String type, int startAddress, int endAddress, int handlerAddress) {
        TryBounds bounds = getBoundingRanges(startAddress, endAddress);

        MutableTryBlock startBlock = bounds.start;
        MutableTryBlock endBlock = bounds.end;

        ExceptionHandler handler = new ImmutableExceptionHandler(type, handlerAddress);

        int previousEnd = startAddress;
        MutableTryBlock tryBlock = startBlock;

        /*Now we have the start and end ranges that exactly match the start and end
        of the range being added. We need to iterate over all the ranges from the start
        to end range inclusively, and append the handler to the end of each range's handler
        list. We also need to create a new range for any "holes" in the existing ranges*/
        do
        {
            //is there a hole? If so, add a new range to fill the hole
            if (tryBlock.startCodeAddress > previousEnd) {
                MutableTryBlock newBlock = new MutableTryBlock(previousEnd, tryBlock.startCodeAddress);
                tryBlock.prepend(newBlock);
                tryBlock = newBlock;
            }

            tryBlock.addHandler(handler);
            previousEnd = tryBlock.endCodeAddress;
            tryBlock = tryBlock.next;
        } while (tryBlock.prev != endBlock);
    }

    public List<TryBlock> getTryBlocks() {
        return Lists.newArrayList(new Iterator<TryBlock>() {
            // The next TryBlock to return. This has already been merged, if needed.
            @Nullable private MutableTryBlock next;

            {
                next = listStart;
                next = readNextItem();
            }

            /**
             * Read the item that comes after the current value of the next field.
             * @return The next item, or null if there is no next item
             */
            @Nullable protected MutableTryBlock readNextItem() {
                // We can assume that next is not null
                MutableTryBlock ret = next.next;

                if (ret == listEnd) {
                    return null;
                }

                while (ret.next != listEnd) {
                    if (ret.endCodeAddress == ret.next.startCodeAddress &&
                            ret.getExceptionHandlers().equals(ret.next.getExceptionHandlers())) {
                        ret.mergeNext();
                    } else {
                        break;
                    }
                }
                return ret;
            }

            @Override public boolean hasNext() {
                return next != null;
            }

            @Override public TryBlock next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                TryBlock ret = next;
                next = readNextItem();
                return ret;
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
