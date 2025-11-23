package org.noear.solon.idea.plugin.run;

import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Solon 运行配置服务，负责管理自动创建的运行配置
 */
@Service(Service.Level.PROJECT)
public final class SolonRunConfigurationService {

    private final Project project;
    private final SolonMainClassScanner scanner;

    public SolonRunConfigurationService(@NotNull Project project) {
        this.project = project;
        this.scanner = new SolonMainClassScanner(project);
    }

    /**
     * 扫描并创建运行配置
     */
    public void scanAndCreateConfigurations() {
        List<PsiClass> mainClasses = scanner.findSolonMainClasses();
        RunManager runManager = RunManager.getInstance(project);

        for (PsiClass mainClass : mainClasses) {
            String className = mainClass.getQualifiedName();
            if (className != null && !configurationExists(className)) {
                createRunConfiguration(mainClass);
            }
        }
    }

    /**
     * 检查配置是否已存在
     */
    private boolean configurationExists(@NotNull String className) {
        RunManager runManager = RunManager.getInstance(project);
        for (RunConfiguration config : runManager.getAllConfigurationsList()) {
            if (config instanceof SolonRunConfiguration) {
                SolonRunConfiguration solonConfig = (SolonRunConfiguration) config;
                if (className.equals(solonConfig.getMainClass())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 创建运行配置
     */
    private void createRunConfiguration(@NotNull PsiClass mainClass) {
        String className = mainClass.getQualifiedName();
        String configName = mainClass.getName() + " (Solon)";

        RunManager runManager = RunManager.getInstance(project);
        SolonConfigurationType configurationType = ConfigurationTypeUtil.findConfigurationType(SolonConfigurationType.class);
        SolonRunConfiguration configuration = (SolonRunConfiguration) runManager.createConfiguration(configName, configurationType.getConfigurationFactories()[0]).getConfiguration();

        configuration.setMainClass(className);
        configuration.setVmParameters("-Dfile.encoding=UTF-8");

        // 添加到运行管理器
        runManager.addConfiguration(runManager.createConfiguration(configuration, configuration.getFactory()));

        // 设置为选中状态
        runManager.setSelectedConfiguration(runManager.createConfiguration(configuration, configuration.getFactory()));

        // 刷新运行配置
        SolonRunDashboardContributor.refreshDashboard(project);
    }

    /**
     * 清除所有自动创建的 Solon 运行配置
     */
    public void clearAllConfigurations() {
        RunManager runManager = RunManager.getInstance(project);
        List<RunConfiguration> configurations = runManager.getAllConfigurationsList();

        for (RunConfiguration config : configurations) {
            if (config instanceof SolonRunConfiguration) {
                runManager.removeConfiguration(runManager.createConfiguration(config, config.getFactory()));
            }
        }
    }
}