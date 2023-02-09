package org.noear.solon.idea.plugin.common.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author liupeiqiang
 * @date 2023/2/9 8:47
 */
public class LoggerUtil {

    private static final Map<Class<?>, com.intellij.openapi.diagnostic.Logger> loggerMap = new HashMap<>();

    public static void debug(@NotNull Class<?> cl, Consumer<com.intellij.openapi.diagnostic.Logger> consumer){
        Logger loggerCache = loggerMap.getOrDefault(cl, null);
        if (loggerCache == null){
            loggerCache = Logger.getInstance(cl);
            loggerMap.put(cl, loggerCache);
        }
        if (loggerCache.isDebugEnabled()){
            consumer.accept(loggerMap.get(cl));
        }
    }

    public static com.intellij.openapi.diagnostic.Logger getLogger(@NotNull Class<?> cl){
        Logger loggerCache = loggerMap.getOrDefault(cl, null);
        if (loggerCache == null){
            loggerCache = Logger.getInstance(cl);
            loggerMap.put(cl, loggerCache);
        }
        return loggerCache;
    }

}
