package org.jf.smalidea.findUsages;

import com.intellij.usages.impl.rules.UsageType;

public class ClassUsageTypeTest extends UsageTypeTest {
    public ClassUsageTypeTest() {
        super(new SmaliUsageTypeProvider());
    }

    public void testClassUsageTypes() throws Exception {
        doTest("blah.smali", "" +
                        ".class public Lbl<ref:1>ah;\n" +
                        ".super Lbl<ref:2>ah;\n" +
                        ".implements Lbl<ref:3>ah;\n" +
                        "\n" +
                        ".annotation build Lbl<ref:22>ah;\n" +
                        "    value = .subannotation Lbl<ref:23>ah;\n" +
                        "                value = Lbl<ref:24>ah;\n" +
                        "            .end subannotation\n" +
                        ".end annotation\n" +
                        "\n" +
                        ".field static public blah:Lbl<ref:4>ah; = Lbl<ref:25>ah;\n" +
                        "\n" +
                        ".method public blah(Lbl<ref:5>ah;)Lbl<ref:6>ah;\n" +
                        "    .registers 2\n" +
                        "    .local p0, \"this\":Lbl<ref:7>ah;\n" +
                        "\n" +
                        "    :start\n" +
                        "        iget-object v0, v0, Lbl<ref:8>ah;->blah:Lbl<ref:9>ah;\n" +
                        "\n" +
                        "        invoke-virtual {v0}, Lbl<ref:10>ah;->blah(Lbl<ref:11>ah;)Lbl<ref:12>ah;\n" +
                        "\n" +
                        "        instance-of v0, v0, Lbl<ref:13>ah;\n" +
                        "        check-cast v0, Lbl<ref:14>ah;\n" +
                        "        new-instance v0, Lbl<ref:15>ah;\n" +
                        "        const-class v0, Lbl<ref:16>ah;\n" +
                        "        throw-verification-error generic-error, Lbl<ref:17>ah;\n" +
                        "\n" +
                        "        filled-new-array {v0, v0, v0, v0, v0}, Lbl<ref:18>ah;\n" +
                        "        new-array v0, v0, Lbl<ref:19>ah;\n" +
                        "        filled-new-array/range {v0}, Lbl<ref:20>ah;\n" +
                        "    :end\n" +
                        "\n" +
                        "    .catch Lbl<ref:21>ah; { :start .. :end } :handler\n" +
                        "    :handler\n" +
                        "    return-void\n" +
                        ".end method",
                1, SmaliUsageTypeProvider.CLASS_DECLARATION,
                2, UsageType.CLASS_EXTENDS_IMPLEMENTS_LIST,
                3, UsageType.CLASS_EXTENDS_IMPLEMENTS_LIST,
                4, UsageType.CLASS_FIELD_DECLARATION,
                5, UsageType.CLASS_METHOD_PARAMETER_DECLARATION,
                6, UsageType.CLASS_METHOD_RETURN_TYPE,
                7, UsageType.CLASS_LOCAL_VAR_DECLARATION,
                8, SmaliUsageTypeProvider.FIELD_DECLARING_TYPE_REFERENCE,
                9, SmaliUsageTypeProvider.FIELD_TYPE_REFERENCE,
                10, SmaliUsageTypeProvider.METHOD_DECLARING_TYPE_REFERENCE,
                11, SmaliUsageTypeProvider.METHOD_PARAM_REFERENCE,
                12, SmaliUsageTypeProvider.METHOD_RETURN_TYPE_REFERENCE,
                13, UsageType.CLASS_INSTANCE_OF,
                14, UsageType.CLASS_CAST_TO,
                15, UsageType.CLASS_NEW_OPERATOR,
                16, UsageType.CLASS_CLASS_OBJECT_ACCESS,
                17, SmaliUsageTypeProvider.VERIFICATION_ERROR,
                18, UsageType.CLASS_NEW_ARRAY,
                19, UsageType.CLASS_NEW_ARRAY,
                20, UsageType.CLASS_NEW_ARRAY,
                21, UsageType.CLASS_CATCH_CLAUSE_PARAMETER_DECLARATION,
                22, UsageType.ANNOTATION,
                23, UsageType.ANNOTATION,
                24, SmaliUsageTypeProvider.LITERAL,
                25, SmaliUsageTypeProvider.LITERAL);
    }
}
