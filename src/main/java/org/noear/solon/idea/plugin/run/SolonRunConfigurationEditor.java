package org.noear.solon.idea.plugin.run;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import jakarta.validation.constraints.NotNull;

import javax.swing.*;
import java.awt.*;

public class SolonRunConfigurationEditor extends SettingsEditor<SolonRunConfiguration> {
    private final Project project;
    private TextFieldWithBrowseButton mainClassField;

    public SolonRunConfigurationEditor(Project project) {
        this.project = project;
    }

    @Override
    protected void resetEditorFrom(@NotNull SolonRunConfiguration configuration) {
        mainClassField.setText(configuration.getName());
    }

    @Override
    protected void applyEditorTo(@NotNull SolonRunConfiguration configuration) {
        configuration.setName(mainClassField.getText());
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        JPanel panel = new JPanel(new GridLayout(0, 1));

        mainClassField = new TextFieldWithBrowseButton();
        mainClassField.addActionListener(e -> chooseMainClass());

        panel.add(new JLabel("主类:"));
        panel.add(mainClassField);

        return panel;
    }

    private void chooseMainClass() {
        TreeClassChooser dialog = TreeClassChooserFactory.getInstance(project)
                .createInheritanceClassChooser(
                        "选择 Solon 主类",
                        GlobalSearchScope.projectScope(project),
                        null, // 不限制基类
                        null,
                        psiClass -> {
                            return psiClass.findMethodsByName("main", true).length > 0; // 只显示包含 main 方法的类
                        }
                );

        dialog.showDialog();

        PsiClass selected = dialog.getSelected();
        if (selected != null) {
            mainClassField.setText(selected.getQualifiedName());
        }
    }
}