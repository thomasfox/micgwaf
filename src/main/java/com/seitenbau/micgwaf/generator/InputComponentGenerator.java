package com.seitenbau.micgwaf.generator;

import javax.servlet.http.HttpServletRequest;

import com.seitenbau.micgwaf.component.Component;
import com.seitenbau.micgwaf.component.HtmlElementComponent;
import com.seitenbau.micgwaf.component.InputComponent;

public class InputComponentGenerator extends HtmlElementComponentGenerator
{
  @Override
  public JavaClassName getClassName(
      Component component,
      String targetPackage)
  {
    return toJavaClassName(component.getId(), targetPackage);
  }
  
  @Override
  public String generate(
      Component component,
      String targetPackage)
  {
    InputComponent inputComponent = (InputComponent) component;
    // In the generated class, the value of the attribute "name" should not contain any loop parts.
    // They will be added again by calls to the inLoop method.
    inputComponent.attributes.put(
        InputComponent.NAME_ATTR, 
        removeLoopPart(inputComponent.attributes.get(InputComponent.NAME_ATTR)));
    String rootContent = super.generate(component, targetPackage);

    StringBuilder fileContent = new StringBuilder();
    
    // replace inheritance
    rootContent = rootContent.replace(" extends " + HtmlElementComponent.class.getSimpleName(), 
        " extends " + InputComponent.class.getSimpleName());

    // remove last "}"
    rootContent = rootContent.substring(0,  rootContent.lastIndexOf("}"));
    
    // add import
    int indexOfImport = rootContent.indexOf("\nimport");
    fileContent.append(rootContent.substring(0, indexOfImport))
        .append("\nimport ").append(HttpServletRequest.class.getName()).append(";\n")
        .append("import ").append(InputComponent.class.getName()).append(";")
        .append(rootContent.substring(indexOfImport));

    fileContent.append("}\n");
    
    return fileContent.toString();
  }

  @Override
  public String generateExtension(
      Component component,
      String targetPackage)
  {
    String className = getClassName(component, targetPackage).getSimpleName();
    String extensionClassName = getExtensionClassName(component, targetPackage).getSimpleName();

    StringBuilder fileContent = new StringBuilder();
    fileContent.append("package ").append(targetPackage).append(";\n\n");
    fileContent.append("\n");
    fileContent.append("import ").append(Component.class.getName()).append(";\n");
    fileContent.append("\n");
    fileContent.append("public class ").append(extensionClassName)
        .append(" extends ").append(className)
        .append("\n");
    fileContent.append("{\n");
    fileContent.append("  public " + extensionClassName + "(Component parent)\n");
    fileContent.append("  {\n");
    fileContent.append("    super(parent);\n");
    fileContent.append("  }\n");
    fileContent.append("}\n");
    return fileContent.toString();
  }
}
