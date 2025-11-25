package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Solon 启动活动，在项目打开时自动扫描 @SolonMain 启动类并创建运行配置
 */
public class SolonStartupActivity implements ProjectActivity {

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().invokeLater(() ->
                ApplicationManager.getApplication().runReadAction(() -> {
                    SolonRunConfigurationService service = project.getService(SolonRunConfigurationService.class);
                    if (service != null) {
                        service.scanAndCreateConfigurations();
                    }
                })
        );
        return Unit.INSTANCE;
    }
}
