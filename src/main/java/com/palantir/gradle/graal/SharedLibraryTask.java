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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;

/**
 * Runs GraalVM's native-image command configured to produce a shared library.
 */
public class SharedLibraryTask extends BaseGraalCompileTask {

    public SharedLibraryTask() {
        setDescription(
                "Runs GraalVM's native-image command configured to produce a shared library."
        );
        // must use an anonymous inner class instead of a lambda to get Gradle staleness checking
        doLast(new LogAction());
    }

    /**
     * Returns a platform-dependent file extension for libraries.
     *
     * @return ".dylib" on {@link Platform.OperatingSystem#MAC MAC}, ".so" on
     *          {@link Platform.OperatingSystem#LINUX LINUX}, ".dll" on {@link Platform.OperatingSystem#WINDOWS WINDOWS}
     */
    @Override
    protected String getArchitectureSpecifiedOutputExtension() {
        switch (Platform.operatingSystem()) {
            case MAC:
                return ".dylib";
            case LINUX:
                return ".so";
            case WINDOWS:
                return ".dll";
            default:
                throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        }
    }

    @TaskAction
    public final void sharedLibrary() throws IOException {
        List<String> args = new ArrayList<>();
        args.add("--shared");
        configureArgs(args);
        getProject().exec(spec -> {
            spec.setExecutable(getExecutable());
            spec.setArgs(args);
            configurePlatformSpecifics(spec);
        });
    }

    private final class LogAction implements Action<Task> {
        @Override
        public void execute(Task _task) {
            getLogger().warn("shared library available at {} ({} MB)",
                    getProject().relativePath(getOutputFile().get().getAsFile()),
                    fileSizeMegabytes(getOutputFile().get()));
        }
    }

}
