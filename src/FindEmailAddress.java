/*
*
* The project is for finding email address in the web site
*
* Create a command line program that will take an internet domain name (i.e. “jana.com”) and
* print out a list of the email addresses that were found on that website only.
* the application will find email addresses on any discoverable page of the website.
*
* We implemented a multithread method. each url request and email&href check is a threadTask
*
*
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.net.URI;
import java.util.concurrent.*;

/*
*  This is the main class to get the email addresses
*
*/
public class FindEmailAddress {

    private String rootURL;
    private String requestDomain;
    private Set<String> urlHistory;
    private Set<String> emails;
    private static int maxUrlCheck = 2000; //to avoid too many pages to check we add maximum number of url to check, it can be modified
    private ExecutorService executor;
    private Integer tasksNumber;

    private Object lockObj = new Object();

    //private final Semaphore semaphore = new Semaphore(maxUrlCheck, true);

    public static void main(String[] args)
    {
        String requestUrl = null;

        if(args.length >0)
        {
            requestUrl =  args[0];
        }
        if (requestUrl == null) {
            System.out.println("please input proper URL and try again (example: java FindEmailAddress web.mit.edu");
            return;
        }
        FindEmailAddress findEmailAddress;
        try {
            findEmailAddress = new FindEmailAddress(requestUrl);
        }catch (java.net.URISyntaxException e)
        {
            System.out.println("Bad URL, could not get any result!");
            return;
        }
        findEmailAddress.startWebCrawl();
        findEmailAddress.printout();
    }

    /*
    *  @param requestURL is the root url. All the webpage with same domain will be searched for email address
    */
    public FindEmailAddress(String requestURL) throws java.net.URISyntaxException
    {
        this.rootURL = requestURL;
        if(!requestURL.toLowerCase().startsWith("http"))
        {
            rootURL = "http://" + requestURL;
        }

        URI uri = new URI(rootURL);
        this.requestDomain= uri.getHost();

        this.emails = new HashSet<String>();
        this.urlHistory = new HashSet<String>();
        this.urlHistory.add(rootURL);
        tasksNumber = 0;
    }

    /*
     *  This is the function to do web crawling
     *  it will create a thread pool
     *  if all the tasks are done, it will stop.
     */

    public void startWebCrawl()
    {
        if(rootURL == null || rootURL.trim().length()<=0)
            return;

        executor = Executors.newFixedThreadPool(10);
        tasksNumber ++;
        executor.execute(new Handler(rootURL));


        synchronized (lockObj) {
            try {
                lockObj.wait();
            } catch (InterruptedException e) {
                System.out.println("Lock obj thread was interrupted while waiting for the result. Exception:");
                e.printStackTrace();
            }
        }
        executor.shutdown();
    }

//    public void startWebCrawl()
//    {
//        if(rootURL == null || rootURL.trim().length()<=0)
//            return;
//
//
//        executor = Executors.newFixedThreadPool(10);
//        tasksNumber ++;
//        executor.execute(new Handler(rootURL));
//
//        do{
//            try {
//                Thread.sleep(3000);
//            }catch(Exception e)
//            {
//                e.printStackTrace();
//            }
//        }while(this.tasksNumber >0);
//
//        executor.shutdown();
//    }


    /*
* this inline class is for http request and email check running task
*/
    class Handler implements Runnable {
        String requestURL;
        Handler(String requestURL) {
            this.requestURL = requestURL;
        }
        public void run() {
            System.out.println("start search url :" + requestURL);
            makeHttpRequest(requestURL);
        }
    }

