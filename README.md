java-xml-server
===============
A simple webserver for xml files where xsl files can be added using url parameter. As an add-on you can add a special xml tag which will be replaced by the result of an sql query. This server is a simple hack for a special use case. It is not intended to be used on a public machine.

Example URLs:
-------------
- Without the need for sql
  
  `http://localhost:8000/list/src/test/cd-catalogue.xml?xsl=titles.xsl`
  
  `http://localhost:8000/list/src/test/cd-catalogue.xml?xsl=artists.xsl`

- For sql you can add more sql parameters: 

  db_driver

  db_url

  db_user

  db_pass

  `http://localhost:8000/list/src/test/cd-catalogue.xml?xsl=artists.xsl&db_driver=org.hsqldb.jdbcDriver&db_user=sa&db_url=jdbc:hsqldb:mem:.`
