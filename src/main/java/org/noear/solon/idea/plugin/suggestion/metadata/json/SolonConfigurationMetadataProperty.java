package org.noear.solon.idea.plugin.suggestion.metadata.json;

/**
 * Refer to http://solon.noear.org/article/443
 */
public class SolonConfigurationMetadataProperty {

    private String name;

    private String type;

    private String defaultValue;

    private String description;

    public String getName() {
        return name;
    }

    public SolonConfigurationMetadataProperty setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public SolonConfigurationMetadataProperty setType(String type) {
        this.type = type;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public SolonConfigurationMetadataProperty setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SolonConfigurationMetadataProperty setDescription(String description) {
        this.description = description;
        return this;
    }
}
