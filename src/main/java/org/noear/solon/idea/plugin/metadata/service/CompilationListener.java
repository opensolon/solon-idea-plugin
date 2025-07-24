package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTaskListener;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.source.MetadataFileIndex;
import org.noear.solon.idea.plugin.misc.ModuleRootUtils;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
class CompilationListener implements CompilationStatusListener, ProjectTaskListener {
    private static final Logger log = Logger.getInstance(CompilationListener.class);
    private final Project project;

    CompilationListener(Project project) {
        this.project = project;
    }


    /**
     * For Maven project only
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        enqueueBackgroundReloadTask("CompilationFinished", null);
    }


    /**
     * For gradle delegated build
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void finished(@NotNull ProjectTaskManager.Result result) {
        if (result.isAborted()) return;
        enqueueBackgroundReloadTask("Finished", getAffectedModules(result));
    }


    private Set<Module> getAffectedModules(ProjectTaskManager.Result result) {
        Set<Module> modules = new HashSet<>();
        result.anyTaskMatches((task, state) -> {
            if (task instanceof ModuleBuildTask mbt && !state.isFailed() && !state.isSkipped()) {
                modules.add(mbt.getModule());
                return true;
            } else {
                return false;
            }
        });
        return modules;
    }


    private void enqueueBackgroundReloadTask(String mark, Collection<Module> affectedModules) {
        //The index recreates too late, we have to find the generated metadata files without the index.
        new Task.Backgroundable(project, "%s Reloading solon configuration metadata".formatted(mark)) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.pushState();
                indicator.setIndeterminate(false);
                List<VirtualFile> affectedClassRoots = new ArrayList<>();
                double i = 0;
//                if (affectedModules == null) {
//                    affectedModules = List.of(compileContext.getCompileScope().getAffectedModules());
//                }
                if(affectedModules != null) {
                    for (Module module : affectedModules) {
                        assert module.getProject().equals(project);   // The 2 topics we are listening are all project-level.
                        indicator.setText2(module.getName());
                        indicator.setFraction(i++ / affectedModules.size());
                        affectedClassRoots.addAll(Arrays.asList(ModuleRootUtils.getClassRootsWithoutLibraries(module)));
                    }
                }

                indicator.popState();
                List<VirtualFile> newMetaFiles = affectedClassRoots.stream()
                        .map(MetadataFileIndex::findMetaFileInClassRoot)
                        .filter(Objects::nonNull)
                        .toList();
                if (newMetaFiles.isEmpty()) return;
                // Looks like the IDE won't reload the generated metadata file automatically,
                // so we have to refresh it for use by IndexFromOneFile#reSync
                newMetaFiles.forEach(vf -> vf.refresh(true, false));
                i = 0;
                for (Module module : affectedModules) {
                    indicator.setText2(module.getName());
                    indicator.setFraction(i++ / affectedModules.size());
                    refreshModuleAndDependencies(List.of(module), newMetaFiles);
                }
            }
        }.queue();
    }


    private void refreshModuleAndDependencies(
            Iterable<Module> modules, Collection<VirtualFile> additionalMetaFiles) {
        for (Module module : modules) {
            if (module.isDisposed()) continue;
            ModuleMetadataServiceImpl mms = (ModuleMetadataServiceImpl) module.getServiceIfCreated(
                    ModuleMetadataService.class);
            if (mms != null) {
                mms.refreshMetadata(additionalMetaFiles);
            }
            try {
                refreshModuleAndDependencies(ModuleManager.getInstance(project).getModuleDependentModules(module),
                        additionalMetaFiles);
            } catch (Exception e) {
                log.warn("refreshModuleAndDependencies failed", e);
            }
        }
    }
}
