package org.noear.solon.idea.plugin.suggestion.metadata.json;

/**
 * Refer to http://solon.noear.org/article/443
 */
public class SolonConfigurationMetadataHintValue {

    private String value;

    private String description;

    public String getValue() {
        return value;
    }

    public SolonConfigurationMetadataHintValue setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SolonConfigurationMetadataHintValue setDescription(String description) {
        this.description = description;
        return this;
    }
}
