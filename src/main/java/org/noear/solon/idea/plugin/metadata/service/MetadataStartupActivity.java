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
                        for (Module module : ModuleManager.getInstance(project).getModules()) {
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
