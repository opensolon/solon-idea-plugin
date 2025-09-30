package org.noear.solon.idea.plugin.common.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.metadata.source.MetadataFileIndex;
import org.noear.solon.idea.plugin.misc.ModuleRootUtils;

import java.util.HashSet;
import java.util.Set;

public class ProjectUtil {
    public static @NotNull Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
        Set<VirtualFile> files = new HashSet<>();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            for (VirtualFile classRoot : ModuleRootUtils.getClassRootsWithoutLibraries(module)) {
                VirtualFile metaDir = classRoot.findChild(MetadataFileIndex.META_FILE_DIR);
                if (metaDir == null) {
                    continue;
                }
                VirtualFile solonDir = metaDir.findChild(MetadataFileIndex.SOLON_FILE_DIR);
                if (solonDir == null) {
                    continue;
                }
                VirtualFile f = solonDir.findChild(MetadataFileIndex.METADATA_FILE_NAME);
                if (f != null) {
                    files.add(f);
                }
                f = solonDir.findChild(MetadataFileIndex.ADDITIONAL_METADATA_FILE_NAME);
                if (f != null) {
                    files.add(f);
                }
            }
        }
        return files;
    }
}
