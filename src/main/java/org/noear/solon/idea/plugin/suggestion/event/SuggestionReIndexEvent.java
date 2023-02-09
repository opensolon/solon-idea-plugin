package org.noear.solon.idea.plugin.suggestion.event;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;


public class SuggestionReIndexEvent implements CompilationStatusListener {

    /**
     * Reindexing the suggestion when compilation finished.
     */
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {

        Project project = compileContext.getProject();
        SuggestionService service = SuggestionService.getInstance(project);

        LoggerUtil.debug(this.getClass(), logger -> logger.debug("Received compilation status event for project " + project.getName()));
        if (errors == 0) {
            CompileScope projectCompileScope = compileContext.getProjectCompileScope();
            CompileScope compileScope = compileContext.getCompileScope();
            if (projectCompileScope == compileScope) {
                service.reIndex(project);
            } else {
                service.reIndex(project, compileContext.getCompileScope().getAffectedModules());
            }
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("Compilation status processed for project " + project.getName()));
        } else {
            LoggerUtil.debug(this.getClass(), logger -> logger
                    .debug("Skipping reindexing completely as there are " + errors + " errors"));
        }
    }
}
