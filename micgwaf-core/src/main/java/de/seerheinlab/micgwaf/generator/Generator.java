package de.seerheinlab.micgwaf.generator;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import de.seerheinlab.micgwaf.component.ChildListComponent;
import de.seerheinlab.micgwaf.component.Component;
import de.seerheinlab.micgwaf.component.ComponentRegistry;
import de.seerheinlab.micgwaf.component.FormComponent;
import de.seerheinlab.micgwaf.component.HtmlElementComponent;
import de.seerheinlab.micgwaf.component.InputComponent;
import de.seerheinlab.micgwaf.component.parse.DefineComponent;
import de.seerheinlab.micgwaf.component.parse.InsertComponent;
import de.seerheinlab.micgwaf.component.parse.PartListComponent;
import de.seerheinlab.micgwaf.component.parse.ReferenceComponent;
import de.seerheinlab.micgwaf.component.parse.UseTemplateComponent;
import de.seerheinlab.micgwaf.generator.component.ChildListComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.ComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.DefineComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.FormComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.GenerationContext;
import de.seerheinlab.micgwaf.generator.component.HtmlElementComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.InputComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.InsertComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.PartListComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.RefComponentGenerator;
import de.seerheinlab.micgwaf.generator.component.UseTemplateComponentGenerator;
import de.seerheinlab.micgwaf.generator.config.GeneratorConfiguration;
import de.seerheinlab.micgwaf.parser.HtmlParser;
import de.seerheinlab.micgwaf.util.Assertions;

/**
 * Entry point for generating the component classes from HTML.
 * The generate() methods parses the HTML files and writes the resulting component sources into the target
 * directories. The directory structure in the source folder is retained.
 * The main method
 */
public class Generator
{
  private static final char DOT = '.';

  private static final char SLASH = '/';

  /** All parsed root components, keyed by their component id. */
  public static final Map<Class<? extends Component>, ComponentGenerator> componentGeneratorMap
      = new HashMap<>();

  /** Post-processor to remove unused imports. */
  public static RemoveUnusedImports removeUnusedImports = new RemoveUnusedImports();

  /** The configuration of the generator. */
  public static GeneratorConfiguration generatorConfiguration;

  /** Reference to the default generator configuration. */
  public static String configurationClasspathResource
      = "/de/seerheinlab/micgwaf/config/default-micgwaf-codegen.properties";

  static
  {
    componentGeneratorMap.put(PartListComponent.class, new PartListComponentGenerator());
    componentGeneratorMap.put(HtmlElementComponent.class, new HtmlElementComponentGenerator());
    componentGeneratorMap.put(InputComponent.class, new InputComponentGenerator());
    componentGeneratorMap.put(FormComponent.class, new FormComponentGenerator());
    componentGeneratorMap.put(ReferenceComponent.class, new RefComponentGenerator());
    componentGeneratorMap.put(ChildListComponent.class, new ChildListComponentGenerator());
    componentGeneratorMap.put(InsertComponent.class, new InsertComponentGenerator());
    componentGeneratorMap.put(UseTemplateComponent.class, new UseTemplateComponentGenerator());
    componentGeneratorMap.put(DefineComponent.class, new DefineComponentGenerator());
  }

  /**
   * Generates code for all xhtml files in a directory and the contained subdirectories.
   * The generated code includes base component classes, extension component classes,
   * and a component registry class.
   *
   * @param sourceDirectory The directory containing the XHTML files (extension .xhtml).
   * @param targetDirectory The directory where component source files are written.
   *        Existing files in this directory are overwritten each generation run without notice.
   * @param extensionsTargetDirectory The directory where component extension source files are written.
   *        These files are intended for modification by the user, thus existing files are not overwritten.
   * @param baseComponentPackage the base package for component classes.
   *
   * @throws IOException if generated files cannot be written to the file system.
   * @throws RuntimeException if an error during generation occurs.
   */
  public void generate(
        File sourceDirectory,
        File targetDirectory,
        File extensionsTargetDirectory,
        String baseComponentPackage)
      throws IOException
  {
    generate(sourceDirectory, targetDirectory, extensionsTargetDirectory, baseComponentPackage, null);
  }

