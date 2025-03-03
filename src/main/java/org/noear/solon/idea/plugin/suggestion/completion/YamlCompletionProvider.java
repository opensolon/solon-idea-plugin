package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileOpenedSyncListener;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.filetype.SolonYamlFileType;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> implements FileOpenedSyncListener {

    private final String SUB_OPTION = ".";


    @Override
    public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file, @NotNull List<FileEditorWithProvider> editorsWithProviders) {
        // 检查文件是否是 YAML 文件
        if (file.getFileType().getName().equalsIgnoreCase(SolonYamlFileType.INSTANCE.getName())) {
            // 这里可以执行你的逻辑，当打开 YAML 文件时触发
            System.out.println("YAML file opened: " + file.getName());
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            Document document = fileDocumentManager.getDocument(file);
            assert document != null;
            String text = document.getText();
            try {
                Yaml yaml = new Yaml();
                Map yamlMap = yaml.load(text);
                YamlCompletionContributor.yamlMapCache.put(file.getName(), yamlMap);
            } catch (RuntimeException ignored) {
                LoggerUtil.debug(this.getClass(), logger -> logger.debug("yaml get fail"));

            }
            DocumentListener yamlDocumentListener = new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    DocumentListener.super.documentChanged(event);
                    String text = event.getDocument().getText();
                    try {
                        Yaml yaml = new Yaml();
                        Map yamlMap = yaml.load(text);
                        YamlCompletionContributor.yamlMapCache.put(file.getName(), yamlMap);
                    } catch (RuntimeException ignored) {

                    }
                }
            };
            document.addDocumentListener(yamlDocumentListener);
        }
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        if (element instanceof PsiComment) {
            return;
        }

//        SuggestionService suggestionService = getService(element);
        Project project = element.getProject();
        SuggestionService suggestionService = SuggestionService.getInstance(project);
        if (!suggestionService.canProvideSuggestions()) {
            return;
        }

        YAMLPlainTextImpl yaml = PsiTreeUtil.getParentOfType(element, YAMLPlainTextImpl.class);
        if (yaml == null) {
            return;
        }
        String queryWithDotDelimitedPrefixes = GenericUtil.truncateIdeaDummyIdentifier(element);
        List<LookupElementBuilder> elementBuilders = new ArrayList<>();
        String yamlKey = getYamlKey(yaml);
        if (queryWithDotDelimitedPrefixes.contains(".") || (yaml.getParent() != null && yaml.getParent().getClass() == YAMLBlockMappingImpl.class)) {
            yamlKey = (StringUtils.isEmpty(yamlKey) ? yamlKey : yamlKey + SUB_OPTION);
            queryWithDotDelimitedPrefixes = queryWithDotDelimitedPrefixes.equals(SUB_OPTION) ? yamlKey : yamlKey + queryWithDotDelimitedPrefixes;
            elementBuilders = suggestionService.findYamlSuggestionsForQueryPrefix(queryWithDotDelimitedPrefixes);
        } else {
            elementBuilders = suggestionService.findHintSuggestionsForQueryPrefix(yamlKey, queryWithDotDelimitedPrefixes);
        }
        elementBuilders.forEach(resultSet::addElement);
    }

    private String getYamlKey(YAMLPlainTextImpl yamlPlainText) {
        if (yamlPlainText == null) {
            return "";
        }
        List<String> keys = new ArrayList<>();
        PsiElement parent = yamlPlainText.getFirstChild();
        StringBuffer yamlKey = new StringBuffer();
        while (parent != null) {
            if (parent instanceof YAMLKeyValue) {
                YAMLKeyValue keyValue = (YAMLKeyValue) parent;
                String key = keyValue.getKeyText();
                keys.add(key);
            }
            try {
                parent = parent.getFirstChild();
            } catch (Exception ex) {
                parent = null;
            }
        }
        for (int i = keys.size() - 1; i >= 0; i--) {
            yamlKey.append(keys.get(i));
            if (i != 0) {
                yamlKey.append(":");
            }
        }
        return yamlKey.toString().replace(":", ".");
    }

    private <T> T getParentOfType(PsiElement element, Class<T> clazz) {
        PsiElement parent = element.getParent();
        if (parent.getClass() == clazz) {
            return (T) parent;
        }
        return (T) element;
    }


}
