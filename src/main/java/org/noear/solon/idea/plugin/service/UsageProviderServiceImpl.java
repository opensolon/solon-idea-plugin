package org.noear.solon.idea.plugin.service;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
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
    private final String[] CLASS_ANNOTATION = new String[]{"org.noear.solon.annotation.Controller", "org.noear.solon.annotation.Configuration", "org.noear.solon.annotation.Component"};
    private final String[] METHOD_ANNOTATION = new String[]{"org.noear.solon.annotation.Mapping", "org.noear.solon.annotation.Bean"};
    private final String[] FIELD_ANNOTATION = new String[]{"org.noear.solon.data.annotation.Ds", "org.apache.ibatis.solon.annotation.Db", "com.jfinal.plugin.activerecord.solon.annotation.Db", "org.hibernate.solon.annotation.Db", "org.noear.solon.annotation.Inject"};


    /**
     * Class is never used
     *
     * @param element target element
     * @return is never used
     */
    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof PsiField) {
            return hasAnnotation((PsiField) element);
        }
        // 如果 element 本身是方法，直接判断注解
        if (element instanceof PsiMethod) {
            return hasAnnotation((PsiMethod) element);
        }

        // 否则去找父方法，strict=false 包含自身
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
        if (psiMethod != null) {
            return hasAnnotation(psiMethod);
        }

        // 查找类，strict=false 包含自身
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        return hasAnnotation(psiClass);
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        try {
            return hasAnnotation((PsiField) element);
        } catch (ClassCastException exception) {
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
     * @param element method
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

    /**
     * 判断目标属性是否包含指定注解
     * Determine whether the target method contains the specified annotation
     *
     * @param element method
     * @return contains
     */
    public boolean hasAnnotation(PsiField element) {
        if (element == null) {
            return false;
        }
        for (String annotation : FIELD_ANNOTATION) {
            if (element.hasAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }
}