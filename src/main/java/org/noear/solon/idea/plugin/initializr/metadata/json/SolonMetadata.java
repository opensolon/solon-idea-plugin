package org.noear.solon.idea.plugin.initializr.metadata.json;

import java.util.List;

public class SolonMetadata {

    private SolonMetadataOption project;
    private SolonMetadataOption language;
    private SolonMetadataOption solonVer;
    private SolonMetadataOption group;
    private SolonMetadataOption artifact;
    private SolonMetadataOption description;
    private SolonMetadataOption packageName;
    private SolonMetadataOption packaging;
    private SolonMetadataOption javaVer;
    private SolonMetadataOption dependencies;

    public SolonMetadataOption getProject() {
        return project;
    }

    public SolonMetadata setProject(SolonMetadataOption project) {
        this.project = project;
        return this;
    }

    public SolonMetadataOption getLanguage() {
        return language;
    }

    public SolonMetadata setLanguage(SolonMetadataOption language) {
        this.language = language;
        return this;
    }

    public SolonMetadataOption getSolonVer() {
        return solonVer;
    }

    public SolonMetadata setSolonVer(SolonMetadataOption solonVer) {
        this.solonVer = solonVer;
        return this;
    }

    public SolonMetadataOption getGroup() {
        return group;
    }

    public SolonMetadata setGroup(SolonMetadataOption group) {
        this.group = group;
        return this;
    }

    public SolonMetadataOption getArtifact() {
        return artifact;
    }

    public SolonMetadata setArtifact(SolonMetadataOption artifact) {
        this.artifact = artifact;
        return this;
    }

    public SolonMetadataOption getDescription() {
        return description;
    }

    public SolonMetadata setDescription(SolonMetadataOption description) {
        this.description = description;
        return this;
    }

    public SolonMetadataOption getPackageName() {
        return packageName;
    }

    public SolonMetadata setPackageName(SolonMetadataOption packageName) {
        this.packageName = packageName;
        return this;
    }

    public SolonMetadataOption getPackaging() {
        return packaging;
    }

    public SolonMetadata setPackaging(SolonMetadataOption packaging) {
        this.packaging = packaging;
        return this;
    }

    public SolonMetadataOption getJavaVer() {
        return javaVer;
    }

    public SolonMetadata setJavaVer(SolonMetadataOption javaVer) {
        this.javaVer = javaVer;
        return this;
    }

    public SolonMetadataOption getDependencies() {
        return dependencies;
    }

    public SolonMetadata setDependencies(SolonMetadataOption dependencies) {
        this.dependencies = dependencies;
        return this;
    }
}
