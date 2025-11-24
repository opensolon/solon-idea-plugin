package org.noear.solon.idea.plugin.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Solon 应用运行配置
 */
public class SolonRunConfiguration extends RunConfigurationBase<SolonRunConfigurationOptions> {

    public SolonRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }
    
    protected @NotNull SolonRunConfigurationOptions createOptions() {
        return new SolonRunConfigurationOptions();
    }

    @NotNull
    @Override
    protected SolonRunConfigurationOptions getOptions() {
        return (SolonRunConfigurationOptions) super.getOptions();
    }

    @NotNull
    public String getMainClass() {
        return getOptions().getMainClass();
    }

    public void setMainClass(@NotNull String mainClass) {
        getOptions().setMainClass(mainClass);
    }

    @NotNull
    public String getProgramParameters() {
        return getOptions().getProgramParameters();
    }

    public void setProgramParameters(@NotNull String programParameters) {
        getOptions().setProgramParameters(programParameters);
    }

    @NotNull
    public String getVmParameters() {
        return getOptions().getVmParameters();
    }

    public void setVmParameters(@NotNull String vmParameters) {
        getOptions().setVmParameters(vmParameters);
    }

    @NotNull
    public String getWorkingDirectory() {
        return getOptions().getWorkingDirectory();
    }

    public void setWorkingDirectory(@NotNull String workingDirectory) {
        getOptions().setWorkingDirectory(workingDirectory);
    }

    private Module findModuleByMainClass() throws ExecutionException {
        String className = getMainClass();
        if (className.isEmpty()) {
            throw new ExecutionException("主类未指定");
        }

        Project project = getProject();
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.projectScope(project));

        if (psiClass == null) {
            throw new ExecutionException("找不到主类: " + className);
        }

        VirtualFile file = psiClass.getContainingFile().getVirtualFile();
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
        if (module == null) {
            throw new ExecutionException("无法确定主类所在模块: " + className);
        }
        return module;
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new JavaCommandLineState(environment) {
            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                JavaParameters javaParameters = new JavaParameters();

                String mainClass = getMainClass();


                // 设置项目类路径
                javaParameters.configureByModule(findModuleByMainClass(), JavaParameters.CLASSES_AND_TESTS);

                // 设置主类
                javaParameters.setMainClass(mainClass);

                // 设置程序参数
                String programParams = getProgramParameters();
                if (!programParams.isEmpty()) {
                    javaParameters.getProgramParametersList().addParametersString(programParams);
                }

                // 设置JVM参数
                String vmParams = getVmParameters();
                if (!vmParams.isEmpty()) {
                    javaParameters.getVMParametersList().addParametersString(vmParams);
                }

                // 设置工作目录
                String workingDir = getWorkingDirectory();
                if (!workingDir.isEmpty()) {
                    javaParameters.setWorkingDirectory(workingDir);
                }

                return javaParameters;
            }
        };
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();

        if (getMainClass().isEmpty()) {
            throw new RuntimeConfigurationException("未指定主类");
        }
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SolonRunConfigurationEditor(getProject());
    }
}
