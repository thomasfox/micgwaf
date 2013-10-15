package com.seitenbau.sicgwaf.parser.contenthandler;

import org.xml.sax.Attributes;

public class SnippetListContentHandlerFactory implements ContentHandlerFactory
{

  @Override
  public ContentHandler create(String uri, String localName,
      String qName, Attributes attributes)
  {
    return new SnippetListContentHandler();
  }

}
