# Introduction #

Many programming projects rely on third party libraries or modules. When embedding those open source libraries into your product, a proper release documentation must be provided in which you document that the particular requirements of the licences are met.

In a larger team you can easily loose track of 3rd party libraries that are used in your project and it is hard keep the release documentation up to date. Hence, we developed a little tool which does this work for us:

License Collector is an ANT task that generates a html file in which all integrated 3rd party libraries are listed with their license specification and acknowlegdements.

The only thing the developer needs to think of is creating an information sheet when integrating/commiting libraries. Then this small tool will render a nice Third Party License Term List, and your legal counsel will be happy.

See the [Documentation](Documentation.md) for more information and usage instructions.