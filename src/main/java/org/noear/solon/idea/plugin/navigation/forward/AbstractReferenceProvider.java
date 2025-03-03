package org.noear.solon.idea.plugin.navigation.forward;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceService;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.navigation.ReferenceService;

abstract class AbstractReferenceProvider extends PsiReferenceProvider {
    @Override
    @NotNull
    public PsiReference @NotNull [] getReferencesByElement(
            @NotNull PsiElement element, @NotNull ProcessingContext context) {
        PsiElement source = getRefSource(element, context);
        if (source != null) {
            var svc = ReferenceService.getInstance(element.getProject());
            return new PsiReference[]{svc.forwardReference(source)};
        } else {
            return PsiReference.EMPTY_ARRAY;
        }
    }

    @Override
    public boolean acceptsHints(@NotNull PsiElement element, PsiReferenceService.@NotNull Hints hints) {
//    if (hints == PsiReferenceService.Hints.HIGHLIGHTED_REFERENCES) return false;
        return super.acceptsHints(element, hints);
    }

    @Nullable
    protected abstract PsiElement getRefSource(@NotNull PsiElement element, @NotNull ProcessingContext context);
}
