package org.noear.solon.idea.plugin.metadata.index;

import org.noear.solon.idea.plugin.metadata.index.hint.provider.ValueProvider;
import org.noear.solon.idea.plugin.metadata.index.hint.value.ValueHint;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata.Hint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface MetadataHint {
  Hint getMetadata();

  @NotNull
  List<ValueHint> getValues();

  @NotNull
  List<ValueProvider> getProviders();
}
