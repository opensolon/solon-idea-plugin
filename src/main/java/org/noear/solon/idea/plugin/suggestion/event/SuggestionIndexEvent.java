package org.noear.solon.idea.plugin.suggestion.event;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;

import java.io.IOException;


public class SuggestionIndexEvent implements  ProjectActivity {

    /**
     * Indexing the suggestion when project opened.
     */
//    @Override
//    public void runActivity(@NotNull Project project) {
//        SuggestionService service = SuggestionService.getInstance(project);
//
//        try {
//            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Project " + project.getName() + " is opened, indexing will start"));
//            service.init(project);
//        } catch (IOException e) {
//            LoggerUtil.getLogger(this.getClass()).error(e);
//        } finally {
//            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Indexing complete for project " + project.getName()));
//        }
//    }

    /**
     * Indexing the suggestion when project opened.
     */
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        SuggestionService service = SuggestionService.getInstance(project);

        try {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Project " + project.getName() + " is opened, indexing will start"));
            service.init(project);
        } catch (IOException e) {
            LoggerUtil.getLogger(this.getClass()).error(e);
        } finally {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Indexing complete for project " + project.getName()));
        }
        return null;
    }
}
