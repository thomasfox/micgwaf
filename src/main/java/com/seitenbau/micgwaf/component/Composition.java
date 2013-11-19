package com.seitenbau.micgwaf.component;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Component for compositions. Only used for parsing.
 */
public class Composition extends Component
{
  /** serialVersionUID. */
  private static final long serialVersionUID = 1L;
  
  public Map<String, Component> definitions = new LinkedHashMap<>();
  
  public String templateId;
  
  public SnippetListComponent template;

  public Composition(Component parent)
  {
    super(null, parent);
  }

  @Override
  public void resolveComponentReferences(Map<String, ? extends Component> allComponents)
  {
    super.resolveComponentReferences(allComponents);
    Component templateCandidate = allComponents.get(templateId);
    if (templateCandidate == null)
    {
      throw new IllegalStateException("unknown template component with id " + templateId);
    }
    if (!(templateCandidate instanceof SnippetListComponent))
    {
      throw new IllegalStateException("template must be of class SnippetListComponent but is of class" 
          + templateCandidate.getClass().getName());
    }
    template = ((SnippetListComponent) templateCandidate).copy();
    for (SnippetListComponent.ComponentPart part : template.parts)
    {
      if (part.component instanceof AnyComponent)
      {
        String name = ((AnyComponent) part.component).name;
        part.component = definitions.get(name);
        if (part.component == null)
        {
          throw new RuntimeException("No component definition exists for insert with name " + name);
        }
        if (part.component instanceof DefineComponent)
        {
          Component referencedComponent = ((DefineComponent) part.component).referencedComponent;
          if (referencedComponent == null)
          {
            throw new IllegalStateException("No component bound to component definition, name=" 
                + ((DefineComponent) part.component).name);
          }
          part.component = referencedComponent;
        }
      }
    }
  }
  
  @Override
  public List<? extends Component> getChildren()
  {
    List<Component> children = new ArrayList<>();
    children.addAll(definitions.values());
    return children;
  }

  @Override
  public void render(Writer writer) throws IOException
  {
    template.render(writer);
  }

  @Override
  public void afterRender()
  {
    template.afterRender();
  }
}