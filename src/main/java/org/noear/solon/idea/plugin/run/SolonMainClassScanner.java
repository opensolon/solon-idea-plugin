package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 扫描服务，用于检测项目中带有 @SolonMain 注解的启动类
 */
@Service(Service.Level.PROJECT)
public final class SolonMainClassScanner {

    private final Project project;

    public SolonMainClassScanner(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 扫描项目中的所有 @SolonMain 启动类
     *
     * @return 启动类列表
     */
    @NotNull
    public List<PsiClass> findSolonMainClasses() {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
        String[] allClassNames = cache.getAllClassNames();
        if (allClassNames.length == 0) {
            return Collections.emptyList();
        }

        List<PsiClass> mainClasses = new ArrayList<>();
        for (String className : allClassNames) {
            PsiClass[] classesByName = cache.getClassesByName(className, projectScope);
            for (PsiClass psiClass : classesByName) {
                if (psiClass != null && psiClass.isValid() && SolonRunUtils.hasSolonMainAnnotation(psiClass)) {
                    mainClasses.add(psiClass);
                }
            }
        }

        return mainClasses;
    }
}
