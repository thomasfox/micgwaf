package de.seerheinlab.micgwaf.parser.contenthandler;

import java.util.Arrays;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.seerheinlab.micgwaf.component.Component;
import de.seerheinlab.micgwaf.component.PartListComponent;
import de.seerheinlab.micgwaf.util.Constants;

public class PartListContentHandler extends ContentHandler
{
  private static final char ESCAPE_CHAR = '\\';

  private static final String VARIABLE_START = "${";

  private static final char VARIABLE_END = '}';

  private static final char VARIABLE_DEFAULT_VALUE_SEPARATOR = ':';

  public StringBuilder currentStringPart = new StringBuilder();

  private final PartListComponent component = new PartListComponent(null);

  @Override
  public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
      throws SAXException
  {
    currentStringPart.append("<").append(localName);
    if (attributes != null)
    {
      for (int i = 0; i < attributes.getLength(); i++)
      {
        String attributeName = attributes.getLocalName(i); // Attr name
        if ("".equals(attributeName))
        {
          attributeName = attributes.getQName(i);
        }
        String attributeValue = attributes.getValue(i);
        if (attributeName.startsWith("xmlns:") && Constants.XML_NAMESPACE.equals(attributeValue))
        {
          // do not output definition of our own namespace
          continue;
        }
        currentStringPart.append(" ")
            .append(attributeName)
            .append("=\"");
        extractVariablesAndSnippets(attributeValue);
        currentStringPart.append("\"");
      }
    }
    currentStringPart.append(">");
  }

  @Override
  public void endElement(String uri, String localName, String qName)
      throws SAXException
  {
    currentStringPart.append("</").append(localName).append(">");
  }

  @Override
  public void characters(char[] ch, int start, int length)
      throws SAXException
  {
    String characterString = new String(Arrays.copyOfRange(ch, start, start + length));
    extractVariablesAndSnippets(characterString);
  }

  /**
   * Extracts variables and snippets from a String.
   *
   * @param toParse the String to parse, not null.
   *
   * @throws SAXException if an imcomplete variable definition is found.
   */
  protected void extractVariablesAndSnippets(String toParse)
      throws SAXException
  {
    while (true)
    {
      int indexOfStart = toParse.indexOf(VARIABLE_START);
      if ((indexOfStart > 0 && toParse.charAt(indexOfStart - 1) == ESCAPE_CHAR))
      {
        currentStringPart.append(toParse.substring(0, indexOfStart + 2));
        toParse = toParse.substring(indexOfStart + 2);
        continue;
      }
      if (indexOfStart == -1)
      {
        currentStringPart.append(toParse);
        break;
      }
      int indexOfEnd = toParse.indexOf(VARIABLE_END, indexOfStart);
      if (indexOfEnd == -1)
      {
        throw new SAXException("unbalanced " + VARIABLE_START + " in text");
      }
      if (indexOfEnd == indexOfStart + 2)
      {
        throw new SAXException("empty variable (" + VARIABLE_START + VARIABLE_END + ") in text");
      }
      currentStringPart.append(toParse.substring(0, indexOfStart));
      if (currentStringPart.length() > 0)
      {
        component.parts.add(PartListComponent.ComponentPart.fromHtmlSnippet(currentStringPart.toString()));
        currentStringPart = new StringBuilder();
      }
      String variableContent
          = toParse.substring(indexOfStart + VARIABLE_START.length(), indexOfEnd);
      int indexOfColon = variableContent.indexOf(VARIABLE_DEFAULT_VALUE_SEPARATOR);
      if (indexOfColon == -1)
      {
        component.parts.add(PartListComponent.ComponentPart.fromVariable(
            variableContent, VARIABLE_START + variableContent + VARIABLE_END));
      }
      else
      {
        component.parts.add(PartListComponent.ComponentPart.fromVariable(
            variableContent.substring(0, indexOfColon), variableContent.substring(indexOfColon + 1)));
      }
      toParse = toParse.substring(indexOfEnd + 1);
    }
  }


  @Override
  public void ignorableWhitespace(char[] ch, int start, int length)
      throws SAXException
  {
    for (int i = start; i < start + length; ++i)
    {
      currentStringPart.append(ch[i]);
    }
  }

  @Override
  public void child(Component child)
  {
    component.parts.add(PartListComponent.ComponentPart.fromHtmlSnippet(currentStringPart.toString()));
    currentStringPart = new StringBuilder();
    component.parts.add(PartListComponent.ComponentPart.fromComponent(child));
    child.setParent(component);
  }

  @Override
  public PartListComponent finished() throws SAXException
  {
    component.parts.add(PartListComponent.ComponentPart.fromHtmlSnippet(currentStringPart.toString()));
    return component;
  }
}