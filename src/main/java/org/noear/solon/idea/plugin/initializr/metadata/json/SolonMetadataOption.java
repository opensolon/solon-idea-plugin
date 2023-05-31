package org.noear.solon.idea.plugin.initializr.metadata.json;

import com.intellij.openapi.util.text.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

public class SolonMetadataOption {

    private String defaultValue;
    private List<SolonMetadataOptionItem> options;
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setOptions(List<SolonMetadataOptionItem> options) {
        this.options = options;
    }
    public List<SolonMetadataOptionItem> getOptions() {
        return options;
    }

    public SolonMetadataOptionItem getByValue(String value){
        if (options == null){
            return null;
        }
        List<SolonMetadataOptionItem> collect = options.stream().filter((item) -> StringUtil.equals(item.getValue(), value)).collect(Collectors.toList());
        if (collect.size() == 0){
            return options.get(0);
        }
        return collect.get(0);
    }

}
