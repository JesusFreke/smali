package org.jf.smalidea;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;

public class ClassRenameTest extends MultiFileTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    @NotNull
    @Override
    protected String getTestRoot() {
        return "/classRename/";
    }

    public void testBasicNoPackage() {
        doTest("blah", "blah2");
    }

    public void testBasicWithPackage() {
        doTest("my.blah", "blah2");
    }

    private void doTest(@NotNull final String oldQualifiedName, @NotNull final String newName) {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                doRename(oldQualifiedName, newName);
            }
        });
    }

    private void doRename(String oldQualifiedName, String newName) throws Exception {
        PsiClass testClass = myJavaFacade.findClass(oldQualifiedName, GlobalSearchScope.allScope(getProject()));

        RenameProcessor processor = new RenameProcessor(getProject(), testClass, newName, false, false);
        processor.run();

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();
    }

}
