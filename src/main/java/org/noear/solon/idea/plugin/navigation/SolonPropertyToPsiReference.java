package org.noear.solon.idea.plugin.navigation;

import com.intellij.codeInspection.reference.PsiMemberReference;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.service.ModuleMetadataService;

import java.util.Iterator;

abstract class SolonPropertyToPsiReference<T extends PsiElement>
        extends PsiReferenceBase<T> implements PsiMemberReference {

    SolonPropertyToPsiReference(@NotNull T source) {
        super(source, true);
    }

    @Override
    public @Nullable PsiElement resolve() {
        Module module = ModuleUtil.findModuleForPsiElement(getElement());
        if (module == null) return null;

        Iterator<String> candidates = candidateKeys(getElement());

        ModuleMetadataService metadataService = ModuleMetadataService.getInstance(module);
        while (candidates.hasNext()) {
            String key = candidates.next();
            MetadataItem propertyOrGroup = metadataService.getIndex().getPropertyOrGroup(key);
            if (propertyOrGroup == null) continue;
            if (propertyOrGroup instanceof MetadataProperty property) {
                return property.getSourceField().map(f -> (PsiElement) f).or(property::getSourceType).orElse(null);
            } else {
                return propertyOrGroup.getSourceType().orElse(null);
            }
        }
        return null;
    }

    protected abstract Iterator<String> candidateKeys(T source);
}