  /**
   * Generates code for all xhtml files in a directory and the contained subdirectories.
   * The generated code includes base component classes, extension component classes,
   * and a component registry class.
   *
   * @param sourceDirectory The directory containing the XHTML files (extension .xhtml).
   * @param targetDirectory The directory where component source files are written.
   *        Existing files in this directory are overwritten each generation run without notice.
   * @param extensionsTargetDirectory The directory where component extension source files are written.
   *        These files are intended for modification by the user, thus existing files are not overwritten.
   * @param rootPackage the base package for component classes.
   * @param classLoader the class loader to use for component lib discovery,
   *       or null to use the default classloader.
   *
   * @throws IOException if generated files cannot be written to the file system.
   * @throws RuntimeException if an error during generation occurs.
   */
  public void generate(
        File sourceDirectory,
        File targetDirectory,
        File extensionsTargetDirectory,
        String rootPackage,
        ClassLoader classLoader)
      throws IOException
  {
    HtmlParser parser = new HtmlParser();
    Map<String, Component> componentMap = parser.readComponents(sourceDirectory, classLoader);
    for (Map.Entry<String, Component> entry : componentMap.entrySet())
    {
      Component component = entry.getValue();
      component.resolveComponentReferences(componentMap);

      String key = entry.getKey();
      String subpackage = getComponentSubpackage(key);

      Map<JavaClassName, String> componentFilesToWrite = new HashMap<>();
      Map<JavaClassName, String> extensionFilesToWrite = new HashMap<>();
      if (component.getGenerationParameters() != null
          && component.getGenerationParameters().fromComponentLib)
      {
        continue;
      }
      GenerationContext generationContext = new GenerationContext(component, rootPackage, subpackage);
      generateComponentBaseClass(generationContext, componentFilesToWrite);
      generateComponentExtensionClass(generationContext, extensionFilesToWrite);
      for (Map.Entry<JavaClassName, String> fileToWriteEntry : componentFilesToWrite.entrySet())
      {
        File targetFile = new File(
            targetDirectory,
            fileToWriteEntry.getKey().getSourceFile());
        writeFile(targetFile, fileToWriteEntry.getValue(), true);
      }
      for (Map.Entry<JavaClassName, String> fileToWriteEntry : extensionFilesToWrite.entrySet())
      {
        File targetFile = new File(
            extensionsTargetDirectory,
            fileToWriteEntry.getKey().getSourceFile());
        writeFile(targetFile, fileToWriteEntry.getValue(), false);
      }
    }
    generateComponentRegistry(componentMap, targetDirectory, rootPackage);
  }

  /**
   * Returns a component's subpackage for a component key.
   *
   * @param componentKey the component key, containing slashes ('/') as part separator.
   *
   * @return the package prefix, which is the key with slashes replaced by dots.
   */
  public static String getComponentSubpackage(String componentKey)
  {
    return componentKey.replace(SLASH, DOT);
  }

  /**
   * Writes a file to the file system using ISO-8859-1 encoding.
   * Parent directories are created if they do not exist.
   *
   * @param targetFile the file to write to, not null.
   * @param content the content of the file, not null.
   * @param overwrite whether existing files should be overwritten.
   *
   * @throws IOException if writing to the file system fails.
   */
  public void writeFile(File targetFile, String content, boolean overwrite) throws IOException
  {
    File targetDir = targetFile.getParentFile();
    if (!targetDir.exists())
    {
      if (!targetDir.mkdirs())
      {
        throw new IOException("Could not create directory " + targetDir.getAbsolutePath());
      }
    }
    if (overwrite || !targetFile.exists())
    {
      FileUtils.writeStringToFile(targetFile, content, "ISO-8859-1");
    }
  }

