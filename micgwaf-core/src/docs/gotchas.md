Gotchas
=======

- Returning null in Component.processRequest() is the default. 
  Returnung null redisplays the submitted page unchanged except the changes made explicitly in processRequest,
  without creating a new instance of the page
  
- HTML code is per default rendered as in the source pages, with the following exceptions
  - form components always have POST as method.
  - input components use the id of the component as value of the name attribute if no name is explicitly set.
  - input components in loops add :${loopCounter} to the name attribute.
  - input components which are not buttons use the submitted value as new value of the "value" attribute
    if they are re-rendered.
  - the content inside micgwaf:componentRef and micgwaf:remove elements is removed.