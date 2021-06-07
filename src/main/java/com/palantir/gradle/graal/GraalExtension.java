/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.gradle.graal;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

/**
 * Contains options and settings for tuning GraalVM use.
 */
public class GraalExtension {
    private static final String WINDOWS_7_ENV_PATH =
            "C:\\Program Files\\Microsoft SDKs\\" + "Windows\\v7.1\\Bin\\SetEnv.cmd";
    private static final List<String> SUPPORTED_WINDOWS_VS_VERSIONS = Arrays.asList("2019", "2017");
    private static final List<String> SUPPORTED_WINDOWS_VS_EDITIONS =
            Arrays.asList("Enterprise", "Professional", "Community");
    private static final String DEFAULT_WINDOWS_VS_PATH = "C:\\Program Files (x86)\\" + "Microsoft Visual Studio";
    private static final String DEFAULT_WINDOWS_VS_VARS_PATH = "C:\\Program Files (x86)\\Microsoft Visual Studio\\"
            + "{version}\\{edition}\\VC\\Auxiliary\\"
            + "Build\\vcvars64.bat";

    private static final String DEFAULT_DOWNLOAD_BASE_URL = "https://github.com/oracle/graal/releases/download/";
    private static final String DOWNLOAD_BASE_URL_GRAAL_19_3 =
            "https://github.com/graalvm/graalvm-ce-builds/" + "releases/download/";
    private static final String DEFAULT_GRAAL_VERSION = "20.2.0";
    private static final List<String> SUPPORTED_JAVA_VERSIONS = Arrays.asList("16", "11", "8");
    private static final String DEFAULT_JAVA_VERSION = "8";

    private final Property<String> downloadBaseUrl;
    private final Property<String> graalVersion;
    private final Property<String> javaVersion;
    private final Property<String> windowsVsVarsPath;
    private final Property<String> windowsVsVersion;
    private final Property<String> windowsVsEdition;
    private final Property<String> mainClass;
    private final Property<String> outputName;
    private final ListProperty<String> options;

    private ProviderFactory providerFactory;

    public GraalExtension(Project project) {
        downloadBaseUrl = project.getObjects().property(String.class);
        graalVersion = project.getObjects().property(String.class);
        javaVersion = project.getObjects().property(String.class);
        windowsVsVarsPath = project.getObjects().property(String.class);
        windowsVsVersion = project.getObjects().property(String.class);
        windowsVsEdition = project.getObjects().property(String.class);
        mainClass = project.getObjects().property(String.class);
        outputName = project.getObjects().property(String.class);
        options = project.getObjects().listProperty(String.class).empty(); // .empty() required to initialize
        providerFactory = project.getProviders();

        // defaults
        graalVersion.set(DEFAULT_GRAAL_VERSION);
        javaVersion.set(DEFAULT_JAVA_VERSION);
    }

    public final void downloadBaseUrl(String value) {
        downloadBaseUrl.set(value);
    }

    /**
     * Returns the base URL to use for downloading GraalVM binaries.
     *
     * <p>Defaults to {@link #DEFAULT_DOWNLOAD_BASE_URL} for GraalVM lower than 19.3.</p>
     * <p>Defaults to {@link #DOWNLOAD_BASE_URL_GRAAL_19_3} for GraalVM higher or equal to 19.3.</p>
     */
    public final Provider<String> getDownloadBaseUrl() {
        return downloadBaseUrl.orElse(getDefaultDownloadBaseUrl());
    }

    public final void mainClass(String value) {
        mainClass.set(value);
    }

    /**
     * Returns the main class to use as the entry point to the generated executable file.
     */
    public final Provider<String> getMainClass() {
        return mainClass;
    }

    public final void outputName(String value) {
        outputName.set(value);
    }

    /**
     * Returns the outputName to use for the generated executable file.
     *
     * <p>Check {@link org.gradle.api.provider.Provider#isPresent()} to determine if an override has been set.</p>
     */
    public final Provider<String> getOutputName() {
        return outputName;
    }

    public final void graalVersion(String value) {
        graalVersion.set(value);
    }

    /**
     * Returns the javaVersion for GraalVM to use.
     *
     * <p>Defaults to {@link #DEFAULT_JAVA_VERSION}</p>
     */
    public final Provider<String> getJavaVersion() {
        return javaVersion;
    }