  /**
   * Generates the base class for a component and its children,
   * and adds the generated content to the <code>filesToWrite</code> map.
   *
   * @param GenerationContext the generation context for the generated class, not null
   * @param filesToWrite a map where the generated files are stored: the key is the class name,
   *        and the value is the content of the file.
   */
  public void generateComponentBaseClass(
      GenerationContext generationContext,
      Map<JavaClassName, String> filesToWrite)
  {
    ComponentGenerator componentGenerator = getGenerator(generationContext.component.getClass());
    boolean writeClassFile = componentGenerator.generateExtensionClass(generationContext.component);
    GeneratedClass generatedClass = new GeneratedClass();
    if (!writeClassFile)
    {
      generationContext.generatedClass.innerClasses.add(generatedClass);
    }
    generationContext.generatedClass = generatedClass;

    componentGenerator.generate(generationContext);
    for (Component child : generationContext.component.getChildren())
    {
      // allow for setting generatedClass null in generatonContext
      generationContext.generatedClass = generatedClass;
      generateComponentBaseClass(new GenerationContext(generationContext, child), filesToWrite);
    }

    if (generationContext.generatedClass != null && writeClassFile)
    {
      removeUnusedImports.removeUnusedImports(generationContext.generatedClass);
      String result = generationContext.generatedClass.toString();
      filesToWrite.put(
          componentGenerator.getClassName(generationContext),
          result);
    }
  }

  /**
   * Generates the extension class for a component and its children,
   * and adds the generated content to the <code>filesToWrite</code> map.
   *
   * @param GenerationContext the generation context for the generated class, not null
   * @param filesToWrite a map where the generated files are stored: the key is the class name,
   *        and the value is the content of the file.
   */
  public void generateComponentExtensionClass(
      GenerationContext generationContext,
      Map<JavaClassName, String> filesToWrite)
  {
    ComponentGenerator componentGenerator = getGenerator(generationContext.component.getClass());
    generationContext.generatedClass = new GeneratedClass();

    if (componentGenerator.generateExtensionClass(generationContext.component))
    {
      componentGenerator.generateExtension(generationContext);
      String result = generationContext.generatedClass.toString();
      filesToWrite.put(
          componentGenerator.getExtensionClassName(generationContext),
          result);
    }
    for (Component child : generationContext.component.getChildren())
    {
      generateComponentExtensionClass(new GenerationContext(generationContext, child), filesToWrite);
    }
  }

  /**
   * Generates the component registry class and writes it to the file system.
   *
   * @param componentMap a map where all referenceable components are stored:
   *        the key is the component id, and the value is the Component itself.
   * @param targetDirectory the root directory (excluding package structure) to which the source file
   *        should be written.
   * @param rootPackage the package for the component registry class, not null.
   */
  public void generateComponentRegistry(
      Map<String, Component> componentMap,
      File targetDirectory,
      String rootPackage) throws IOException
  {
    JavaClassName javaClassName = new JavaClassName("ComponentRegistryImpl", rootPackage);
    String className = javaClassName.getSimpleName();
    StringBuilder content = new StringBuilder();
    content.append("package ").append(rootPackage).append(";\n\n");
    content.append("import ").append(ComponentRegistry.class.getName()).append(";\n");
    for (Map.Entry<String, Component> entry : componentMap.entrySet())
    {
      String key = entry.getKey();
      Component component = entry.getValue();
      if (component.getGenerationParameters() != null
          && component.getGenerationParameters().fromComponentLib)
      {
        continue;
      }
      ComponentGenerator componentGenerator = getGenerator(component.getClass());
      String subpackage = getComponentSubpackage(key);
      GenerationContext generationContext = new GenerationContext(component, rootPackage, subpackage);
      JavaClassName componentClassName = componentGenerator.getReferencableClassName(generationContext);
      content.append("import ").append(componentClassName.getName()).append(";\n");
    }
    content.append("\n");
    content.append("public class ").append(className)
        .append(" extends ").append(ComponentRegistry.class.getSimpleName())
        .append("\n");
    content.append("{\n");
    content.append("  public ").append(className).append("()\n");
    content.append("  {\n");
    for (Map.Entry<String, Component> entry : componentMap.entrySet())
    {
      Component component = entry.getValue();
      if (component.getGenerationParameters() != null
          && component.getGenerationParameters().fromComponentLib)
      {
        continue;
      }
      ComponentGenerator componentGenerator = getGenerator(component.getClass());
      GenerationContext generationContext = new GenerationContext(component, rootPackage, component.getId());
      JavaClassName componentClassName = componentGenerator.getReferencableClassName(generationContext);
      content.append("    components.put(\"").append(componentClassName.getSimpleName())
          .append("\", new ").append(componentClassName.getSimpleName()).append("(null, null));\n");
    }
    content.append("  }\n");
    content.append("}\n");
    File targetFile = new File(targetDirectory, javaClassName.getSourceFile());
    writeFile(targetFile, content.toString(), true);
  }

