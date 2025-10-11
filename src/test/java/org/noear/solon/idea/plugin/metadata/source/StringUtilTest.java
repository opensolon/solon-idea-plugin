package org.noear.solon.idea.plugin.metadata.source;

import org.noear.solon.idea.plugin.common.util.StringUtil;

public class StringUtilTest {
    public static void main(String[] args) {
//  queryString: demo.
        //  lookupString: demo.aConfigMap.*.name
        //  resultLookupString: demo.aConfigMap.*.name
        testProcessLookupString("demo.", "demo.aConfigMap.*.name", "demo.aConfigMap.*.name");

        // queryString: demo.aConfigMap.ueryString: demo.bConfigs[10]
        // lookupString: demo.bConfigs[*].defaultValue
        // resultLookupString: demo.bConfigs[10].defaultValue
        testProcessLookupString("demo.bConfigs[10]", "demo.bConfigs[*].defaultValue", "demo.bConfigs[10].defaultValue");
        //  queryString: demo.aConfigMap.aaaff
        //  lookupString: demo.aConfigMap.*.defaultValue
        //  resultLookupString: demo.aConfigMap.aaaff.defaultValue
        testProcessLookupString("demo.aConfigMap.aaaff", "demo.aConfigMap.*.defaultValue", "demo.aConfigMap.aaaff.defaultValue");
    }

    public static void testProcessLookupString(String queryString, String lookupString, String resultLookupString) {
        String actual = StringUtil.processLookupString(queryString, lookupString);
        System.out.println("queryString: " + queryString);
        System.out.println("lookupString: " + lookupString);
        System.out.println("resultLookupString: " + resultLookupString);
        System.out.println("actual: " + actual);
        boolean flag = resultLookupString.equals(actual);
        System.out.println("flag: " + flag);
        if (!flag) {
            throw new RuntimeException("resultLookupString not equals actual: " + resultLookupString + " != " + actual);
        }
    }
}
