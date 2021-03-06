/*
 * Copyright (c) 2014, Victor Nazarov <asviraspossible@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *
 *  3. Neither the name of the copyright holder nor the names of its contributors
 *     may be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.sviperll.adt4j;

import com.github.sviperll.meta.JCodeModelJavaxLangModelAdapter;
import com.github.sviperll.meta.FilerCodeWriter;
import com.github.sviperll.adt4j.model.ValueClassModelFactory;
import com.github.sviperll.meta.ErrorTypeFound;
import com.github.sviperll.meta.ProcessingException;
import com.github.sviperll.meta.SourceException;
import com.github.sviperll.meta.Visitor;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.github.sviperll.adt4j.GenerateValueClassForVisitor")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GenerateValueClassForVisitorProcessor extends AbstractProcessor {
    private final Set<String> remainingElements = new HashSet<String>();
    private final List<String> errors = new ArrayList<String>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) {
                for (String path: remainingElements) {
                    errors.add("Unable to process " + path);
                }
                for (String error: errors) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
                }
            } else {
                Set<TypeElement> elements = new HashSet<TypeElement>();
                for (Element element: roundEnv.getElementsAnnotatedWith(GenerateValueClassForVisitor.class)) {
                    elements.add((TypeElement)element);
                }
                for (String path: remainingElements) {
                    elements.add(processingEnv.getElementUtils().getTypeElement(path));
                }
                remainingElements.clear();
                processElements(elements);
            }
        } catch (IOException ex) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
        }
        return true;
    }

    private void processElements(Set<? extends TypeElement> elements) throws IOException {
        for (TypeElement element: elements) {
            try {
                JCodeModel jCodeModel = new JCodeModel();
                Visitor visitorAnnotation = element.getAnnotation(Visitor.class);
                if (visitorAnnotation == null)
                    throw new SourceException("No " + Visitor.class.getName() + " annotation for " + element.getQualifiedName() + " class annotated with " + GenerateValueClassForVisitor.class.getName() + " annotation");
                GenerateValueClassForVisitor generateAnnotation = element.getAnnotation(GenerateValueClassForVisitor.class);
                JCodeModelJavaxLangModelAdapter adapter = new JCodeModelJavaxLangModelAdapter(jCodeModel, processingEnv.getElementUtils());
                JDefinedClass visitorModel = adapter.getClass(element);
                JDefinedClass valueClass = ValueClassModelFactory.createValueClass(visitorModel, visitorAnnotation, generateAnnotation);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generated value class " + valueClass.fullName() + " for " + element + " visitor interface");
                FilerCodeWriter writer = new FilerCodeWriter(processingEnv.getFiler(), processingEnv.getMessager());
                try {
                    jCodeModel.build(writer);
                } finally {
                    writer.close();
                }
            } catch (ErrorTypeFound ex) {
                remainingElements.add(element.getQualifiedName().toString());
            } catch (ProcessingException ex) {
                errors.add(element + ": " + ex.getMessage());
            } catch (RuntimeException ex) {
                errors.add(element + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }
    }
}
