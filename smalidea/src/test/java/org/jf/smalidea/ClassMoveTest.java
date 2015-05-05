package org.jf.smalidea;

import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.move.moveClassesOrPackages.AutocreatingSingleSourceRootMoveDestination;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClassMoveTest extends MultiFileTestCase {
    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    @NotNull
    @Override
    protected String getTestRoot() {
        return "/classMove/";
    }

    public void testBasicFromNoPackage() {
        doTest("blah", "my");
    }

    public void testBasicToNoPackage() {
        doTest("my.blah", "");
    }

    private void doTest(@NotNull final String oldQualifiedName, @NotNull final String newPackage) {
        doTest(new PerformAction() {
            @Override
            public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
                doMove(oldQualifiedName, newPackage);
            }
        });
    }

    private void doMove(String oldQualifiedName, final String newPackage) throws Exception {
        final PsiClass testClass = myJavaFacade.findClass(oldQualifiedName, GlobalSearchScope.allScope(getProject()));

        final List<VirtualFile> contentSourceRoots =
                JavaProjectRootsUtil.getSuitableDestinationSourceRoots(getProject());

        new MoveClassesOrPackagesProcessor(getProject(), new PsiClass[] {testClass},
                new AutocreatingSingleSourceRootMoveDestination(new PackageWrapper(getPsiManager(), newPackage),
                        contentSourceRoots.get(0)), false, false, null).run();
    }

}
