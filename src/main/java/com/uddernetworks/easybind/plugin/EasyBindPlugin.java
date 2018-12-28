package com.uddernetworks.easybind.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;

public class EasyBindPlugin implements Plugin<Project> {
    public void apply(Project project) {
        Task createdTask = project.getTasks().create("easybind", EasyBind.class, task -> {
            File out = new File(project.getBuildDir(), "generated-sources");
            task.setGeneratedSource(out);
        });

        Task compileJavaTask = project.getTasks().getByName("compileJava");
        compileJavaTask.dependsOn(createdTask);
    }
}