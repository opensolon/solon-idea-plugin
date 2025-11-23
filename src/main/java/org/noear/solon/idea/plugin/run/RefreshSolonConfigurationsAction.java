package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * 刷新 Solon 运行配置的动作
 */
public class RefreshSolonConfigurationsAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            SolonRunConfigurationService service = project.getService(SolonRunConfigurationService.class);
            if (service != null) {
                service.clearAllConfigurations();
                service.scanAndCreateConfigurations();
                Messages.showInfoMessage(project, "Solon 运行配置已刷新", "刷新完成");
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
}