package org.jf.smalidea.dexlib.analysis;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.ResolveScopeManager;
import org.jf.dexlib2.analysis.ClassProvider;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.smalidea.dexlib.SmalideaClassDef;
import org.jf.smalidea.util.NameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SmalideaClassProvider implements ClassProvider {
    private final Project project;
    private final VirtualFile file;

    public SmalideaClassProvider(@Nonnull Project project, @Nonnull VirtualFile file) {
        this.project = project;
        this.file = file;
    }

    @Nullable @Override public ClassDef getClassDef(String type) {
        ResolveScopeManager manager = ResolveScopeManager.getInstance(project);
        PsiClass psiClass = NameUtils.resolveSmaliType(project, manager.getDefaultResolveScope(file), type);
        if (psiClass != null) {
            return new SmalideaClassDef(psiClass);
        }
        return null;
    }
}
