package org.noear.solon.idea.plugin.suggestion.metadata.json;

import java.util.List;

/**
 * Refer to http://solon.noear.org/article/443
 */
public class SolonConfigurationMetadataProperties {

    private List<SolonConfigurationMetadataProperty> properties;

    public List<SolonConfigurationMetadataProperty> getProperties() {
        return properties;
    }

    public SolonConfigurationMetadataProperties setProperties(List<SolonConfigurationMetadataProperty> properties) {
        this.properties = properties;
        return this;
    }
}
