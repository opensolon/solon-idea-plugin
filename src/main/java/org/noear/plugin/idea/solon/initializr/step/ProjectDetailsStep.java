package org.noear.plugin.idea.solon.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import org.noear.plugin.idea.solon.initializr.SolonInitializrBuilder;

import javax.swing.*;

public class ProjectDetailsStep extends ModuleWizardStep implements Disposable {

    private final SolonInitializrBuilder moduleBuilder;
    private final WizardContext wizardContext;

    private ProjectDetails detailsForm;

    public ProjectDetailsStep(SolonInitializrBuilder moduleBuilder, WizardContext wizardContext) {
        this.moduleBuilder = moduleBuilder;
        this.wizardContext = wizardContext;
    }

    @Override
    public JComponent getComponent() {
        detailsForm = new ProjectDetails(this.moduleBuilder, this.wizardContext);
        return detailsForm.getRoot();
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void dispose() {

    }
}
