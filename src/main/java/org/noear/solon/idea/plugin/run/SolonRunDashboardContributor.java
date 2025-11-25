package org.noear.solon.idea.plugin.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Solon 运行配置服务，用于管理 Solon 应用的运行配置
 */
public class SolonRunDashboardContributor {

    private static final String TYPE_ID = "SolonApplication";

    /**
     * 刷新运行配置
     */
    public static void refreshDashboard(@NotNull Project project) {
        RunDashboardManager manager = RunDashboardManager.getInstance(project);
        if (manager != null) {
            manager.updateDashboard(false);
        }
    }

    /**
     * 检查项目是否包含 Solon 运行配置
     */
    public boolean hasSolonConfigurations(@NotNull Project project) {
        return !ContainerUtil.isEmpty(getSolonConfigurations(project));
    }

    /**
     * 获取项目中所有的 Solon 运行配置
     */
    public @NotNull List<RunConfiguration> getSolonConfigurations(@NotNull Project project) {
        return ContainerUtil.filter(RunManager.getInstance(project).getAllConfigurationsList(),
                configuration -> configuration instanceof SolonRunConfiguration);
    }

    /**
     * 获取配置类型 ID
     */
    public @NotNull String getType() {
        return TYPE_ID;
    }
}
