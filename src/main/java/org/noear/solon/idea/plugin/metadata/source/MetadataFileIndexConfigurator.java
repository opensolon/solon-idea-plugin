package org.noear.solon.idea.plugin.metadata.source;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.IndexableSetContributor;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.common.util.ProjectUtil;

import java.util.Set;

/**
 * Add project's {@code <module-output-dir>/META-INF/(additional-)solon-configuration-metadata.json} files into file-based index, for {@link MetadataFileIndex}.
 */
public class MetadataFileIndexConfigurator extends IndexableSetContributor {

    private static final Logger LOG = Logger.getInstance(MetadataFileIndex.class);

    @Override
    public @NotNull Set<VirtualFile> getAdditionalRootsToIndex() {
        return Set.of();
    }

    @Override
    public @NotNull Set<VirtualFile> getAdditionalProjectRootsToIndex(@NotNull Project project) {
        return ProjectUtil.getAdditionalProjectRootsToIndex(project);
    }
}
