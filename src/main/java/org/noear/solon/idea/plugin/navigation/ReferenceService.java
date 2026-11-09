package org.noear.solon.idea.plugin.navigation;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.collection.CompositeCollection;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlRecursivePsiElementVisitor;
import org.noear.solon.idea.plugin.filetype.SolonPropertiesFileType;
import org.noear.solon.idea.plugin.filetype.SolonYamlFileType;
import org.noear.solon.idea.plugin.metadata.index.MetadataGroup;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;
import org.noear.solon.idea.plugin.metadata.service.SolonConfigFilesService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service(Service.Level.PROJECT)
public final class ReferenceService {
    private static final Logger LOG = Logger.getInstance(ReferenceService.class);

    private final Project project;
    private final YamlIndexHolder yamlIndexHolder = new YamlIndexHolder();
    private final PropertiesIndexHolder propertiesIndexHolder = new PropertiesIndexHolder();


    public ReferenceService(Project project) {
        this.project = project;
    }


    public static ReferenceService getInstance(Project project) {
        return project.getService(ReferenceService.class);
    }

    /**
     * 元数据刷新后清空正向/反向索引缓存。
     * <p>索引缓存的依赖目前只有文件列表，元数据（配置类扫描结果）更新不会自动让缓存失效，
     * 否则首次 Find Usages 若早于扫描完成，会一直命中空结果。</p>
     */
    public void clearIndexCache() {
        yamlIndexHolder.invalidate();
        propertiesIndexHolder.invalidate();
    }

    @Nullable
    private static String getCanonicalName(PsiElement element) {
        if (element instanceof PsiField) {
            PsiClass containingClass = ReadAction.compute(() -> ((PsiField) element).getContainingClass());
            if (containingClass == null) {
                //Not a standard java field, should not happen
                return null;
            }
            return ReadAction.compute(() -> containingClass.getQualifiedName() + "." + ((PsiField) element).getName());
        } else if (element instanceof PsiClass) {
            return ReadAction.compute(() -> ((PsiClass) element).getQualifiedName());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public PsiReference forwardReference(PsiElement source) {
        if (source instanceof YAMLKeyValue yamlKeyValue) {
            return forwardReference(yamlKeyValue);
        } else if (source instanceof PropertyKeyImpl propertyKey) {
            return forwardReference(propertyKey);
        } else {
            throw new IllegalStateException("Unexpected value: " + source);
        }
    }

    public PsiReference forwardReference(YAMLKeyValue yamlKeyValue) {
        return CachedValuesManager.getCachedValue(yamlKeyValue, () ->
                CachedValueProvider.Result.create(new YamlToPsiReference(yamlKeyValue), yamlKeyValue));
    }

    public PsiReference forwardReference(PropertyKeyImpl property) {
        return CachedValuesManager.getCachedValue(property, () ->
                CachedValueProvider.Result.create(new PropertiesToPsiReference(property), property));
    }

    @NotNull
    public Collection<PsiReference> backwardReference(PsiElement psiElement) {
        if (!(psiElement instanceof PsiField || psiElement instanceof PsiClass)) {
            return Collections.emptySet();
        }
        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project)
                    .showDumbModeNotificationForFunctionality("Index is not ready", DumbModeBlockedFunctionality.FindUsages);
            return Collections.emptySet();
        }
        String canonicalName = getCanonicalName(psiElement);

        MultiValuedMap<String, PsiReference> yamlIndex = refreshYamlIndex();
        Collection<PsiReference> yamlReferences = yamlIndex != null ? yamlIndex.get(canonicalName) : Set.of();
        MultiValuedMap<String, PsiReference> propertiesIndex = refreshPropertiesIndex();
        Collection<PsiReference> propertiesReferences =
                propertiesIndex != null ? propertiesIndex.get(canonicalName) : Set.of();
        return new CompositeCollection<>(yamlReferences, propertiesReferences);
    }

