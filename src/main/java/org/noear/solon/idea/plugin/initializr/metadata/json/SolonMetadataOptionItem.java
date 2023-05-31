package org.noear.solon.idea.plugin.initializr.metadata.json;

import com.intellij.openapi.util.text.StringUtil;

public class SolonMetadataOptionItem {

    private String value;
    private String title;
    private String description;

    public void setValue(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SolonMetadataOptionItem setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        return title + (StringUtil.isNotEmpty(description) ? " - " + description : "");
    }
}
