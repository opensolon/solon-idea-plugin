package org.noear.solon.idea.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SuggestionServiceImpl implements SuggestionService{

    @Override
    public void init(Project project) throws IOException {

    }

    @Override
    public void reIndex(Project project) {

    }

    @Override
    public void reindex(Project project, Module[] modules) {

    }

    @Override
    public void reindex(Project project, Module module) {

    }

    @Override
    public @Nullable List<LookupElementBuilder> findSuggestionsForQueryPrefix(Project project, Module module, FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude) {
        return null;
    }

}
