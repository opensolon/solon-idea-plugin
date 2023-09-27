package org.noear.solon.idea.plugin.suggestion.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.patterns.PlatformPatterns;
import org.jetbrains.yaml.YAMLLanguage;

import java.util.HashMap;
import java.util.Map;

public class YamlCompletionContributor extends CompletionContributor {
    public static Map<String,Map<String,Map>> yamlMapCache=new HashMap();
    public YamlCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider()
        );
    }
}
