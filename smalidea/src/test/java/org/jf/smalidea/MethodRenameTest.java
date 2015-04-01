package org.jf.smalidea;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;

public class MethodRenameTest extends MultiFileTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    @NotNull
    @Override
    protected String getTestRoot() {
        return "/methodRename/";
    }

    public void testMethodRename() {
        doTest("blah", "blah", "blort");
    }

    private void doTest(@NotNull final String containingClass, @NotNull final String oldMethodName,
                        @NotNull final String newMethodName) {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                doRename(containingClass, oldMethodName, newMethodName);
            }
        });
    }

    private void doRename(String containingClass, String oldMethodName, String newMethodName) throws Exception {
        PsiClass testClass = myJavaFacade.findClass(containingClass, GlobalSearchScope.allScope(getProject()));

        PsiMethod method = testClass.findMethodsByName(oldMethodName, false)[0];

        RenameProcessor processor = new RenameProcessor(getProject(), method, newMethodName, false, false);
        processor.run();

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        FileDocumentManager.getInstance().saveAllDocuments();
    }
}
