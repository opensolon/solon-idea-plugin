package org.noear.solon.idea.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.metadata.MetadataContainer;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataHints;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataProperties;
import org.noear.solon.idea.plugin.suggestion.metadata.json.SolonConfigurationMetadataProperty;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class SuggestionServiceImpl implements SuggestionService{

    private final Map<String, Map<String, MetadataContainer>> moduleNameToMetadataPathToMetadataContainer;

    private final Map<String, SolonConfigurationMetadataProperties> propertiesSearchIndex;
    private final Map<String, SolonConfigurationMetadataHints> hintsSearchIndex;

    private Future<?> currentExecution;
    private volatile boolean indexingInProgress;

    public SuggestionServiceImpl() {
        moduleNameToMetadataPathToMetadataContainer = new HashMap<>();
        propertiesSearchIndex = new HashMap<>();
        hintsSearchIndex = new HashMap<>();
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
                try {
                    for (Module module : modules) {
                        LoggerUtil.debug(this.getClass(), logger -> logger.debug("--> Indexing requested for module " + module.getName()));
                        StopWatch moduleTimer = new StopWatch();
                        moduleTimer.start();
                        try {
                            reIndexModule(module);
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



    private void reIndexModule(Module module) {
        Map<String, MetadataContainer> moduleSeenMetadataPathToSeenMetadataContainer =
                moduleNameToMetadataPathToMetadataContainer
                        .computeIfAbsent(module.getName(), k -> new HashMap<>());

        /**
         * Order entries include SDK, libraries and other modules the module uses.
         * https://plugins.jetbrains.com/docs/intellij/module.html#how-do-i-get-dependencies-and-classpath-of-a-module
         */
        OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

        List<MetadataContainer> newModuleContainersToProcess =
                computeNewContainersToProcess(moduleOrderEnumerator,
                        moduleSeenMetadataPathToSeenMetadataContainer);

        List<MetadataContainer> moduleContainersToRemove =
                computeContainersToRemove(moduleOrderEnumerator,
                        moduleSeenMetadataPathToSeenMetadataContainer);

        processContainers(module, newModuleContainersToProcess, moduleContainersToRemove,
                moduleSeenMetadataPathToSeenMetadataContainer, propertiesSearchIndex, hintsSearchIndex);
    }

    private List<MetadataContainer> computeNewContainersToProcess(OrderEnumerator orderEnumerator,
                                                                  Map<String, MetadataContainer> moduleSeenMetadataPathToSeenMetadataContainer) {
        List<MetadataContainer> containersToProcess = new ArrayList<>();
        for (VirtualFile metadataFileContainer : orderEnumerator.recursively().classes().getRoots()) {
            Collection<MetadataContainer> metadataContainerInfos =
                    MetadataContainer.newInstances(metadataFileContainer);
            for (MetadataContainer metadataContainerInfo : metadataContainerInfos) {

                boolean seenBefore = moduleSeenMetadataPathToSeenMetadataContainer
                        .containsKey(metadataContainerInfo.getContainerArchiveOrFileRef());

                boolean updatedSinceLastSeen = false;
                if (seenBefore) {
                    MetadataContainer seenMetadataContainerInfo = moduleSeenMetadataPathToSeenMetadataContainer
                            .get(metadataContainerInfo.getContainerArchiveOrFileRef());
                    updatedSinceLastSeen = metadataContainerInfo.isModified(seenMetadataContainerInfo);
                    if (updatedSinceLastSeen) {
                        LoggerUtil.debug(this.getClass(), (logger) -> logger.debug("Container seems to have been updated. Previous version: "
                                + seenMetadataContainerInfo + "; Newer version: " + metadataContainerInfo));
                    }
                }

                boolean looksFresh = !seenBefore || updatedSinceLastSeen;
                boolean processMetadata = looksFresh && metadataContainerInfo.containsMetadataFile();
                if (processMetadata) {
                    containersToProcess.add(metadataContainerInfo);
                }

                if (looksFresh) {
                    moduleSeenMetadataPathToSeenMetadataContainer
                            .put(metadataContainerInfo.getContainerArchiveOrFileRef(), metadataContainerInfo);
                }
            }
        }

        if (containersToProcess.size() == 0) {
            LoggerUtil.debug(this.getClass(), logger -> logger.debug("No (new)metadata files to index"));
        }
        return containersToProcess;
    }

    private List<MetadataContainer> computeContainersToRemove(OrderEnumerator orderEnumerator,
                                                              Map<String, MetadataContainer> moduleSeenMetadataPathToSeenMetadataContainer) {
        Set<String> newContainerPaths = Arrays.stream(orderEnumerator.recursively().classes().getRoots())
                .flatMap(MetadataContainer::getContainerArchiveOrFileRefs).collect(Collectors.toSet());
        Set<String> knownContainerPathSet = new HashSet<>(moduleSeenMetadataPathToSeenMetadataContainer.keySet());
        knownContainerPathSet.removeAll(newContainerPaths);
        return knownContainerPathSet.stream().map(moduleSeenMetadataPathToSeenMetadataContainer::get)
                .collect(Collectors.toList());
    }

    @Override
    public @Nullable List<LookupElementBuilder> findSuggestionsForQueryPrefix(Project project, Module module, FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude) {
        return null;
    }
}
