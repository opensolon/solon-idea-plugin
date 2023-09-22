package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> {

    private final String SUB_OPTION=".";

    private SuggestionService suggestionService;
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        if (element instanceof PsiComment) {
            return;
        }

        SuggestionService suggestionService = getService(element);

        if (!suggestionService.canProvideSuggestions()) {
            return;
        }

        YAMLPlainTextImpl yaml = getParentOfType(element, YAMLPlainTextImpl.class);

        String queryWithDotDelimitedPrefixes = GenericUtil.truncateIdeaDummyIdentifier(element);
        List<LookupElementBuilder> elementBuilders = new ArrayList<>();
        String yamlKey = getYamlKey(yaml);
        if (yaml.getParent().getClass() == YAMLKeyValueImpl.class) {
            elementBuilders = suggestionService.findHintSuggestionsForQueryPrefix(yamlKey, queryWithDotDelimitedPrefixes);
        } else{
            yamlKey = (StringUtils.isEmpty(yamlKey)?yamlKey:yamlKey+SUB_OPTION);
            queryWithDotDelimitedPrefixes=queryWithDotDelimitedPrefixes.equals(SUB_OPTION)?yamlKey:yamlKey+queryWithDotDelimitedPrefixes;
            elementBuilders = suggestionService.findYamlSuggestionsForQueryPrefix(queryWithDotDelimitedPrefixes);
        }
        elementBuilders.forEach(resultSet::addElement);
    }

    private SuggestionService getService(PsiElement element){
        return Optional.ofNullable(suggestionService).orElseGet(() -> {
            Project project = element.getProject();
            SuggestionService suggestionService = SuggestionService.getInstance(project);
            return suggestionService;
        });
    }


    private String getYamlKey(YAMLPlainTextImpl yamlPlainText){
        List<String> keys = new ArrayList<>();
        PsiElement parent = yamlPlainText.getParent();
        StringBuffer yamlKey=new StringBuffer();
        while (parent != null) {
            if (parent instanceof YAMLKeyValue) {
                YAMLKeyValue keyValue = (YAMLKeyValue) parent;
                String key = keyValue.getKeyText();
                keys.add(key);
            }
            try {
                parent = parent.getParent();
            }catch (Exception ex){
                parent=null;
            }
        }
        for (int i = keys.size()-1 ; i >= 0; i--) {
            yamlKey.append(keys.get(i));
            if(i!=0){
                yamlKey.append(":");
            }
        }
        return  yamlKey.toString().replace(":",".");
    }

    private <T> T getParentOfType(PsiElement element, Class<T> clazz) {
        PsiElement parent = element.getParent();
        if (parent.getClass() == clazz) {
            return (T) parent;
        }
        return null;
    }
}
