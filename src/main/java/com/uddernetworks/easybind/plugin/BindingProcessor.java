package com.uddernetworks.easybind.plugin;

import com.uddernetworks.easybind.depend.FXProperty;
import javafx.beans.property.*;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.CoreFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BindingProcessor extends AbstractProcessor<CtField> {

    private List<Class> propertableClasses = Arrays.asList(Boolean.class, Double.class, Float.class, Integer.class, List.class, Long.class, Map.class, Object.class, Set.class, String.class);
    private List<Class> propertyClasses = Arrays.asList(BooleanProperty.class, DoubleProperty.class, FloatProperty.class, IntegerProperty.class, ListProperty.class, LongProperty.class, MapProperty.class, ObjectProperty.class, SetProperty.class, StringProperty.class);

    @Override
    public void process(CtField element) { // TODO: Implement real processor
        try {
            if (!element.hasAnnotation(FXProperty.class)) return;
            FXProperty fxProperty = element.getAnnotation(FXProperty.class);
            CtClass parent = element.getParent(CtClass.class);

            Class type = element.getType().getActualClass();

            String name = element.getSimpleName();
            String gsName = generateGSName(name);

            CoreFactory core = getFactory().Core();
            CodeFactory code = getFactory().Code();

            if (propertableClasses.contains(type)) {
                Class property = Class.forName("javafx.beans.property." + type.getSimpleName() + "Property");

                System.out.println("Binding to a normal field: " + type.getCanonicalName());


                String propertyName = generatePropertyName(name);
                int index = getUncollidingIndex(parent, element, name, 0);
                System.out.println("Colliding index: " + index);

                if (index > 0) {
                    propertyName += index;
                    gsName += index;
                }


                // Generating field like:   StringProperty setting = new SimpleStringProperty();
                CtField genField = core.createField();
                genField.setType(code.createCtTypeReference(property));
                genField.addModifier(ModifierKind.PUBLIC);
                genField.setSimpleName(propertyName);
                genField.setDefaultExpression(code.createCodeSnippetExpression("new javafx.beans.property.Simple" + type.getSimpleName() + "Property()"));

                parent.addField(genField);

                generateGetterAndSetter(parent, gsName, type, propertyName, fxProperty);
            } else if (propertyClasses.contains(type)) {
                String prefixing = type.equals(List.class) || type.equals(Map.class) || type.equals(Set.class) ? "java.util." : "java.lang.";
                Class originalClass = Class.forName(prefixing + type.getSimpleName().replace("Property", ""));
                System.out.println("Binding to a property: " + type.getCanonicalName());
                System.out.println("Original calculated class: " + originalClass.getCanonicalName());


                generateGetterAndSetter(parent, gsName, originalClass, name, fxProperty);
            } else {
                try {
                    throw new Exception("Invalid class with @FXProperty binding: " + type.getCanonicalName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void generateGetterAndSetter(CtClass parent, String gsName, Class originalClass, String name, FXProperty fxProperty) {
        CoreFactory core = getFactory().Core();
        CodeFactory code = getFactory().Code();

        // Generating getter like:   public String getSetting() { return this.setting.get(); }
        CtMethod getter = core.createMethod();
        getter.setType(code.createCtTypeReference(originalClass));
        getter.addModifier(ModifierKind.PUBLIC);
        getter.setSimpleName("get" + gsName);
        getter.setBody(code.createCodeSnippetStatement("return this." + name + ".get()"));

        // Creating setter like above

        CtParameter parameter = core.createParameter();
        parameter.setSimpleName("value");
        parameter.setType(code.createCtTypeReference(originalClass));

        CtMethod setter = core.createMethod();
        setter.setType(getFactory().Class().VOID_PRIMITIVE);
        setter.addParameter(parameter);
        setter.addModifier(ModifierKind.PUBLIC);
        setter.setSimpleName("set" + gsName);
        String body = "this." + name + ".set(value)";

        if (!fxProperty.onSetMethod().equals("")) {
            body += ";\n" + fxProperty.onSetMethod() + "(value)";
        }

        if (!fxProperty.onSetCode().equals("")) {
            body += ";\n" + fxProperty.onSetCode();
        }

        setter.setBody(code.createCodeSnippetStatement(body));

        parent.addMethod(getter);
        parent.addMethod(setter);
    }

    private int getUncollidingIndex(CtClass<?> parent, CtField originalField, String start, int index) {
        if (parent.getFields().stream().anyMatch(ctField -> !ctField.equals(originalField) && ctField.getSimpleName().equals(start + (index > 0 ? index : ""))))
            return getUncollidingIndex(parent, originalField, start, index + 1);
        return index;
    }

    private String generatePropertyName(String name) {
        return name + "Property";
    }

    private String generateGSName(String name) {
        if (name.length() == 1) return name.toUpperCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

}