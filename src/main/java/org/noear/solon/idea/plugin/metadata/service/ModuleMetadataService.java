package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.noear.solon.idea.plugin.metadata.index.MetadataIndex;
import org.jetbrains.annotations.NotNull;

public interface ModuleMetadataService {

  static ModuleMetadataService getInstance(Module module) {
    return module.getService(ModuleMetadataService.class);
  }

  @NotNull MetadataIndex getIndex();

}
