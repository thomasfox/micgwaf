micgwaf demo
============

This project is a demo application to demonstrate how to implement a web application using micgwaf.
It represents a book store, where an administrator can add, edit and remove books.

Building
--------

The application is built using maven. 
Call e.g. "mvn package" to create a war bundle, or "mvn eclipse:eclipse" to create an eclipse 
configuration for the project.
Within eclipse, run or debug the class de.seerheinlab.test.micgwaf.Start to start the web application
(or deploy the packaged war to a servlet container of your choice).

Project Layout
--------------

The source HTML files from which the micgwaf component structure is built is in the /src/main/html folder.
The micgwaf classes are built by the maven micgwaf plugin in the target/generate-sources and
src/main/generated-java folder. 
The src/main/html folder contains the files which are meant to be edited 
(forms ant those marked with m:generateExtensionClass="true"). 
It contains only a part of the generated components.
There, the programmer can handle events such as "submit button pressed" or mapping input values to the model.
The target/generated-sources folder contains all the components which are generated by micgwaf.
These are not meant to be edited and will be overwritten without warning each time the generation is run.
For keeping the generated components up to date,
running "mvn generate-sources" or an equivalent command is necessary after changing the source HTML files.

Best practices
--------------

- It is typically a good idea to concentrate the code for a page in a well defined place.
  (micgwaf typically offers several places to hook in, e.g. for a button the button component itself and
  the surrounding form, so a standard place to hook in assures the code is easily understood).
  In most cases, a good place for this ares the form component if the page contains a form 
  (the form is where most actions happen).
  If no form is contained, another good place is the Page component itself.

Blueprints
----------

A page containing a list of items (books) and actions on these items
(open edit page, edit inline within table, edit inline using ajax) as well as some
page-wide actions is implemented in the de.seerheinlab.test.micgwaf.component.bookListPage package.
Within the src/main/generated-java package, it contains the page component (BookListPage),
the form page component (BookListForm) and a component representing a row in the table (BookRow).

The main component containing the implementation details is the form class, the other classes are quite small:

In the BookListPage page, the page content is initialized after construction, 
calling the display method of the form, and that's it.
The BookRow page knows how to display business objects (Books) within the display(Book) method.
To do this, the HTML span elements containing the displayed text have been assigned a m:id attribute,
so their content can be easily changed in the BookRow by calling the setTextContent method of the
components generated for them.
The BookListForm page 

Architecture
------------

Micgwaf applications follow the object oriented principle.
This means, the main actors in a micgwaf application are page components 
which can display themselves and handle any input directed to them.
Typically, they use business services to 

### Why not MVC ?

The Model-View-Controller (MVC) pattern is NOT inherently used in micgwaf. 
A micgwaf page component holds the controller (it processes the GUI user's actions)
and the view (it renders the output HTML) and the model (it stores and holds the user input).
Of course, one can delegate the different aspects to external model, view and controller
classes, if needed, but this is in no way required by micgwaf.

The reason for not using the MVC pattern is that the objectives of the MVC pattern are often not met.
Usually, there is only one presentation technique (namely HTML) employed in web applications,
so that the objective of re-using controllers and models for different view technologies is not used.
Also, even if different presentation techniques are used, view specific details often
make re-use of the controller difficult, if not impossible.

The advantage of having the three MVC aspects in one class is that the whole GUI behavior is controlled
at one place. This makes interaction between the parts easy to program and to understand.
In fact, this is the object-oriented way: aspects that are tightly bound togethether should be implemented
in one class.


