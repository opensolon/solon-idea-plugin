package org.noear.solon.idea.plugin.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.SolonIcons;

/**
 * 运行 Solon 应用的动作
 */
public class RunSolonApplicationAction extends AnAction {

    private final Project project;
    private final PsiClass mainClass;

    public RunSolonApplicationAction(@NotNull Project project, @NotNull PsiClass mainClass) {
        super("Run Solon Application", "Run the Solon application", SolonIcons.SolonIcon_16x16);
        this.project = project;
        this.mainClass = mainClass;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        runSolonApplication();
    }

    private void runSolonApplication() {
        String className = mainClass.getQualifiedName();
        if (className == null) {
            return;
        }

        // 查找或创建运行配置
        RunConfiguration configuration = findOrCreateRunConfiguration(className);
        if (configuration != null) {
            // 运行配置
            RunManager.getInstance(project).setSelectedConfiguration(
                RunManager.getInstance(project).createConfiguration(configuration, configuration.getFactory())
            );
        }
    }

    @Nullable
    private RunConfiguration findOrCreateRunConfiguration(@NotNull String className) {
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
                mainClass.getName() + " (Solon)",
                configurationType.getConfigurationFactories()[0]
        ).getConfiguration();

        newConfig.setMainClass(className);
        newConfig.setVmParameters("-Dfile.encoding=UTF-8");

        // 添加到运行管理器
        runManager.addConfiguration(runManager.createConfiguration(newConfig, newConfig.getFactory()));
        runManager.setSelectedConfiguration(runManager.createConfiguration(newConfig, newConfig.getFactory()));

        return newConfig;
    }
}