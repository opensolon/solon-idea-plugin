package org.noear.solon.idea.plugin.api;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.noear.solon.idea.plugin.api.model.ApiEndpoint;
import org.noear.solon.idea.plugin.api.model.ApiParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 将 Solon Controller 的 PSI 结构解析为可调用的接口描述。
 *
 * <p>路径拼接规则：类级 @Mapping + 方法级 @Mapping；方法级 @Mapping 可省略。
 * 参数按注解归类：@Path / @Param / @Header / @Cookie 显式声明，@Body 生成 JSON 模板，
 * 未注解的简单类型按查询参数处理，未注解的复杂对象按其字段展开。</p>
 */
public final class ApiEndpointParser {

    private static final int MAX_JSON_DEPTH = 2;

    private ApiEndpointParser() {
    }

    /**
     * 解析 Controller 中的所有接口，按源码顺序返回。
     */
    public static @NotNull List<ApiEndpoint> parseController(@NotNull PsiClass controller) {
        if (!ApiAnnotationSupport.isSolonController(controller)) {
            return List.of();
        }
        List<PsiMethod> methods = new ArrayList<>(Arrays.asList(controller.getMethods()));
        methods.sort(Comparator
                .comparing((PsiMethod method) -> method.getContainingClass() != controller)
                .thenComparingInt(PsiElement::getTextOffset));

        List<ApiEndpoint> endpoints = new ArrayList<>();
        for (PsiMethod method : methods) {
            ApiEndpoint endpoint = doParse(controller, method);
            if (endpoint != null) {
                endpoints.add(endpoint);
            }
        }
        return endpoints;
    }

    /**
     * 解析单个方法对应的接口，非接口方法返回 null。
     */
    public static @Nullable ApiEndpoint parseMethod(@NotNull PsiClass controller, @NotNull PsiMethod method) {
        if (!ApiAnnotationSupport.isSolonController(controller)) {
            return null;
        }
        return doParse(controller, method);
    }

    private static @Nullable ApiEndpoint doParse(@NotNull PsiClass controller, @NotNull PsiMethod method) {
        if (method.isConstructor() || !method.hasModifierProperty(PsiModifier.PUBLIC)) {
            return null;
        }

        List<String> methodPaths = ApiAnnotationSupport.mappingPaths(method);
        PsiAnnotation mapping = ApiAnnotationSupport.findAnnotation(method, ApiAnnotationSupport.MAPPING);
        String httpMethod = ApiAnnotationSupport.httpMethodOf(method);
        if (httpMethod == null) {
            if (mapping == null) {
                return null;
            }
            httpMethod = resolveMappingHttpMethod(mapping);
        }

        List<String> classPaths = ApiAnnotationSupport.mappingPaths(controller);
        String classPath = classPaths.isEmpty() ? "" : classPaths.get(0);
        String methodPath = methodPaths.isEmpty() ? "" : methodPaths.get(0);
        String fullPath = joinPath(classPath, methodPath);

        List<ApiParameter> parameters = new ArrayList<>();
        PsiType bodyType = null;
        List<PsiParameter> modelParameters = new ArrayList<>();

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            PsiAnnotation body = ApiAnnotationSupport.findAnnotation(parameter, "Body");
            if (body != null) {
                if (bodyType == null) {
                    bodyType = parameter.getType();
                }
                continue;
            }

            PsiAnnotation path = ApiAnnotationSupport.findAnnotation(parameter, "Path");
            if (path != null) {
                parameters.add(createParameter(parameter, path, ApiParameter.Location.PATH));
                continue;
            }

            PsiAnnotation param = ApiAnnotationSupport.findAnnotation(parameter, "Param");
            if (param != null) {
                parameters.add(createParameter(parameter, param, ApiParameter.Location.QUERY));
                continue;
            }

            PsiAnnotation header = ApiAnnotationSupport.findAnnotation(parameter, "Header");
            if (header != null) {
                parameters.add(createParameter(parameter, header, ApiParameter.Location.HEADER));
                continue;
            }

            PsiAnnotation cookie = ApiAnnotationSupport.findAnnotation(parameter, "Cookie");
            if (cookie != null) {
                parameters.add(createParameter(parameter, cookie, ApiParameter.Location.COOKIE));
                continue;
            }

            if (isSimpleType(parameter.getType())) {
                parameters.add(new ApiParameter(parameter.getName(),
                        ApiParameter.Location.QUERY,
                        parameter.getType().getPresentableText(),
                        false,
                        null,
                        null));
            } else if (isPageType(parameter.getType())) {
                parameters.add(new ApiParameter("pageNumber", ApiParameter.Location.QUERY, "int", false, "1", null));
                parameters.add(new ApiParameter("pageSize", ApiParameter.Location.QUERY, "int", false, "10", null));
            } else {
                modelParameters.add(parameter);
            }
        }

