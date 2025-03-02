package org.noear.solon.idea.plugin.navigation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.noear.solon.idea.plugin.metadata.index.MetadataIndex;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlKeyToPsiReference extends PsiReferenceBase<PsiElement> {
  @NotNull
  private final YAMLKeyValue yamlKeyValue;
  @Nullable
  private final Module module;


  public YamlKeyToPsiReference(@NotNull YAMLKeyValue yamlKeyValue) {
    super(yamlKeyValue, true);
    this.yamlKeyValue = yamlKeyValue;
    this.module = ModuleUtil.findModuleForPsiElement(yamlKeyValue);
  }


  @Override
  public @Nullable PsiElement resolve() {
    if (module == null) {
      System.err.println("Module is null for key: " + yamlKeyValue.getKeyText());
      return null;
    }

    ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
    MetadataIndex metadata = metadataService.getIndex();
    String fullName = YAMLUtil.getConfigFullName(yamlKeyValue);

    MetadataItem propertyOrGroup = metadata.getPropertyOrGroup(fullName);
    if (propertyOrGroup == null) {
      System.err.println("MetadataService is null for module: " + module.getName());
      return null;
    }
    if (propertyOrGroup instanceof MetadataProperty property) {
      return property.getSourceField().orElse(null);
    } else {
      return propertyOrGroup.getType().orElse(null);
    }
  }
}
