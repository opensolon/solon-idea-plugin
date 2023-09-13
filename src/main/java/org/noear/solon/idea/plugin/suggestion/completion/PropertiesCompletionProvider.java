package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyImpl;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;

import java.util.ArrayList;
import java.util.List;

public class PropertiesCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        if (element instanceof PsiComment){
            return;
        }

        Project project = element.getProject();
        SuggestionService suggestionService = SuggestionService.getInstance(project);

        if (!suggestionService.canProvideSuggestions()){
            return;
        }

        Property property = getParentOfType(element, PropertyImpl.class);

        String queryWithDotDelimitedPrefixes = GenericUtil.truncateIdeaDummyIdentifier(element);
        List<LookupElementBuilder> elementBuilders = new ArrayList<>();
        if (property != null && element.getClass() == PropertyKeyImpl.class){
            elementBuilders = suggestionService.findPropertySuggestionsForQueryPrefix(queryWithDotDelimitedPrefixes);
        }else if(property != null && element.getClass() == PropertyValueImpl.class){
            elementBuilders = suggestionService.findHintSuggestionsForQueryPrefix(property.getKey(), queryWithDotDelimitedPrefixes);
        }
        assert elementBuilders != null;
        elementBuilders.forEach(resultSet::addElement);
    }

    private <T> T getParentOfType (PsiElement element , Class<T> clazz){
        PsiElement parent = element.getParent();
        if (parent.getClass() == clazz){
            return (T) parent;
        }
        return null;
    }
}
