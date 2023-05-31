package org.noear.solon.idea.plugin.initializr.metadata;

public enum TypeOption {

    MAVEN("maven", "Maven"),
    GRADLE_GROOVY("gradle_groovy", "Gradle - Groovy"),

    GRADLE_KOTLIN("gradle_kotlin", "Gradle - Kotlin");

    private String value;
    private String label;

    TypeOption(String value, String label){
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
