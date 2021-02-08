A client wanted to create a server-product that could be sold to large customers. A certain level of customisation was required so an ordinary server setup would not fit the bill.
The overriding classloader made it possible to have a server-core that will load two sets of jars: 'product' and 'project', where 'product' contains the standard, thoroughly tested, broadly used functionality, and 'project' contains the customisation for the specific client. Each would have its own release cycle.

It does break the Java specification - sorta. A central part of the spec is that lower level classloaders may not override classes in higher levels. Use it only if you know exactly what you're doing.
