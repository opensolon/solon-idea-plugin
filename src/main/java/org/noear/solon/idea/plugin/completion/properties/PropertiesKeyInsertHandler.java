package org.noear.solon.idea.plugin.completion.properties;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.noear.solon.idea.plugin.completion.SourceContainer;
import org.noear.solon.idea.plugin.metadata.index.MetadataGroup;
import org.noear.solon.idea.plugin.metadata.index.MetadataItem;
import org.noear.solon.idea.plugin.metadata.index.MetadataProperty;
import org.noear.solon.idea.plugin.metadata.source.ConfigurationMetadata;
import org.noear.solon.idea.plugin.metadata.source.PropertyName;
import org.noear.solon.idea.plugin.misc.PsiTypeUtils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@ThreadSafe
class PropertiesKeyInsertHandler implements InsertHandler<LookupElement> {
    public static final PropertiesKeyInsertHandler INSTANCE = new PropertiesKeyInsertHandler();
    private static final String CARET = "<caret>";

    private static final Logger LOG = Logger.getInstance(PropertiesKeyInsertHandler.class);

    private PropertiesKeyInsertHandler() {
    }

    private static char getCodeStyleDelimiter(Project project) {
        return PropertiesCodeStyleSettings.getInstance(project).getDelimiter();
    }

    @Override
    public void handleInsert(final @NotNull InsertionContext context, final @NotNull LookupElement lookupElement) {
        MetadataItem suggestion = ((SourceContainer) requireNonNull(lookupElement.getPsiElement()))
                .getSourceMetadataItem().orElseThrow();
        @NotNull Project project = context.getProject();

        PsiElement currentElement = context.getFile().findElementAt(context.getStartOffset());
        assert currentElement != null : "no element at " + context.getStartOffset();

        Property sibling = findSibling(context.getFile(), suggestion.getName());
        if (sibling != null && !PsiTreeUtil.isAncestor(sibling, currentElement, false)) {
            //FIXME: if completion is started within an existed key, this will remove too many chars.
            // But I cannot find a way to safely undo the insertion before.
            deleteLookupText(context);
            // This means the suggested property is already in the file, let's move caret to that property then return.
            ASTNode node = Objects.requireNonNullElse(
                    sibling.getNode().findChildByType(PropertiesTokenTypes.KEY_VALUE_SEPARATOR),
                    sibling.getNode());
            context.getEditor().getCaretModel().moveToOffset(node.getStartOffset() + node.getTextLength());
            return;
        }

        deleteLookupTextHonerCompletionChar(context, currentElement);

        String suggestionWithCaret = getSuggestionReplacementWithCaret(project, lookupElement, suggestion);
        LOG.info("Inserting suggestion: " + suggestionWithCaret);
        char delimiter = getCodeStyleDelimiter(project);
        if (StringUtils.removeEnd(suggestionWithCaret, CARET).endsWith(String.valueOf(delimiter))) {
            if (hasSeparator(getStringAfterCaret(context), delimiter)) {
                int pos = StringUtils.lastIndexOf(suggestionWithCaret, delimiter);
                suggestionWithCaret = suggestionWithCaret.substring(0, pos) +
                        StringUtils.substring(suggestionWithCaret, pos + 1);
            }
        }
        EditorModificationUtil.insertStringAtCaret(
                context.getEditor(),
                suggestionWithCaret.replace(CARET, ""),
                false,
                true,
                getCaretIndex(suggestionWithCaret));
        // Trigger value completion automatically for better user experience.
        if (StringUtils.isBlank(getStringAfterCaret(context))) {
            AutoPopupController.getInstance(project).scheduleAutoPopup(context.getEditor());
        }
        //TODO auto generate index for properties of type List
    }

