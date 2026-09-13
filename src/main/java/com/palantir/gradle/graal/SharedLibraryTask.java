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

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.work.DisableCachingByDefault;

/**
 * Runs GraalVM's native-image command configured to produce a shared library.
 */
@DisableCachingByDefault(because = "Not opting into build caching; explicit opt-out is required by Gradle 9.7")
public abstract class SharedLibraryTask extends BaseGraalCompileTask {

    public SharedLibraryTask() {
        setDescription("Runs GraalVM's native-image command configured to produce a shared library.");

        getCommand().addAll(getProject().provider(() -> {
            CommandSpec spec = new CommandSpec(getExecutable());
            spec.addArg("--shared");
            configureArgs(spec);
            return configurePlatformSpecifics(spec).toCommand();
        }));

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
        return switch (Platform.operatingSystem()) {
            case MAC -> ".dylib";
            case LINUX -> ".so";
            case WINDOWS -> ".dll";
            default -> throw new IllegalStateException("No GraalVM support for " + Platform.operatingSystem());
        };
    }

    private final class LogAction implements Action<Task> {
        @SuppressWarnings("for-rollout:IllegalMethodCalledDuringTaskExecution")
        @Override
        public void execute(Task _task) {
            getLogger()
                    .warn(
                            "shared library available at {} ({} MB)",
                            getProject().relativePath(getOutputFile().get().getAsFile()),
                            fileSizeMegabytes(getOutputFile().get()));
        }
    }
}
