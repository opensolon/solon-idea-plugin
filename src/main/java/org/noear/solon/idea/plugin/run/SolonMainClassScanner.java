package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扫描服务，用于检测项目中带有 @SolonMain 注解的启动类
 */
@Service(Service.Level.PROJECT)
public final class SolonMainClassScanner {

    private final Project project;
    private final ConcurrentHashMap<String, PsiClass> cachedMainClasses = new ConcurrentHashMap<>();

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
        List<PsiClass> mainClasses = new ArrayList<>();

        // 使用PSI搜索所有Java文件
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);

        // 搜索所有类
        PsiClass[] allClasses = cache.getClassesByName("*", projectScope);

        for (PsiClass psiClass : allClasses) {
            if (hasSolonMainAnnotation(psiClass)) {
                mainClasses.add(psiClass);
            }
        }

        return mainClasses;
    }

    /**
     * 检查类是否有 @SolonMain 注解
     */
    private boolean hasSolonMainAnnotation(@NotNull PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.endsWith("SolonMain")) {
                return true;
            }
        }
        return false;
    }
}