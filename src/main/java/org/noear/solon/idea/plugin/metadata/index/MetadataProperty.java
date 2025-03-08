package org.noear.solon.idea.plugin.metadata.index;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A spring configuration metadata property
 */
public interface MetadataProperty extends MetadataItem {
  /**
   * The PsiType include type arguments, for example, {@code Map<String, String>}.
   *
   * @see ConfigurationMetadata.Property#getType()
   */
  Optional<PsiType> getFullType();

  /**
   * @return the field that this property will be bound to, null if not present.
   */
  Optional<PsiField> getSourceField();

  /**
   * get hint or value hint for this property.
   */
  Optional<MetadataHint> getHint();

  /**
   * get key hint for this property if it is a Map.
   */
  Optional<MetadataHint> getKeyHint();

  /**
   * @return whether the specified key can be bound to this property.
   */
  boolean canBind(@NotNull String key);

  default boolean isMapType() {
    return getFullType().filter(p -> PsiTypeUtils.isMap(getIndex().project(), p)).isPresent();
  }

  ConfigurationMetadata.Property getMetadata();
}
