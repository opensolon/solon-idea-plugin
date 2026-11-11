package org.noear.solon.idea.plugin.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 一个可调用的 Solon 接口。
 */
public final class ApiEndpoint {

    private final String httpMethod;
    private final String path;
    private final String methodName;
    private final String className;
    private final String description;
    private final List<ApiParameter> parameters;
    private final String bodyTemplate;

    public ApiEndpoint(@NotNull String httpMethod,
                       @NotNull String path,
                       @NotNull String methodName,
                       @NotNull String className,
                       @Nullable String description,
                       @NotNull List<ApiParameter> parameters,
                       @NotNull String bodyTemplate) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.methodName = methodName;
        this.className = className;
        this.description = description;
        this.parameters = List.copyOf(parameters);
        this.bodyTemplate = bodyTemplate;
    }

    public @NotNull String getHttpMethod() {
        return httpMethod;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getMethodName() {
        return methodName;
    }

    public @NotNull String getClassName() {
        return className;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public @NotNull List<ApiParameter> getParameters() {
        return parameters;
    }

    /**
     * JSON 请求体模板，无请求体时为空字符串。
     */
    public @NotNull String getBodyTemplate() {
        return bodyTemplate;
    }

    public @NotNull String getDisplayName() {
        return httpMethod + " " + path;
    }

    @Override
    public String toString() {
        return getDisplayName() + "  (" + methodName + ")";
    }
}
