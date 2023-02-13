package org.noear.solon.idea.plugin.suggestion.metadata;

import com.intellij.configurationStore.StateMap;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author liupeiqiang
 * @date 2023/2/9 10:40
 */
public class MetadataContainer {

    public static final String SOLON_CONFIGURATION_METADATA_JSON = "solon-configuration-metadata.json";

    /**
     * Can point to archive/directory containing the metadata file
     */
    private String containerArchiveOrFileRef;
    @Nullable
    private String fileUrl;
    private boolean archive;
    private long marker;

    public static Stream<String> getContainerArchiveOrFileRefs(VirtualFile fileContainer) {
        if (fileContainer.getFileType() == FileTypes.ARCHIVE) {
            return Stream.of(getContainerFile(fileContainer).getUrl());
        } else {
            VirtualFile metadataFile =
                    findMetadataFile(fileContainer, SOLON_CONFIGURATION_METADATA_JSON);
            if (metadataFile == null) {
                return Stream.of(fileContainer.getUrl());
            } else {
                return Stream.of(metadataFile.getUrl());
            }
        }
    }

    public static Collection<MetadataContainer> newInstances(VirtualFile fileContainer) {
        Collection<MetadataContainer> containerInfos = new ArrayList<>();
        VirtualFile containerFile = getContainerFile(fileContainer);
        boolean archive = fileContainer.getFileType() == FileTypes.ARCHIVE;
        MetadataContainer containerInfo =
                newInstance(fileContainer, containerFile, SOLON_CONFIGURATION_METADATA_JSON, archive);
        containerInfos.add(containerInfo);
        return containerInfos;
    }

    private static MetadataContainer newInstance(VirtualFile fileContainer,
                                                     VirtualFile containerFile, String metadataFileName, boolean archive) {
        MetadataContainer metadataContainer = new MetadataContainer();
        metadataContainer.setArchive(archive);
        VirtualFile metadataFile = findMetadataFile(fileContainer, metadataFileName);
        if (metadataFile != null) {
            // since build might auto generate the metadata file in the project, its better to rely on
            metadataContainer.setFileUrl(metadataFile.getUrl());
            metadataContainer.setContainerArchiveOrFileRef(archive ? containerFile.getUrl() : metadataFile.getUrl());
            metadataContainer.setMarker(archive ? metadataFile.getModificationCount() : metadataFile.getModificationStamp());
        } else {
            metadataContainer.setContainerArchiveOrFileRef(containerFile.getUrl());
            metadataContainer.setMarker(containerFile.getModificationCount());
        }
        return metadataContainer;
    }

    /**
     * findMetadataFile by DFS
     */
    private static VirtualFile findMetadataFile(VirtualFile root, String metadataFileName) {
        if (!root.is(VFileProperty.SYMLINK)) {
            for (VirtualFile child : Arrays.asList(root.getChildren())) {
                if (child.getName().equals(metadataFileName)) {
                    return child;
                }
                VirtualFile matchedFile = findMetadataFile(child, metadataFileName);
                if (matchedFile != null) {
                    return matchedFile;
                }
            }
        }
        return null;
    }

    private static VirtualFile getContainerFile(VirtualFile fileContainer) {
        if (fileContainer.getFileType() == FileTypes.ARCHIVE) {
            return requireNonNull(JarFileSystem.getInstance().getLocalVirtualFileFor(fileContainer));
        } else {
            return fileContainer;
        }
    }

    public boolean containsMetadataFile() {
        return fileUrl != null;
    }

    public VirtualFile getMetadataFile() {
        assert fileUrl != null;
        return VirtualFileManager.getInstance().findFileByUrl(fileUrl);
    }

    public void setContainerArchiveOrFileRef(String containerArchiveOrFileRef) {
        this.containerArchiveOrFileRef = containerArchiveOrFileRef;
    }

    public long getMarker() {
        return marker;
    }

    public MetadataContainer setMarker(long marker) {
        this.marker = marker;
        return this;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public String getContainerArchiveOrFileRef() {
        return containerArchiveOrFileRef;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public boolean isArchive() {
        return archive;
    }

    public boolean isModified(MetadataContainer other) {
        return this.marker != other.marker;
    }
}
