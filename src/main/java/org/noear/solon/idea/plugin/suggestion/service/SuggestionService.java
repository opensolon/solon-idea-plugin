package org.noear.solon.idea.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public interface SuggestionService {

    static SuggestionService getInstance(@NotNull Project project) {
        return project.getService(SuggestionService.class);
    }

    void init(Project project) throws IOException;

    void reIndex(Project project);

    void reIndex(Project project, Module[] modules);

    void reIndex(Project project, Module module);

    @Nullable
    List<LookupElementBuilder> findPropertySuggestionsForQueryPrefix(String queryWithDotDelimitedPrefixes);

    @Nullable
    List<LookupElementBuilder> findHintSuggestionsForQueryPrefix(String key, String queryWithDotDelimitedPrefixes);

    @Nullable
    List<LookupElementBuilder> findYamlSuggestionsForQueryPrefix(String queryWithDotDelimitedPrefixes);
    boolean canProvideSuggestions();
}
