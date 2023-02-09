package org.noear.solon.idea.plugin.suggestion.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

/**
 * Index the suggestion when project opened.
 */
public class SuggestionIndex implements ProjectManagerListener {

    @Override
    public void projectOpened(@NotNull Project project) {
        ProjectManagerListener.super.projectOpened(project);
    }
}
