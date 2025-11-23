package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * Solon 启动活动，在项目打开时自动扫描 @SolonMain 启动类并创建运行配置
 */
public class SolonStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        // 延迟执行，确保 IDE 完全启动后再进行扫描
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.application.ApplicationManager.getApplication().runReadAction(() -> {
                SolonRunConfigurationService service = project.getService(SolonRunConfigurationService.class);
                if (service != null) {
                    service.scanAndCreateConfigurations();
                }
            });
        });
    }
}