package org.noear.solon.idea.plugin.completion.properties;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.patterns.PlatformPatterns;
import org.noear.solon.idea.plugin.filetype.SolonPropertiesFileType;

import static com.intellij.patterns.PlatformPatterns.virtualFile;

public class SolonPropertiesCompletionContributor extends CompletionContributor {
    public SolonPropertiesCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(PropertiesLanguage.INSTANCE)
                        .inVirtualFile(virtualFile().ofType(SolonPropertiesFileType.INSTANCE)),
                new PropertiesCompletionProvider()
        );
    }
}
