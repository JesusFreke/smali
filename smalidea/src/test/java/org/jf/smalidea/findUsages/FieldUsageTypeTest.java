package org.jf.smalidea.findUsages;

import com.intellij.usages.impl.rules.UsageType;

public class FieldUsageTypeTest extends UsageTypeTest {
    public FieldUsageTypeTest() {
        super(new SmaliUsageTypeProvider());
    }

    public void testFieldUsageTypes() throws Exception {
        doTest("blah.smali", "" +
                        ".class public Lblah;\n" +
                        ".super Ljava/lang/Object;\n" +
                        "\n" +
                        ".annotation runtime Lblah;\n" +
                        "    element = Lblah;->bl<ref:1>ah:Lblah;\n" +
                        "    element2 = .enum Lblah;->bl<ref:2>ah:Lblah;\n" +
                        ".end annotation\n" +
                        "\n" +
                        ".field public blah:Lblah;\n" +
                        "\n" +
                        ".method public blah(Lblah;)Lblah;\n" +
                        "    .registers 2\n" +
                        "\n" +
                        "    iget v0, v0, Lblah;->bl<ref:3>ah:Lblah;\n" +
                        "    iget-object v0, v0, Lblah;->bl<ref:4>ah:Lblah;\n" +
                        "    iget-byte v0, v0, Lblah;->bl<ref:5>ah:Lblah;\n" +
                        "    iget-char v0, v0, Lblah;->bl<ref:6>ah:Lblah;\n" +
                        "    iget-object v0, v0, Lblah;->bl<ref:7>ah:Lblah;\n" +
                        "    iget-object-volatile v0, v0, Lblah;->bl<ref:8>ah:Lblah;\n" +
                        "    iget-short v0, v0, Lblah;->bl<ref:9>ah:Lblah;\n" +
                        "    iget-volatile v0, v0, Lblah;->bl<ref:10>ah:Lblah;\n" +
                        "    iget-wide v0, v0, Lblah;->bl<ref:11>ah:Lblah;\n" +
                        "    iget-wide-volatile v0, v0, Lblah;->bl<ref:12>ah:Lblah;\n" +
                        "    sget v0, Lblah;->bl<ref:13>ah:Lblah;\n" +
                        "    sget-boolean v0, Lblah;->bl<ref:14>ah:Lblah;\n" +
                        "    sget-byte v0, Lblah;->bl<ref:15>ah:Lblah;\n" +
                        "    sget-char v0, Lblah;->bl<ref:16>ah:Lblah;\n" +
                        "    sget-object v0, Lblah;->bl<ref:17>ah:Lblah;\n" +
                        "    sget-object-volatile v0, Lblah;->bl<ref:18>ah:Lblah;\n" +
                        "    sget-short v0, Lblah;->bl<ref:19>ah:Lblah;\n" +
                        "    sget-volatile v0, Lblah;->bl<ref:20>ah:Lblah;\n" +
                        "    sget-wide v0, Lblah;->bl<ref:21>ah:Lblah;\n" +
                        "    sget-wide-volatile v0, Lblah;->bl<ref:22>ah:Lblah;\n" +
                        "    \n" +
                        "    iput v0, v0, Lblah;->bl<ref:23>ah:Lblah;\n" +
                        "    iput-object v0, v0, Lblah;->bl<ref:24>ah:Lblah;\n" +
                        "    iput-byte v0, v0, Lblah;->bl<ref:25>ah:Lblah;\n" +
                        "    iput-char v0, v0, Lblah;->bl<ref:26>ah:Lblah;\n" +
                        "    iput-object v0, v0, Lblah;->bl<ref:27>ah:Lblah;\n" +
                        "    iput-object-volatile v0, v0, Lblah;->bl<ref:28>ah:Lblah;\n" +
                        "    iput-short v0, v0, Lblah;->bl<ref:29>ah:Lblah;\n" +
                        "    iput-volatile v0, v0, Lblah;->bl<ref:30>ah:Lblah;\n" +
                        "    iput-wide v0, v0, Lblah;->bl<ref:31>ah:Lblah;\n" +
                        "    iput-wide-volatile v0, v0, Lblah;->bl<ref:32>ah:Lblah;\n" +
                        "    sput v0, Lblah;->bl<ref:33>ah:Lblah;\n" +
                        "    sput-boolean v0, Lblah;->bl<ref:34>ah:Lblah;\n" +
                        "    sput-byte v0, Lblah;->bl<ref:35>ah:Lblah;\n" +
                        "    sput-char v0, Lblah;->bl<ref:36>ah:Lblah;\n" +
                        "    sput-object v0, Lblah;->bl<ref:37>ah:Lblah;\n" +
                        "    sput-object-volatile v0, Lblah;->bl<ref:38>ah:Lblah;\n" +
                        "    sput-short v0, Lblah;->bl<ref:39>ah:Lblah;\n" +
                        "    sput-volatile v0, Lblah;->bl<ref:40>ah:Lblah;\n" +
                        "    sput-wide v0, Lblah;->bl<ref:41>ah:Lblah;\n" +
                        "    sput-wide-volatile v0, Lblah;->bl<ref:42>ah:Lblah;\n" +
                        "\n" +
                        "    throw-verification-error generic-error, Lblah;->bl<ref:43>ah:Lblah;\n" +
                        "\n" +
                        "    return-void\n" +
                        ".end method\n",
                1, SmaliUsageTypeProvider.LITERAL,
                2, SmaliUsageTypeProvider.LITERAL,
                3, UsageType.READ,
                4, UsageType.READ,
                5, UsageType.READ,
                6, UsageType.READ,
                7, UsageType.READ,
                8, UsageType.READ,
                9, UsageType.READ,
                10, UsageType.READ,
                11, UsageType.READ,
                12, UsageType.READ,
                13, UsageType.READ,
                14, UsageType.READ,
                15, UsageType.READ,
                16, UsageType.READ,
                17, UsageType.READ,
                18, UsageType.READ,
                19, UsageType.READ,
                20, UsageType.READ,
                21, UsageType.READ,
                22, UsageType.READ,
                23, UsageType.WRITE,
                24, UsageType.WRITE,
                25, UsageType.WRITE,
                26, UsageType.WRITE,
                27, UsageType.WRITE,
                28, UsageType.WRITE,
                29, UsageType.WRITE,
                30, UsageType.WRITE,
                31, UsageType.WRITE,
                32, UsageType.WRITE,
                33, UsageType.WRITE,
                34, UsageType.WRITE,
                35, UsageType.WRITE,
                36, UsageType.WRITE,
                37, UsageType.WRITE,
                38, UsageType.WRITE,
                39, UsageType.WRITE,
                40, UsageType.WRITE,
                41, UsageType.WRITE,
                42, UsageType.WRITE,
                43, SmaliUsageTypeProvider.VERIFICATION_ERROR);
    }
}
