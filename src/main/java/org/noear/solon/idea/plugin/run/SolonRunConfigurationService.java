package org.noear.solon.idea.plugin.run;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        for (PsiClass mainClass : mainClasses) {
            ensureRunConfiguration(mainClass);
        }
    }

    /**
     * 运行指定的 Solon 主类
     */
    public void runConfiguration(@NotNull PsiClass mainClass) {
        RunnerAndConfigurationSettings settings = ensureRunConfiguration(mainClass);
        if (settings == null) {
            return;
        }

        RunManager runManager = RunManager.getInstance(project);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    /**
     * 确保运行配置存在，如不存在则创建
     */
    @Nullable
    public RunnerAndConfigurationSettings ensureRunConfiguration(@NotNull PsiClass mainClass) {
        String className = mainClass.getQualifiedName();
        if (className == null) {
            return null;
        }

        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings existing = findConfigurationSettings(runManager, className);
        if (existing != null) {
            return existing;
        }

        return createRunConfiguration(runManager, mainClass, className);
    }

    @Nullable
    private RunnerAndConfigurationSettings findConfigurationSettings(@NotNull RunManager runManager,
                                                                     @NotNull String className) {
        for (RunnerAndConfigurationSettings settings : runManager.getAllSettings()) {
            RunConfiguration configuration = settings.getConfiguration();
            if (configuration instanceof SolonRunConfiguration) {
                SolonRunConfiguration solonConfiguration = (SolonRunConfiguration) configuration;
                String existingClass = solonConfiguration.getMainClassName();
                if (className.equals(existingClass)) {
                    return settings;
                }
            }
        }
        return null;
    }

    @Nullable
    private RunnerAndConfigurationSettings createRunConfiguration(@NotNull RunManager runManager,
                                                                  @NotNull PsiClass mainClass,
                                                                  @NotNull String className) {
        SolonConfigurationType configurationType = ConfigurationTypeUtil.findConfigurationType(SolonConfigurationType.class);
        ConfigurationFactory factory = configurationType.getConfigurationFactories()[0];

        String configName = mainClass.getName() + " (Solon)";
        RunnerAndConfigurationSettings settings = runManager.createConfiguration(configName, factory);
        RunConfiguration runConfiguration = settings.getConfiguration();
        if (!(runConfiguration instanceof SolonRunConfiguration)) {
            return null;
        }

        SolonRunConfiguration configuration = (SolonRunConfiguration) runConfiguration;
        configuration.setMainClass(mainClass);
        configuration.setVMParameters("-Dfile.encoding=UTF-8");

        runManager.addConfiguration(settings);
        SolonRunDashboardContributor.refreshDashboard(project);
        return settings;
    }

    /**
     * 清除所有自动创建的 Solon 运行配置
     */
    public void clearAllConfigurations() {
        RunManager runManager = RunManager.getInstance(project);
        for (RunnerAndConfigurationSettings settings : runManager.getAllSettings()) {
            RunConfiguration configuration = settings.getConfiguration();
            if (configuration instanceof SolonRunConfiguration) {
                runManager.removeConfiguration(settings);
            }
        }
    }
}
