package org.noear.solon.idea.plugin.common.util;

public class StringUtil {
    public static String processLookupString(String queryString, String lookupString) {
        // 如果 lookupString 不包含 *，直接返回
        if (!lookupString.contains("*")) {
            return lookupString;
        }

        // 将 queryString 和 lookupString 按 . 分割
        String[] queryParts = queryString.split("\\.");
        String[] lookupParts = lookupString.split("\\.");

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lookupParts.length; i++) {
            if (i > 0) {
                result.append(".");
            }

            String lookupPart = lookupParts[i];

            // 如果当前部分包含 *
            if (lookupPart.contains("*")) {
                // 检查是否是数组索引格式 [*]
                if (lookupPart.matches(".*\\[\\*\\].*")) {
                    // 如果 queryString 对应位置有具体的索引值，替换 *
                    if (i < queryParts.length && queryParts[i].matches(".*\\[\\d+\\].*")) {
                        // 提取 query 中的索引值
                        String queryPart = queryParts[i];
                        String indexValue = queryPart.replaceAll(".*\\[(\\d+)\\].*", "$1");
                        result.append(lookupPart.replace("*", indexValue));
                    } else {
                        result.append(lookupPart);
                    }
                } else {
                    // 普通的 * 通配符
                    if (i < queryParts.length) {
                        // 如果 query 有对应的部分，用 query 的值替换 *
                        String queryPart = queryParts[i];
                        result.append(lookupPart.replace("*", queryPart));
                    } else {
                        result.append(lookupPart);
                    }
                }
            } else {
                result.append(lookupPart);
            }
        }

        return result.toString();
    }
}
