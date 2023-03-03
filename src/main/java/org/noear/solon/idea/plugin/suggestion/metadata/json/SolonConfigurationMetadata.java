package org.noear.solon.idea.plugin.suggestion.metadata.json;

import org.apache.commons.collections.list.PredicatedList;

import java.util.ArrayList;
import java.util.List;

public class SolonConfigurationMetadata {

    private List<SolonConfigurationMetadataProperty> properties;

    private List<SolonConfigurationMetadataHint> hints;

    public List<SolonConfigurationMetadataProperty> getProperties() {
        if (properties == null){
            return new ArrayList<>();
        }
        return properties;
    }

    public SolonConfigurationMetadata setProperties(List<SolonConfigurationMetadataProperty> properties) {
        this.properties = properties;
        return this;
    }

    public List<SolonConfigurationMetadataHint> getHints() {
        if (hints == null){
            return new ArrayList<>();
        }
        return hints;
    }

    public SolonConfigurationMetadata setHints(List<SolonConfigurationMetadataHint> hints) {
        this.hints = hints;
        return this;
    }
}
