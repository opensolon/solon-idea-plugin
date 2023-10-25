package org.noear.solon.idea.plugin.suggestion.service;

import com.alibaba.fastjson2.JSON;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.completion.YamlCompletionContributor;
import org.noear.solon.idea.plugin.suggestion.metadata.MetadataContainer;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadata;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataHint;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataHintValue;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataProperty;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;

public class SuggestionServiceImpl implements SuggestionService {

    private final Trie<String, SolonConfigurationMetadataProperty> propertiesSearchIndex;
    private final Trie<String, SolonConfigurationMetadataHint> hintsSearchIndex;

    private Future<?> currentExecution;
    private volatile boolean indexingInProgress;

    private final String DELIMITER = "\\.";
    private final String LINE = "\n";
    private final String YML_FORMAT = "  ";
    private final String YML_DELIMITER = ":";

    public SuggestionServiceImpl() {
        this.propertiesSearchIndex = new PatriciaTrie<>();
        this.hintsSearchIndex = new PatriciaTrie<>();
    }

    private void clearSearchIndex() {
        this.propertiesSearchIndex.clear();
        this.hintsSearchIndex.clear();
    }

    @Override
    public void init(Project project) throws IOException {
        reIndex(project);
    }

    @Override
    public void reIndex(Project project) {
        reIndex(project, ModuleManager.getInstance(project).getModules());
    }

    @Override
    public void reIndex(Project project, Module module) {
        reIndex(project, new Module[]{module});
    }

