package org.noear.solon.idea.plugin.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.noear.solon.idea.plugin.SolonIcons;

import javax.swing.*;

public class SolonYamlFileType extends LanguageFileType {

    public static final SolonYamlFileType INSTANCE = new SolonYamlFileType();

    private SolonYamlFileType() {
        super(YAMLLanguage.INSTANCE, true);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "solon-yaml-file";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Solon yaml file";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "yaml";
    }

    @Override
    public @Nullable Icon getIcon() {
        return SolonIcons.SolonIcon_16x16;
    }
}
