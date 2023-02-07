package org.noear.plugin.idea.solon.initializr;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import org.noear.plugin.idea.solon.initializr.util.SolonInitializrUtil;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

import static com.intellij.openapi.util.io.FileUtil.sanitizeFileName;
import static com.intellij.psi.impl.PsiNameHelperImpl.getInstance;

/**
 * @author liupeiqiang
 * @date 2023/2/7 0:42
 */
public class SolonCreationMetadata {

    private String name = "demo";

    private String location = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();

    private boolean isInitGit = false;

    private String groupId = "com.example";

    private String artifactId = "demo";

    private String packageName = "com.example.demo";

    private Sdk sdk;

    private String javaVersion = "8";

    private static String sanitize(String input) {
        return sanitizeFileName(input, false).replace(' ', '-').toLowerCase();
    }

    public boolean hasValidName() {
        return StringUtil.isNotEmpty(this.name);
    }

    public boolean hasValidLocation() {
        File file = new File(this.location);
        return file.isDirectory() && file.exists() && file.isAbsolute();
    }

    public boolean hasValidGroupId() {
        return StringUtil.isNotEmpty(this.groupId);
    }

    public boolean hasValidArtifactId() {
        return StringUtil.isNotEmpty(this.artifactId) && sanitize(this.artifactId).equals(this.artifactId);
    }

    public boolean hasValidPackageName() {
        return StringUtil.isNotEmpty(this.packageName) && getInstance().isQualifiedName(this.packageName);
    }

    public boolean hasCompatibleJavaVersion(ModuleBuilder moduleBuilder,
                                            WizardContext wizardContext) {
        JavaSdkVersion wizardSdkVersion = SolonInitializrUtil.from(wizardContext, moduleBuilder);
        if (wizardSdkVersion != null) {
            LanguageLevel selectedLanguageLevel = LanguageLevel.parse(javaVersion);
            JavaSdkVersion selectedSdkVersion =
                    selectedLanguageLevel != null ? JavaSdkVersion.fromLanguageLevel(selectedLanguageLevel) : null;
            // only if selected java version is compatible with wizard version
            return selectedSdkVersion == null || wizardSdkVersion.isAtLeast(selectedSdkVersion);
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isInitGit() {
        return isInitGit;
    }

    public void setInitGit(boolean initGit) {
        isInitGit = initGit;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Sdk getSdk() {
        return sdk;
    }

    public void setSdk(Sdk sdk) {
        this.sdk = sdk;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }


}
