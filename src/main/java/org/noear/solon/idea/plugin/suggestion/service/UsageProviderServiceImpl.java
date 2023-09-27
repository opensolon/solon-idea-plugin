package org.noear.solon.idea.plugin.suggestion.service;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Used to handle various classes, methods, and fields marked as follows in the Solon framework
 * 1.Class is never used
 * 2.assigned but not used
 * 3.initialized to a non-null value
 *
 * @author hans
 */
public class UsageProviderServiceImpl implements ImplicitUsageProvider {
    private final String[] CLASS_ANNOTATION = new String[]{"org.noear.solon.annotation.Controller","org.noear.solon.annotation.Configuration","org.noear.solon.annotation.Component"};
    private final String[] METHOD_ANNOTATION = new String[]{"org.noear.solon.annotation.Mapping","org.noear.solon.annotation.Bean"};


    /**
     * Class is never used
     * @param element target element
     * @return is never used
     */
    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        try {
            PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (psiMethod == null) {
                PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
                return hasAnnotation(psiClass);
            }
            return hasAnnotation(psiMethod);
        }catch (RuntimeException exception){
            return false;
        }

    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        try{
            return ((PsiField) element).hasAnnotation("org.noear.solon.annotation.Inject");
        }catch (ClassCastException exception){
            return false;
        }
    }

    /**
     * 判断目标类是否包含指定注解
     * Determine whether the target class contains the specified annotation
     *
     * @param element class
     * @return contains
     */
    public boolean hasAnnotation(PsiClass element) {
        if (element == null) {
            return false;
        }
        for (String annotation : CLASS_ANNOTATION) {
            if (element.hasAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断目标方法是否包含指定注解
     * Determine whether the target method contains the specified annotation
     *
     * @param element    method
     * @return contains
     */
    public boolean hasAnnotation(PsiMethod element) {
        if (element == null) {
            return false;
        }
        for (String annotation : METHOD_ANNOTATION) {
            if (element.hasAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }
}
