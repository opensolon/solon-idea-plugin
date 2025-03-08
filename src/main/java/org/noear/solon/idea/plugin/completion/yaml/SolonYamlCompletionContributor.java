package org.noear.solon.idea.plugin.completion.yaml;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import org.jetbrains.yaml.YAMLLanguage;
import org.noear.solon.idea.plugin.filetype.SolonYamlFileType;

import static com.intellij.patterns.PlatformPatterns.virtualFile;

public class SolonYamlCompletionContributor extends CompletionContributor {
    public SolonYamlCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE)
                        .inVirtualFile(virtualFile().ofType(SolonYamlFileType.INSTANCE)),
                new YamlCompletionProvider()
        );
    }
}
