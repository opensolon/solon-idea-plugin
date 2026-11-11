package org.noear.solon.idea.plugin.api;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Solon 注解识别工具。统一使用全限定名做匹配，避免与同名注解冲突。
 */
public final class ApiAnnotationSupport {

    public static final String ANNOTATION_PACKAGE = "org.noear.solon.annotation.";
    public static final String CONTROLLER = "Controller";
    public static final String MAPPING = "Mapping";

    private static final Map<String, String> HTTP_METHOD_ANNOTATIONS;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Get", "GET");
        map.put("Post", "POST");
        map.put("Put", "PUT");
        map.put("Delete", "DELETE");
        map.put("Patch", "PATCH");
        map.put("Head", "HEAD");
        map.put("Options", "OPTIONS");
        HTTP_METHOD_ANNOTATIONS = Collections.unmodifiableMap(map);
    }

    private ApiAnnotationSupport() {
    }

    public static boolean isSolonController(@NotNull PsiModifierListOwner owner) {
        return findAnnotation(owner, CONTROLLER) != null;
    }

    public static @Nullable PsiAnnotation findAnnotation(@NotNull PsiModifierListOwner owner,
                                                         @NotNull String simpleName) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return null;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (isSolonAnnotation(annotation, simpleName)) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean isSolonAnnotation(@Nullable PsiAnnotation annotation, @NotNull String simpleName) {
        if (annotation == null) {
            return false;
        }
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName != null) {
            return qualifiedName.equals(ANNOTATION_PACKAGE + simpleName);
        }
        // 工程未引入 Solon 依赖时注解无法解析，退化为简单名匹配
        PsiJavaCodeReferenceElement reference = annotation.getNameReferenceElement();
        return reference != null && simpleName.equals(reference.getReferenceName());
    }

    /**
     * 读取方法上 @Get / @Post / ... 标注的 HTTP 方法。
     */
    public static @Nullable String httpMethodOf(@NotNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return null;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String simpleName = simpleName(annotation);
            if (simpleName == null) {
                continue;
            }
            String method = HTTP_METHOD_ANNOTATIONS.get(simpleName);
            if (method != null && isSolonAnnotation(annotation, simpleName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 读取 @Mapping 上声明的路径，支持 value / path 与数组写法。
     */
    public static @NotNull List<String> mappingPaths(@NotNull PsiModifierListOwner owner) {
        PsiAnnotation mapping = findAnnotation(owner, MAPPING);
        if (mapping == null) {
            return List.of();
        }
        List<String> paths = new ArrayList<>();
        addStringValues(paths, mapping.findAttributeValue("value"));
        addStringValues(paths, mapping.findAttributeValue("path"));
        return paths;
    }

    private static void addStringValues(@NotNull List<String> target, @Nullable PsiAnnotationMemberValue value) {
        if (value == null) {
            return;
        }
        if (value instanceof PsiArrayInitializerMemberValue array) {
            for (PsiAnnotationMemberValue initializer : array.getInitializers()) {
                addStringValues(target, initializer);
            }
            return;
        }
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof String text) {
            String trimmed = text.trim();
            if (!trimmed.isEmpty() && !target.contains(trimmed)) {
                target.add(trimmed);
            }
        }
    }

    public static @Nullable String stringAttribute(@Nullable PsiAnnotation annotation,
                                                   @NotNull String attribute) {
        if (annotation == null) {
            return null;
        }
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attribute);
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof String text) {
            return text;
        }
        return null;
    }

    public static boolean booleanAttribute(@Nullable PsiAnnotation annotation,
                                           @NotNull String attribute,
                                           boolean defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attribute);
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof Boolean flag) {
            return flag;
        }
        return defaultValue;
    }

    /**
     * 读取枚举类型属性（如 @Mapping(method = MethodType.POST)），返回枚举常量名。
     */
    public static @Nullable String enumAttribute(@Nullable PsiAnnotation annotation,
                                                 @NotNull String attribute) {
        if (annotation == null) {
            return null;
        }
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attribute);
        if (value instanceof PsiArrayInitializerMemberValue array) {
            PsiAnnotationMemberValue[] initializers = array.getInitializers();
            if (initializers.length == 0) {
                return null;
            }
            value = initializers[0];
        }
        if (value instanceof PsiReferenceExpression reference) {
            return reference.getReferenceName();
        }
        if (value == null) {
            return null;
        }
        String text = value.getText();
        int index = text.lastIndexOf('.');
        return index >= 0 ? text.substring(index + 1) : text;
    }

    private static @Nullable String simpleName(@NotNull PsiAnnotation annotation) {
        PsiJavaCodeReferenceElement reference = annotation.getNameReferenceElement();
        return reference == null ? null : reference.getReferenceName();
    }
}
