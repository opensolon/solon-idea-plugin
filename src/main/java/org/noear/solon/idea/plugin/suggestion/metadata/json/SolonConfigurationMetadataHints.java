package org.noear.solon.idea.plugin.suggestion.metadata.json;

import java.util.List;

/**
 * Refer to http://solon.noear.org/article/443
 */
public class SolonConfigurationMetadataHints {

    private List<SolonConfigurationMetadataHint> hints;

    public List<SolonConfigurationMetadataHint> getHints() {
        return hints;
    }

    public SolonConfigurationMetadataHints setHints(List<SolonConfigurationMetadataHint> hints) {
        this.hints = hints;
        return this;
    }
}