        String bodyTemplate = buildBodyTemplate(httpMethod, bodyType, modelParameters, parameters);

        String className = controller.getQualifiedName() == null ? controller.getName() : controller.getQualifiedName();
        return new ApiEndpoint(httpMethod,
                fullPath,
                method.getName(),
                className == null ? "" : className,
                firstDocLine(method),
                parameters,
                bodyTemplate);
    }

    private static @NotNull String buildBodyTemplate(@NotNull String httpMethod,
                                                     @Nullable PsiType bodyType,
                                                     @NotNull List<PsiParameter> modelParameters,
                                                     @NotNull List<ApiParameter> parameters) {
        if (bodyType != null) {
            return jsonSample(bodyType, 0);
        }
        if (writesBody(httpMethod)) {
            return modelJson(modelParameters);
        }
        // GET 等无请求体的方法：未注解的复杂对象展开为查询参数
        for (PsiParameter model : modelParameters) {
            PsiClass modelClass = resolveClass(model.getType());
            if (modelClass == null) {
                continue;
            }
            for (PsiField field : modelClass.getAllFields()) {
                if (!isBindableField(field)) {
                    continue;
                }
                parameters.add(new ApiParameter(field.getName(),
                        ApiParameter.Location.QUERY,
                        field.getType().getPresentableText(),
                        false,
                        null,
                        null));
            }
        }
        return "";
    }

    private static @NotNull ApiParameter createParameter(@NotNull PsiParameter parameter,
                                                         @NotNull PsiAnnotation annotation,
                                                         @NotNull ApiParameter.Location location) {
        String name = firstNonBlank(ApiAnnotationSupport.stringAttribute(annotation, "value"),
                ApiAnnotationSupport.stringAttribute(annotation, "name"),
                parameter.getName());
        boolean required = ApiAnnotationSupport.booleanAttribute(annotation, "required", true);
        return new ApiParameter(name == null ? parameter.getName() : name,
                location,
                parameter.getType().getPresentableText(),
                required,
                emptyToNull(ApiAnnotationSupport.stringAttribute(annotation, "defaultValue")),
                emptyToNull(ApiAnnotationSupport.stringAttribute(annotation, "description")));
    }

    private static @NotNull String resolveMappingHttpMethod(@NotNull PsiAnnotation mapping) {
        String enumValue = ApiAnnotationSupport.enumAttribute(mapping, "method");
        if (enumValue == null) {
            return "POST";
        }
        String upper = enumValue.toUpperCase(Locale.ROOT);
        if ("ALL".equals(upper) || "HTTP".equals(upper) || "UNKNOWN".equals(upper)) {
            return "POST";
        }
        return upper;
    }

    public static @NotNull String joinPath(@Nullable String classPath, @Nullable String methodPath) {
        String left = trimSlashes(classPath);
        String right = trimSlashes(methodPath);
        if (left.isEmpty() && right.isEmpty()) {
            return "/";
        }
        if (left.isEmpty()) {
            return "/" + right;
        }
        if (right.isEmpty()) {
            return "/" + left;
        }
        return "/" + left + "/" + right;
    }

    private static @NotNull String trimSlashes(@Nullable String text) {
        if (text == null) {
            return "";
        }
        String result = text.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean writesBody(@NotNull String httpMethod) {
        return "POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod);
    }

    private static boolean isSimpleType(@NotNull PsiType type) {
        PsiType base = type instanceof PsiArrayType array ? array.getComponentType() : type;
        if (base instanceof PsiPrimitiveType) {
            return true;
        }
        PsiClass resolved = resolveClass(base);
        if (resolved == null) {
            return true;
        }
        if (resolved.isEnum()) {
            return true;
        }
        String qualifiedName = resolved.getQualifiedName();
        if (qualifiedName == null) {
            return true;
        }
        return qualifiedName.startsWith("java.lang.")
                || qualifiedName.startsWith("java.time.")
                || qualifiedName.startsWith("java.math.")
                || qualifiedName.equals("java.util.Date")
                || qualifiedName.equals("java.sql.Date")
                || qualifiedName.equals("java.sql.Timestamp")
                || qualifiedName.equals("java.util.UUID");
    }

    private static boolean isPageType(@NotNull PsiType type) {
        PsiClass resolved = resolveClass(type);
        return resolved != null
                && resolved.findFieldByName("pageNumber", true) != null
                && resolved.findFieldByName("pageSize", true) != null;
    }

    private static @Nullable PsiClass resolveClass(@Nullable PsiType type) {
        if (type instanceof PsiClassType classType) {
            return classType.resolve();
        }
        return null;
    }

    private static boolean isBindableField(@NotNull PsiField field) {
        return !field.hasModifierProperty(PsiModifier.STATIC) && !"serialVersionUID".equals(field.getName());
    }

    private static @NotNull String modelJson(@NotNull List<PsiParameter> models) {
        List<String> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PsiParameter model : models) {
            PsiClass modelClass = resolveClass(model.getType());
            if (modelClass == null) {
                continue;
            }
            for (PsiField field : modelClass.getAllFields()) {
                if (!isBindableField(field) || !seen.add(field.getName())) {
                    continue;
                }
                entries.add("\"" + field.getName() + "\": " + jsonSample(field.getType(), 1));
            }
        }
        if (entries.isEmpty()) {
            return "";
        }
        return "{\n  " + String.join(",\n  ", entries) + "\n}";
    }

    private static @NotNull String jsonSample(@NotNull PsiType type, int depth) {
        if (type instanceof PsiArrayType) {
            return "[]";
        }
        String canonical = type.getCanonicalText();
        if ("java.lang.String".equals(canonical) || "char".equals(canonical) || "java.lang.Character".equals(canonical)) {
            return "\"\"";
        }
        if ("boolean".equals(canonical) || "java.lang.Boolean".equals(canonical)) {
            return "false";
        }
        if (isCanonicalNumber(canonical)) {
            return "0";
        }

        PsiClass resolved = resolveClass(type);
        if (resolved != null) {
            String qualifiedName = resolved.getQualifiedName();
            if (qualifiedName != null) {
                if (isCollection(qualifiedName)) {
                    return "[]";
                }
                if (isMap(qualifiedName)) {
                    return "{}";
                }
                if (isDateLike(qualifiedName)) {
                    return "\"2026-01-01 00:00:00\"";
                }
            }
            if (resolved.isEnum()) {
                return "null";
            }
            if (depth < MAX_JSON_DEPTH) {
                List<String> entries = new ArrayList<>();
                for (PsiField field : resolved.getAllFields()) {
                    if (!isBindableField(field)) {
                        continue;
                    }
                    entries.add("\"" + field.getName() + "\": " + jsonSample(field.getType(), depth + 1));
                }
                if (!entries.isEmpty()) {
                    return "{\n  " + String.join(",\n  ", entries) + "\n}";
                }
            }
        }
        return "null";
    }

    private static boolean isCanonicalNumber(@NotNull String canonical) {
        return switch (canonical) {
            case "int", "long", "short", "byte", "double", "float",
                 "java.lang.Integer", "java.lang.Long", "java.lang.Short", "java.lang.Byte",
                 "java.lang.Double", "java.lang.Float", "java.math.BigDecimal", "java.math.BigInteger" -> true;
            default -> false;
        };
    }

    private static boolean isCollection(@NotNull String qualifiedName) {
        return qualifiedName.startsWith("java.util.List")
                || qualifiedName.startsWith("java.util.Set")
                || qualifiedName.startsWith("java.util.Collection")
                || qualifiedName.startsWith("java.util.ArrayList")
                || qualifiedName.startsWith("java.util.LinkedList")
                || qualifiedName.startsWith("java.util.HashSet");
    }

    private static boolean isMap(@NotNull String qualifiedName) {
        return qualifiedName.startsWith("java.util.Map")
                || qualifiedName.startsWith("java.util.HashMap")
                || qualifiedName.startsWith("java.util.LinkedHashMap");
    }

    private static boolean isDateLike(@NotNull String qualifiedName) {
        return qualifiedName.equals("java.util.Date")
                || qualifiedName.equals("java.sql.Date")
                || qualifiedName.equals("java.sql.Timestamp")
                || qualifiedName.equals("java.time.LocalDate")
                || qualifiedName.equals("java.time.LocalDateTime")
                || qualifiedName.equals("java.time.OffsetDateTime")
                || qualifiedName.equals("java.time.Instant");
    }

    private static @Nullable String firstDocLine(@NotNull PsiMethod method) {
        PsiDocComment docComment = method.getDocComment();
        if (docComment == null) {
            return null;
        }
        String text = docComment.getText().replace("/**", " ").replace("*/", " ");
        for (String rawLine : text.split("\n")) {
            String line = rawLine.trim();
            while (line.startsWith("*")) {
                line = line.substring(1).trim();
            }
            if (!line.isEmpty() && !line.startsWith("@")) {
                return line;
            }
        }
        return null;
    }

    private static @Nullable String firstNonBlank(String @Nullable ... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
