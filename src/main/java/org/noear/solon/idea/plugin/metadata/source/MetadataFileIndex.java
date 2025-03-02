package org.noear.solon.idea.plugin.metadata.source;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.noear.solon.idea.plugin.misc.ModuleRootUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class MetadataFileIndex extends ScalarIndexExtension<String> {

  private static final Logger log = Logger.getInstance(MetadataFileIndex.class);

  public static final String META_FILE_DIR = "META-INF";
  public static final String SOLON_FILE_DIR = "solon";
  public static final String METADATA_FILE_NAME = "solon-configuration-metadata.json";
  public static final String METADATA_FILE = META_FILE_DIR + "/" + SOLON_FILE_DIR + "/" + METADATA_FILE_NAME;

  public static final String ADDITIONAL_METADATA_FILE_NAME = "additional-solon-configuration-metadata.json";
  public static final String ADDITIONAL_METADATA_FILE = META_FILE_DIR + "/" + SOLON_FILE_DIR + "/" + ADDITIONAL_METADATA_FILE_NAME;
  public static final ID<String, Void> NAME = ID.create(MetadataFileIndex.class.getCanonicalName());

  private static final String UNIQUE_KEY = "META";


  @NotNull
  public static Collection<VirtualFile> getFiles(@NotNull GlobalSearchScope scope) {
    Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(NAME, UNIQUE_KEY, scope);
    Collection<VirtualFile> filteredFiles = files.stream()
            .filter(vf -> isMetaFile(vf, Objects.requireNonNull(scope.getProject())))
            .toList();
    return filteredFiles;
  }


  public static boolean maybeMetaFile(VirtualFile file) {
    String name = file.getName();
    if (!name.equals(METADATA_FILE_NAME) && !name.equals(ADDITIONAL_METADATA_FILE_NAME)) {
      return false;
    }
    VirtualFile parent = file.getParent();
    if (parent == null || !parent.getName().equals(SOLON_FILE_DIR)) {
      return false;
    }
    // If the 'solon-configuration-metadata.json' is generated, 'additional-solon-configuration-metadata.json' will
    // be merged into it. But 'additional-solon-configuration-metadata.json' should be load in case of the
    // 'solon-configuration-metadata.json' is not generated, i.e., there is no `@ConfigurationProperties` annotated class.
    return !name.equals(ADDITIONAL_METADATA_FILE_NAME) || parent.findChild(METADATA_FILE_NAME) == null;
  }


  public static boolean isMetaFile(@NotNull VirtualFile file, @NotNull Project project) {
    if (!file.isValid()) {
      return false;
    }
    String name = file.getName();
    if (!name.equals(METADATA_FILE_NAME) && !name.equals(ADDITIONAL_METADATA_FILE_NAME)) {
      return false;
    }
    VirtualFile classRoot;
    if (file.isInLocalFileSystem()) {
      @Nullable Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file, false);
      if (module == null) {
        return false;
      }
      VirtualFile[] classesRoots = ModuleRootUtils.getClassRootsWithoutLibraries(module);
      classRoot = null;
      for (VirtualFile root : classesRoots) {
        if (VfsUtilCore.isAncestor(root, file, false)) {
          classRoot = root;
          break;
        }
      }
      if (classRoot == null) {
        return false;
      }
    } else {
      classRoot = VfsUtilCore.getRootFile(file);
    }
    String relPath = VfsUtilCore.getRelativePath(file, classRoot);
    return relPath != null && (relPath.equals(METADATA_FILE) || relPath.equals(ADDITIONAL_METADATA_FILE));
  }


  @Nullable
  public static VirtualFile findMetaFileInClassRoot(@NotNull VirtualFile classRoot) {
    VirtualFile f = classRoot.findFileByRelativePath(METADATA_FILE);
    if (f != null) {
      return f;
    }
    return classRoot.findFileByRelativePath(ADDITIONAL_METADATA_FILE);
  }


  @Override
  public @NotNull ID<String, Void> getName() {
    return NAME;
  }


  @Override
  public @NotNull DataIndexer<String, Void, FileContent> getIndexer() {
    return new DataIndexer<>() {
      @Override
      public @NotNull Map<String, Void> map(@NotNull FileContent inputData) {
        return Collections.singletonMap(UNIQUE_KEY, null);
      }
    };
  }


  @Override
  public @NotNull FileBasedIndex.InputFilter getInputFilter() {
    return MetadataFileIndex::maybeMetaFile;
  }


  @Override
  public @NotNull KeyDescriptor<String> getKeyDescriptor() {
    return EnumeratorStringDescriptor.INSTANCE;
  }


  @Override
  public int getVersion() {
    return 1;
  }


  @Override
  public boolean dependsOnFileContent() {
    return false;
  }
}
