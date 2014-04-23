/*
 * Copyright 2014 Victor Nazarov <asviraspossible@gmail.com>.
 */
package com.github.sviperll.adt4j;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFormatter;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Victor Nazarov <asviraspossible@gmail.com>
 */
public class ADTVisitorInterfaceModel {
    private final JDefinedClass visitorInterfaceModel;
    private final DataVisitor dataVisitor;

    ADTVisitorInterfaceModel(JDefinedClass visitorInterfaceModel, DataVisitor dataVisitor) {
        this.visitorInterfaceModel = visitorInterfaceModel;
        this.dataVisitor = dataVisitor;
    }

    String getSimpleName() {
        return visitorInterfaceModel.name();
    }

    String getPackageName() {
        return visitorInterfaceModel._package().name();
    }

    Collection<JTypeVar> getDataTypeParameters() {
        List<JTypeVar> result = new ArrayList<>();
        for (JTypeVar typeVariable: visitorInterfaceModel.typeParams()) {
            if (!shouldBeOverridenOnInvokation(typeVariable.name()) && !isSelf(typeVariable.name()))
                result.add(typeVariable);
        }
        return result;
    }

    private boolean shouldBeOverridenOnInvokation(String name) {
        return name.equals(dataVisitor.result()) || name.equals(dataVisitor.exception());
    }

    private boolean isSelf(String name) {
        return name.equals(dataVisitor.self());
    }

    JTypeVar getResultTypeParameter() {
        for (JTypeVar typeVariable: visitorInterfaceModel.typeParams()) {
            if (typeVariable.name().equals(dataVisitor.result()))
                return typeVariable;
        }
        return null;
    }

    JTypeVar getExceptionTypeParameter() {
        for (JTypeVar typeVariable: visitorInterfaceModel.typeParams()) {
            if (typeVariable.name().equals(dataVisitor.exception()))
                return typeVariable;
        }
        return null;
    }

    private JTypeVar getSelfTypeParameter() {
        for (JTypeVar typeVariable: visitorInterfaceModel.typeParams()) {
            if (isSelf(typeVariable.name()))
                return typeVariable;
        }
        return null;
    }

    JClass narrowed(JClass usedDataType, JType resultType, JType exceptionType) {
        return narrowed(usedDataType, resultType, exceptionType, usedDataType);
    }

    JClass narrowed(JClass usedDataType, JType resultType, JType exceptionType, JType selfType) {
        Iterator<JClass> dataTypeArgumentIterator = usedDataType.getTypeParameters().iterator();
        JClass result = visitorInterfaceModel;
        for (JTypeVar typeVariable: visitorInterfaceModel.typeParams()) {
            if (typeVariable.name().equals(dataVisitor.exception()))
                result = result.narrow(exceptionType);
            else if (typeVariable.name().equals(dataVisitor.result()))
                result = result.narrow(resultType);
            else if (typeVariable.name().equals(dataVisitor.self()))
                result = result.narrow(selfType);
            else {
                result = result.narrow(dataTypeArgumentIterator.next());
            }
        }
        return result;
    }

    Collection<JMethod> methods() {
        return visitorInterfaceModel.methods();
    }

    @Override
    public String toString() {
        StringWriter sb = new StringWriter();
        visitorInterfaceModel.generate(new JFormatter(sb));
        return sb.toString();
    }

    JType substituteTypeParameter(JType type, JClass usedDataType, JType resultType, JType exceptionType) {
        if (type.name().equals(dataVisitor.exception()))
            return exceptionType;
        else if (type.name().equals(dataVisitor.result()))
            return resultType;
        else if (type.name().equals(dataVisitor.self()))
            return usedDataType;
        else
            return type;
    }

    boolean hasSelfTypeParameter() {
        return getSelfTypeParameter() != null;
    }
}