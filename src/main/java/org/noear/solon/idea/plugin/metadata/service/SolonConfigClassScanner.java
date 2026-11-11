package org.noear.solon.idea.plugin.metadata.service;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.impl.java.stubs.index.JavaMethodNameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.AnnotatedMembersSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YamlRecursivePsiElementVisitor;
import org.noear.solon.idea.plugin.metadata.index.AggregatedMetadataIndex;
import org.noear.solon.idea.plugin.metadata.index.MetadataIndex;
import org.noear.solon.idea.plugin.misc.MutableReference;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 扫描项目源码中的 Solon 配置绑定声明，生成 {@link MetadataIndex}。
 *
 * <p>Solon 框架的 jar 里自带 {@code META-INF/solon/solon-configuration-metadata.json}，
 * 但只覆盖框架自身的配置项；用户自定义的配置类必须由本插件从源码里提取。</p>
 *
 * <h3>覆盖的绑定路径（参考 solon.noear.org/article/31）</h3>
 * <ol>
 *   <li>{@code @Inject("${key}")}：类 / 字段 / {@code @Bean} 方法参数</li>
 *   <li>{@code @BindProps(prefix = "...")}：类 / {@code @Bean} 方法</li>
 *   <li>{@code cfg().toBean("prefix", XxxClass.class)}</li>
 *   <li>{@code cfg().getBean("prefix", XxxClass.class)} —— toBean 的别名</li>
 *   <li>{@code cfg().getProp("prefix").bindTo(XxxClass.class)} —— 链式调用</li>
 *   <li>{@code cfg().loadAdd*("classpath:xxx.yml")} 加载的插件默认配置</li>
 *   <li>约定式匹配：{@code app*.yml} 等 Solon 配置中的前缀 ↔ 配置类（含依赖库中的类）</li>
 * </ol>
 *
 * <p><b>注意</b>：查找注解类与框架方法时必须用 {@link GlobalSearchScope#allScope}
 * （包含库），否则 {@code org.noear.solon.annotation.Inject} 位于 jar 内，
 * 用 {@code projectScope} 会直接查不到，导致整个扫描静默失效。</p>
 */
final class SolonConfigClassScanner {
    private static final Logger LOG = Logger.getInstance(SolonConfigClassScanner.class);

    private static final String INJECT_FQN = "org.noear.solon.annotation.Inject";
    private static final String BIND_PROPS_FQN = "org.noear.solon.annotation.BindProps";

    private static final Set<String> BEAN_METHOD_NAMES = Set.of("toBean", "getBean");

    /** 约定式匹配时认可的配置类后缀。 */
    private static final Set<String> CONFIG_CLASS_SUFFIXES =
            Set.of("Properties", "Property", "Props", "Config", "Configuration", "Options", "Settings");

    /** 显式 Key 保证缓存命中（两参 getCachedValue 在每次新建 lambda 时可能无法复用缓存）。 */
    private static final Key<CachedValue<MetadataIndex>> SCAN_CACHE_KEY =
            Key.create("solon.config.class.scan");

    @NotNull
    static MetadataIndex scan(@NotNull Project project) {
        if (DumbService.isDumb(project)) {
            return new AggregatedMetadataIndex();
        }
        // 项目级缓存：多模块项目里每个模块都会调用一次，避免重复全量扫描
        return CachedValuesManager.getManager(project).getCachedValue(project, SCAN_CACHE_KEY,
                () -> CachedValueProvider.Result.create(doScan(project), PsiModificationTracker.MODIFICATION_COUNT),
                false);
    }

    @NotNull
    private static MetadataIndex doScan(@NotNull Project project) {
        ScanContext ctx = new ScanContext(project);
        try {
            ReadAction.run(() -> {
                scanInjectAnnotations(ctx);
                scanBindPropsAnnotations(ctx);
                scanToBeanLikeCalls(ctx);
                scanGetPropBindToCalls(ctx);
                scanConventionBindings(ctx);
            });
            LOG.info("Solon config scan finished: " + ctx.registeredCount()
                    + " binding keys registered");
        } catch (Exception e) {
            LOG.warn("SolonConfigClassScanner failed", e);
        }
        return ctx.index;
    }

    // ================================================================
    // A 系列：注解驱动
    // ================================================================

    /** 扫描 {@code @Inject("${key}")}：类 / 字段 / 方法（返回类型）/ 方法参数。 */
    private static void scanInjectAnnotations(@NotNull ScanContext ctx) {
        PsiClass injectClass = JavaPsiFacade.getInstance(ctx.project)
                .findClass(INJECT_FQN, ctx.allScope);
        if (injectClass == null) {
            LOG.warn("Annotation " + INJECT_FQN + " not found on the project classpath");
            return;
        }

        for (PsiModifierListOwner member : AnnotatedMembersSearch.search(injectClass, ctx.allScope)) {
            String key = extractInjectKey(member);
            if (key != null) {
                ctx.register(key, member);
            }
        }

        // 方法参数不是 PsiMember，需要单独按元素类型搜索
        for (PsiParameter parameter : AnnotatedElementsSearch
                .searchElements(injectClass, ctx.allScope, PsiParameter.class)) {
            String key = extractInjectKey(parameter);
            if (key != null) {
                ctx.register(key, parameter);
            }
        }
    }

    /** 扫描 {@code @BindProps(prefix = "...")}：类 / {@code @Bean} 方法。 */
    private static void scanBindPropsAnnotations(@NotNull ScanContext ctx) {
        PsiClass bindPropsClass = JavaPsiFacade.getInstance(ctx.project)
                .findClass(BIND_PROPS_FQN, ctx.allScope);
        if (bindPropsClass == null) {
            // 旧版 Solon 没有该注解，属正常情况
            return;
        }

        for (PsiModifierListOwner member : AnnotatedMembersSearch.search(bindPropsClass, ctx.allScope)) {
            PsiAnnotation annotation = member.getAnnotation(BIND_PROPS_FQN);
            if (annotation == null) continue;
            String prefix = extractStringAttribute(annotation, "prefix");
            if (prefix != null) {
                ctx.register(prefix, member);
            }
        }
    }

    // ================================================================
    // B 系列：API 驱动
    // ================================================================

    /** 扫描 {@code cfg().toBean("prefix", XxxClass.class)} / {@code getBean(...)}。 */
    private static void scanToBeanLikeCalls(@NotNull ScanContext ctx) {
        for (String methodName : BEAN_METHOD_NAMES) {
            Collection<PsiMethod> methods = JavaMethodNameIndex.getInstance()
                    .get(methodName, ctx.project, ctx.allScope);
            for (PsiMethod method : methods) {
                PsiParameter[] parameters = method.getParameterList().getParameters();
                if (parameters.length != 2 || !isClassType(parameters[1].getType())) {
                    continue;
                }
                ReferencesSearch.search(method, ctx.sourceScope).forEach(reference -> {
                    PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(
                            reference.getElement(), PsiMethodCallExpression.class, false);
                    if (call == null) return;

                    PsiExpressionList arguments = call.getArgumentList();
                    if (arguments.getExpressionCount() < 2) return;
                    String prefix = extractStringLiteralArg(arguments.getExpressions()[0]);
                    if (prefix == null || prefix.isBlank()) return;

                    PsiType beanType = resolveClassArgType(arguments.getExpressions()[1]);
                    ctx.registerByType(prefix, beanType);
                });
            }
        }
    }

    /** 扫描 {@code cfg().getProp("prefix").bindTo(XxxClass.class)} 链式调用。 */
    private static void scanGetPropBindToCalls(@NotNull ScanContext ctx) {
        Collection<PsiMethod> bindToMethods = JavaMethodNameIndex.getInstance()
                .get("bindTo", ctx.project, ctx.allScope);
        for (PsiMethod bindTo : bindToMethods) {
            PsiParameter[] parameters = bindTo.getParameterList().getParameters();
            if (parameters.length != 1 || !isClassType(parameters[0].getType())) {
                continue;
            }
            ReferencesSearch.search(bindTo, ctx.sourceScope).forEach(reference -> {
                PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(
                        reference.getElement(), PsiMethodCallExpression.class, false);
                if (call == null || !"bindTo".equals(call.getMethodExpression().getReferenceName())) {
                    return;
                }

                PsiExpressionList arguments = call.getArgumentList();
                if (arguments.getExpressionCount() < 1) return;
                PsiType beanType = resolveClassArgType(arguments.getExpressions()[0]);
                if (beanType == null) return;

                PsiExpression receiver = call.getMethodExpression().getQualifierExpression();
                if (!(receiver instanceof PsiMethodCallExpression receiverCall)) return;
                // 注意：要取 getProp("prefix") 的参数，而不是把调用表达式本身当字符串解析
                PsiExpressionList receiverArgs = receiverCall.getArgumentList();
                if (receiverArgs.getExpressionCount() < 1) return;
                String prefix = extractStringLiteralArg(receiverArgs.getExpressions()[0]);
                if (prefix == null || prefix.isBlank()) return;

                ctx.registerByType(prefix, beanType);
            });
        }
    }

    // ================================================================
    // C 系列：约定式匹配（配置前缀 ↔ 配置类）
    // ================================================================

    /**
     * 当某段配置没有显式的 {@code toBean}/{@code getBean}/{@code @Inject}/{@code bindTo} 绑定时，
     * 按约定把配置前缀关联到配置类。
     *
     * <p>典型场景（如 {@code ctsz-plugin-cache}）：组件自身没有 yml，配置键写在<em>消费端工程</em>
     * 的 {@code app.yml} 里，而 {@code toBean("ctsz.cache", CacheProperty.class)} 位于消费端引入的
     * <em>依赖 jar</em> 内。消费端工程扫描不到 jar 里的绑定调用，只能靠约定补齐：</p>
     *
     * <ul>
     *   <li>配置前缀的最后一段（{@code ctsz.cache} → {@code cache}）</li>
     *   <li>配置类名去掉后缀（{@code CacheProperty} → {@code Cache}，
     *       {@code OAuth2Properties} → {@code OAuth2}）</li>
     * </ul>
     *
     * <p>为了不误配，必须同时满足：类非接口/抽象、该前缀下至少一个子键能对上类的字段名
     * （忽略大小写与 {@code -}/{@code _}）。查找范围包含依赖库（同名时优先项目源码、同模块类）。
     * 已通过 {@code toBean}/{@code @Inject} 显式绑定的前缀不会被约定覆盖。</p>
     */
    private static void scanConventionBindings(@NotNull ScanContext ctx) {
        List<SolonConfigFilesService.ConfigFile> configFiles =
                SolonConfigFilesService.getInstance(ctx.project).getSolonConfigFiles();
        if (configFiles.isEmpty()) return;

        // 一次遍历建立「类名去后缀（归一化） -> 简单类名」索引，避免每个前缀都全量枚举类名
        Map<String, List<String>> classNamesByBase = collectConfigClassNames(ctx.project);

        for (SolonConfigFilesService.ConfigFile config : configFiles) {
            PsiFile psiFile = PsiManager.getInstance(ctx.project).findFile(config.file());
            if (psiFile == null) continue;

            List<String> fullKeys = collectFullKeys(psiFile);
            if (fullKeys.isEmpty()) continue;

            Map<String, Set<String>> childrenByPrefix = collectChildrenByPrefix(fullKeys);
            for (Map.Entry<String, Set<String>> entry : childrenByPrefix.entrySet()) {
                String prefix = entry.getKey();
                if (ctx.isBoundPrefix(prefix)) continue;
                PsiClass configClass = findConfigClass(ctx, config.module(), prefix,
                        entry.getValue(), classNamesByBase);
                if (configClass == null) continue;

                PsiType type = JavaPsiFacade.getElementFactory(ctx.project).createType(configClass);
                ctx.registerByType(prefix, type);
                LOG.info("Convention binding: " + prefix + " -> "
                        + configClass.getQualifiedName() + " (" + config.file().getName() + ")");
            }
        }
    }

    /** 收集项目内所有「带配置类后缀」的类名，按去后缀归一化后的名字分组。 */
    private static Map<String, List<String>> collectConfigClassNames(@NotNull Project project) {
        Map<String, List<String>> result = new HashMap<>();
        for (String simpleName : PsiShortNamesCache.getInstance(project).getAllClassNames()) {
            String base = stripConfigSuffix(simpleName);
            if (base == null) continue;
            result.computeIfAbsent(normalizeName(base), k -> new ArrayList<>()).add(simpleName);
        }
        return result;
    }

    /** 收集 YAML / properties 文件里的完整 key（{@code ctsz.oauth2.enabled}）。 */
    private static List<String> collectFullKeys(@NotNull PsiFile file) {
        List<String> keys = new ArrayList<>();
        if (file instanceof YAMLFile) {
            file.accept(new YamlRecursivePsiElementVisitor() {
                @Override
                public void visitKeyValue(@NotNull YAMLKeyValue keyValue) {
                    String name = YAMLUtil.getConfigFullName(keyValue);
                    if (name != null && !name.isBlank()) {
                        keys.add(name);
                    }
                    super.visitKeyValue(keyValue);
                }
            });
        } else if (file instanceof PropertiesFile propertiesFile) {
            for (IProperty property : propertiesFile.getProperties()) {
                String key = property.getUnescapedKey();
                if (key != null && !key.isBlank()) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    /**
     * {@code ctsz.oauth2.jwt.issuer} → {@code ctsz.oauth2} / {@code ctsz.oauth2.jwt}
     * 都作为候选前缀，值为该前缀下直接子键的集合。
     */
    private static Map<String, Set<String>> collectChildrenByPrefix(@NotNull List<String> fullKeys) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (String fullKey : fullKeys) {
            String[] segments = fullKey.split("\\.");
            // 至少 2 段前缀 + 1 段子键；单段前缀（如 sa-token.*）多为三方库自带配置，不做约定匹配
            for (int i = 2; i < segments.length; i++) {
                String child = segments[i];
                if (child.isEmpty() || child.contains("*")) continue;
                String prefix = String.join(".", Arrays.copyOfRange(segments, 0, i));
                result.computeIfAbsent(prefix, k -> new LinkedHashSet<>()).add(child);
            }
        }
        return result;
    }

    /**
     * 按「前缀末段 == 配置类名去后缀」在项目 + 依赖库中找配置类。
     *
     * <p>候选类必须至少有一个字段能对上该前缀下的子键；同名候选按
     * 「项目源码 > 同模块 > 字段命中数 > 字段总数」排序，避免误取依赖里的同名壳类。</p>
     */
    @Nullable
    private static PsiClass findConfigClass(
            @NotNull ScanContext ctx, @Nullable Module preferredModule, @NotNull String prefix,
            @NotNull Set<String> children,
            @NotNull Map<String, List<String>> classNamesByBase) {
        int lastDot = prefix.lastIndexOf('.');
        String lastSegment = lastDot >= 0 ? prefix.substring(lastDot + 1) : prefix;
        List<String> simpleNames = classNamesByBase.get(normalizeName(lastSegment));
        if (simpleNames == null) return null;

        PsiShortNamesCache shortNamesCache = PsiShortNamesCache.getInstance(ctx.project);
        GlobalSearchScope allScope = GlobalSearchScope.allScope(ctx.project);
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(ctx.project);
        PsiClass best = null;
        int bestScore = Integer.MIN_VALUE;

        for (String simpleName : simpleNames) {
            for (PsiClass candidate : shortNamesCache.getClassesByName(simpleName, allScope)) {
                if (candidate.isInterface() || candidate.isEnum() || candidate.isAnnotationType()) {
                    continue;
                }
                if (candidate.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    continue;
                }
                int matches = countFieldMatches(candidate, children);
                if (matches == 0) {
                    continue;
                }

                int score = matches;
                PsiFile containingFile = candidate.getContainingFile();
                VirtualFile virtualFile = containingFile == null ? null : containingFile.getVirtualFile();
                if (virtualFile != null && fileIndex.isInSourceContent(virtualFile)) {
                    score += 100;
                }
                if (preferredModule != null
                        && ModuleUtil.findModuleForPsiElement(candidate) == preferredModule) {
                    score += 50;
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** 该前缀下能对上配置类字段的子键数量。 */
    private static int countFieldMatches(@NotNull PsiClass configClass, @NotNull Set<String> children) {
        Set<String> fieldNames = new HashSet<>();
        for (PsiField field : configClass.getAllFields()) {
            fieldNames.add(normalizeName(field.getName()));
        }
        int matches = 0;
        for (String child : children) {
            if (fieldNames.contains(normalizeName(child))) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * 去掉常见配置类后缀；没有后缀则返回 null（嵌套类如 {@code Jwt} 由外层类的递归展开覆盖）。
     */
    @Nullable
    private static String stripConfigSuffix(@NotNull String simpleName) {
        for (String suffix : CONFIG_CLASS_SUFFIXES) {
            if (simpleName.length() > suffix.length() && simpleName.endsWith(suffix)) {
                return simpleName.substring(0, simpleName.length() - suffix.length());
            }
        }
        return null;
    }

    /** 忽略大小写与 {@code -}/{@code _}/{@code .}，用于 yaml key ↔ 类名/字段名比较。 */
    @NotNull
    private static String normalizeName(@NotNull String name) {
        return name.replace("-", "").replace("_", "").replace(".", "").toLowerCase(Locale.ENGLISH);
    }

    // ================================================================
    // 工具方法
    // ================================================================

    @Nullable
    private static String extractInjectKey(@NotNull PsiElement annotated) {
        if (!(annotated instanceof PsiModifierListOwner owner)) return null;
        PsiAnnotation annotation = owner.getAnnotation(INJECT_FQN);
        if (annotation == null) return null;

        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue("value");
        if (!(value instanceof PsiLiteralExpression literal)) return null;
        if (!(literal.getValue() instanceof String text)) return null;
        if (!text.startsWith("${") || !text.endsWith("}")) return null;

        String body = text.substring(2, text.length() - 1);
        // ${classpath:xxx.yml} 之类不是普通配置项
        if (body.startsWith("classpath:")) return null;
        int colon = body.indexOf(':');
        if (colon > 0) {
            body = body.substring(0, colon);
        }
        return body.isBlank() ? null : body;
    }

    @Nullable
    private static String extractStringAttribute(@NotNull PsiAnnotation annotation,
                                                 @NotNull String attributeName) {
        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue(attributeName);
        if (!(value instanceof PsiLiteralExpression literal)) return null;
        if (literal.getValue() instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    @Nullable
    private static PsiType typeOf(@NotNull PsiElement element) {
        if (element instanceof PsiClass psiClass) {
            return JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass);
        } else if (element instanceof PsiField field) {
            return field.getType();
        } else if (element instanceof PsiParameter parameter) {
            return parameter.getType();
        } else if (element instanceof PsiMethod method) {
            PsiType returnType = method.getReturnType();
            // setter 风格：@Inject 打在方法上、返回 void，真正的类型是唯一的参数
            if (returnType != null && PsiType.VOID.equals(returnType)
                    && method.getParameterList().getParametersCount() == 1) {
                return method.getParameterList().getParameters()[0].getType();
            }
            return returnType;
        }
        return null;
    }

    private static boolean isClassType(@NotNull PsiType type) {
        if (!(type instanceof PsiClassType classType)) return false;
        PsiClass resolved = classType.resolve();
        return resolved != null && "java.lang.Class".equals(resolved.getQualifiedName());
    }

    @Nullable
    private static String extractStringLiteralArg(@NotNull PsiExpression expression) {
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

    @Nullable
    private static PsiType resolveClassArgType(@NotNull PsiExpression classArgument) {
        if (!(classArgument instanceof PsiClassObjectAccessExpression classObject)) return null;
        PsiTypeElement[] children = PsiTreeUtil.getChildrenOfType(classObject, PsiTypeElement.class);
        if (children == null || children.length == 0) return null;
        try {
            return children[0].getType();
        } catch (Exception e) {
            return null;
        }
    }

    // ================================================================
    // 扫描上下文
    // ================================================================

    private static final class ScanContext {
        final Project project;
        final AggregatedMetadataIndex index = new AggregatedMetadataIndex();
        /** 查注解类、框架方法必须包含库。 */
        final GlobalSearchScope allScope;
        /** 查注解使用点只关心项目源码。 */
        final GlobalSearchScope sourceScope;
        private final Set<String> registered = new HashSet<>();
        /** 已经显式绑定过的配置前缀（toBean / @Inject / bindTo），约定式匹配不得覆盖。 */
        private final Set<String> boundPrefixes = new HashSet<>();

        ScanContext(Project project) {
            this.project = project;
            this.allScope = GlobalSearchScope.allScope(project);
            this.sourceScope = GlobalSearchScope.projectScope(project);
        }

        void register(@NotNull String name, @NotNull PsiElement source) {
            PsiType type = typeOf(source);
            if (type == null) return;
            if (PsiTypeUtils.isValueType(type)
                    || PsiTypeUtils.isCollection(project, type)
                    || PsiTypeUtils.isMap(project, type)) {
                // 单值 / 容器绑定无法从类结构展开，直接登记为一条属性
                if (!registered.add(name + "#" + type.getCanonicalText())) return;
                project.getService(ProjectClassMetadataService.class)
                        .getPropertyMetadata(name, type, source)
                        .ifPresent(metadata -> index.addFirst(MutableReference.immutable(metadata)));
            } else {
                registerByType(name, type);
            }
        }

        void registerByType(@NotNull String name, @Nullable PsiType type) {
            if (type == null || PsiTypeUtils.isValueType(type)) return;
            if (!registered.add(name + "#" + type.getCanonicalText())) return;
            boundPrefixes.add(name);
            project.getService(ProjectClassMetadataService.class)
                    .getMetadata(name, type)
                    .ifPresent(metadata -> index.addFirst(MutableReference.immutable(metadata)));
        }

        boolean isBoundPrefix(@NotNull String name) {
            return boundPrefixes.contains(name);
        }

        int registeredCount() {
            return registered.size();
        }
    }
}
