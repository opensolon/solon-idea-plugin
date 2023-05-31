package org.noear.solon.idea.plugin.initializr.metadata;

import com.alibaba.fastjson2.JSON;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.io.HttpRequests;
import org.noear.solon.idea.plugin.initializr.metadata.json.SolonMetadata;
import org.noear.solon.idea.plugin.initializr.util.SolonInitializrUtil;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;

import static com.intellij.openapi.util.io.FileUtil.sanitizeFileName;
import static com.intellij.psi.impl.PsiNameHelperImpl.getInstance;

/**
 * @author liupeiqiang
 * @date 2023/2/7 0:42
 */
public class SolonCreationMetadata {

    private String serverUrl = "https://solon.noear.org";

    private String name;

    private String location = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();

    private boolean isInitGit = false;

    private String language = "java";

    private String type = "maven";

    private String groupId;

    private String artifactId;

    private String packageName;

    private Sdk sdk;

    private String javaVersion;

    private String packaging = "jar";

    private String solonVer;

    private String dependency;

    private String description;

    private SolonMetadata initMetadata = null;

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

    public boolean hasValidDescription() {
        return StringUtil.isNotEmpty(this.description);
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

    public boolean hasCompatibleJavaVersion() {
        if (this.sdk != null){
            JavaSdkVersion wizardSdkVersion = JavaSdk.getInstance().getVersion(this.sdk);
            if (wizardSdkVersion != null) {
                LanguageLevel selectedLanguageLevel = LanguageLevel.parse(javaVersion);
                JavaSdkVersion selectedSdkVersion =
                        selectedLanguageLevel != null ? JavaSdkVersion.fromLanguageLevel(selectedLanguageLevel) : null;
                // only if selected java version is compatible with wizard version
                return selectedSdkVersion == null || wizardSdkVersion.isAtLeast(selectedSdkVersion);
            }
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

    public String getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public SolonCreationMetadata setDescription(String description) {
        this.description = description;
        return this;
    }

    public SolonCreationMetadata setLanguage(String language) {
        this.language = language;
        return this;
    }

    public String getType() {
        return type;
    }

    public SolonCreationMetadata setType(String type) {
        this.type = type;
        return this;
    }

    public String getPackaging() {
        return packaging;
    }

    public SolonCreationMetadata setPackaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public String getSolonVer() {
        return solonVer;
    }

    public SolonCreationMetadata setSolonVer(String solonVer) {
        this.solonVer = solonVer;
        return this;
    }

    public String getDependency() {
        return dependency;
    }

    public SolonCreationMetadata setDependency(String dependency) {
        this.dependency = dependency;
        return this;
    }

    public SolonMetadata getInitMetadata() {
        return initMetadata;
    }

    public SolonCreationMetadata setInitMetadata(SolonMetadata initMetadata) {
        this.initMetadata = initMetadata;
        return this;
    }

    public String buildDownloadUrl() {
        return serverUrl + "/start/build.do" + "?"
                + SolonInitializrUtil.nameAndValueAsUrlParam("project", this.getType()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("language", this.getLanguage()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("solonVar", this.getSolonVer()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("group", this.getGroupId()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("artifact", this.getArtifactId()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("description", this.getDescription()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("packageName", this.getPackageName()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("packaging", this.getPackaging()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("javaVer", this.getJavaVersion()) + "&"
                + SolonInitializrUtil.nameAndValueAsUrlParam("dependencies", this.getDependency());
    }
    private String buildMetadataGetUrl(){
        return serverUrl + "/start/metadata.json";
    }

    public void refreshMetadataOptions(ProgressIndicator indicator) throws IOException {
        String responseText = HttpRequests.request(buildMetadataGetUrl()).readString();
        SolonMetadata solonMetadata = JSON.parseObject(responseText, SolonMetadata.class);
        setInitMetadata(solonMetadata);
        setName(solonMetadata.getArtifact().getDefaultValue());
        setGroupId(solonMetadata.getGroup().getDefaultValue());
        setArtifactId(solonMetadata.getArtifact().getDefaultValue());
        setPackageName(solonMetadata.getPackageName().getDefaultValue());
        setJavaVersion(solonMetadata.getJavaVer().getDefaultValue());
        setSolonVer(solonMetadata.getSolonVer().getDefaultValue());
        setDependency(solonMetadata.getDependencies().getDefaultValue());
        setDescription(solonMetadata.getDescription().getDefaultValue());
    }
}
