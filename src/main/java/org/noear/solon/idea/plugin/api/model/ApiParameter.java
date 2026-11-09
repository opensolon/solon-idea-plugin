package org.noear.solon.idea.plugin.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 接口参数描述（对应 Solon 的 @Param / @Path / @Header / @Cookie 注解）
 */
public record ApiParameter(@NotNull String name,
                           @NotNull Location location,
                           @NotNull String typeName,
                           boolean required,
                           @Nullable String defaultValue,
                           @Nullable String description) {

    public enum Location {
        PATH,
        QUERY,
        HEADER,
        COOKIE
    }
}
