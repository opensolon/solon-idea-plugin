package org.noear.solon.idea.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import org.noear.solon.idea.plugin.initializr.SolonCreationMetadata;
import org.noear.solon.idea.plugin.initializr.SolonInitializrBuilder;

import javax.swing.*;
import java.io.File;

public class ProjectDetailsStep extends ModuleWizardStep implements Disposable {

    private final SolonInitializrBuilder moduleBuilder;
    private final WizardContext wizardContext;
    private final SolonCreationMetadata metadata;

    private ProjectDetails detailsForm;

    public ProjectDetailsStep(SolonInitializrBuilder moduleBuilder, WizardContext wizardContext) {
        this.moduleBuilder = moduleBuilder;
        this.wizardContext = wizardContext;
        this.metadata = moduleBuilder.getMetadata();
    }

    @Override
    public JComponent getComponent() {
        detailsForm = new ProjectDetails(this.moduleBuilder, this.wizardContext);
        return detailsForm.getRoot();
    }

    @Override
    public boolean validate() throws ConfigurationException {
        if (this.metadata == null){
            throw new ConfigurationException(
                    "Could not fetch metadata. Please go back and try again.",
                    "Error");
        }
        return detailsForm.validate(this.moduleBuilder, this.wizardContext);
    }

    @Override
    public void updateDataModel() {
        this.wizardContext.setProjectJdk(this.metadata.getSdk());
        this.wizardContext.setProjectName(this.metadata.getName());
        this.wizardContext.setProjectFileDirectory(this.metadata.getLocation() + File.separator + this.metadata.getName());
    }

    @Override
    public void dispose() {
    }
}
