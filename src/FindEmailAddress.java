/*
*
* The project is for finding email address in the web site
*
* Create a command line program that will take an internet domain name (i.e. “jana.com”) and
* print out a list of the email addresses that were found on that website only.
* the application will find email addresses on any discoverable page of the website.
*
*
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.net.URI;

/*
*  This is the main class to get the email addresses
*
*/
public class FindEmailAddress {

    public String requestDomain;
    public Vector<String> urls;
    public Hashtable<String,String> urlHistory;
    public Hashtable<String,String> emails;

    public static void main(String[] args)
    {
        String requestUrl = null;

        if(args.length >0)
        {
            requestUrl =  args[0];
        }
        if (requestUrl == null)
            return;

        FindEmailAddress findEmailAddress = new FindEmailAddress(requestUrl);
        findEmailAddress.startWebCrawl();
        findEmailAddress.printout();
    }

    /*
    *  @param requestURL is the root url. All the webpage with same domain will be searched for email address
    */
    public FindEmailAddress(String requestURL)
    {

        String thisRequestURL = requestURL;
        if(!requestURL.toLowerCase().startsWith("http"))
        {
            thisRequestURL = "http://" + requestURL;
        }
        try {
            URI uri = new URI(thisRequestURL);
            this.requestDomain= uri.getHost();

        }catch(java.net.URISyntaxException e)
        {
            e.printStackTrace();
        }
        this.emails = new Hashtable<String,String>();
        this.urls   = new Vector<String>();
        this.urlHistory = new Hashtable<String, String>();
        this.urls.add(thisRequestURL);
        this.urlHistory.put(thisRequestURL,"");
    }

    /*
     *  This is the function to do web crawling
     */
    public void startWebCrawl()
    {
        while(this.urls.size()>0)
        {
            String requestURL = this.urls.elementAt(0);
            this.urls.remove(0);
            makeHttpReuqest(requestURL);
        }
    }


    //need multithread
    /*
    * This is the function to make http call.
    * And this function will also do the email search and same domain url search.
    * the email address will be saved in a hashtable
    * the urls will be saved in a vector, and a history tashtable is for filtering the urls already visited.
    */
    public String makeHttpReuqest(String httpURLString)
    {
        String responseString = null;
        BufferedReader reader = null;
        StringBuffer response = null;
        try {
            URL urlObj = new URL(httpURLString);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0"); //some website if did not see the user agent will take it robot and cause http call failing
            int responseCode = con.getResponseCode();
            if(responseCode == 200) {
                reader = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                response = new StringBuffer();

                while ((inputLine = reader.readLine()) != null) {
                    searchEmailAndHRef(inputLine);
                    response.append(inputLine);
                }
            }
        }catch(Exception ex)
        {
            System.out.println("Cannnot reach url:" + httpURLString);
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
        }

        if(response != null)
        {
            responseString = response.toString();
        }

        return responseString;

    }


    /*
    *  Search Email and Href
    *  if it is email we do not need to check it is same domain url anymore.
     */
    public void searchEmailAndHRef(String responseStr)
    {
        //System.out.println("The searchEmailAndHRef string is :" + responseStr);
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
    public boolean checkIfEmail(String inputString)
    {
        //System.out.println("The checkIFEmail string is :" + inputString);
        String testString = inputString.trim();
        boolean isEmail = false;
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
                emails.put(emailAddress,"");
                isEmail = true;
            }
        }
        return isEmail;
    }


    /*
    *  check if this is a valid url we need to scrawl
    */
    public boolean checkIfSameDomainHtmlLink(String inputString)
    {
        //System.out.println("The checkIfSameDomainHtmlLink string is :" + inputString);
        boolean isSameDomainUrl = false;
        String testString = inputString.trim();

        if(!testString.toLowerCase().startsWith("http"))
        {
            if(!testString.startsWith("/"))
                return isSameDomainUrl;

            while(testString.startsWith("/"))
            {
                testString = testString.substring(1);
            }
            testString = "http://" + testString;
        }else{
            int parthStartIndex = testString.indexOf("//");
            if(parthStartIndex <=0)
            {
                return isSameDomainUrl;
            }

            String protocolHeader = testString.substring(0,parthStartIndex+2);
            testString = testString.substring(parthStartIndex+2);
            while(testString.startsWith("/"))
            {
                testString = testString.substring(1);
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

            if(path == null)
            {
                path = "/";
            }

            if(path == null) //this a folder with same domain
            {
                isSameDomainUrl = true;
            }else{
                int dotIndex = path.lastIndexOf('.');
                if (dotIndex >= 0) {
                    String fileExt = path.substring(dotIndex + 1);
                    if (fileExt.equalsIgnoreCase("html") || fileExt.equalsIgnoreCase("htm")) {
                            isSameDomainUrl = true;
                    }

                } else { // this mean this is a folder using default html page
                       isSameDomainUrl = true;
                }
            }

            if(isSameDomainUrl)
            {
                if(this.urlHistory.get(testString)==null) {
                        this.urlHistory.put(testString, "");
                        this.urls.add(testString);
                }

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
        System.out.println("The results are: ");
        java.util.Set<String> keys = this.emails.keySet();
        for(String key: keys){
            System.out.println(key);
        }


//        System.out.println("urls accessed are: ");
//        keys = this.urlHistory.keySet();
//        for(String key: keys){
//            System.out.println(key);
//        }
    }

}
