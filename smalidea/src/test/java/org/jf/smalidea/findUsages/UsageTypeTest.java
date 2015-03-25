package org.jf.smalidea.findUsages;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class UsageTypeTest extends PsiTestCase {
    // e.g. <ref:1>, <ref:1234>, etc.
    private static final Pattern REF_PATTERN = Pattern.compile("(<ref:([0-9]+)>)");

    @NotNull
    private final UsageTypeProvider usageTypeProvider;

    public UsageTypeTest(@NotNull UsageTypeProvider usageTypeProvider) {
        this.usageTypeProvider = usageTypeProvider;
    }

    protected void doTest(@NotNull String fileName, @NotNull String text, @NotNull Object... expectedUsageTypes)
            throws Exception {
        Assert.assertTrue(expectedUsageTypes.length % 2 == 0);

        Map<Integer, UsageType> expectedUsageTypesMap = Maps.newHashMap();
        for (int i=0; i<expectedUsageTypes.length; i+=2) {
            expectedUsageTypesMap.put((Integer) expectedUsageTypes[i], (UsageType) expectedUsageTypes[i + 1]);
        }

        PsiFile psiFile = createFile(fileName, REF_PATTERN.matcher(text).replaceAll(""));
        Map<Integer, Integer> refIndexMap = getRefIndexes(text);

        for (Map.Entry<Integer, Integer> entry: refIndexMap.entrySet()) {
            int refId = entry.getKey();
            int index = entry.getValue();

            PsiReference reference = psiFile.getFirstChild().findReferenceAt(index);
            Assert.assertNotNull(reference);
            if (reference instanceof PsiMultiReference) {
                // If there are multiple reference parents, the default seems to be the last one,
                // i.e. the highest parent. We actually want the lowest one here.
                reference = ((PsiMultiReference) reference).getReferences()[0];
            }

            UsageType usageType = usageTypeProvider.getUsageType(reference.getElement());
            Assert.assertNotNull(usageType);
            Assert.assertSame(expectedUsageTypesMap.get(refId), usageType);
            expectedUsageTypesMap.remove(refId);
        }
        Assert.assertTrue(expectedUsageTypesMap.isEmpty());
    }

    @NotNull
    private Map<Integer, Integer> getRefIndexes(@NotNull String text) {
        Matcher m = REF_PATTERN.matcher(text);
        int correction = 0;
        Map<Integer, Integer> refIndexes = new HashMap<Integer, Integer>();
        while (m.find()) {
            int refId = Integer.parseInt(m.group(2));
            refIndexes.put(refId, m.start() - correction);
            correction += m.end() - m.start();
        }
        return refIndexes;
    }
}
