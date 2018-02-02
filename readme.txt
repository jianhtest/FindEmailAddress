This a java application to find email addresses in webpages. Based on the domain of input URL, all the urls
with the same domain if (direct or indirect) linked with the the first page, will be searched
for the email address.

To run the application:

go to the folder /out/production/FindEmailAddress
type java FindEmailAddress [website]
for example:
       java FindEmailAddress web.mit.edu
       

To make a build:
(1) if you have IntelliJ
     you can just open the project with IntelliJ.
     And make a build.
     
(2) if you install the java SDK on your computer
	 go to /src
	 type javac FindEmailAddress.java
	 

The application will print out the urls it visited.
And it will print out the email address it found at the end
 

