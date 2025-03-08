package org.noear.solon.idea.plugin.metadata.index.hint.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.PrefixMatcher;
import org.noear.solon.idea.plugin.metadata.index.hint.Hint;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Solon configuration metadata hint, value providers.
 */
public interface ValueProvider {
  static ValueProvider create(ConfigurationMetadata.Hint.ValueProvider metadata) {
    return null;
  }

  default ConfigurationMetadata.Hint.ValueProvider.Type getType() {
    return getMetadata().getName();
  }

  ConfigurationMetadata.Hint.ValueProvider getMetadata();

  Collection<Hint> provideValues(
      @NotNull CompletionParameters completionParameters,
      @Nullable PrefixMatcher prefixMatcher
  );
}
