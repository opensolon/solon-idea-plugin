package org.noear.solon.idea.plugin.run;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.SolonIcons;

import java.util.Collection;
import java.util.List;

/**
 * 为 @SolonMain 启动类添加行标记（图标）
 */
public class SolonMainClassLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiClass)) {
            return null;
        }

        PsiClass psiClass = (PsiClass) element;

        // 检查是否是主类（顶级类）
        if (psiClass.getContainingClass() != null) {
            return null;
        }

        // 检查是否有 @SolonMain 注解
        if (!hasSolonMainAnnotation(psiClass)) {
            return null;
        }

        PsiElement nameIdentifier = psiClass.getNameIdentifier();
        if (nameIdentifier == null) {
            return null;
        }

        PsiClass targetClass = psiClass;

        // 创建行标记信息
        return new LineMarkerInfo<>(
                nameIdentifier,
                nameIdentifier.getTextRange(),
                SolonIcons.SolonIcon_16x16,
                psiElement -> "Solon Application",
                (mouseEvent, element1) -> runSolonApplication(targetClass),
                GutterIconRenderer.Alignment.LEFT,
                () -> "Run Solon Application"
        );
    }


    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            LineMarkerInfo<?> lineMarker = getLineMarkerInfo(element);
            if (lineMarker != null) {
                result.add(lineMarker);
            }
        }
    }

    /**
     * 运行 Solon 应用
     */
    private void runSolonApplication(@NotNull PsiClass psiClass) {
        SolonRunConfigurationService service = psiClass.getProject().getService(SolonRunConfigurationService.class);
        if (service != null) {
            service.runConfiguration(psiClass);
        }
    }

    /**
     * 检查类是否有 @SolonMain 注解
     */
    private boolean hasSolonMainAnnotation(@NotNull PsiClass psiClass) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && (qualifiedName.endsWith("SolonMain") || qualifiedName.equals("SolonMain"))) {
                return true;
            }
        }
        return false;
    }
}
