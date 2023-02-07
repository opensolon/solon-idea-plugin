package org.noear.plugin.idea.solon.initializr.step;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.noear.plugin.idea.solon.initializr.SolonCreationMetadata;
import org.noear.plugin.idea.solon.initializr.SolonInitializrBuilder;

import javax.swing.*;

/**
 * @author liupeiqiang
 * @date 2023/2/6 11:06
 */
public class ProjectDetails {

    private final WizardContext wizardContext;
    private final SolonInitializrBuilder moduleBuilder;
    private final SolonCreationMetadata metadata;
    private JPanel Panel_Root;
    private JBTextField TextField_Name;
    private TextFieldWithBrowseButton TextField_Location;
    private JBTextField TextField_Group;
    private JBTextField TextField_Artifact;
    private JBTextField TextField_PackageName;
    private ComboBox ComboBox_JavaVersion;
    private JCheckBox CheckBox_InitGit;
    private JdkComboBox ComboBox_JDK;

    public ProjectDetails(SolonInitializrBuilder moduleBuilder, WizardContext context){
        
        this.moduleBuilder = moduleBuilder;
        this.wizardContext = context;
        this.metadata = this.moduleBuilder.getMetadata();
        
        ComboBox_JDK.addActionListener(e -> {
            this.wizardContext.setProjectJdk(ComboBox_JDK.getSelectedJdk());
            this.metadata.setSdk(ComboBox_JDK.getSelectedJdk());
        });

        TextField_Name.setText(this.metadata.getName());
        TextField_Name.addCaretListener(e -> {
            this.metadata.setName(TextField_Name.getText());
        });

        TextField_Group.setText(this.metadata.getGroupId());
        TextField_Group.addCaretListener(e -> {
            this.metadata.setGroupId(TextField_Group.getText());
        });

        TextField_Artifact.setText(this.metadata.getArtifactId());
        TextField_Artifact.addCaretListener(e -> {
            this.metadata.setArtifactId(TextField_Artifact.getText());
        });

        TextField_PackageName.setText(this.metadata.getPackageName());
        TextField_PackageName.addCaretListener(e -> {
            this.metadata.setPackageName(TextField_PackageName.getText());
        });

        ComboBox_JavaVersion.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_JavaVersion.addActionListener(e -> {
            this.metadata.setJavaVersion(ComboBox_JavaVersion.getSelectedItem().toString());
        });

        CheckBox_InitGit.setSelected(this.metadata.isInitGit());
        CheckBox_InitGit.addActionListener(e -> {
            this.metadata.setInitGit(CheckBox_InitGit.isSelected());
        });

        TextField_Location.setText(this.metadata.getLocation());
        TextField_Location.addBrowseFolderListener(null, null, context.getProject(), FileChooserDescriptorFactory.createSingleFolderDescriptor(), new TextComponentAccessor<JTextField>() {

            @Override
            public @NlsSafe String getText(JTextField jTextField) {
                return jTextField.getText();
            }

            @Override
            public void setText(JTextField jTextField, @NlsSafe @NotNull String s) {
                jTextField.setText(s);
                moduleBuilder.getMetadata().setLocation(jTextField.getText());
            }
        });
    }

    public JPanel getRoot(){
        return Panel_Root;
    }

    public void createUIComponents(){
        Project project = this.wizardContext.getProject() != null ? wizardContext.getProject() : ProjectManager.getInstance().getDefaultProject();

        ProjectStructureConfigurable projectConfig = ProjectStructureConfigurable.getInstance(project);
        ProjectSdksModel jdksModel = projectConfig.getProjectJdksModel();
        ComboBox_JDK = new JdkComboBox(project, jdksModel, (sdk) -> sdk instanceof JavaSdkType, null, null, null);

    }

}
