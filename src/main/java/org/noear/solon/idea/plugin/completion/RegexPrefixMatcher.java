package org.noear.solon.idea.plugin.completion;

import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexPrefixMatcher extends PrefixMatcher {
    private static final Logger LOG = Logger.getInstance(RegexPrefixMatcher.class);
    private final Pattern pattern;
    private final boolean caseSensitive;

    public RegexPrefixMatcher(@NotNull String prefix, boolean caseSensitive) {
        super(prefix);
        this.caseSensitive = caseSensitive;
        this.pattern = createPattern(prefix, caseSensitive);
    }

    private Pattern createPattern(String prefix, boolean caseSensitive) {
        try {
            // 将 * 转换为 .* 以匹配任意字符串
            String regexPattern = prefix
                    .replace(".", "\\.")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("*", ".*");
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(regexPattern, flags);
        } catch (PatternSyntaxException e) {
            // 如果正则表达式无效，回退到字面量匹配
            String escaped = Pattern.quote(prefix);
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            return Pattern.compile(escaped, flags);
        }
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
        boolean result = pattern.matcher(name).find();
        LOG.info("prefixMatches: " + result + " for name: " + name + " with pattern: " + pattern.pattern());
        return result;
    }

    @Override
    public boolean prefixMatches(@NotNull LookupElement element) {
        return element.getAllLookupStrings().stream()
                .anyMatch(this::prefixMatches);
    }

    @Override
    public boolean isStartMatch(@NotNull String name) {
        return pattern.matcher(name).lookingAt();
    }

    @Override
    public boolean isStartMatch(@NotNull LookupElement element) {
        return element.getAllLookupStrings().stream()
                .anyMatch(this::isStartMatch);
    }

    @Override
    public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
        if (prefix.equals(myPrefix)) {
            return this;
        }
        return new RegexPrefixMatcher(prefix, caseSensitive);
    }

    @Override
    public int matchingDegree(@NotNull String string) {
        if (!prefixMatches(string)) {
            return Integer.MIN_VALUE;
        }

        // 计算匹配度：完全匹配 > 开始匹配 > 包含匹配
        if (string.equals(myPrefix.replace("*", ""))) {
            return 100;
        } else if (isStartMatch(string)) {
            return 50;
        } else {
            return 10;
        }
    }
}