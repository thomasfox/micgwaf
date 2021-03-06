package de.seerheinlab.micgwaf.generator;

import java.util.ArrayList;
import java.util.List;

public class GeneratedClass
{
  public String classPackage = "";

  public List<String> imports = new ArrayList<>();

  public StringBuilder classJavadoc = new StringBuilder();

  public List<String> classAnnotations = new ArrayList<>();

  public StringBuilder classDefinition = new StringBuilder();

  public StringBuilder classBody = new StringBuilder();

  public List<GeneratedClass> innerClasses = new ArrayList<>();

  @Override
  public String toString()
  {
    StringBuilder result = new StringBuilder();
    result.append("package ").append(classPackage).append(";\n\n");
    for (String toImport : calculateImports())
    {
      result.append("import ").append(toImport).append(";\n");
    }
    result.append("\n");
    appendMainPart(result, 0, true);
    return result.toString();
  }

  public void appendMainPart(StringBuilder result, int indent, boolean innerClassesInClassBody)
  {
    if (!isSelfEmpty())
    {
      result.append("\n");
      result.append(GeneratorHelper.indent(classJavadoc.toString(), indent)).append("\n");
      for (String classAnnotation : classAnnotations)
      {
        result.append(GeneratorHelper.indent(classAnnotation, indent)).append("\n");
      }
      result.append(GeneratorHelper.indent(classDefinition.toString(), indent)).append("\n");
      result.append(GeneratorHelper.indent("{\n", indent));
      result.append(GeneratorHelper.indent(classBody.toString(), indent)).append("\n");
      if (!innerClassesInClassBody)
      {
        result.append(GeneratorHelper.getIndentString(indent)).append("}\n");
      }
    }
    for (GeneratedClass innerClass : innerClasses)
    {
      if (!innerClass.isEmpty())
      {
        innerClass.appendMainPart(result, indent == 0 ? 2 : indent, false);
      }
    }
    if (innerClassesInClassBody && !isSelfEmpty())
    {
      result.append(GeneratorHelper.getIndentString(indent)).append("}\n");
    }
  }

  public List<String> calculateImports()
  {
    List<String> result = new ArrayList<>(imports);
    for (GeneratedClass innerClass : innerClasses)
    {
      for (String toImport : innerClass.calculateImports())
      {
        if (!result.contains(toImport))
        {
          result.add(toImport);
        }
      }
    }
    return result;
  }

  public void clearImportsRecursively()
  {
    imports.clear();
    for (GeneratedClass innerClass : innerClasses)
    {
      innerClass.clearImportsRecursively();
    }
  }

  public boolean isEmpty()
  {
    return (classAnnotations.isEmpty()
        && classDefinition.length() == 0
        && classBody.length() == 0
        && innerClasses.isEmpty());
  }

  public boolean isSelfEmpty()
  {
    return (classAnnotations.isEmpty()
        && classDefinition.length() == 0
        && classBody.length() == 0);
  }
}
