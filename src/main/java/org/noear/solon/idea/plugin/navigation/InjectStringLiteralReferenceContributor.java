package org.noear.solon.idea.plugin.navigation;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.PsiJavaPatterns.literalExpression;

/**
 * 为 {@code @Inject("${key}")} 字符串字面量注册引用，使 IDEA 能：
 * <ul>
 *   <li>Ctrl+B 从 {@code "${xxx}"} 跳到 yaml/properties 中的 key 定义位置（经由 metadata 索引 → source field/class）；</li>
 *   <li>Find Usages 从配置类字段 / 配置类本身同时找到 yaml key 和 {@code @Inject("${xxx}")} 使用点。</li>
 * </ul>
 * 这补齐了已有 {@link YamlToPsiReferenceContributor} / {@link PropertiesToPsiReferenceContributor} 只覆盖配置文件 key → 代码方向的缺口。
 */
public class InjectStringLiteralReferenceContributor extends PsiReferenceContributor {

    private static final String INJECT_ANNOTATION = "org.noear.solon.annotation.Inject";

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // 不对字面量父节点做严格限制：@Inject("${x}") 是位置参数（字面量父节点是参数列表），
        // 而 @Inject(value="${x}") 是命名参数（父节点是 PsiNameValuePair），
        // 两种 AST 结构不同，用 withParent 容易漏掉其中一种。这里统一在运行时判断。
        registrar.registerReferenceProvider(
                psiElement(PsiLiteralExpression.class),
                new com.intellij.psi.PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(
                            @NotNull PsiElement element, @NotNull ProcessingContext context) {
                        if (!(element instanceof PsiLiteralExpression literalExpression)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(literalExpression, PsiAnnotation.class);
                        if (annotation == null) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        String annotationQN = annotation.getQualifiedName();
                        if (!INJECT_ANNOTATION.equals(annotationQN)) {
                            return PsiReference.EMPTY_ARRAY;
                        }
                        return new PsiReference[]{
                                new InjectStringLiteralToPsiReference(literalExpression)
                        };
                    }
                });
    }

    /**
     * 从字面量文本 {@code "${key}"} 或 {@code "${key:default}"} 中提取配置 key。
     *
     * @return 配置 key（不含花括号与默认值），或 {@code null}（不是 Solon 占位符）
     */
    public static @Nullable String extractConfigKey(@NotNull String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("${") || !trimmed.endsWith("}")) {
            return null;
        }
        String body = trimmed.substring(2, trimmed.length() - 1);
        if (body.isEmpty()) {
            return null;
        }
        // 去掉 ":默认值" 部分（Solon 支持的默认值语法与 Spring 一致）
        int colon = body.indexOf(':');
        if (colon > 0) {
            body = body.substring(0, colon);
        }
        return body.isEmpty() ? null : body;
    }
}
