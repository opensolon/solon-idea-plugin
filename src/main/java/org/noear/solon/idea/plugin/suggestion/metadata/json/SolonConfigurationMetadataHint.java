package org.noear.solon.idea.plugin.suggestion.metadata.json;

import java.util.List;

/**
 * Refer to http://solon.noear.org/article/443
 */
public class SolonConfigurationMetadataHint {

    private String name;

    private List<SolonConfigurationMetadataHintValue> values;

    public String getName() {
        return name;
    }

    public SolonConfigurationMetadataHint setName(String name) {
        this.name = name;
        return this;
    }

    public List<SolonConfigurationMetadataHintValue> getValues() {
        return values;
    }

    public SolonConfigurationMetadataHint setValues(List<SolonConfigurationMetadataHintValue> values) {
        this.values = values;
        return this;
    }
}
