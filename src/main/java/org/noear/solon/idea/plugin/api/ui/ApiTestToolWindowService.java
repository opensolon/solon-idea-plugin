package org.noear.solon.idea.plugin.api.ui;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.api.model.ApiEndpoint;

import java.util.List;

/**
 * 侧边栏面板的项目级持有者，保证 gutter 点击与 ToolWindow 创建共用同一个面板实例。
 */
@Service(Service.Level.PROJECT)
public final class ApiTestToolWindowService {

    public static final String TOOL_WINDOW_ID = "Solon API";

    private final Project project;
    private ApiTestPanel panel;

    public ApiTestToolWindowService(@NotNull Project project) {
        this.project = project;
    }

    public synchronized @NotNull ApiTestPanel getPanel() {
        if (panel == null) {
            panel = new ApiTestPanel(project);
        }
        return panel;
    }

    /**
     * 打开侧边栏并展示指定 Controller 的接口列表。
     */
    public void showEndpoints(@NotNull List<ApiEndpoint> endpoints, int initialIndex) {
        ApiTestPanel target = getPanel();
        target.setEndpoints(endpoints, initialIndex);

        ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (window != null) {
            window.show();
            window.activate(null, true);
        }
    }

    public static void show(@NotNull Project project,
                            @NotNull List<ApiEndpoint> endpoints,
                            int initialIndex) {
        if (project.isDisposed()) {
            return;
        }
        project.getService(ApiTestToolWindowService.class).showEndpoints(endpoints, initialIndex);
    }
}
