package org.noear.solon.idea.plugin.run;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Solon 应用运行配置，复用 IntelliJ Application 配置能力。
 */
public class SolonRunConfiguration extends ApplicationConfiguration {

    public SolonRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(name, project, factory);
    }
}
