package org.noear.solon.idea.plugin.initializr.metadata;

import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.observable.properties.GraphPropertyImpl;
import com.intellij.openapi.observable.properties.PropertyGraph;
import com.intellij.ui.layout.ButtonSelectorToolbar;

public enum LanguageOption {

    JAVA("java", "Java"),
    KOTLIN("kotlin", "Kotlin"),
    GROOVY("groovy", "Groovy");

    private String value;
    private String label;

    LanguageOption(String value, String label){
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