    private Property findSibling(@NotNull PsiFile psiFile, @NotNull PropertyName propertyName) {
        if (!(psiFile instanceof PropertiesFile)) return null;
        AtomicReference<Property> ref = new AtomicReference<>();
        PsiTreeUtil.processElements(psiFile, Property.class, property -> {
            String keyStr = property.getUnescapedKey();
            if (StringUtils.isBlank(keyStr)) return true;
            PropertyName key = PropertyName.adapt(keyStr);
            if (key.equals(propertyName)) {
                ref.set(property);
                return false;
            }
            return true;
        });
        return ref.get();
    }

    private int getCaretIndex(final String suggestionWithCaret) {
        return suggestionWithCaret.indexOf(CARET);
    }

    @NotNull
    private String getStringAfterCaret(final InsertionContext context) {
        int offset = context.getEditor().getCaretModel().getOffset();
        String substring = context.getDocument().getText().substring(offset);
        return StringUtils.substringBefore(substring, '\n');
    }

    private boolean hasSeparator(String string, char delimiter) {
        boolean noSep = Character.isWhitespace(delimiter);
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (c != ' ' && c != '\t' && c != '\f') { //According to java.util.Properties, only this 3 char is white space.
                // According to java.util.Properties, the separator is '=' or ':', but there can be no separator.
                return c == ':' || c == '=';
            } else if (noSep) {
                return true;
            }
        }
        return false;
    }

    private void deleteLookupTextHonerCompletionChar(InsertionContext context, @NotNull PsiElement elementAtCaret) {
        if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
            context.getEditor().getCaretModel().moveToOffset(elementAtCaret.getTextOffset());
            WriteCommandAction.runWriteCommandAction(context.getProject(), elementAtCaret::delete);
        } else {
            deleteLookupText(context);
        }
    }

    private void deleteLookupText(InsertionContext context) {
        context.getEditor().getCaretModel().moveToOffset(context.getStartOffset());
        Document document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());
        context.commitDocument();
    }

    @NotNull
    private String getSuggestionReplacementWithCaret(@NotNull Project project, LookupElement lookupElement, MetadataItem suggestion) {
        String suggestionReplacementWithCaret = this.doGetSuggestionReplacementWithCaret(project, lookupElement, suggestion);
        if (suggestionReplacementWithCaret.contains("*")) {
            suggestionReplacementWithCaret = suggestionReplacementWithCaret.replace("*", CARET);
        }
        return suggestionReplacementWithCaret;
    }

    private String doGetSuggestionReplacementWithCaret(@NotNull Project project, LookupElement lookupElement, MetadataItem suggestion) {
        char delimiter = getCodeStyleDelimiter(project);
        String lookupString = lookupElement.getLookupString();
        String nameStr = suggestion.getNameStr();
        String suggestionWithCaretPrefix = suggestion.getNameStr();
        if (!Objects.equals(lookupString, nameStr)) {
            suggestionWithCaretPrefix = lookupString;
        }
        if (suggestion instanceof MetadataGroup) {
            return suggestionWithCaretPrefix + "." + CARET;
        } else if (suggestion instanceof MetadataProperty property) {
            PsiType propType = property.getFullType().orElse(null);
            if (PsiTypeUtils.isValueType(propType)) {
                ConfigurationMetadata.Property metadata = property.getMetadata();
                if (metadata.getType().contains("[")
                        && metadata.getType().contains("]")) {
                    return suggestionWithCaretPrefix + "[" + CARET + "]" + delimiter + CARET;
                } else {
                    return suggestionWithCaretPrefix + delimiter + CARET;
                }

            } else if (PsiTypeUtils.isCollection(project, propType)) {
                //TODO Auto generate numeric index for this
                return suggestionWithCaretPrefix + "[" + CARET + "]" + delimiter;
            } else if (PsiTypeUtils.isMap(project, propType)) { // map or class
                return suggestionWithCaretPrefix + "." + CARET;
            } else {
                return suggestionWithCaretPrefix + CARET;
            }
        } else {
            throw new IllegalStateException("Unsupported type of suggestion: " + suggestion.getClass());
        }
    }
}
