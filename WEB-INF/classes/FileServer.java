import java.util.*;
import java.io.*;
import java.lang.String.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

/**BrailleConversionServlet takes a UTF8 text file that contains Hebrew characters
* Copyright Jonathan Rhine 2002
* @author Jonathan Rhine
* @date May 15,2002
*/
public class FileServer extends HttpServlet {


		public void doGet(HttpServletRequest rq, HttpServletResponse rs)
	    throws ServletException, IOException {

		String path = "/home/hebrew2/braille/downloads/";
		String rqFile = rq.getParameter("file");
		//System.out.println(rqFile);
		//Return File to user
				rs.setContentType("application/x-Braille-Download");
				rs.setHeader("Content-Disposition", "attachment; filename=\""+ rqFile + "\"");
		PrintWriter outWrite = rs.getWriter();


		//outWrite.print(BrailleFile);
		//outWrite.close();
		try {

		    //			File fileToDownload = new File(path+rqFile);
			// 			FileInputStream fileInputStream = new FileInputStream(fileToDownload);
		    //			InputStream fileInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("/downloads/"+rqFile);
			//		String userInputFile=System.getProperty("java.io.tmpdir")+System.getProperty("file.separator")+ multi.getFilesystemName("downloads"+rqFile);
ServletContext context = getServletContext();
InputStream fileInputStream = context.getResourceAsStream("/downloads/"+rqFile);

 			int i;
 			while ((i=fileInputStream.read())!=-1)
 				{
 				outWrite.write(i);
 				}

 			fileInputStream.close();
 			outWrite.close();

		}
		catch(Exception e) // file IO errors
		{
			e.printStackTrace();
		}

		}//end method doPost

}// end file download servlet