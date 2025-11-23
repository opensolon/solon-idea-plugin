package org.noear.solon.idea.plugin.run;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
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

        // 创建行标记信息
        return new LineMarkerInfo<>(
                psiClass.getNameIdentifier(),
                psiClass.getTextRange(),
                SolonIcons.SolonIcon_16x16,
                psiElement -> "Solon Application",
                null, // 暂时不设置点击处理器
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
        String className = psiClass.getQualifiedName();
        if (className == null) {
            return;
        }

        // 查找或创建运行配置
        RunConfiguration configuration = findOrCreateRunConfiguration(psiClass.getProject(), className, psiClass);
        if (configuration != null) {
            // 运行配置
            RunManager.getInstance(psiClass.getProject()).setSelectedConfiguration(
                RunManager.getInstance(psiClass.getProject()).createConfiguration(configuration, configuration.getFactory())
            );
        }
    }

    /**
     * 查找或创建运行配置
     */
    @Nullable
    private RunConfiguration findOrCreateRunConfiguration(@NotNull com.intellij.openapi.project.Project project,
                                                         @NotNull String className, @NotNull PsiClass psiClass) {
        RunManager runManager = RunManager.getInstance(project);

        // 查找现有配置
        for (RunConfiguration config : runManager.getAllConfigurationsList()) {
            if (config instanceof SolonRunConfiguration) {
                SolonRunConfiguration solonConfig = (SolonRunConfiguration) config;
                if (className.equals(solonConfig.getMainClass())) {
                    return solonConfig;
                }
            }
        }

        // 创建新配置
        SolonConfigurationType configurationType = com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType(SolonConfigurationType.class);
        SolonRunConfiguration newConfig = (SolonRunConfiguration) runManager.createConfiguration(
                psiClass.getName() + " (Solon)",
                configurationType.getConfigurationFactories()[0]
        ).getConfiguration();

        newConfig.setMainClass(className);
        newConfig.setVmParameters("-Dfile.encoding=UTF-8");

        // 添加到运行管理器
        runManager.addConfiguration(runManager.createConfiguration(newConfig, newConfig.getFactory()));
        runManager.setSelectedConfiguration(runManager.createConfiguration(newConfig, newConfig.getFactory()));

        return newConfig;
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