package org.noear.solon.idea.plugin.initializr.util;

import java.io.File;

public class StringUtils {

    private final static String separator= File.separator;

    /**
     * 将路径与项目名组装
     * @param path 文件路径
     * @param name 项目名
     * @return 组装后的路径
     */
    public  static String PathStrAssemble(String path,String name){
        //判断最后是否是含有斜杠
        boolean b = path.lastIndexOf(separator) != name.length() - 1;
        return b?path+separator+name:path+name;
    }
}
