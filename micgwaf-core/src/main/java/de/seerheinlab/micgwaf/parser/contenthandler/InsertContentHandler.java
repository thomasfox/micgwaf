package de.seerheinlab.micgwaf.parser.contenthandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.seerheinlab.micgwaf.component.Component;
import de.seerheinlab.micgwaf.component.parse.InsertComponent;
import de.seerheinlab.micgwaf.util.Constants;

public class InsertContentHandler extends ContentHandler
{
  public static final String NAME_ATTR = "name";

  public static final String INSERT_ELEMENT_NAME = "insert";

  public String name;

  @Override
  public void startElement(
        String uri,
        String localName,
        String qName,
        Attributes attributes)
      throws SAXException
  {
    // check for componentRef elements
    if (!Constants.XML_NAMESPACE.equals(uri) || !InsertContentHandler.INSERT_ELEMENT_NAME.equals(localName))
    {
      throw new SAXException("unknown Element " + uri + ":" + localName);
    }
    name = attributes.getValue(InsertContentHandler.NAME_ATTR);
    if (name == null || "".equals(name.trim()))
    {
      throw new SAXException("Attribute " + InsertContentHandler.NAME_ATTR
          + " is required on element " + qName);
    }
  }


  @Override
  public void child(Component child) throws SAXException
  {
    // ignore all content in componentRef element;
    // TODO write log message
  }

  @Override
  public InsertComponent finished() throws SAXException
  {
    return new InsertComponent(name, null);
  }
}