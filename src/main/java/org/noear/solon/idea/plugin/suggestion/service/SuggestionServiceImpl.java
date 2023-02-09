package org.noear.solon.idea.plugin.suggestion.service;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.psi.PsiElement;
import gnu.trove.THashMap;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.common.util.LoggerUtil;
import org.noear.solon.idea.plugin.suggestion.metadata.MetadataContainer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static java.util.Collections.emptyList;

public class SuggestionServiceImpl implements SuggestionService{

    private final Map<String, Map<String, MetadataContainer>>
            moduleNameToSeenContainerPathToContainerInfo;
    /**
     * Within the trie, all keys are stored in sanitised format to enable us find keys without worrying about hiphens, underscores, e.t.c in the keys themselves
     */
    private final Map<String, Trie<String, MetadataSuggestionNode>> moduleNameToRootSearchIndex;
    private Future<?> currentExecution;
    private volatile boolean indexingInProgress;

    public SuggestionServiceImpl() {
        moduleNameToSeenContainerPathToContainerInfo = new THashMap<>();
        moduleNameToRootSearchIndex = new THashMap<>();
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
                            reIndexModule(emptyList(), emptyList(), module);
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



    private void reIndexModule(List<MetadataContainer> newProjectSourcesToProcess,
                               List<MetadataContainer> projectContainersToRemove, Module module) {
        Map<String, MetadataContainer> moduleSeenContainerPathToSeenContainerInfo =
                moduleNameToSeenContainerPathToContainerInfo
                        .computeIfAbsent(module.getName(), k -> new THashMap<>());
        Trie<String, MetadataSuggestionNode> moduleRootSearchIndex =
                moduleNameToRootSearchIndex.get(module.getName());
        if (moduleRootSearchIndex == null) {
            moduleRootSearchIndex = new PatriciaTrie<>();
            moduleNameToRootSearchIndex.put(module.getName(), moduleRootSearchIndex);
        }

        /**
         * Order entries include SDK, libraries and other modules the module uses.
         * https://plugins.jetbrains.com/docs/intellij/module.html#how-do-i-get-dependencies-and-classpath-of-a-module
         */
        OrderEnumerator moduleOrderEnumerator = OrderEnumerator.orderEntries(module);

        List<MetadataContainer> newModuleContainersToProcess =
                computeNewContainersToProcess(moduleOrderEnumerator,
                        moduleSeenContainerPathToSeenContainerInfo);
        newModuleContainersToProcess.addAll(newProjectSourcesToProcess);

        List<MetadataContainer> moduleContainersToRemove =
                computeContainersToRemove(moduleOrderEnumerator,
                        moduleSeenContainerPathToSeenContainerInfo);
        moduleContainersToRemove.addAll(projectContainersToRemove);

        processContainers(module, newModuleContainersToProcess, moduleContainersToRemove,
                moduleSeenContainerPathToSeenContainerInfo, moduleRootSearchIndex);
    }

    @Override
    public @Nullable List<LookupElementBuilder> findSuggestionsForQueryPrefix(Project project, Module module, FileType fileType, PsiElement element, @Nullable List<String> ancestralKeys, String queryWithDotDelimitedPrefixes, @Nullable Set<String> siblingsToExclude) {
        return null;
    }
}
