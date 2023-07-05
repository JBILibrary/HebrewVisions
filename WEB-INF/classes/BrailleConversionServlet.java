import java.awt.*;
import java.awt.event.*;
import java.awt.List.*;
import java.util.*;
import java.io.*;
import java.lang.String.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

/**BrailleConversionServlet takes a UTF8 text file that contains Hebrew characters
* or characters of any other foreign language and converts it to computer Braille ASCII
* based on two config files. "config.txt" and "2config.txt" . config.txt allows for a single
* to single or single to double char mapping. 2config.txt does contractions after config.txt
* already converted everything and does a double char to double char mapping or double to
* single. Future modifications of this program will allow for more flexible config files.
* Copyright Jonathan Rhine 2002
* @author Jonathan Rhine
* @date May 15,2002
*/
public class BrailleConversionServlet extends HttpServlet {


	private	Mapping MapArray[];
	private Contraction ContractArray[];


	//-------Holds the intger of one Unicode input and the char it maps to.
	private class Mapping {
		int numChar;
	    String BrailleChar;
	    public String toString(){
	          return Integer.toString(numChar);
	     }
	}



	//-------------------Holds one contraction set---------------
	  private class Contraction {
	    String find;
	    String replace;
  	}



	/** init calls the appropriate methods to populate the mappings
	* from the given cofig files
	*/
    /*	public void init(ServletConfig config)throws ServletException
	{

		populateMap();
		populateContractions();

	}//end init-config
    */


	//--------------------------------------------------------------------------------
	/** The doPost method of the BrailleConversionServlet accepts a UTF-8
	*   Txt file and converts it based on the config files that were loaded
	*   in init, - converts the text to Computer Braille Ascii.
	*/
	public void doPost(HttpServletRequest rq, HttpServletResponse rs)
	    throws ServletException, IOException {

	    //moved from init!
	    populateMap();
	    populateContractions();

		/**
		//confirm user has logged in, if not redirect
		HttpSession session = rq.getSession();
		Object done= session.getAttribute("logon.isDone");
		if(done==null){
			rs.sendRedirect("/login.html");
			return;
		}
		*/

		// The default rename policy states that if the file name already exists add a number
		// to the name ie test.txt = test1.txt and test2.txt
		DefaultFileRenamePolicy dfrp = null;
		MultipartRequest multi= null;

		try {
    		// Blindly take it on faith this is a multipart/form-data request

    		// Construct a MultipartRequest to help read the information. Pass in the request,
    		// a directory to saves files to, and the maximum POST size we should attempt to
    		// handle. Here we write to the server root and impose 150 kb limit.
    		dfrp = new DefaultFileRenamePolicy();
		//    		multi = new MultipartRequest(rq, "/home/hebrew2/braille/input", 150 * 1024 , dfrp);
		//I THINK the temp directory will do just fine...
    		multi = new MultipartRequest(rq, System.getProperty("java.io.tmpdir"), 150 * 1024 , dfrp);

    	}//try
		catch (Exception e) {
		    rs.setContentType("text/html");
			//old code -had the message to be sent to user, now we just redirect the user
			//PrintWriter outWrite = rs.getWriter();
			//outWrite.print("Error Uploading file - Possbily exceed size limit of 150kb");
			//outWrite.close();
			rs.sendRedirect("http://jbilibrary.org/error_limit.html");
			e.printStackTrace();
			return;
		}//catch


		//System.out.println(System.getProperties());
		int ch;                         //unicode char read from file
    	int location;                   //Location in Mapping array to find Ascii char
    	String BrailleFile= new String();
    	StringBuffer TempBrailleFile = new StringBuffer();
    	BrailleFile="";
		Mapping key;

		//form name for the input file must be "hebrewTextFile"
		//		String userInputFile="/home/hebrew2/braille/input/" + multi.getFilesystemName("hebrewTextFile");
		String userInputFile=System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+ multi.getFilesystemName("hebrewTextFile");

    	try{//try 1
		  FileInputStream fis = new FileInputStream(userInputFile);
		  InputStreamReader isr = new InputStreamReader(fis,"UTF8");
    	  BufferedReader in = new BufferedReader(isr);

		  while ((ch = in.read()) > -1) {
		      key = new Mapping();
			  key.numChar = ch;
			  location = Arrays.binarySearch(MapArray, key , new Comparator() {
    	      int firstNum, secondNum;
    	      public int compare( Object o1, Object o2) {
                  firstNum=Integer.parseInt( o1.toString());
                  secondNum=Integer.parseInt(o2.toString());
                  if(firstNum<secondNum)
                      return -1;
                  if(firstNum>secondNum)
                      return +1;
                  return 0;
              }
          	});

          	if (location>=0)
          	  TempBrailleFile.append(MapArray[location].BrailleChar);
          	  //BrailleFile=BrailleFile + (MapArray[location].BrailleChar);


  		}//while

		//Created TempBrailleFile as an attempt to speed up the program, program was
		//written with BrailleFile being the only string so....
		BrailleFile=TempBrailleFile.toString();

		System.out.println("Finished reading in user file and first level translate");

    	in.close();

  		}//try
  		catch (IOException e){
  		    rs.setContentType("text/html");
			//PrintWriter outWrite = rs.getWriter();
			//outWrite.print("Error Reading User Input File");
			//outWrite.close();
			rs.sendRedirect("/error_reading.html");
			e.printStackTrace();
			return;
		} //1 Catch

    	//All contractions from the basic hebrew -find and replace

		//orignal code, redone for debuging
		//for(int i=0; i< ContractArray.length; i++)
		//	BrailleFile= replaceAll(BrailleFile,ContractArray[i].find,ContractArray[i].replace);

		for(int i=0; i< ContractArray.length; i++){
			BrailleFile= replaceAll(BrailleFile,ContractArray[i].find,ContractArray[i].replace);
			//System.out.println("Finished Contract " + ContractArray[i].find);
		}


		//System.out.println("finished find replace");

		//WordWrap
		BrailleFile= executeWordWrap(BrailleFile,40,25);

		//System.out.println("Finished word wrap");

		//Return File to user
		rs.setContentType("application/x-Braille-Conversion");
		rs.setHeader("Content-Disposition", "attachment; filename=\"Translated_"+ multi.getFilesystemName("hebrewTextFile") + "\"");
		PrintWriter outWrite = rs.getWriter();
		outWrite.print(BrailleFile);
		outWrite.close();

  	}//end method doPost


