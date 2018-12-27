# EasyBind
EasyBind is a gradle plugin that generates the proper getter and setters for JavaFX custom Node properties. This reduces clutter in your code, and removes boilerplate. The code is generated during compile-time, running a task before Gradle's `build` task automatically.

## Installation
Coming soon!

## Configuring
There is currently only one thing you can change with EasyBind as it is still in development. You can set the path Spoon outputs the files and that `compileJava` compiled the files from. This can be done via:
```Groovy
easybind {
    generatedSource = file("${project.buildDir}/generated-sources")
}
```
In the example above, it shows setting the generated source path to the default location.

## How it works
EasyBind runs a task automatically before Gradle's default `build` task. This task runs [Spoon](https://github.com/INRIA/spoon) on the sourcecode with the internal processors, and then temporarily changes the path the `compileJava` task uses to compile the program to the output of Spoon. This is so EasyBind does not interfere with the program's compilation, and doesn't touch the source files.