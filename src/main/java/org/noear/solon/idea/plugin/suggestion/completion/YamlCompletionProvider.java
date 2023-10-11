package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLBlockMappingImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;
import org.noear.solon.idea.plugin.common.util.GenericUtil;
import org.noear.solon.idea.plugin.suggestion.filetype.SolonYamlFileType;
import org.noear.solon.idea.plugin.suggestion.service.SuggestionService;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class YamlCompletionProvider extends CompletionProvider<CompletionParameters> implements FileEditorManagerListener {

    private final String SUB_OPTION = ".";

    private SuggestionService suggestionService;
    private DocumentListener yamlDocumentListener;
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 检查文件是否是 YAML 文件
        if (file.getFileType().getName().equalsIgnoreCase(SolonYamlFileType.INSTANCE.getName())) {
            // 这里可以执行你的逻辑，当打开 YAML 文件时触发
            System.out.println("YAML file opened: " + file.getName());
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            Document document = fileDocumentManager.getDocument(file);
            assert document != null;
            String text = document.getText();
            try{
                Yaml yaml = new Yaml();
                Map yamlMap = yaml.load(text);
                YamlCompletionContributor.yamlMapCache.put(file.getName(),yamlMap);
            }catch (RuntimeException ignored){

            }
            yamlDocumentListener=new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    DocumentListener.super.documentChanged(event);
                    String text = event.getDocument().getText();
                    try{
                        Yaml yaml = new Yaml();
                        Map yamlMap = yaml.load(text);
                        YamlCompletionContributor.yamlMapCache.put(file.getName(),yamlMap);
                    }catch (RuntimeException ignored){

                    }
                }
            };
            document.addDocumentListener(yamlDocumentListener);
        }
    }

//    @Override
//    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//        // 检查文件是否是 YAML 文件
//        if (file.getFileType().getName().equalsIgnoreCase(SolonYamlFileType.INSTANCE.getName())) {
//            // 这里可以执行你的逻辑，当打开 YAML 文件时触发
//            System.out.println("YAML file opened: " + file.getName());
//            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
//            Document document = fileDocumentManager.getDocument(file);
//            assert document != null;
//            YamlCompletionContributor.yamlMapCache.remove(file.getName());
//            assert yamlDocumentListener != null;
//            document.removeDocumentListener(yamlDocumentListener);
//        }
//    }

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

//        YAMLPlainTextImpl yaml = getParentOfType(element, YAMLPlainTextImpl.class);
        YAMLPlainTextImpl yaml = PsiTreeUtil.getParentOfType(element, YAMLPlainTextImpl.class);
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

    private SuggestionService getService(PsiElement element) {
        return Optional.ofNullable(suggestionService).orElseGet(() -> {
            Project project = element.getProject();
            SuggestionService suggestionService = SuggestionService.getInstance(project);
            return suggestionService;
        });
    }


    private String getYamlKey(YAMLPlainTextImpl yamlPlainText) {
        if (yamlPlainText == null) {
            return "";
        }
        List<String> keys = new ArrayList<>();
        PsiElement parent = yamlPlainText.getParent();
        StringBuffer yamlKey = new StringBuffer();
        while (parent != null) {
            if (parent instanceof YAMLKeyValue) {
                YAMLKeyValue keyValue = (YAMLKeyValue) parent;
                String key = keyValue.getKeyText();
                keys.add(key);
            }
            try {
                parent = parent.getParent();
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
