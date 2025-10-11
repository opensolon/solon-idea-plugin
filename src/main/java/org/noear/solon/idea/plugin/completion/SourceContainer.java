package org.noear.solon.idea.plugin.completion;

import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.light.LightElement;
import kotlin.Pair;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.hint.Hint;

import javax.swing.*;
import java.util.Optional;

/**
 * A PsiElement that carrying a source object.
 * <p>
 * Created by Completion for each LookupElement, useful for InsertHandler, Documentation, etc.
 */
@ToString(of = "source")
public class SourceContainer extends LightElement {
    private final Object source;

    private String lookupString;

    private String processedLookupString;


    SourceContainer(@NotNull MetadataItem metadata, @NotNull Project project) {
        this(metadata, PsiManager.getInstance(project));
    }


    SourceContainer(@NotNull Hint metadata, @NotNull Project project) {
        this(metadata, PsiManager.getInstance(project));
    }

    SourceContainer(@NotNull MetadataItem metadata, @NotNull Project project, String lookupString, String processedLookupString) {
        this(metadata, PsiManager.getInstance(project));
        this.lookupString = lookupString;
        this.processedLookupString = processedLookupString;
    }

    private SourceContainer(@NotNull Object metadata, @NotNull PsiManager psiManager) {
        super(psiManager, Language.ANY);
        this.source = metadata;
        assert metadata instanceof MetadataItem || metadata instanceof Hint;
    }


    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Nullable
            @Override
            public @NlsSafe String getPresentableText() {
                return getSourceMetadataItem().map(MetadataItem::getNameStr).orElseGet(() ->
                        getSourceHint().map(Hint::value).orElse(""));
            }


            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return getSourceMetadataItem().map(MetadataItem::getIcon).map(Pair::getSecond).orElseGet(() ->
                        getSourceHint().map(Hint::icon).orElse(null));
            }
        };
    }


    public Optional<MetadataItem> getSourceMetadataItem() {
        return source instanceof MetadataItem mi ? Optional.of(mi) : Optional.empty();
    }


    public Optional<Hint> getSourceHint() {
        return source instanceof Hint h ? Optional.of(h) : Optional.empty();
    }
}
