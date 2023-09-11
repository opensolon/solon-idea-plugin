package org.noear.solon.idea.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.observable.properties.GraphPropertyImpl;
import com.intellij.openapi.observable.properties.PropertyGraph;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.layout.ButtonSelectorAction;
import com.intellij.ui.layout.ButtonSelectorToolbar;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.initializr.metadata.LanguageOption;
import org.noear.solon.idea.plugin.initializr.metadata.PackagingOption;
import org.noear.solon.idea.plugin.initializr.metadata.SolonCreationMetadata;
import org.noear.solon.idea.plugin.initializr.SolonInitializrBuilder;
import org.noear.solon.idea.plugin.initializr.metadata.TypeOption;
import org.noear.solon.idea.plugin.initializr.metadata.json.SolonMetadataOptionItem;
import org.noear.solon.idea.plugin.initializr.util.StringUtils;

import javax.swing.*;

import java.util.List;

import static java.util.Objects.requireNonNull;

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
    private JCheckBox CheckBox_InitGit;
    private JdkComboBox ComboBox_JDK;
    private ButtonSelectorToolbar ButtonSelectorToolbar_Language;
    private ButtonSelectorToolbar ButtonSelectorToolbar_Type;
    private ButtonSelectorToolbar ButtonSelectorToolbar_Packaging;
    private JComboBox<SolonMetadataOptionItem> ComboBox_SolonVer;
    private JComboBox<SolonMetadataOptionItem> ComboBox_Archetype;
    private JComboBox<SolonMetadataOptionItem> ComboBox_JavaVersion;
    private JLabel LocationTips;

    private boolean TipsLock = false;
    private boolean isNameChanged = false;
    private boolean isGroupChanged = false;
    private boolean isArtifactChanged = false;
    private boolean isPackageNameChanged = false;

    public ProjectDetails(SolonInitializrBuilder moduleBuilder, WizardContext context) {

        this.moduleBuilder = moduleBuilder;
        this.wizardContext = context;
        this.metadata = this.moduleBuilder.getMetadata();

        // Init and select default jdk
        if (ComboBox_JDK != null && ComboBox_JDK.getItemCount() > 0 && !ComboBox_JDK.isProjectJdkSelected()) {
            ComboBox_JDK.setSelectedIndex(0);
            this.metadata.setSdk(ComboBox_JDK.getSelectedJdk());
        }
        ComboBox_JDK.addActionListener(e -> {
            this.metadata.setSdk(ComboBox_JDK.getSelectedJdk());
        });

        TextField_Name.setText(this.metadata.getName());
        TextField_Name.addCaretListener(e -> {
            this.isNameChanged = true;
            this.metadata.setName(TextField_Name.getText());
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(),TextField_Name.getText()));
        });

        LocationTips.setText(this.metadata.getLocation() + "\\" + TextField_Name.getText());

        TextField_Group.setText(this.metadata.getGroupId());
        TextField_Group.addCaretListener(e -> {
            this.isGroupChanged = true;
            this.metadata.setGroupId(TextField_Group.getText());
        });

        TextField_Artifact.setText(this.metadata.getArtifactId());
        TextField_Artifact.addCaretListener(e -> {
            this.isArtifactChanged = true;
            this.metadata.setArtifactId(TextField_Artifact.getText());
        });

        TextField_PackageName.setText(this.metadata.getPackageName());
        TextField_PackageName.addCaretListener(e -> {
            this.isPackageNameChanged = true;
            this.metadata.setPackageName(TextField_PackageName.getText());
        });

        ComboBox_Archetype.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_Archetype.addActionListener(e -> {
            this.metadata.setJavaVersion(ComboBox_Archetype.getItemAt(ComboBox_Archetype.getSelectedIndex()).getValue());
        });

        ComboBox_SolonVer.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_SolonVer.addActionListener(e -> {
            this.metadata.setJavaVersion(ComboBox_SolonVer.getItemAt(ComboBox_SolonVer.getSelectedIndex()).getValue());
        });

        ComboBox_JavaVersion.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_JavaVersion.addActionListener(e -> {
            this.metadata.setJavaVersion(ComboBox_JavaVersion.getItemAt(ComboBox_JavaVersion.getSelectedIndex()).getValue());
        });

        CheckBox_InitGit.setSelected(this.metadata.isInitGit());
        CheckBox_InitGit.addActionListener(e -> {
            this.metadata.setInitGit(CheckBox_InitGit.isSelected());
        });

        TextField_Location.setText(this.metadata.getLocation());
        TextField_Location.addActionListener(e -> {
            this.metadata.setLocation(TextField_Location.getText());
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(),TextField_Name.getText()));
        });

        TextField_Location.getTextField().addCaretListener(e -> {
            this.metadata.setLocation(TextField_Location.getText());
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(),TextField_Name.getText()));
        });

        if (metadata.getInitMetadata() != null) {
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getSolonVer().getOptions()) {
                ComboBox_SolonVer.addItem(option);
            }
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getDependencies().getOptions()) {
                ComboBox_Archetype.addItem(option);
            }
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getJavaVer().getOptions()) {
                ComboBox_JavaVersion.addItem(option);
            }
            ComboBox_SolonVer.setSelectedItem(metadata.getInitMetadata().getSolonVer().getByValue(metadata.getInitMetadata().getSolonVer().getDefaultValue()));
            ComboBox_Archetype.setSelectedItem(metadata.getInitMetadata().getDependencies().getByValue(metadata.getInitMetadata().getDependencies().getDefaultValue()));
            ComboBox_JavaVersion.setSelectedItem(metadata.getInitMetadata().getJavaVer().getByValue(metadata.getInitMetadata().getJavaVer().getDefaultValue()));
        }

        TextField_Location.addBrowseFolderListener(null, null, context.getProject(), FileChooserDescriptorFactory.createSingleFolderDescriptor(), new TextComponentAccessor<JTextField>() {

            @Override
            public @NlsSafe String getText(JTextField jTextField) {
                return jTextField.getText();
            }

            @Override
            public void setText(JTextField jTextField, @NlsSafe @NotNull String s) {
                jTextField.setText(s);
                metadata.setLocation(jTextField.getText());
            }

        });
    }

    public JPanel getRoot() {
        return Panel_Root;
    }

    public void createUIComponents() {
        Project project = this.wizardContext.getProject() != null ? wizardContext.getProject() : ProjectManager.getInstance().getDefaultProject();

        ProjectSdksModel sdksModel = new ProjectSdksModel();
        sdksModel.reset(project);

        ComboBox_JDK = new JdkComboBox(project, sdksModel, sdk -> sdk instanceof JavaSdkType, null, null, null);

        GraphProperty<LanguageOption> languageProperty = new GraphPropertyImpl<>(new PropertyGraph(), () -> LanguageOption.JAVA);
        ButtonSelectorToolbar_Language = new ButtonSelectorToolbar(
                "LanguageSelector",
                new DefaultActionGroup(List.of(
                        new ButtonSelectorAction<>(LanguageOption.JAVA, languageProperty, LanguageOption.JAVA.getLabel()),
                        new ButtonSelectorAction<>(LanguageOption.KOTLIN, languageProperty, LanguageOption.KOTLIN.getLabel()),
                        new ButtonSelectorAction<>(LanguageOption.GROOVY, languageProperty, LanguageOption.GROOVY.getLabel())
                )),
                true
        );

        languageProperty.afterChange(option -> {
            this.metadata.setLanguage(option.getValue());
            return null;
        });

        GraphProperty<TypeOption> typeProperty = new GraphPropertyImpl<>(new PropertyGraph(), () -> TypeOption.MAVEN);
        ButtonSelectorToolbar_Type = new ButtonSelectorToolbar(
                "TypeSelector",
                new DefaultActionGroup(List.of(
                        new ButtonSelectorAction<>(TypeOption.MAVEN, typeProperty, TypeOption.MAVEN.getLabel()),
                        new ButtonSelectorAction<>(TypeOption.GRADLE_GROOVY, typeProperty, TypeOption.GRADLE_GROOVY.getLabel()),
                        new ButtonSelectorAction<>(TypeOption.GRADLE_KOTLIN, typeProperty, TypeOption.GRADLE_KOTLIN.getLabel())
                )),
                true
        );
        typeProperty.afterChange(option -> {
            this.metadata.setType(option.getValue());
            return null;
        });
        GraphProperty<PackagingOption> packagingProperty = new GraphPropertyImpl<>(new PropertyGraph(), () -> PackagingOption.JAR);
        ButtonSelectorToolbar_Packaging = new ButtonSelectorToolbar(
                "PackagingSelector",
                new DefaultActionGroup(List.of(
                        new ButtonSelectorAction<>(PackagingOption.JAR, packagingProperty, PackagingOption.JAR.getLabel()),
                        new ButtonSelectorAction<>(PackagingOption.WAR, packagingProperty, PackagingOption.WAR.getLabel())
                )),
                true
        );
        packagingProperty.afterChange(option -> {
            this.metadata.setPackaging(option.getValue());
            return null;
        });

        ButtonSelectorToolbar_Language.setTargetComponent(Panel_Root);
        ButtonSelectorToolbar_Type.setTargetComponent(Panel_Root);
        ButtonSelectorToolbar_Packaging.setTargetComponent(Panel_Root);
    }

    public boolean validate(ModuleBuilder moduleBuilder, WizardContext wizardContext)
            throws ConfigurationException {
        if (!this.metadata.hasValidName()) {
            throw new ConfigurationException("Invalid name", "Invalid Data");
        } else if (!this.metadata.hasValidLocation()) {
            throw new ConfigurationException("Invalid location", "Invalid Data");
        } else if (!this.metadata.hasValidGroupId()) {
            throw new ConfigurationException("Invalid group id", "Invalid Data");
        } else if (!this.metadata.hasValidArtifactId()) {
            throw new ConfigurationException("Invalid artifact id", "Invalid Data");
        } else if (!this.metadata.hasValidPackageName()) {
            throw new ConfigurationException("Invalid package", "Invalid Data");
        } else if (!this.metadata.hasValidDescription()) {
            throw new ConfigurationException("Invalid description", "Invalid Data");
        } else if (!this.metadata.hasCompatibleJavaVersion()) {
            JavaSdkVersion wizardSdkVersion = JavaSdk.getInstance().getVersion(this.metadata.getSdk());
            throw new ConfigurationException("Selected Java version " + requireNonNull(
                    this.metadata.getJavaVersion())
                    + " is not supported. Max supported version is (" + requireNonNull(wizardSdkVersion)
                    .getMaxLanguageLevel().getPresentableText()
                    + ").\n\n You can go back to first screen and change the Project/Module SDK version there if you need support for newer Java versions",
                    "Java Compatibility");
        }
        return true;
    }
}
