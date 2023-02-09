package org.noear.solon.idea.plugin.suggestion.event;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;

import java.io.IOException;


public class SuggestionIndexEvent implements ProjectManagerListener {

    /**
     * Indexing the suggestion when project opened.
     */
    @Override
    public void projectOpened(@NotNull Project project) {

        SuggestionService service = SuggestionService.getInstance(project);

        try {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Project " + project.getName() + " is opened, indexing will start"));
            service.init(project);
        } catch (IOException e) {
            LoggerUtil.getLogger(this.getClass()).error(e);
        } finally {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Indexing complete for project " + project.getName()));
        }

    }
}