  	/**
  	* Replaces the first occurence of a substring
  	* @param source string that needs to have stuff replaced
  	* @param oldVal name of variable in Source string
  	* @param newVal replacement to put in String
  	*/
  	public static String replace(String source, String oldVal, String newVal)
  	{
  		if (oldVal == null) return source;

  		//cat.debug("replacing:" + oldVal + ": by :" + newVal + ":");
  		StringBuffer SB = new StringBuffer(source);
  		int nameindex = source.indexOf(oldVal);
  		SB.replace(nameindex, nameindex + oldVal.length(), newVal);
  		//cat.debug("new StringBuffer: "+SB);
  		return new String(SB);
  	}


	/**
	* Replaces all occurences of a substring
	* @param source string that needs to have stuff replaced
	* @param oldVal name of variable in Source string
	* @param newVal replacement to put in String
	*/
	public static String replaceAll(String source, String oldVal, String newVal)
	{
		while(source.indexOf(oldVal) != -1)
	  	{
	  		source = replace(source, oldVal, newVal);
	  	}
	  	return source;
	}


	/**
	*populate Map opens config.txt and reads it. the first char is the char to be mapped
	*the = sign is ignored and the 3 and 4 chars are what it should be mapped to. They
  	*are trimmed of all their white space
	*/
	private void populateMap()
	{
		Vector v= new Vector(65,20);
	    int value;
	    Mapping configLine;
	    String configChar;

	    try{//try 1

		//		FileInputStream fis = new FileInputStream(super.getServletContext().getRealPath("")+System.getProperty("file.separator")+"config.txt");
		InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.txt");
		  InputStreamReader isr = new InputStreamReader(fis,"UTF8");
	      BufferedReader in = new BufferedReader(isr);

	      //Manually add char mapping for space
	      configLine= new Mapping();
	      configLine.numChar=32;
	      configLine.BrailleChar=" ";
	      v.addElement(configLine);

	      //Manually add char mapping for 13 Carrige Return
	      configLine= new Mapping();
	      configLine.numChar=13;
	      configLine.BrailleChar="\r";
	      v.addElement(configLine);

	      //Manually add char mapping for Line feed
	      configLine= new Mapping();
	      configLine.numChar=10;
	      configLine.BrailleChar="\n";
	      v.addElement(configLine);


	      //remove the opening char for begining of file
	      value=in.read();

	      while ((value = in.read()) > -1) {
	        configLine= new Mapping();
	        configLine.numChar=value;
	        configChar=in.readLine();
	        if(configChar.length()>=3)
	          configLine.BrailleChar= configChar.substring(1,3).trim();
	        else
	          configLine.BrailleChar= configChar.substring(1,2).trim();
	        v.addElement(configLine);
	      }
	      in.close();

	    }catch (IOException e){
	      System.out.println("Error reading config file");
	    }

	    v.trimToSize();
	    //-----Convert Vector to an Array------------

	    MapArray = new Mapping[v.size()];
	    for (int count=0; count< v.size() ; count++)
	         MapArray[count]=new Mapping();
	    v.toArray(MapArray);


	    //----Sort the Array---------------------------

	    Arrays.sort(MapArray, new Comparator() {
	    	int firstNum, secondNum;
	    	public int compare( Object o1, Object o2) {
	    	    firstNum=Integer.parseInt( o1.toString());
        	    secondNum=Integer.parseInt(o2.toString());
        	    if(firstNum<secondNum)
        		       return -1;
        	    if(firstNum>secondNum)
        		       return +1;
        	    return 0;
     		}//close compare
 		}//close comparator
 		);//close sort statement
	}//close populateMap