  /**
   * Returns a ComponentGenerator for a component.
   *
   * @param component the component, not null.
   *
   * @return the component generator, or null if no component generator is registersed for the component.
   */
  public static ComponentGenerator getGenerator(Component component)
  {
    Assertions.assertNotNull(component, "component");
    return getGenerator(component.getClass());
  }

  /**
   * Returns the ComponentGenerator for the passed component class.
   *
   * @param componentClass the component class to get a generator for, not null.
   *
   * @return the ComponentGenerator for the generator class, not null.
   *
   * @throws IllegalArgumentException if the componentClass does not have a ComponentGenerator.
   */
  public static ComponentGenerator getGenerator(Class<? extends Component> componentClass)
  {
    ComponentGenerator result = componentGeneratorMap.get(componentClass);
    if (result == null)
    {
      throw new IllegalArgumentException("Unknown component class " + componentClass.getName());
    }
    return result;
  }

  /**
   * Returns the configuration of the generator.
   * If the configuration is not yet loaded, it will be loaded, using the current value of
   * configurationClasspathResource.
   *
   * @return the generator configuration, not null.
   *
   * @throws RuntimeException if the configuration could not be loaded.
   */
  public static GeneratorConfiguration getGeneratorConfiguration()
  {
    if (generatorConfiguration == null)
    {
      try
      {
        generatorConfiguration = new GeneratorConfiguration(configurationClasspathResource);
      }
      catch (Exception e)
      {
        throw new RuntimeException("Could not load generator configuration from classpath resource "
            + configurationClasspathResource,
          e);
      }
    }
    return generatorConfiguration;
  }

  /**
   * Runs the generation.
   * If <code>argv</code> does not contain 5 elements, an usage message is written and nothing is done.
   *
   * @param argv The arguments. Must contains 5 elements, these are
   *        configurationClasspathResource, componentDir, targetDirectory, extensionsTargetDirectory,
   *        baseComponentPackage
   *
   * @throws IOException if generated files cannot be written to the file system.
   * @throws RuntimeException if an error during generation occurs.
   */
  public static void main(String[] argv) throws IOException
  {
    if (argv.length != 5)
    {
      System.out.println("Generation failed, Arguments cannot be parsed. "
          + "Should be configurationClasspathResource, componentDir, targetDirectory, "
          + "extensionsTargetDirectory, baseComponentPackage");
      return;
    }
    String configurationClasspathResource = argv[0];
    String componentDir = argv[1];
    String targetDirectory = argv[2];
    String extensionsTargetDirectory = argv[3];
    String baseComponentPackage = argv[4];
    System.out.println("micgwaf generator running with:\n"
        + "configurationClasspathResource: " + configurationClasspathResource + "\n"
        + "componentDir                  : " + componentDir + "\n"
        + "targetDirectory               : " + targetDirectory + "\n"
        + "extensionsTargetDirectory     : " + extensionsTargetDirectory + "\n"
        + "baseComponentPackage          : " + baseComponentPackage);
    Generator.configurationClasspathResource = configurationClasspathResource;
    new Generator().generate(
        new File(componentDir),
        new File(targetDirectory),
        new File(extensionsTargetDirectory),
        baseComponentPackage);
    System.out.println("Generation successful");
    return;
  }
}
