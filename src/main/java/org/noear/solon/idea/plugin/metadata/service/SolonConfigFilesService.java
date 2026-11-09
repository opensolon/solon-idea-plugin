package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.filetype.SolonPropertiesFileType;
import org.noear.solon.idea.plugin.filetype.SolonYamlFileType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Solon 配置文件识别服务。
 *
 * <p>本插件原本只把 {@code app*.yml} / {@code app*.properties} 当作 Solon 配置文件
 * （见 {@link SolonYamlFileType}、{@link SolonPropertiesFileType}）。但 Solon 插件
 * 组件（如 {@code ctsz-plugin-oauth2}）惯用
 * {@code context.cfg().loadAddIfAbsent("classpath:oauth2.yml")} 在 {@code Plugin#start}
 * 里加载自带的默认配置，这些文件既不在 app 命名规则内，也不再有任何 {@code @Inject}
 * 或 {@code toBean} 绑定声明（绑定可能发生在别的工程），因此需要单独识别。</p>
 *
 * <p>识别规则：扫描项目源码中对 {@code Props#loadAdd(String)} /
 * {@code Props#loadAddIfAbsent(String)} 的调用，取出 {@code classpath:} 字面量，
 * 在调用方所在模块内解析出真实的资源文件。</p>
 */
@Service(Service.Level.PROJECT)
public final class SolonConfigFilesService {
    private static final Logger LOG = Logger.getInstance(SolonConfigFilesService.class);

    /** Solon {@code Props} 中接收 classpath 字符串的加载方法。 */
    private static final Set<String> LOAD_METHOD_NAMES = Set.of("loadAdd", "loadAddIfAbsent");
    private static final String CLASSPATH_SCHEME = "classpath:";

    private static final Key<CachedValue<List<LoadedConfig>>> LOADED_CONFIGS_KEY =
            Key.create("solon.loaded.config.files");
    private static final Key<CachedValue<List<ConfigFile>>> CONFIG_FILES_KEY =
            Key.create("solon.config.files");

    /** 识别 Solon 配置文件的扩展名。 */
    private static final List<String> CONFIG_EXTENSIONS = List.of("yml", "yaml", "properties");

    private final Project project;

    public SolonConfigFilesService(Project project) {
        this.project = project;
    }

    public static SolonConfigFilesService getInstance(@NotNull Project project) {
        return project.getService(SolonConfigFilesService.class);
    }

    /**
     * 被 {@code loadAdd*} 加载的配置文件。
     *
     * @param file     配置文件本身
     * @param callSite {@code loadAdd*} 调用点，便于后续把 key 定位到加载代码
     * @param module   调用方所在模块，约定式匹配只在该模块内找配置类
     */
    public record LoadedConfig(
            @NotNull VirtualFile file,
            @NotNull PsiElement callSite,
            @NotNull Module module) {
    }

    /**
     * 项目内的 Solon 配置文件。
     *
     * @param file   配置文件本身
     * @param module 文件所在模块，可能为 null（不在任何模块内容根下）
     */
    public record ConfigFile(
            @NotNull VirtualFile file,
            @Nullable Module module) {
    }

    /** 判断文件是否应被视为 Solon 配置文件（app 文件，或由 {@code loadAdd*} 加载的文件）。 */
    public boolean isSolonConfigFile(@Nullable VirtualFile file) {
        if (file == null || file.isDirectory()) {
            return false;
        }
        if (file.getFileType() == SolonYamlFileType.INSTANCE
                || file.getFileType() == SolonPropertiesFileType.INSTANCE) {
            return true;
        }
        for (LoadedConfig config : getLoadedConfigs()) {
            if (file.equals(config.file())) {
                return true;
            }
        }
        return false;
    }

    /** 项目内所有通过 {@code loadAdd*} 声明的插件默认配置文件（按文件去重）。 */
    @NotNull
    public List<LoadedConfig> getLoadedConfigs() {
        if (DumbService.isDumb(project)) {
            return List.of();
        }
        return CachedValuesManager.getManager(project).getCachedValue(project, LOADED_CONFIGS_KEY,
                () -> CachedValueProvider.Result.create(computeLoadedConfigs(),
                        // loadAdd* 调用只存在于 Java 源码中，按 Java PSI 修改计数失效即可；
                        // 用全局 MODIFICATION_COUNT 会导致编辑任意 yml 都重算一次 ReferencesSearch。
                        PsiModificationTracker.getInstance(project).forLanguage(JavaLanguage.INSTANCE)),
                false);
    }

    /**
     * 项目内所有 Solon 配置文件：{@code app*.yml/yaml/properties}
     * 以及被 {@code loadAdd*} 加载的插件默认配置。
     *
     * <p>用于约定式属性 ↔ 配置类映射。消费端工程（如 qingzhou-boot）的 app.yml
     * 必须在这里出现，否则配置在插件 jar 中的对象无法被关联。</p>
     */
    @NotNull
    public List<ConfigFile> getSolonConfigFiles() {
        if (DumbService.isDumb(project)) {
            return List.of();
        }
        return CachedValuesManager.getManager(project).getCachedValue(project, CONFIG_FILES_KEY,
                () -> CachedValueProvider.Result.create(computeConfigFiles(),
                        PsiModificationTracker.MODIFICATION_COUNT),
                false);
    }

    @NotNull
    private List<ConfigFile> computeConfigFiles() {
        List<ConfigFile> result = new ArrayList<>();
        try {
            ReadAction.run(() -> {
                Set<String> seen = new HashSet<>();
                GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
                for (String extension : CONFIG_EXTENSIONS) {
                    for (VirtualFile file : FilenameIndex.getAllFilesByExt(project, extension, scope)) {
                        if (!isSolonConfigFile(file) || !seen.add(file.getUrl())) {
                            continue;
                        }
                        result.add(new ConfigFile(file,
                                ModuleUtil.findModuleForFile(file, project)));
                    }
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to collect Solon config files", e);
        }
        return result;
    }

    @NotNull
    private List<LoadedConfig> computeLoadedConfigs() {
        List<LoadedConfig> result = new ArrayList<>();
        try {
            ReadAction.run(() -> {
                Set<String> seen = new HashSet<>();
                GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
                GlobalSearchScope sourceScope = GlobalSearchScope.projectScope(project);

                for (String methodName : LOAD_METHOD_NAMES) {
                    Collection<PsiMethod> methods = JavaMethodNameIndex.getInstance()
                            .get(methodName, project, allScope);
                    for (PsiMethod method : methods) {
                        PsiParameter[] parameters = method.getParameterList().getParameters();
                        if (parameters.length != 1
                                || !parameters[0].getType().equalsToText("java.lang.String")) {
                            // 跳过 loadAdd(Import/URL/Properties) 等重载
                            continue;
                        }
                        ReferencesSearch.search(method, sourceScope).forEach(reference -> {
                            PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(
                                    reference.getElement(), PsiMethodCallExpression.class, false);
                            if (call == null) return;

                            PsiExpressionList arguments = call.getArgumentList();
                            if (arguments.getExpressionCount() != 1) return;
                            String uri = literalText(arguments.getExpressions()[0]);
                            if (uri == null || !uri.startsWith(CLASSPATH_SCHEME)) return;

                            Module module = ModuleUtil.findModuleForPsiElement(call);
                            if (module == null) return;
                            VirtualFile file = resolveClasspathResource(module,
                                    uri.substring(CLASSPATH_SCHEME.length()));
                            if (file == null) return;
                            if (seen.add(file.getUrl())) {
                                result.add(new LoadedConfig(file, call, module));
                            }
                        });
                    }
                }
                LOG.info("Solon plugin config files loaded by loadAdd*: " + result.size());
            });
        } catch (Exception e) {
            LOG.warn("Failed to detect Solon plugin config files", e);
        }
        return result;
    }

    /**
     * 把 {@code classpath:} 后面的相对路径解析为模块内的资源文件。
     * <p>注意不能用文件名索引直接取第一个结果：同项目多模块经常有同名 yml，
     * 必须按调用方模块 + 路径后缀精确匹配。</p>
     */
    @Nullable
    private VirtualFile resolveClasspathResource(@NotNull Module module, @NotNull String path) {
        String normalized = path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            return null;
        }
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;

        PsiFile[] candidates = FilenameIndex.getFilesByName(
                project, fileName, GlobalSearchScope.moduleScope(module));
        for (PsiFile psiFile : candidates) {
            VirtualFile candidate = psiFile.getVirtualFile();
            if (candidate == null || !candidate.isValid() || candidate.isDirectory()
                    || !candidate.isInLocalFileSystem()) {
                // jar 内的资源无法作为可编辑的配置来源，跳过
                continue;
            }
            String filePath = candidate.getPath();
            if (filePath.endsWith("/" + normalized)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 提取字符串字面量；也支持 {@code private static final String PATH = "classpath:x.yml"} 这类常量引用。
     */
    @Nullable
    public static String literalText(@NotNull PsiExpression expression) {
        if (expression instanceof PsiLiteralExpression literal
                && literal.getValue() instanceof String text) {
            return text;
        }
        if (expression instanceof PsiReferenceExpression reference
                && reference.resolve() instanceof PsiField field
                && field.getInitializer() instanceof PsiLiteralExpression initializer
                && initializer.getValue() instanceof String text) {
            return text;
        }
        return null;
    }
}