	/*
	*Read 2config.txt and create a mapping for contractions
	*/
	private void populateContractions()
	{
		Vector v= new Vector(15,10);
	    Contraction configLine;
	    String contractLine;

      	//read 2config.txt
      	try{//try 1
	    //	   	    FileInputStream fis = new FileInputStream(super.getServletContext().getRealPath("")+System.getProperty("file.separator")+"2config.txt");
		InputStream fis = Thread.currentThread().getContextClassLoader().getResourceAsStream("2config.txt");
	    	InputStreamReader isr = new InputStreamReader(fis);
      		BufferedReader in = new BufferedReader(isr);

      		while ((contractLine = in.readLine()) !=null) {
        		configLine= new Contraction();
        		configLine.find= contractLine.substring(0,2).trim();
        		configLine.replace=contractLine.substring(3,5).trim();
        		v.addElement(configLine);
      		}
      		in.close();
      	}catch (IOException e){
      		System.out.println("Error reading 2config file");
    	} //Catch

    	v.trimToSize();

    	//-----Convert Vector to an Array------------
    	ContractArray = new Contraction[v.size()];
    	for (int count=0; count< v.size() ; count++)
          	ContractArray[count]=new Contraction();
     	v.toArray(ContractArray);

	}//end populateContractions


	/**executeWordWrap wraps the string passed to it at line Length. Words that do not fit
	*  within linelen are wraped. Words Longer than lineLen are split at lineLen-1 and a hyphen
	*  is placed at the end of the line.
	*  @param numOfLines determines how many lines are placed on a page
	*/
	public static String executeWordWrap( String s, int lineLen, int numOfLines) {

		StringBuffer str = new StringBuffer(s);

	    // Inserts a newline character before every
	    // (char)12 and (char)10(char)13 as well as
	    // before every word that causes the lenght
	    // of a line to exceed lineLen
	    // If a single word exceeds lineLen, it is
	    // preserved on its own line.

	    int charsThisLine = 0;
	    int totalChars = 0;
	    int lastSpace = 0;
	    int lineCount = 0;
	    char c;
	    int i = 0;
	    while( i < str.length() ) {
	      c = str.charAt(i);
	      if ( c == ' ' ) lastSpace = i;

	      // Insert a new line before (char)12 or
	      // (char)10(char)13
	      if (( c == 12 ) || (( c == 13 ) && ( i + 1 < str.length() ) &&
	         ( str.charAt( i + 1 ) == 10 ))) {
	        //str.insert( i++, '\n');
	        i++;

	        //page breaking
	        if (c==12)
	          lineCount=0;
	        else
	          lineCount++;

	        if(lineCount==numOfLines){
	           str.insert(i,"\u000c");
	           i++;
	           lineCount=0;
	        }


	        charsThisLine = 0;
	        lastSpace = 0;
	        totalChars = i;

	    	continue;
	      }

	      // End of the line, we must break
	      if (charsThisLine++ > lineLen) {

	        lineCount++;

	        // If we exceeded the line length with a space
	        // we have to find the previous space
	        if ( lastSpace == i ) {
	          for( int j = i - 1; j > 0; j-- ) {
	        if ( str.charAt(j) == ' ' ) {
	          lastSpace = j;
	              break;
	            }
	          }
	        }
	        // lastSpace points to the space after which we
	        // will break.  If the next character is already
	        // a newline, then set lastSpace = 0 to insert
	        // a hyphen or break at the next space.
	        if ( lastSpace > 0 ) {
	          if (( str.length() > lastSpace + 1) && ( str.charAt(lastSpace + 1) != '\n' )) {
	            str.deleteCharAt(lastSpace);
	            str.insert(lastSpace, "\r\n");
	            i = lastSpace + 2;

	            if(lineCount==numOfLines){
	              str.insert(i,"\u000c");
	              i++;
	              lineCount=0;
	            }

	            charsThisLine = 0;
	            lastSpace = 0;
	            totalChars = i;
	            continue;
	          } else {
	            lastSpace = 0;
	          }
	        }

	        if ( lastSpace == 0) {
	          str.insert( totalChars + lineLen - 1, '-' );
	          str.insert( totalChars + lineLen, "\r\n" );
	          i = totalChars + lineLen + 1;


	          if(lineCount==numOfLines){
	            str.insert(i,"\u000c");
	            i++;
	            lineCount=0;
	          }


	          charsThisLine = 0;
	          lastSpace = 0;
	          totalChars = i+1;
	          continue;
	       }//close if lastSpace==0
	      }//close if (charsThisLine++ > lineLen)

	      i++;
	    }
	    str.insert(i,"\r\n\u000c");
	    return str.toString();
	  }//end executeWordWrap



}//end BrailleConversionServlet
