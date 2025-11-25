package org.noear.solon.idea.plugin.run;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
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
        SolonRunConfigurationService service = project.getService(SolonRunConfigurationService.class);
        if (service != null) {
            service.runConfiguration(mainClass);
        }
    }
}
