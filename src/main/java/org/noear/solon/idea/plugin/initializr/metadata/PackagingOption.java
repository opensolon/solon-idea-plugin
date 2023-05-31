package org.noear.solon.idea.plugin.initializr.metadata;

public enum PackagingOption {

    JAR("jar", "Jar"),
    WAR("war", "War");

    private String value;
    private String label;

    PackagingOption(String value, String label){
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