    public final void javaVersion(String value) {
        if (!SUPPORTED_JAVA_VERSIONS.contains(value)) {
            throw new GradleException(
                    "Java version " + value + " is not supported. Supported versions are: " + SUPPORTED_JAVA_VERSIONS);
        }
        javaVersion.set(value);
    }

    /**
     * Returns the VS 64-bit Variables Path to use.
     *
     * <p>Defaults to {@link #DEFAULT_WINDOWS_VS_VARS_PATH} for JDK higher or equal to 11</p>
     * <p>Defaults to {@link #WINDOWS_7_ENV_PATH} for JDK lower than 11</p>
     */
    public final Provider<String> getWindowsVsVarsPath() {
        return windowsVsVarsPath.orElse(searchWindowsVsVarsPath());
    }

    private String searchWindowsVsVarsPath() {
        String searchedVsVersion = windowsVsVersion.getOrElse(getNewestWindowsVsVersionInstalled());
        String searchedVsEdition = windowsVsEdition.getOrElse(getBiggestWindowsVsEditionInstalled(searchedVsVersion));
        if (searchedVsEdition == null || searchedVsVersion == null) {
            return "";
        }

        String searchedVsVarsPath = Integer.parseInt(javaVersion.get()) >= 11
                ? DEFAULT_WINDOWS_VS_VARS_PATH
                        .replaceAll("\\{version}", searchedVsVersion)
                        .replaceAll("\\{edition}", searchedVsEdition)
                : WINDOWS_7_ENV_PATH;
        if (WINDOWS_7_ENV_PATH.equals(searchedVsVarsPath)) {
            if (!new File(WINDOWS_7_ENV_PATH).exists()) {
                return "";
            }
        }
        return searchedVsVarsPath;
    }

    private String getNewestWindowsVsVersionInstalled() {
        return FileUtil.getFirstFromDirectory(new File(DEFAULT_WINDOWS_VS_PATH), SUPPORTED_WINDOWS_VS_VERSIONS);
    }

    private String getBiggestWindowsVsEditionInstalled(String version) {
        if (version == null) {
            return null;
        }

        return FileUtil.getFirstFromDirectory(
                new File(DEFAULT_WINDOWS_VS_PATH, version), SUPPORTED_WINDOWS_VS_EDITIONS);
    }

    public final void windowsVsVarsPath(String value) {
        windowsVsVarsPath.set(value);
    }

    public final void windowsVsVersion(String value) {
        windowsVsVersion.set(value);
    }

    public final void windowsVsEdition(String value) {
        windowsVsEdition.set(value);
    }

    /**
     * Returns the graalVersion of GraalVM to use.
     *
     * <p>Defaults to {@link #DEFAULT_GRAAL_VERSION}</p>
     */
    public final Provider<String> getGraalVersion() {
        return graalVersion;
    }

    public final Provider<List<String>> getOptions() {
        return options;
    }

    /**
     * Add option from https://github.com/oracle/graal/blob/master/substratevm/OPTIONS.md.
     */
    public final void option(String option) {
        if (option.trim().startsWith("-H:Name=")) {
            throw new GradleException("Use 'outputName' instead of '" + option + "'");
        }
        this.options.add(option);
    }

    public final Provider<String> getGraalDirectoryName() {
        return providerFactory.provider(() -> {
            if (GraalVersionUtil.isGraalVersionGreaterOrEqualThan(graalVersion.get(), 19, 3)) {
                return "graalvm-ce-java" + javaVersion.get() + "-" + graalVersion.get();
            }
            return "graalvm-ce-" + graalVersion.get();
        });
    }

    private String getDefaultDownloadBaseUrl() {
        if (javaVersion.get().equals("16")
                && !GraalVersionUtil.isGraalVersionGreaterOrEqualThan(graalVersion.get(), 21, 1)) {
            throw new GradleException(
                    "Unsupported GraalVM version " + graalVersion.get() + " for Java 16, needs >= 21.1.0.");
        } else if (GraalVersionUtil.isGraalVersionGreaterOrEqualThan(graalVersion.get(), 19, 3)) {
            return DOWNLOAD_BASE_URL_GRAAL_19_3;
        } else if (!javaVersion.get().equals("8")) {
            throw new GradleException("Unsupported Java version for GraalVM version.");
        }
        return DEFAULT_DOWNLOAD_BASE_URL;
    }
}
