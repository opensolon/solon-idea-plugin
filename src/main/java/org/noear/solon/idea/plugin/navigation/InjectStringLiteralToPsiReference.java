package org.noear.solon.idea.plugin.navigation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.metadata.index.MetadataGroup;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;

/**
 * 对应 {@code @Inject("${key}")} 中字符串字面量的引用，resolve 到配置项所属的
 * {@link PsiField 配置字段} 或配置类（group 场景）。
 */
public class InjectStringLiteralToPsiReference extends PsiReferenceBase<PsiLiteralExpression> {

    public InjectStringLiteralToPsiReference(@NotNull PsiLiteralExpression literalExpression) {
        super(literalExpression, computeRangeInElement(literalExpression), true);
    }

    private static com.intellij.openapi.util.TextRange computeRangeInElement(
            @NotNull PsiLiteralExpression literalExpression) {
        com.intellij.openapi.util.TextRange range = literalExpression.getTextRangeInParent();
        String text = literalExpression.getText();
        int from = text.indexOf("${");
        int to = text.indexOf('}', from);
        if (from >= 0 && to > from) {
            return new com.intellij.openapi.util.TextRange(
                    range.getStartOffset() + from + 2,
                    range.getStartOffset() + to);
        }
        return range;
    }

    @Override
    public @Nullable PsiElement resolve() {
        String key = extractKey();
        if (key == null) {
            return null;
        }
        Module module = ModuleUtil.findModuleForPsiElement(getElement());
        if (module == null) {
            return null;
        }
        ModuleMetadataService metadataService = module.getService(ModuleMetadataService.class);
        MetadataItem item = metadataService.getIndex().getPropertyOrGroup(key);
        if (item == null) {
            return null;
        }
        if (item instanceof MetadataProperty property) {
            return property.getSourceField().map(f -> (PsiElement) f).or(property::getSourceType).orElse(null);
        } else if (item instanceof MetadataGroup group) {
            return group.getType().orElse(null);
        }
        return null;
    }

    @Nullable
    private String extractKey() {
        Object value = getElement().getValue();
        if (!(value instanceof String text)) {
            return null;
        }
        return InjectStringLiteralReferenceContributor.extractConfigKey(text);
    }

    @Override
    public @NotNull String getCanonicalText() {
        String key = extractKey();
        return key == null ? "" : key;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        PsiElement resolved = resolve();
        if (resolved == null) {
            return false;
        }
        if (resolved.equals(element)) {
            return true;
        }
        // 覆盖 "字段引用类" 的情况：element 是类，resolved 是该类字段
        if (resolved instanceof PsiField field
                && element instanceof com.intellij.psi.PsiClass clazz
                && field.getContainingClass() != null) {
            return field.getContainingClass().equals(clazz);
        }
        return false;
    }
}
