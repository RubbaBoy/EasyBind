package com.uddernetworks.easybind;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;

public class BindingProcessor extends AbstractProcessor<CtExecutable> {

    @Override
    public void process(CtExecutable element) { // TODO: Implement real processor
        CtCodeSnippetStatement snippet = getFactory().Core().createCodeSnippetStatement();

        // Snippet which contains the log.
        final String value = String.format("System.out.println(\"Enter in the method %s from the class %s\");",
                element.getSimpleName(),
                element.getParent(CtClass.class).getSimpleName());
        snippet.setValue(value);

        // Inserts the snippet at the beginning of the method body.
        if (element.getBody() != null) {
            element.getBody().insertBegin(snippet);
        }
    }

}