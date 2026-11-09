package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.concurrency.AppExecutorUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetadataStartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 使用后台线程执行耗时操作
        DumbService.getInstance(project).runWhenSmart(() -> {
            ReadAction.nonBlocking(() -> {
                        Module[] modules = ModuleManager.getInstance(project).getModules();
                        // 多模块项目中配置文件可能位于任意模块，必须逐个刷新，
                        // 否则只有 modules[0] 具备元数据，其它模块的 yaml 无法跳转。
                        for (Module module : modules) {
                            if (module.isDisposed()) continue;
                            ModuleMetadataService service = module.getService(ModuleMetadataService.class);
                            if (service instanceof ModuleMetadataServiceImpl impl) {
                                impl.refreshMetadata();
                            }
                        }
                        return null;
                    })
                    .inSmartMode(project)
                    .submit(AppExecutorUtil.getAppExecutorService());
        });
        return Unit.INSTANCE;
    }
}
