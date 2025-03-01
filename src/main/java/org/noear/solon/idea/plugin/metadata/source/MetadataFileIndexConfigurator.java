package org.noear.solon.idea.plugin.metadata.source;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.IndexableSetContributor;
import org.noear.solon.idea.plugin.misc.ModuleRootUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Add project's {@code <module-output-dir>/META-INF/(additional-)spring-configuration-metadata.json} files into file-based index, for {@link MetadataFileIndex}.
 */
public class MetadataFileIndexConfigurator extends IndexableSetContributor {
  @Override
  public @NotNull Set<VirtualFile> getAdditionalRootsToIndex() {
    return Set.of();
  }


  @Override
  public @NotNull Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
    Set<VirtualFile> files = new HashSet<>();
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      for (VirtualFile classRoot : ModuleRootUtils.getClassRootsWithoutLibraries(module)) {
        VirtualFile metaDir = classRoot.findChild(MetadataFileIndex.META_FILE_DIR);
        if (metaDir == null) continue;
        VirtualFile f = metaDir.findChild(MetadataFileIndex.METADATA_FILE_NAME);
        if (f != null) files.add(f);
        f = metaDir.findChild(MetadataFileIndex.ADDITIONAL_METADATA_FILE_NAME);
        if (f != null) files.add(f);
      }
    }
    return files;
  }
}
