Gotchas
=======

- Returning null in Component.processRequest() is the default. 
  Returning null redisplays the submitted page unchanged except the changes made explicitly in processRequest,
  without creating a new instance of the page
  
- HTML code is per default rendered as in the source pages, with the following exceptions
  - form components always have POST as method.
  - input components use the id of the component as value of the name attribute if no name is explicitly set.
  - input components in loops add :${loopCounter} to the name attribute.
  - input components which are not buttons use the submitted value as new value of the "value" attribute
    if they are re-rendered.
  - the content inside m:reference and m:remove elements is removed.
  
- Changing the id of active components (e.g. Buttons) can result in renaming methods in the base classes.
  However, as the extension classes are generated only once, method names do not change automatically there.
  They need to be changed manually to match the new generated classes.
  Changing ids also results in unused classes both in the src/main/generated-java 
  and target/generated-sources directory, which need to be removed.
  So, ids of active components should be chosen with care.