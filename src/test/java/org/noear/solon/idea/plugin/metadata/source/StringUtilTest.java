package org.noear.solon.idea.plugin.metadata.source;

import org.junit.jupiter.api.Test;
import org.noear.solon.idea.plugin.common.util.StringUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilTest {

    @Test
    void shouldKeepLookupStringWhenQueryHasNoMatch() {
        // queryString: demo.
        // lookupString: demo.aConfigMap.*.name
        // resultLookupString: demo.aConfigMap.*.name
        testProcessLookupString("demo.", "demo.aConfigMap.*.name", "demo.aConfigMap.*.name");
    }

    @Test
    void shouldReplaceWildcardIndexWithQueryIndex() {
        // queryString: demo.bConfigs[10]
        // lookupString: demo.bConfigs[*].defaultValue
        // resultLookupString: demo.bConfigs[10].defaultValue
        testProcessLookupString("demo.bConfigs[10]", "demo.bConfigs[*].defaultValue", "demo.bConfigs[10].defaultValue");
    }

    @Test
    void shouldReplaceMapKeyWildcardWithQueryKey() {
        // queryString: demo.aConfigMap.aaaff
        // lookupString: demo.aConfigMap.*.defaultValue
        // resultLookupString: demo.aConfigMap.aaaff.defaultValue
        testProcessLookupString("demo.aConfigMap.aaaff", "demo.aConfigMap.*.defaultValue", "demo.aConfigMap.aaaff.defaultValue");
    }

    private static void testProcessLookupString(String queryString, String lookupString, String expected) {
        String actual = StringUtil.processLookupString(queryString, lookupString);
        assertEquals(expected, actual,
                "queryString=" + queryString + ", lookupString=" + lookupString);
    }
}
