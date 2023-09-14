package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;

import java.util.ArrayList;
import java.util.List;

public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        if (element instanceof PsiComment) {
            return;
        }

        Project project = element.getProject();
        SuggestionService suggestionService = SuggestionService.getInstance(project);

        if (!suggestionService.canProvideSuggestions()) {
            return;
        }

        YAMLPlainTextImpl yaml = getParentOfType(element, YAMLPlainTextImpl.class);

        String queryWithDotDelimitedPrefixes = GenericUtil.truncateIdeaDummyIdentifier(element);
        List<LookupElementBuilder> elementBuilders = new ArrayList<>();
        if (yaml != null && queryWithDotDelimitedPrefixes.contains(": ")) {
            elementBuilders = suggestionService.findHintSuggestionsForQueryPrefix(queryWithDotDelimitedPrefixes, queryWithDotDelimitedPrefixes);
        } else if (yaml != null) {
            elementBuilders = suggestionService.findYamlSuggestionsForQueryPrefix(queryWithDotDelimitedPrefixes);

        }
        assert elementBuilders != null;
        elementBuilders.forEach(resultSet::addElement);
    }

    private <T> T getParentOfType(PsiElement element, Class<T> clazz) {
        PsiElement parent = element.getParent();
        if (parent.getClass() == clazz) {
            return (T) parent;
        }
        return null;
    }
}