    //need multithread
    /*
    * This is the function to make http call.
    * And this function will also do the email search and same domain url search.
    * the email address will be saved in a hashtable
    * the urls will be saved in a vector, and a history tashtable is for filtering the urls already visited.
    */
    private void makeHttpRequest(String httpURLString)
    {
        if(httpURLString == null || httpURLString.trim().length()<=0) {
            synchronized (tasksNumber) {
                tasksNumber --;
            }
            return;
        }
        BufferedReader reader = null;
        try {
            URL urlObj = new URL(httpURLString);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0"); //some website if did not see the user agent will take it robot and cause http call failing

            reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = reader.readLine()) != null) {
                searchEmailAndHRef(inputLine);
            }

        }catch(Exception ex)
        {
            System.out.println("Cannot reach url:" + httpURLString);
        }finally{
            if(reader != null)
            {
                try {
                    reader.close();
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            synchronized (tasksNumber) {
                tasksNumber--;
            }

            if(tasksNumber == 0)
            {
                synchronized (lockObj)
                {
                    lockObj.notify();
                }
            }
        }

        return;

    }


    /*
    *  Search Email and Href
    *  if it is email we do not need to check it is same domain url anymore.
     */
    private void searchEmailAndHRef(String responseStr)
    {
        if(responseStr == null || responseStr.trim().length()<=0)
            return;
        String lowerString = responseStr.toLowerCase();
        int startIndex = lowerString.indexOf("href=\"");
        while(startIndex >=0)
        {
            startIndex += 6;
            int endIndex = lowerString.indexOf("\"",startIndex);
            String hrefString = lowerString.substring(startIndex,endIndex);
            if(!checkIfEmail(hrefString))
            {
                checkIfSameDomainHtmlLink(hrefString);
            }
            startIndex = lowerString.indexOf("href=\"",startIndex);
        }
    }


    /*
     * check if this is validate email address
     */
    private boolean checkIfEmail(String inputString)
    {
        boolean isEmail = false;
        if(inputString == null || inputString.trim().length()<=0)
            return isEmail;

        String testString = inputString.trim();

        if(testString.startsWith("mailto:"))
        {
            String emailAddress = testString.substring(7);
            //some mailto has parameters, we need to remove those characotrs.
            int indexOfAt = emailAddress.indexOf("@");
            if(indexOfAt >0)
            {
                int endIndex = emailAddress.indexOf("?",indexOfAt);
                if(endIndex >0)
                {
                    emailAddress = emailAddress.substring(0,endIndex);

                }else
                {
                    endIndex = emailAddress.indexOf(" ",indexOfAt);
                    if(endIndex >0)
                    {
                        emailAddress = emailAddress.substring(0,endIndex);
                    }
                }
                emails.add(emailAddress);
                isEmail = true;
            }
        }
        return isEmail;
    }


    /*
    * sometime the url will be looks like http:////....
    * or //.....
    * we need normalize them, otherwise URL will not be able to recognize them.
     */
    private String removeExtraSlash(String inputString)
    {
        if(inputString == null)
            return null;

        int point = 0;
        int length = inputString.length();
        while(point < length && inputString.charAt(point) == '/' )
        {
            point ++;
        }
        if(point<length)
        {
            return inputString.substring(point);
        }else{
            return "";
        }

    }


    /*
    *  check if this is a valid url we need to scrawl
    */
    private boolean checkIfSameDomainHtmlLink(String inputString)
    {
        boolean isSameDomainUrl = false;
        if(inputString == null || inputString.trim().length()<=0)
            return isSameDomainUrl;

        String testString = inputString.trim();

        if(!testString.toLowerCase().startsWith("http"))
        {
            if(!testString.startsWith("/"))
                return isSameDomainUrl;

            testString = removeExtraSlash(testString);
            testString = "http://" + testString;
        }else{
            int parthStartIndex = testString.indexOf("//");
            if(parthStartIndex <=0)
            {
                return isSameDomainUrl;
            }

            String protocolHeader = testString.substring(0,parthStartIndex+2);
            testString = testString.substring(parthStartIndex+2);
            if(testString.startsWith("/")) {
                testString = removeExtraSlash(testString);
            }
            testString = protocolHeader + testString;
        }

        try {

            URL inputURL = new URL(testString);
            String host = inputURL.getHost();
            String path = inputURL.getPath();


            if(host == null)
            {
                host = this.requestDomain;
            }else{
                if(!host.equalsIgnoreCase(this.requestDomain))
                {
                    return isSameDomainUrl;
                }

            }

            int dotIndex = path.lastIndexOf('.');
            if (dotIndex >= 0) {
                String fileExt = path.substring(dotIndex + 1);
                if (fileExt.equalsIgnoreCase("html") || fileExt.equalsIgnoreCase("htm")) {
                        isSameDomainUrl = true;
                }

            } else { // this mean this is a folder using default html page
                   isSameDomainUrl = true;
            }


            if(testString.endsWith("/"))
            {
                testString = testString.substring(0,testString.length()-1);
            }

            //if this is a sameDomainURL, first check if it is already visited then add a new task to the thread pool
            if(isSameDomainUrl)
            {
                //synchronized (urlHistory) {
                    if (!this.urlHistory.contains(testString) && urlHistory.size() < maxUrlCheck) {

                        this.urlHistory.add(testString);
                        synchronized (tasksNumber) {
                            this.tasksNumber++;
                        }
                        this.executor.execute(new Handler(testString));
                    }
                //}

            }

        }catch(Exception e)
        {
            System.out.println("The URL has issue " + inputString);
            e.printStackTrace();
        }

        return isSameDomainUrl;
    }


    /*
    * print out result
    * if you need to see all the urls have been visited please include the commentted code.
     */
    public void printout()
    {
        System.out.println();
        System.out.println();
        System.out.println("The results are: ");

        for(String key: emails){
            System.out.println(key);
        }
    }

}
