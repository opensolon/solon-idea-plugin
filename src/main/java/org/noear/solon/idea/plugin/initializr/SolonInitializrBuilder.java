package org.noear.solon.idea.plugin.initializr;

import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.GitRepositoryInitializer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.SolonIcons;
import org.noear.solon.idea.plugin.initializr.metadata.SolonCreationMetadata;
import org.noear.solon.idea.plugin.initializr.processor.ModuleBuilderPostProcessor;
import org.noear.solon.idea.plugin.initializr.step.ProjectDetailsStep;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolonInitializrBuilder extends ModuleBuilder {

    private SolonCreationMetadata metadata;


    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        doAddContentEntry(modifiableRootModel);
    }

    @Override
    public @NotNull Module createModule(@NotNull ModifiableModuleModel moduleModel) throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, ConfigurationException, JDOMException {

        // TODO: Change module type to SOLON Module
        ModuleBuilder.deleteModuleFile(super.getModuleFilePath());
        // Change module type to JAVA Module
        Module module = moduleModel.newModule(super.getModuleFilePath(), StdModuleTypes.JAVA.getId());
        super.setupModule(module);
        ApplicationManager.getApplication().invokeLater(() -> {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                try{
                    InitializerDownloader downloader = new InitializerDownloader(this);
                    downloader.execute(ProgressManager.getInstance().getProgressIndicator());
                } catch (IOException e){
                    ApplicationManager.getApplication().invokeLater(
                            () -> Messages.showErrorDialog("Error: " + e.getMessage(), "Creation Failed")
                    );
                }
                // Init git repository
                if (metadata.isInitGit()) {
                    GitRepositoryInitializer.getInstance().initRepository(module.getProject(), LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(super.getContentEntryPath())));
                }
            }, "Downloading required files...", true, null);
            ModuleBuilderPostProcessor[] postProcessors =
                    ModuleBuilderPostProcessor.EXTENSION_POINT_NAME.getExtensions();
            for (ModuleBuilderPostProcessor postProcessor : postProcessors) {
                if (!postProcessor.postProcess(module)) {
                    break;
                }
            }
        }, ModalityState.current());
        return module;

    }

    @Override
    public @NotNull List<Class<? extends ModuleWizardStep>> getIgnoredSteps() {
        List<Class<? extends ModuleWizardStep>> ignoredStep = new ArrayList<>();
        ignoredStep.add(ProjectSettingsStep.class);
        return ignoredStep;
    }

    // first wizard page
    @Override
    public @Nullable ModuleWizardStep getCustomOptionsStep(WizardContext wizardContext, Disposable parentDisposable) {
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
            try {
                this.getMetadata().refreshMetadataOptions(ProgressManager.getInstance().getProgressIndicator());
            } catch (IOException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> Messages.showErrorDialog("Error: " + e.getMessage(), "Pull metadata Failed")
                );
            }
        });
        //暂时保留防止executeNonCancelableSection方法不正确用于参考
//        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
//            try {
//                this.getMetadata().refreshMetadataOptions(ProgressManager.getInstance().getProgressIndicator());
//            } catch (IOException e) {
//                ApplicationManager.getApplication().invokeLater(
//                        () -> Messages.showErrorDialog("Error: " + e.getMessage(), "Pull metadata Failed")
//                );
//            }
//        }, "Pulling metadata...", true, null);
        return new ProjectDetailsStep(this, wizardContext);
    }

    // next wizard page
    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
        return super.createWizardSteps(wizardContext, modulesProvider);
    }

    @Override
    public Icon getNodeIcon() {
        return SolonIcons.SolonIcon_16x16;
    }

    @Override
    public @Nullable @NonNls String getBuilderId() {
        return "SOLON_BUILDER_ID";
    }

    @Override
    public @NlsContexts.DetailedDescription String getDescription() {
        return "Create <b>Solon</b> applications using the Solon Initializr service";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getPresentableName() {
        return "Solon Initializr";
    }

    @Override
    public String getParentGroup() {
        return "Build Tools";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ModuleType<?> getModuleType() {
        return ModuleType.EMPTY;
    }

    public SolonCreationMetadata getMetadata() {
        if (metadata == null){
            metadata = new SolonCreationMetadata();
        }
        return metadata;
    }

}