    private MultiValuedMap<String, PsiReference> refreshYamlIndex() {
        Collection<VirtualFile> files = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            Set<VirtualFile> result = new HashSet<>(
                    FileTypeIndex.getFiles(SolonYamlFileType.INSTANCE, scope));
            // 插件组件通过 loadAdd* 加载的 yml 不在 app 命名规则内，也需纳入反向索引
            result.addAll(FilenameIndex.getAllFilesByExt(project, "yml", scope));
            result.addAll(FilenameIndex.getAllFilesByExt(project, "yaml", scope));
            SolonConfigFilesService configFiles = SolonConfigFilesService.getInstance(project);
            result.removeIf(file -> !configFiles.isSolonConfigFile(file));
            return result;
        });

        try {
            return this.yamlIndexHolder.getIndex(files);
        } catch (Throwable ex) {
            return new HashSetValuedHashMap<>();
        }
    }

    private MultiValuedMap<String, PsiReference> refreshPropertiesIndex() {
        Collection<VirtualFile> files = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
            GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
            Set<VirtualFile> result = new HashSet<>(
                    FileTypeIndex.getFiles(SolonPropertiesFileType.INSTANCE, scope));
            result.addAll(FilenameIndex.getAllFilesByExt(project, "properties", scope));
            SolonConfigFilesService configFiles = SolonConfigFilesService.getInstance(project);
            result.removeIf(file -> !configFiles.isSolonConfigFile(file));
            return result;
        });

        try {
            return this.propertiesIndexHolder.getIndex(files);
        } catch (Throwable ex) {
            return new HashSetValuedHashMap<>();
        }
    }

    private abstract class AbstractIndexHolder<E extends PsiElement> extends UserDataHolderBase {
        private Collection<VirtualFile> files = Set.of();

        /** 丢弃缓存的索引，下次访问时按最新元数据重建。 */
        void invalidate() {
            clearUserData();
        }

        /**
         * @return A map for {@linkplain #getCanonicalName(PsiElement) canonical} name of a field or class to the YamlKeyValues in application.yaml
         */
        public MultiValuedMap<String, PsiReference> getIndex(Collection<VirtualFile> files) {
            dropCacheIfCollectionChanged(files);
            return CachedValuesManager.getManager(project).getCachedValue(this, () -> {
                // In this block, we return null at any error, makes this function is **Result equivalence**.
                MultiValuedMap<String, PsiReference> index = new HashSetValuedHashMap<>();
                PsiManager psiManager = PsiManager.getInstance(project);
                for (VirtualFile file : files) {
                    if (!file.isValid()) continue;
                    PsiFile psiFile = ReadAction.compute(() -> psiManager.findFile(file));
                    if (psiFile == null) continue;
                    Module module = ModuleUtil.findModuleForFile(psiFile);
                    if (module == null) continue;
                    ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
                    indexFile(psiFile, (key, src) -> {
                        // find if any property matches this key
                        MetadataProperty property = metadataService.getIndex().getProperty(key);
                        if (property != null) {
                            property.getSourceField()
                                    .map(ReferenceService::getCanonicalName)
                                    .filter(Objects::nonNull)
                                    .ifPresent(cn -> index.put(cn, createReference(src)));
                        }
                        // find if any group matches this key
                        MetadataGroup group = metadataService.getIndex().getGroup(key);
                        if (group != null) {
                            group.getType()
                                    .map(ReferenceService::getCanonicalName)
                                    .filter(Objects::nonNull)
                                    .ifPresent(cn -> index.put(cn, createReference(src)));
                        }
                    });
                }
                if (CollectionUtils.isEmpty(files)) {
                    LOG.warn("No files found for indexing");
                    return CachedValueProvider.Result.create(index, ModificationTracker.NEVER_CHANGED);
                }
                return CachedValueProvider.Result.create(index, files);
            });
        }

        private void dropCacheIfCollectionChanged(Collection<VirtualFile> files) {
            Set<String> current = this.files.stream()
                    .filter(VirtualFile::isValid)
                    .map(VirtualFile::getUrl)
                    .collect(Collectors.toSet());
            Set<String> newFiles = files.stream().filter(VirtualFile::isValid).map(VirtualFile::getUrl)
                    .collect(Collectors.toSet());
            if (!newFiles.equals(current)) {
                this.clearUserData();
                this.files = files;
            }
        }


        protected abstract void indexFile(PsiFile file, BiConsumer<String, E> sourceConsumer);

        protected abstract PsiReference createReference(E source);
    }


    private class YamlIndexHolder extends AbstractIndexHolder<YAMLKeyValue> {
        @Override
        protected void indexFile(PsiFile file, BiConsumer<String, YAMLKeyValue> sourceConsumer) {
            assert file instanceof YAMLFile;
            file.accept(new YamlRecursivePsiElementVisitor() {
                @Override
                public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
                    sourceConsumer.accept(ReadAction.compute(() -> YAMLUtil.getConfigFullName(keyValue)), keyValue);
                    super.visitKeyValue(keyValue);
                }
            });
        }


        @Override
        protected PsiReference createReference(YAMLKeyValue source) {
            return forwardReference(source);
        }
    }


    private class PropertiesIndexHolder extends AbstractIndexHolder<Property> {
        @Override
        protected void indexFile(PsiFile file, BiConsumer<String, Property> sourceConsumer) {
            if (file instanceof PropertiesFile propertiesFile) {
                propertiesFile.getProperties().forEach(p -> {
                    if (p instanceof Property property) sourceConsumer.accept(property.getUnescapedKey(), property);
                });
            }
        }


        @Override
        protected PsiReference createReference(Property source) {
            return forwardReference(PsiTreeUtil.findChildOfType(source, PropertyKeyImpl.class));
        }
    }
}
