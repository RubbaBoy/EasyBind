package com.uddernetworks.easybind.plugin;

import com.uddernetworks.easybind.depend.FXProperty;
import javafx.beans.property.ObjectProperty;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.CodeFactory;
import spoon.reflect.factory.CoreFactory;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BindingProcessor extends AbstractProcessor<CtField> {

    private List<CtTypeReference> propertableClasses;

    @Override
    public void process(CtField element) {
        try {
            if (!element.hasAnnotation(FXProperty.class)) return;
            FXProperty fxProperty = element.getAnnotation(FXProperty.class);
            CtClass parent = element.getParent(CtClass.class);

            CtType type = element.getType().getTypeDeclaration();

            String name = element.getSimpleName();
            String gsName = generateGSName(name);

            CoreFactory core = getFactory().Core();
            CodeFactory code = getFactory().Code();

            if (this.propertableClasses == null) this.propertableClasses = Arrays.asList(code.createCtTypeReference(Boolean.class), code.createCtTypeReference(Double.class), code.createCtTypeReference(Float.class), code.createCtTypeReference(Integer.class), code.createCtTypeReference(List.class), code.createCtTypeReference(Long.class), code.createCtTypeReference(Map.class), code.createCtTypeReference(Object.class), code.createCtTypeReference(Set.class), code.createCtTypeReference(String.class));

            CtPackageReference propertyPackage = code.createCtPackageReference(ObjectProperty.class.getPackage());

            if (!type.getPackage().getReference().equals(propertyPackage)) { // If the value is NOT a property
                boolean isObject = !this.propertableClasses.contains(type.getReference());
                String className = isObject ? "javafx.beans.property.ObjectProperty" : "javafx.beans.property." + type.getSimpleName() + "Property";

                Class property = Class.forName(className);

                String propertyName = generatePropertyName(name);
                int index = getUncollidingIndex(parent, element, name, 0);

                if (index > 0) {
                    propertyName += index;
                    gsName += index;
                }

                CtTypeReference ref = code.createCtTypeReference(property);
                if (isObject) ref.addActualTypeArgument(type.getReference());

                // Generating field like:   StringProperty setting = new SimpleStringProperty();
                CtField genField = core.createField();
                genField.setType(ref);
                genField.addModifier(ModifierKind.PUBLIC);
                genField.setSimpleName(propertyName);
                genField.setDefaultExpression(code.createCodeSnippetExpression(isObject ? "new javafx.beans.property.SimpleObjectProperty<>()" : "new javafx.beans.property.Simple" + type.getSimpleName() + "Property()"));

                parent.addField(genField);

                generateGetterAndSetter(parent, gsName, type.getReference(), propertyName, fxProperty);
            } else { // If this IS a property
                List<CtTypeReference<?>> actualTypeArguments = element.getType().getActualTypeArguments();
                if (actualTypeArguments.size() == 0) {
                    System.err.println("Don't know what to do with no types defined for field " + name);
                    return;
                }

                CtTypeReference<?> actualType = actualTypeArguments.get(0);

                generateGetterAndSetter(parent, gsName, actualType, name, fxProperty);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void generateGetterAndSetter(CtClass parent, String gsName, CtTypeReference originalClass, String name, FXProperty fxProperty) {
        CoreFactory core = getFactory().Core();
        CodeFactory code = getFactory().Code();

        // Generating getter like:   public String getSetting() { return this.setting.get(); }
        CtMethod getter = core.createMethod();
        getter.setType(originalClass);
        getter.addModifier(ModifierKind.PUBLIC);
        getter.setSimpleName("get" + gsName);
        getter.setBody(code.createCodeSnippetStatement("return this." + name + ".get()"));

        // Creating setter like above

        CtParameter parameter = core.createParameter();
        parameter.setSimpleName("value");
        parameter.setType(originalClass);

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