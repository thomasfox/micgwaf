package de.seerheinlab.test.micgwaf.component.messageBox;

import de.seerheinlab.micgwaf.component.Component;

/**
 * This class represents the HTML element with m:id errorMessage.
 * Instances of this class are used whenever these elements are rendered
 * or when form date from a page containing these elements is processed.
 **/
public class ErrorMessage extends BaseErrorMessage
{
  /** Serial Version UID. */
  private static final long serialVersionUID = 1L;


  /**
  * Constructor. 
  *
  * @param parent the parent component, or null if this is a standalone component (e.g. a page)
  */
  public ErrorMessage(Component parent)
  {
    super(parent);
  }
  
  /**
  * Constructor. 
  *
  * @param parent the parent component, or null if this is a standalone component (e.g. a page)
  * @param text the text of the message, not null.
  */
  public ErrorMessage(Component parent, String text)
  {
    super(parent);
    this.setTextContent(text);
  }
}
