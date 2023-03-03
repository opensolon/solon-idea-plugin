package org.noear.solon.idea.plugin.suggestion.filetype;

import com.intellij.lang.Language;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.noear.solon.idea.plugin.SolonIcons;

import javax.swing.*;

public class SolonPropertiesFileType extends LanguageFileType {

    public static final SolonPropertiesFileType INSTANCE = new SolonPropertiesFileType();

    private SolonPropertiesFileType() {
        super(PropertiesLanguage.INSTANCE, true);
    }

    @Override
    public @NonNls @NotNull String getName() {
        return "solon-properties-file";
    }

    @Override
    public @NlsContexts.Label @NotNull String getDescription() {
        return "Solon properties file";
    }

    @Override
    public @NlsSafe @NotNull String getDefaultExtension() {
        return "properties";
    }

    @Override
    public @Nullable Icon getIcon() {
        return SolonIcons.SolonIcon_16x16;
    }
}
