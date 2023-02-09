package org.noear.solon.idea.plugin.suggestion.index;

import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import org.jetbrains.annotations.NotNull;

/**
 * Reindex the suggestion when compilation finished.
 */
public class SuggestionReIndex implements CompilationStatusListener {

    @Override
    public void compilationFinished(boolean aborted, int errors, int warnings, @NotNull CompileContext compileContext) {
        CompilationStatusListener.super.compilationFinished(aborted, errors, warnings, compileContext);
    }
}
