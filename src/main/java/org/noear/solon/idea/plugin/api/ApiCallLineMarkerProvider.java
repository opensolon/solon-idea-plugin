package org.noear.solon.idea.plugin.api;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.SolonIcons;
import org.noear.solon.idea.plugin.api.model.ApiEndpoint;
import org.noear.solon.idea.plugin.api.ui.ApiTestToolWindowService;

import java.util.List;

/**
 * 为 Solon Controller 类名和接口方法添加行标记（gutter 图标）。
 *
 * <p>点击类标记：弹窗中可选择该类下任意接口；点击方法标记：直接打开该接口的调用窗口。</p>
 */
public class ApiCallLineMarkerProvider implements LineMarkerProvider {

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element instanceof PsiClass psiClass) {
            if (psiClass.getContainingClass() != null) {
                return null;
            }
            if (!ApiAnnotationSupport.isSolonController(psiClass)) {
                return null;
            }
            List<ApiEndpoint> endpoints = ApiEndpointParser.parseController(psiClass);
            if (endpoints.isEmpty()) {
                return null;
            }
            PsiElement identifier = psiClass.getNameIdentifier();
            if (identifier == null) {
                return null;
            }
            return createMarker(identifier, endpoints, 0,
                    "Call Solon API (" + endpoints.size() + " endpoints)");
        }

        if (element instanceof PsiMethod method) {
            if (method.isConstructor()) {
                return null;
            }
            PsiClass containingClass = method.getContainingClass();
            if (containingClass == null) {
                return null;
            }
            ApiEndpoint endpoint = ApiEndpointParser.parseMethod(containingClass, method);
            if (endpoint == null) {
                return null;
            }
            PsiElement identifier = method.getNameIdentifier();
            if (identifier == null) {
                return null;
            }
            return createMarker(identifier, List.of(endpoint), 0,
                    "Call API " + endpoint.getDisplayName());
        }

        return null;
    }

    private @NotNull LineMarkerInfo<?> createMarker(@NotNull PsiElement identifier,
                                                    @NotNull List<ApiEndpoint> endpoints,
                                                    int initialIndex,
                                                    @NotNull String tooltip) {
        return new LineMarkerInfo<>(
                identifier,
                identifier.getTextRange(),
                SolonIcons.ApiCall_16x16,
                element -> tooltip,
                (mouseEvent, element) -> {
                    if (identifier.isValid()) {
                        ApiTestToolWindowService.show(identifier.getProject(), endpoints, initialIndex);
                    }
                },
                GutterIconRenderer.Alignment.LEFT,
                () -> tooltip
        );
    }
}
