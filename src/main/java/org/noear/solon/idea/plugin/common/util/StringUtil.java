package org.noear.solon.idea.plugin.common.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {
    public static String processLookupString(String queryString, String lookupString) {
        String qs = queryString.endsWith(".") ? queryString.substring(0, queryString.length() - 1) : queryString;
        String[] queryParts = qs.split("\\.");
        String[] lookupParts = lookupString.split("\\.");
        List<String> resultParts = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < lookupParts.length && j < queryParts.length) {
            String lookupPart = lookupParts[i];
            String queryPart = queryParts[j];
            if ("*".equals(lookupPart)) {
                resultParts.add(queryPart);
                i++;
                j++;
            } else {
                if (queryPart.equals(lookupPart)) {
                    resultParts.add(lookupPart);
                    i++;
                    j++;
                } else if (queryPart.startsWith(lookupPart)) {
                    if (i + 1 < lookupParts.length && "*".equals(lookupParts[i + 1])) {
                        resultParts.add(queryPart);
                        i += 2;
                        j++;
                    } else {
                        for (int k = i; k < lookupParts.length; k++) {
                            resultParts.add(lookupParts[k]);
                        }
                        return String.join(".", resultParts);
                    }
                } else {
                    for (int k = i; k < lookupParts.length; k++) {
                        resultParts.add(lookupParts[k]);
                    }
                    return String.join(".", resultParts);
                }
            }
        }
        if (i < lookupParts.length) {
            for (int k = i; k < lookupParts.length; k++) {
                resultParts.add(lookupParts[k]);
            }
        }
        return String.join(".", resultParts);
    }
}