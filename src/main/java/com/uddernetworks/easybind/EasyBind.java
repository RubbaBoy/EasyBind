package com.uddernetworks.easybind;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;

import java.io.File;

public class EasyBind extends DefaultTask {
    private File generatedSource;

    @TaskAction
    void runTransformer() {
        this.generatedSource.mkdirs();

        Task compileJavaTask = getProject().getTasks().getByName("compileJava");
        compileJavaTask.setProperty("source", this.generatedSource);

        System.out.println("Running transformer...");

        final String[] args = {
                "-i", "src/main/java/",
                "-o", this.generatedSource.getAbsolutePath(),
                "-p", "com.uddernetworks.easybind.BindingProcessor",
                "--noclasspath"
        };

        final Launcher launcher = new Launcher();
        launcher.setArgs(args);
        launcher.run();

        System.out.println("Completed class transformation");
    }

    public File getGeneratedSource() {
        return generatedSource;
    }

    public void setGeneratedSource(File generatedSource) {
        this.generatedSource = generatedSource;
    }
}