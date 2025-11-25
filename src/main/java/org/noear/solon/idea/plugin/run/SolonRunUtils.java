package org.noear.solon.idea.plugin.run;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

/**
 * Solon 运行相关的 Psi 辅助方法。
 */
public final class SolonRunUtils {

    private static final String MAIN_ANNOTATION = "org.noear.solon.annotation.SolonMain";

    private SolonRunUtils() {
    }

    public static boolean hasSolonMainAnnotation(@NotNull PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && qualifiedName.equalsIgnoreCase(MAIN_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }
}
