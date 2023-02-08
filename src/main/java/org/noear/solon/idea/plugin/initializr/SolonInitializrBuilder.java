package org.noear.solon.idea.plugin.initializr;

import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.NlsContexts;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.SolonIcons;
import org.noear.solon.idea.plugin.initializr.step.ProjectDetailsStep;

import javax.swing.*;
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

        // Create Maven Project
        Module module = super.createModule(moduleModel);
        return module;

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
        return ModuleType.EMPTY;
    }

    public SolonCreationMetadata getMetadata() {
        if (metadata == null){
            metadata = new SolonCreationMetadata();
        }
        return metadata;
    }

}
