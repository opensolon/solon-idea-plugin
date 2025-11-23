package org.noear.solon.idea.plugin.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.SolonIcons;

import javax.swing.*;

/**
 * Solon 配置类型
 */
public class SolonConfigurationType implements ConfigurationType {

    @Override
    public @NotNull String getDisplayName() {
        return "Solon Application";
    }

    @Override
    public @NotNull String getConfigurationTypeDescription() {
        return "Run Solon applications";
    }

    @Override
    public @NotNull Icon getIcon() {
        return SolonIcons.SolonIcon_16x16;
    }

    @Override
    public @NotNull String getId() {
        return "SolonConfigurationType";
    }

    @Override
    public @NotNull ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new SolonConfigurationFactory(this)};
    }
}