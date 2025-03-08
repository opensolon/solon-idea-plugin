package org.noear.solon.idea.plugin.initializr.step;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.roots.ui.configuration.JdkComboBox;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.initializr.SolonInitializrBuilder;
import org.noear.solon.idea.plugin.initializr.metadata.SolonCreationMetadata;
import org.noear.solon.idea.plugin.initializr.metadata.json.SolonMetaServer;
import org.noear.solon.idea.plugin.initializr.metadata.json.SolonMetadataOptionItem;
import org.noear.solon.idea.plugin.initializr.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.text.MessageFormat;

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
    private TextFieldWithBrowseButton TextField_Location;
    private JBTextField TextField_Group;
    private JBTextField TextField_Artifact;
    private JBTextField TextField_PackageName;
    private JCheckBox CheckBox_InitGit;
    private JdkComboBox ComboBox_JDK;
    private JComboBox<SolonMetadataOptionItem> ComboBox_SolonVer;
    private JComboBox<SolonMetadataOptionItem> ComboBox_Archetype;
    private JComboBox<SolonMetadataOptionItem> ComboBox_JavaVersion;
    private JLabel LocationTips;
    private JComboBox<SolonMetadataOptionItem> ComboBox_Language;
    private JComboBox<SolonMetadataOptionItem> ComboBox_Type;
    private JComboBox<SolonMetadataOptionItem> ComboBox_Packaging;
    private JPanel ServerUrl_Panel;
    private JBLabel ServerUrl_JBLabel;

    public ProjectDetails(SolonInitializrBuilder moduleBuilder, WizardContext context) {

        this.moduleBuilder = moduleBuilder;
        this.wizardContext = context;
        this.metadata = this.moduleBuilder.getMetadata();
        SolonMetaServer server = this.metadata.getServer();
        // 创建超链接文本
        ServerUrl_JBLabel = new JBLabel(server.getTitle());
        ServerUrl_JBLabel.setForeground(JBColor.BLUE);
        ServerUrl_JBLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 添加鼠标事件监听器，以在点击时打开超链接
        ServerUrl_JBLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(server.getUrl()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ServerUrl_JBLabel.setText(MessageFormat.format("<html><a href={0}>{1}</a></html>", server.getUrl(), server.getTitle()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ServerUrl_JBLabel.setText(server.getTitle());
            }
        });
        ServerUrl_Panel.setLayout(new BorderLayout());
        ServerUrl_Panel.add(ServerUrl_JBLabel, BorderLayout.CENTER);
        // Init and select default jdk
        if (ComboBox_JDK != null && ComboBox_JDK.getItemCount() > 0 && !ComboBox_JDK.isProjectJdkSelected()) {
            ComboBox_JDK.setSelectedIndex(0);
            this.metadata.setSdk(ComboBox_JDK.getSelectedJdk());

            ComboBox_JDK.addActionListener(e -> {
                this.metadata.setSdk(ComboBox_JDK.getSelectedJdk());
            });
        }

        TextField_Group.setText(this.metadata.getGroupId());
        TextField_Group.addCaretListener(e -> {
            this.metadata.setGroupId(TextField_Group.getText());
            TextField_PackageName.setText(TextField_Group.getText() + "." + TextField_Artifact.getText());
        });

        TextField_Artifact.setText(this.metadata.getArtifactId());
        TextField_Artifact.addCaretListener(e -> {
            this.metadata.setArtifactId(TextField_Artifact.getText());
            String artifact = TextField_Artifact.getText();
            if (artifact.contains("-")) {
                artifact = artifact.replace("-", "_");
            }
            TextField_PackageName.setText(TextField_Group.getText() + "." + artifact);
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(), TextField_Artifact.getText()));
        });

        LocationTips.setText(StringUtils.PathStrAssemble(this.metadata.getLocation(), TextField_Artifact.getText()));

        TextField_PackageName.setText(this.metadata.getPackageName());
        TextField_PackageName.addCaretListener(e -> {
            this.metadata.setPackageName(TextField_PackageName.getText());

        });

        ComboBox_Archetype.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_Archetype.addActionListener(e -> {
            this.metadata.setDependency(ComboBox_Archetype.getItemAt(ComboBox_Archetype.getSelectedIndex()).getValue());
        });

        ComboBox_SolonVer.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_SolonVer.addActionListener(e -> {
            this.metadata.setSolonVer(ComboBox_SolonVer.getItemAt(ComboBox_SolonVer.getSelectedIndex()).getValue());
        });

        ComboBox_JavaVersion.setSelectedItem(this.metadata.getJavaVersion());
        ComboBox_JavaVersion.addActionListener(e -> {
            this.metadata.setJavaVersion(ComboBox_JavaVersion.getItemAt(ComboBox_JavaVersion.getSelectedIndex()).getValue());
        });

        ComboBox_Language.setSelectedItem(this.metadata.getLanguage());
        ComboBox_Language.addActionListener(e -> {
            this.metadata.setLanguage(ComboBox_Language.getItemAt(ComboBox_Language.getSelectedIndex()).getValue());
        });

        ComboBox_Type.setSelectedItem(this.metadata.getType());
        ComboBox_Type.addActionListener(e -> {
            this.metadata.setType(ComboBox_Type.getItemAt(ComboBox_Type.getSelectedIndex()).getValue());
        });

        ComboBox_Packaging.setSelectedItem(this.metadata.getPackaging());
        ComboBox_Packaging.addActionListener(e -> {
            this.metadata.setPackaging(ComboBox_Packaging.getItemAt(ComboBox_Packaging.getSelectedIndex()).getValue());
        });

        CheckBox_InitGit.setSelected(this.metadata.isInitGit());
        CheckBox_InitGit.addActionListener(e -> {
            this.metadata.setInitGit(CheckBox_InitGit.isSelected());
        });

        TextField_Location.setText(this.metadata.getLocation());
        // 选择文件路径后的监听
        TextField_Location.addActionListener(e -> {
            this.metadata.setLocation(TextField_Location.getText());
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(), TextField_Artifact.getText()));
        });

        // 直接修改框内值的监听
        TextField_Location.getTextField().addCaretListener(e -> {
            this.metadata.setLocation(TextField_Location.getText());
            LocationTips.setText(StringUtils.PathStrAssemble(TextField_Location.getText(), TextField_Artifact.getText()));
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
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getLanguage().getOptions()) {
                ComboBox_Language.addItem(option);
            }
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getProject().getOptions()) {
                ComboBox_Type.addItem(option);
            }
            for (SolonMetadataOptionItem option : metadata.getInitMetadata().getPackaging().getOptions()) {
                ComboBox_Packaging.addItem(option);
            }
            ComboBox_SolonVer.setSelectedItem(metadata.getInitMetadata().getSolonVer().getByValue(metadata.getInitMetadata().getSolonVer().getDefaultValue()));
            ComboBox_Archetype.setSelectedItem(metadata.getInitMetadata().getDependencies().getByValue(metadata.getInitMetadata().getDependencies().getDefaultValue()));
            ComboBox_JavaVersion.setSelectedItem(metadata.getInitMetadata().getJavaVer().getByValue(metadata.getInitMetadata().getJavaVer().getDefaultValue()));
            ComboBox_Language.setSelectedItem(metadata.getInitMetadata().getLanguage().getByValue(metadata.getInitMetadata().getLanguage().getDefaultValue()));
            ComboBox_Type.setSelectedItem(metadata.getInitMetadata().getProject().getByValue(metadata.getInitMetadata().getProject().getDefaultValue()));
            ComboBox_Packaging.setSelectedItem(metadata.getInitMetadata().getPackaging().getByValue(metadata.getInitMetadata().getPackaging().getDefaultValue()));
        }

        TextField_Location.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener(TextField_Location, context.getProject(), FileChooserDescriptorFactory.createSingleFolderDescriptor(), new TextComponentAccessor<JTextField>() {
            @Override
            public @NlsSafe String getText(JTextField jTextField) {
                return jTextField.getText();
            }

            @Override
            public void setText(JTextField jTextField, @NlsSafe @NotNull String s) {
                jTextField.setText(s);
                metadata.setLocation(jTextField.getText());
            }

        }));
    }

    public JPanel getRoot() {
        return Panel_Root;
    }

    public void createUIComponents() {
        Project project = this.wizardContext.getProject() != null ? wizardContext.getProject() : ProjectManager.getInstance().getDefaultProject();

        ProjectSdksModel sdksModel = new ProjectSdksModel();
        sdksModel.reset(project);

        ComboBox_JDK = new JdkComboBox(project, sdksModel, sdk -> sdk instanceof JavaSdkType, null, null, null);


    }

    public boolean validate(ModuleBuilder moduleBuilder, WizardContext wizardContext)
            throws ConfigurationException {
        if (!this.metadata.hasValidLocation()) {
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
