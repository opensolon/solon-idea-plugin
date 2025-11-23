package org.noear.solon.idea.plugin.run;

import com.intellij.execution.configurations.RunConfigurationOptions;

/**
 * Solon 运行配置选项
 */
public class SolonRunConfigurationOptions extends RunConfigurationOptions {

    private String mainClass = "";
    private String programParameters = "";
    private String vmParameters = "-Dfile.encoding=UTF-8";
    private String workingDirectory = "";

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getProgramParameters() {
        return programParameters;
    }

    public void setProgramParameters(String programParameters) {
        this.programParameters = programParameters;
    }

    public String getVmParameters() {
        return vmParameters;
    }

    public void setVmParameters(String vmParameters) {
        this.vmParameters = vmParameters;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}