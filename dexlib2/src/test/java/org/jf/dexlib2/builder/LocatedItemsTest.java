package org.jf.dexlib2.builder;

import com.google.common.collect.Sets;
import org.jf.dexlib2.builder.debug.BuilderLineNumber;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LocatedItemsTest {

    private List<BuilderDebugItem> createItems(int count) {
        List<BuilderDebugItem> items = new ArrayList<>();
        for(int i = 0; i < count; ++i) {
            items.add(new BuilderLineNumber(i));
        }
        return items;
    }

    private void doTestMergeIntoKeepsOrderOfDebugItems(int countLocation1, int countLocation2) {
        MethodLocation location1 = new MethodLocation(null, 123, 1);
        MethodLocation location2 = new MethodLocation(null, 456, 2);

        List<BuilderDebugItem> items1 = createItems(countLocation1);
        List<BuilderDebugItem> items2 = createItems(countLocation2);
        location1.getDebugItems().addAll(items1);
        location2.getDebugItems().addAll(items2);

        location1.mergeInto(location2);

        Assert.assertEquals(Sets.newHashSet(), location1.getDebugItems());
        // items1 appear BEFORE items2
        List<BuilderDebugItem> expectedItems = new ArrayList<>(items1);
        expectedItems.addAll(items2);
        Assert.assertEquals(expectedItems, new ArrayList<>(location2.getDebugItems()));
    }

    @Test
    public void testMergeIntoKeepsOrderOfDebugItems() {
        doTestMergeIntoKeepsOrderOfDebugItems(2, 2);
        doTestMergeIntoKeepsOrderOfDebugItems(0, 0);
        doTestMergeIntoKeepsOrderOfDebugItems(0, 2);
        doTestMergeIntoKeepsOrderOfDebugItems(2, 0);
    }
}
