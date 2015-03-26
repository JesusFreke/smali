package org.jf.smalidea.findUsages;

import com.intellij.usages.impl.rules.UsageType;

public class MethodUsageTypeTest extends UsageTypeTest {
    public MethodUsageTypeTest() {
        super(new SmaliUsageTypeProvider());
    }

    public void testMethodUsageTypes() throws Exception {
        doTest("blah.smali", "" +
                        ".class public Lblah;\n" +
                        ".super Ljava/lang/Object;\n" +
                        "\n" +
                        ".annotation runtime Lblah;\n" +
                        "    element = Lblah;->bl<ref:1>ah()V;\n" +
                        ".end annotation\n" +
                        "\n" +
                        ".method public blah()V\n" +
                        "    .registers 2\n" +
                        "\n" +
                        "    invoke-direct {v0}, Lblah;->bl<ref:2>ah()V\n" +
                        "    invoke-direct/empty {v0}, Lblah;->bl<ref:3>ah()V\n" +
                        "    invoke-direct/range {v0}, Lblah;->bl<ref:4>ah()V\n" +
                        "    invoke-interface {v0}, Lblah;->bl<ref:5>ah()V\n" +
                        "    invoke-interface/range {v0}, Lblah;->bl<ref:6>ah()V\n" +
                        "    invoke-object-init/range {v0}, Lblah;->bl<ref:7>ah()V\n" +
                        "    invoke-static {v0}, Lblah;->bl<ref:8>ah()V\n" +
                        "    invoke-static/range {v0}, Lblah;->bl<ref:9>ah()V\n" +
                        "    invoke-super {v0}, Lblah;->bl<ref:10>ah()V\n" +
                        "    invoke-super/range {v0}, Lblah;->bl<ref:11>ah()V\n" +
                        "    invoke-virtual {v0}, Lblah;->bl<ref:12>ah()V\n" +
                        "    invoke-virtual/range {v0}, Lblah;->bl<ref:13>ah()V\n" +
                        "\n" +
                        "    throw-verification-error generic-error, Lblah;->bl<ref:14>ah()V\n" +
                        "\n" +
                        "    return-void\n" +
                        ".end method\n",
                1, SmaliUsageTypeProvider.LITERAL,
                2, UsageType.UNCLASSIFIED,
                3, UsageType.UNCLASSIFIED,
                4, UsageType.UNCLASSIFIED,
                5, UsageType.UNCLASSIFIED,
                6, UsageType.UNCLASSIFIED,
                7, UsageType.UNCLASSIFIED,
                8, UsageType.UNCLASSIFIED,
                9, UsageType.UNCLASSIFIED,
                10, UsageType.UNCLASSIFIED,
                11, UsageType.UNCLASSIFIED,
                12, UsageType.UNCLASSIFIED,
                13, UsageType.UNCLASSIFIED,
                14, SmaliUsageTypeProvider.VERIFICATION_ERROR);
    }
}
