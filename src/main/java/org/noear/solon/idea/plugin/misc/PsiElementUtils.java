package org.noear.solon.idea.plugin.misc;

import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator;
import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class PsiElementUtils {
  private static final Logger LOG = Logger.getInstance(PsiElementUtils.class);


  public static boolean isInFileOfType(PsiElement element, FileType fileType) {
    VirtualFile virtualFile = PsiUtil.getVirtualFile(element);
    if (virtualFile == null) {
      return false;
    }
    FileTypeManager ftm = FileTypeManager.getInstance();
    return ftm.isFileOfType(virtualFile, fileType);
  }


  public static String getDocument(PsiJavaDocumentedElement element) {
    JavaDocInfoGenerator generator = new JavaDocInfoGenerator(element.getProject(), element);
    PsiDocComment comment = getDocComment(element);
    if (comment == null) return null;
    StringBuilder doc = new StringBuilder();
    generator.generateCommonSection(doc, comment);
    // We use the CONTENT part of document generated by JavaDocInfoGenerator.generateCommonSection only,
    // because the SECTIONS part is not complete, and we don't want it influence user.
    int idx = doc.indexOf(DocumentationMarkup.CONTENT_END);
    if (idx >= 0) {
      doc.delete(idx, doc.length());
      idx = doc.indexOf(DocumentationMarkup.CONTENT_START);
      assert idx >= 0;
      doc.delete(idx, idx + DocumentationMarkup.CONTENT_START.length());
    } else {
      return null;
    }
    return doc.toString().strip();
  }


  public static String createLinkForDoc(@NotNull PsiJvmMember member) {
    String label;
    String ref;
    PsiClass containingClass = member.getContainingClass();
    if (containingClass == null) {
      if (member instanceof PsiClass psiClass) {
        label = psiClass.getQualifiedName();
        ref = psiClass.getQualifiedName();
      } else {
        label = member.getName();
        ref = member.getName();
      }
    } else {
      label = containingClass.getQualifiedName() + "."
          + member.getName() + (member instanceof PsiMethod ? "()" : "");
      ref = containingClass.getQualifiedName() + "#" + member.getName();
    }
    return createHyperLink(ref, label);
  }


  private static @NotNull String createHyperLink(String ref, String label) {
    StringBuilder buffer = new StringBuilder();
    DocumentationManagerUtil.createHyperlink(buffer, ref, label, false);
    return buffer.toString();
  }


  private static @Nullable PsiDocComment getDocComment(PsiJavaDocumentedElement docOwner) {
    PsiElement navElement = docOwner.getNavigationElement();
    if (!(navElement instanceof PsiJavaDocumentedElement)) {
      LOG.info("Wrong navElement: " + navElement + "; original = " + docOwner + " of class " + docOwner.getClass());
      return null;
    }
    PsiDocComment comment = ((PsiJavaDocumentedElement) navElement).getDocComment();
    if (comment == null) { //check for non-normalized fields
      PsiModifierList modifierList = docOwner instanceof PsiDocCommentOwner
          ? ((PsiDocCommentOwner) docOwner).getModifierList() : null;
      if (modifierList != null) {
        PsiElement parent = modifierList.getParent();
        if (parent instanceof PsiDocCommentOwner && parent.getNavigationElement() instanceof PsiDocCommentOwner) {
          return ((PsiDocCommentOwner) parent.getNavigationElement()).getDocComment();
        }
      }
    }
    return comment;
  }
}
