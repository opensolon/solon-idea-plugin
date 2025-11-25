package org.noear.solon.idea.plugin.run;

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Solon 配置工厂
 */
public class SolonConfigurationFactory extends ConfigurationFactory {

    protected SolonConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new SolonRunConfiguration(project, this, "Solon Application");
    }

    @Override
    public @NotNull String getId() {
        return "Solon";
    }

    @Override
    public @NotNull Class<? extends RunConfigurationOptions> getOptionsClass() {
        return JvmMainMethodRunConfigurationOptions.class;
    }
}
