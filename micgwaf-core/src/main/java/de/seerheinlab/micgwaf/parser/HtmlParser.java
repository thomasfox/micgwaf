package de.seerheinlab.micgwaf.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.seerheinlab.micgwaf.component.Component;
import de.seerheinlab.micgwaf.component.GenerationParameters;

/**
 * Parses HTML files and generates components from them.
 * The parsed components can render the HTML source file again.
 */
public class HtmlParser
{
  private static final String COMPONENTS_PROPERTIES_RESOURCE = "META-INF/micgwaf-components.properties";

  private static final String SAX_NAMESPACE_FEATURE_NAME = "http://xml.org/sax/features/namespaces";
  
  /**
   * Parses the HTML files in a directory and generates components from them.
   * Only files directly in the given directory with the suffix .xhtml are parsed,
   * all other files are ignored.
   * The created components are stored in the returned map.
   * The map key is the component id, and the map value is the root component for a HTML source file.
   * 
   * @param sourceDirectory the directory where the parsed files reside, not null.
   * 
   * @return the map with the parsed components, one entry for each parsed file.
   */
  public Map<String, Component> readComponents(File sourceDirectory)
  {
    return readComponents(sourceDirectory, null);
  }

  /**
   * Parses the HTML files in a directory and generates components from them.
   * Only files directly in the given directory with the suffix .xhtml are parsed,
   * all other files are ignored.
   * The created components are stored in the returned map.
   * The map key is the component id, and the map value is the root component for a HTML source file.
   * 
   * @param sourceDirectory the directory where the parsed files reside, not null.
   * @param classLoader the class loader to use for component lib discovery, 
   *       or null to use this classes classloader.
   * 
   * @return the map with the parsed components, one entry for each parsed file.
   */
  public Map<String, Component> readComponents(File sourceDirectory, ClassLoader classLoader)
  {
    if (sourceDirectory == null)
    {
      throw new NullPointerException("sourceDirectory must not be null");
    }
    if (!sourceDirectory.isDirectory())
    {
      throw new IllegalArgumentException(
          sourceDirectory.getAbsolutePath() + " is no Directory");
    }
    
    Map<String, Component> result = discoverComponentsFromClasspath(classLoader);
    File[] files = sourceDirectory.listFiles();
    for (File file : files)
    {
      String fileName = file.getName();
      if (fileName.endsWith(".xhtml"))
      {
        try
        {
          FileInputStream inputStream = new FileInputStream(file);
          Component component = parse(inputStream);
          if (component.getId() == null)
          {
            component.setId(fileName.substring(0, fileName.length() - 6));
          }
          result.put(component.getId(), component);
        }
        catch (SAXException | IOException | ParserConfigurationException e)
        {
          throw new RuntimeException(e);
        } 
      }
    }
    return result;
  }  
  
  /**
   * Parses a HTML file and returns the root component.
   * 
   * @param inputStream the input stream with the content of the file, not null.
   * 
   * @return the root component, not null.
   * 
   * @throws SAXException if XML parsing fails.
   * @throws IOException if reading from the stream fails.
   * @throws ParserConfigurationException if the SAX parser cannot be configured.
   */
  public Component parse(InputStream inputStream) 
      throws SAXException, IOException, ParserConfigurationException
  {
    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    SAXParser saxParser = saxParserFactory.newSAXParser(); 
    XMLReader xmlReader = saxParser.getXMLReader(); 
    DelegatingContentHandler handler = new DelegatingContentHandler();
    xmlReader.setContentHandler(handler);
    xmlReader.setEntityResolver(handler);
    xmlReader.setDTDHandler(handler);
    xmlReader.setErrorHandler(handler);
    xmlReader.setFeature(SAX_NAMESPACE_FEATURE_NAME, true);
    xmlReader.parse(new InputSource(inputStream)); 
    return handler.currentResult;
  }
  
  /**
   * Reads all component definitions from the classpath and returns component instances.
   * 
   * @param classLoader the class loader to use, or null to use this classes classloader.
   * 
   * @return the map with the component instances, keyed by their name.
   */
  public Map<String, Component> discoverComponentsFromClasspath(ClassLoader classLoader)
  {
    Map<String, Component> result = new HashMap<>();
    if (classLoader == null)
    {
      classLoader = getClass().getClassLoader();
    }
    Enumeration<URL> urls;
    try
    {
      urls = classLoader.getResources(COMPONENTS_PROPERTIES_RESOURCE);
      while (urls.hasMoreElements())
      {
        URL url = urls.nextElement();
        Properties properties = new Properties();
        try (InputStream inStream = url.openStream())
        {
          properties.load(inStream);
        }
        for (Map.Entry<Object, Object> entry: properties.entrySet())
        {
          String key = entry.getKey().toString();
          String value = entry.getValue().toString();
          Class<?> clazz = Class.forName(value);
          if (!Component.class.isAssignableFrom(clazz))
          {
            throw new RuntimeException(
                "Class " + value + " defined in " + url + " is not a Component");
          }
          @SuppressWarnings("unchecked")
          Class<? extends Component> componentClass = (Class<? extends Component>) clazz;
          Component instance = Component.getInstance(componentClass);
          if (instance.getGenerationParameters() == null)
          {
            instance.setGenerationParameters(new GenerationParameters());
          }
          instance.getGenerationParameters().fromComponentLib = true;
          result.put(key, instance);
        }
        
      }
    } 
    catch (IOException | ClassNotFoundException e)
    {
      throw new RuntimeException(e);
    }
    return result;
  }
}