    @Override
    public void reIndex(Project project, Module[] modules) {
        if (indexingInProgress) {
            if (currentExecution != null) {
                currentExecution.cancel(false);
            }
        }
        currentExecution = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                LoggerUtil.debug(this.getClass(), logger -> logger.debug(
                        "-> Indexing requested for a subset of modules of project " + project.getName()));
                indexingInProgress = true;
                StopWatch timer = new StopWatch();
                timer.start();
                clearSearchIndex();
                try {
                    for (Module module : modules) {
                        LoggerUtil.debug(this.getClass(), logger -> logger.debug("--> Indexing requested for module " + module.getName()));
                        StopWatch moduleTimer = new StopWatch();
                        moduleTimer.start();
                        try {
                            loadModuleSearchIndex(module);
                        } finally {
                            moduleTimer.stop();
                            LoggerUtil.debug(this.getClass(), logger -> logger.debug(
                                    "<-- Indexing took " + moduleTimer + " for module " + module
                                            .getName()));
                        }
                    }
                } finally {
                    indexingInProgress = false;
                    timer.stop();
                    LoggerUtil.debug(this.getClass(), logger -> logger
                            .debug("<- Indexing took " + timer + " for project " + project.getName()));
                }
            });
        });
    }


    private void loadModuleSearchIndex(Module module) {
        /**
         * Order entries include SDK, libraries and other modules the module uses.
         * https://plugins.jetbrains.com/docs/intellij/module.html#how-do-i-get-dependencies-and-classpath-of-a-module
         */
        OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

        List<MetadataContainer> containersToProcess =
                computeNewContainersToProcess(moduleOrderEnumerator);

        processContainers(module, containersToProcess);
    }

    private void processContainers(Module module, List<MetadataContainer> containersToProcess) {
        for (MetadataContainer metadataContainer : containersToProcess) {
            String metadataFilePath = metadataContainer.getFileUrl();
            try (InputStream inputStream = metadataContainer.getMetadataFile().getInputStream()) {
                SolonConfigurationMetadata solonConfigurationMetadata = JSON.parseObject(new BufferedReader(new InputStreamReader(inputStream)), SolonConfigurationMetadata.class);
                buildMetadataHierarchy(module, solonConfigurationMetadata);
            } catch (IOException e) {
                LoggerUtil.getLogger(this.getClass()).infoWithDebug("Exception encountered while processing metadata file: " + metadataFilePath, e);
            }
        }
    }

    private void buildMetadataHierarchy(Module module, SolonConfigurationMetadata solonConfigurationMetadata) {
        for (SolonConfigurationMetadataProperty property : solonConfigurationMetadata.getProperties()) {
            this.propertiesSearchIndex.put(property.getName(), property);
        }
        for (SolonConfigurationMetadataHint hint : solonConfigurationMetadata.getHints()) {
            this.hintsSearchIndex.put(hint.getName(), hint);
        }

    }

    private List<MetadataContainer> computeNewContainersToProcess(OrderEnumerator orderEnumerator) {
        List<MetadataContainer> containersToProcess = new ArrayList<>();
        for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
            Collection<MetadataContainer> metadataContainerInfos =
                    MetadataContainer.newInstances(metadataFileContainer);
            for (MetadataContainer metadataContainerInfo : metadataContainerInfos) {
                boolean processMetadata = metadataContainerInfo.containsMetadataFile();
                if (processMetadata) {
                    containersToProcess.add(metadataContainerInfo);
                }
            }
        }

        if (containersToProcess.size() == 0) {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("No (new)metadata files to index"));
        }
        return containersToProcess;
    }

    @Override
    public @Nullable List<LookupElementBuilder> findPropertySuggestionsForQueryPrefix(String queryWithDotDelimitedPrefixes) {
        SortedMap<String, SolonConfigurationMetadataProperty> sortedMap = this.propertiesSearchIndex.prefixMap(queryWithDotDelimitedPrefixes);
        List<LookupElementBuilder> builders = new ArrayList<>();
        for (Map.Entry<String, SolonConfigurationMetadataProperty> entry : sortedMap.entrySet()) {
            builders.add(toLookupElementBuilder(entry.getValue()).withInsertHandler((context, item) -> {
                int index = queryWithDotDelimitedPrefixes.lastIndexOf(DELIMITER);
                Editor editor = context.getEditor();
                int startOffset = context.getStartOffset();
                int endOffset = context.getTailOffset();
                Document document = editor.getDocument();
                String text = item.getLookupString();
                if (index > 0) {
                    text = text.substring(index + 1);
                }
                document.replaceString(startOffset, endOffset, text);
                editor.getCaretModel().moveToOffset(endOffset);
            }));
        }
        return builders;
    }

    @Override
    public @Nullable List<LookupElementBuilder> findHintSuggestionsForQueryPrefix(String property, String queryWithDotDelimitedPrefixes) {
        SolonConfigurationMetadataHint hint = this.hintsSearchIndex.get(property);
        if (hint == null) {
            return new ArrayList<>();
        }
        List<LookupElementBuilder> builders = new ArrayList<>();
        for (SolonConfigurationMetadataHintValue hintValue : hint.getValues()) {
            if (hintValue.getValue() == null) {
                continue;
            }
            builders.add(toLookupElementBuilder(hintValue));
        }
        return builders;
    }

    @Override
    public @Nullable List<LookupElementBuilder> findYamlSuggestionsForQueryPrefix(String queryWithDotDelimitedPrefixes) {
        SortedMap<String, SolonConfigurationMetadataProperty> sortedMap = this.propertiesSearchIndex.prefixMap(queryWithDotDelimitedPrefixes);
        List<LookupElementBuilder> builders = new ArrayList<>();
        for (Map.Entry<String, SolonConfigurationMetadataProperty> entry : sortedMap.entrySet()) {
            builders.add(toLookupElementBuilder(entry.getValue()).withInsertHandler((context, item) -> {
                //选定提示事件
                Editor editor = context.getEditor();
                int startOffset = context.getStartOffset();
                int endOffset = context.getTailOffset();
                Document document = editor.getDocument();
                String text = item.getLookupString();
                boolean lastDot = queryWithDotDelimitedPrefixes.length() == queryWithDotDelimitedPrefixes.lastIndexOf(".") + 1;
/*                boolean queryNotEmpty = queryWithDotDelimitedPrefixes.length() > 1;
                int queryLength = queryNotEmpty ? (queryWithDotDelimitedPrefixes.length() - 1) : queryWithDotDelimitedPrefixes.length();
                String query = lastDot ?
                        queryWithDotDelimitedPrefixes.substring(0, queryLength) : queryWithDotDelimitedPrefixes;
                String supplementText = text.substring(lastDot ? query.length() : query.length() - 1);
                supplementText = supplementText.indexOf(".") == 0 ? supplementText.substring(1) : supplementText;*/
                String[] supplementTextArr = text.split(DELIMITER);
                StringBuffer resultText = new StringBuffer();
                String[] prefixArr = queryWithDotDelimitedPrefixes.split(DELIMITER);
                Map stringStringMap = YamlCompletionContributor.yamlMapCache.get(context.getFile().getName());
                for (int i = 0; i < supplementTextArr.length; i++) {
                    if (stringStringMap != null && i < prefixArr.length && stringStringMap.get(prefixArr[i]) != null) {
                        stringStringMap = (Map) stringStringMap.get(prefixArr[i]);
                        continue;
                    }
//                    if(i<prefixArr.length && supplementTextArr[i].equals(prefixArr[i])){
//                        allYamlTemplate.append(supplementTextArr[i]);
//                        allYamlTemplate.append(YML_DELIMITER);
//                        allYamlTemplate.append(LINE);
//                        allYamlTemplate.append(YML_FORMAT.repeat(i + 1));
//                        continue;
//                    }
                    resultText.append(supplementTextArr[i]);
                    resultText.append(YML_DELIMITER);

                    if (i == supplementTextArr.length - 1) {
                        break;
                    }
                    resultText.append(LINE);
                    resultText.append(YML_FORMAT.repeat(i + 1));

                }
                text = resultText.toString();
                document.replaceString(lastDot ? startOffset - 1 : startOffset, endOffset, text);
                editor.getCaretModel().moveToOffset(startOffset + (lastDot ? text.length() - 1 : text.length()));
            }));
        }
        return builders;
    }

    private LookupElementBuilder toLookupElementBuilder(SolonConfigurationMetadataHintValue hintValue) {
        LookupElementBuilder builder = LookupElementBuilder.create(hintValue.getValue());
        if (hintValue.getDescription() != null) {
            builder.withTypeText(hintValue.getDescription(), true);
        }
        SolonConfigurationMetadataProperty property = this.propertiesSearchIndex.get(hintValue.getValue());
        if (property != null && property.getDefaultValue() != null) {
            if (hintValue.getValue().toString().equals(property.getDefaultValue().toString())) {
                builder.bold();
            }
        }
        return builder;
    }

    private LookupElementBuilder toLookupElementBuilder(SolonConfigurationMetadataProperty property) {
        LookupElementBuilder builder = LookupElementBuilder.create(property.getName());
        if (property.getDescription() != null) {
//            @NotNull Project[] openProjects = ProjectUtil.getOpenProjects();
//            EncodingProjectManager encodingManager = EncodingProjectManager.getInstance(openProjects[0]);

            builder = builder.withTypeText(property.getDescription(), true);
        }
        return builder;
    }

    @Override
    public boolean canProvideSuggestions() {
        return this.propertiesSearchIndex.size() + this.hintsSearchIndex.size() > 0;
    }
}
