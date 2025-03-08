package org.noear.solon.idea.plugin.metadata.index;

import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiMethod;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata.Group;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;

public interface MetadataGroup extends MetadataItem {

  @Override
  default @NotNull Pair<String, Icon> getIcon() {
    return new Pair<>("AllIcons.Nodes.Folder", AllIcons.Nodes.Folder);
  }

  Optional<PsiMethod> getSourceMethod();

  Group getMetadata();
}
