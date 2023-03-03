package org.noear.solon.idea.plugin.common.util;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInsight.completion.CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED;

public class GenericUtil {

    public static String truncateIdeaDummyIdentifier(@NotNull PsiElement element) {
        return truncateIdeaDummyIdentifier(element.getText());
    }

    public static String truncateIdeaDummyIdentifier(String text) {
        return text.replace(DUMMY_IDENTIFIER_TRIMMED, "");
    }

}
