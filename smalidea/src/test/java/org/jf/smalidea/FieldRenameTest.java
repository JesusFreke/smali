package org.jf.smalidea;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;

public class FieldRenameTest extends MultiFileTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    @NotNull
    @Override
    protected String getTestRoot() {
        return "/fieldRename/";
    }

    public void testFieldRename() {
        doTest("blah", "blah", "blort");
    }

    private void doTest(@NotNull final String containingClass, @NotNull final String oldFieldName,
                        @NotNull final String newFieldName) {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                doRename(containingClass, oldFieldName, newFieldName);
            }
        });
    }

    private void doRename(String containingClass, String oldFieldName, String newFieldName) throws Exception {
        PsiClass testClass = myJavaFacade.findClass(containingClass, GlobalSearchScope.allScope(getProject()));

        PsiField field = testClass.findFieldByName(oldFieldName, false);

        RenameProcessor processor = new RenameProcessor(getProject(), field, newFieldName, false, false);
        processor.run();

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();
    }
}
