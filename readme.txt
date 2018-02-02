This a java application to email addresses based on the domain of input URL. All the urls
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
	 

The application will print out the urls it cannot reach.
To see the urls the application visited, please go to the function printout() and uncomment
the last five line code
 
This is a application need to be improved by multi-thread. Due to the time limit we did not
Implement that way.
