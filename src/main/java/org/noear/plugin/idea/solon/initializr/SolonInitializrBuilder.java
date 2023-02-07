package org.noear.plugin.idea.solon.initializr;

import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.plugin.idea.solon.SolonIcons;
import org.noear.plugin.idea.solon.initializr.step.ProjectDetailsStep;
import org.noear.plugin.idea.solon.module.SolonModuleType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class SolonInitializrBuilder extends ModuleBuilder {

    private SolonCreationMetadata metadata;

    public SolonCreationMetadata getMetadata() {
        if (metadata == null){
            metadata = new SolonCreationMetadata();
        }
        return metadata;
    }

    @Override
    public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
        super.setupRootModel(modifiableRootModel);
    }

    @Override
    public @NotNull List<Class<? extends ModuleWizardStep>> getIgnoredSteps() {
        List<Class<? extends ModuleWizardStep>> ignoredStep = new ArrayList<>();
        ignoredStep.add(ProjectSettingsStep.class);
        return ignoredStep;
    }

    @Override
    public @Nullable ModuleWizardStep getCustomOptionsStep(WizardContext wizardContext, Disposable parentDisposable) {
        return new ProjectDetailsStep(this, wizardContext);
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
        return SolonModuleType.getInstance();
    }


}
