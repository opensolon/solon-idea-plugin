package org.noear.solon.idea.plugin.run;

import com.intellij.execution.configurations.RunConfigurationOptions;
import com.intellij.openapi.components.StoredProperty;

/**
 * Solon 运行配置选项
 */
public class SolonRunConfigurationOptions extends RunConfigurationOptions {

    private final StoredProperty<String> mainClass = string("").provideDelegate(this, "mainClass");
    private final StoredProperty<String> programParameters = string("").provideDelegate(this, "programParameters");
    private final StoredProperty<String> vmParameters = string("-Dfile.encoding=UTF-8").provideDelegate(this, "vmParameters");
    private final StoredProperty<String> workingDirectory = string("").provideDelegate(this, "workingDirectory");

    public String getMainClass() {
        return mainClass.getValue(this);
    }

    public void setMainClass(String mainClass) {
        this.mainClass.setValue(this, mainClass);
    }

    public String getProgramParameters() {
        return programParameters.getValue(this);
    }

    public void setProgramParameters(String programParameters) {
        this.programParameters.setValue(this, programParameters);
    }

    public String getVmParameters() {
        return vmParameters.getValue(this);
    }

    public void setVmParameters(String vmParameters) {
        this.vmParameters.setValue(this, vmParameters);
    }

    public String getWorkingDirectory() {
        return workingDirectory.getValue(this);
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory.setValue(this, workingDirectory);
    }
}
