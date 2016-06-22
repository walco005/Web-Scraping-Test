import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jaunt.*;

import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;


public class test {
	//This UserAgent traverses into each doctor's web page.
	private static UserAgent USER_AGENT = new UserAgent();
	private static FileWriter FW = null;
	private static CSVPrinter CSVFP = null;
	private static CSVFormat CSV_FILE_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n");
	private static String CSV_FILE_NAME = "results.csv";
	private static final Object [] HEADER = 
		{"name","city","state","zip","licenseNum","expirationDate","licenseStatus"};
	//The previous Doctor in the loops is stored in TEMP_DOC, used for checking repeats.
	private static Doctor TEMP_DOC = new Doctor();
	static CharSequence JS_SEQUENCE = "javascript";
	
	public static void main(String[] args) throws IOException {
		try {
			//This UserAgent traverses the upper-level search results.
			UserAgent mainUA = new UserAgent();
			WebClient mainWC = new WebClient();
			FW = new FileWriter(CSV_FILE_NAME);
			CSVFP = new CSVPrinter(FW, CSV_FILE_FORMAT);
			CSVFP.printRecord(HEADER);
			
			//The UserAgent accesses the starting page and performs the given search.
			mainUA.visit("http://www.armedicalboard.org/public/verify/default.aspx");
			mainUA.doc.apply(1, "za");
			mainUA.doc.submit("Search");
			
			/*
			 * Each page is navigated to with the WebClient, then the XML is passed to the UserAgent.
			 * This can be changed if more pages of results are added in the future.
			 */
			HtmlPage page1 = mainWC.getPage(mainUA.getLocation());
			mainUA.openContent(page1.asXml());
			Elements hrefs1 = mainUA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs1);
			
			HtmlPage page2 = page1.getAnchorByHref
					("javascript:__doPostBack"
							+ "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$2')").click();
			mainUA.openContent(page2.asXml());
			Elements hrefs2 = mainUA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs2);
			
			HtmlPage page3 = page2.getAnchorByHref
					("javascript:__doPostBack"
							+ "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$3')").click();
			mainUA.openContent(page3.asXml());
			Elements hrefs3 = mainUA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs3);
			
			FW.flush();
			FW.close();
			CSVFP.close();
			mainWC.close();
		} catch(JauntException e) {
			System.err.println(e);
		}
	}

	/**Accesses the url in each href given and then writes it to the output CSV file
	 * 
	 * @param hrefs		A list of elements that contains <a href> tags that contain urls of
	 * 								doctors' web pages.
	 * @throws IOException
	 */
	public static void accessURL(Elements hrefs) throws IOException {
		String tmp = "";
		for(Element e : hrefs) {
			/*
			 * This if checks if the string contains "javascript" since those strings are not meant
			 * to be put through this loop, they are ignored.
			 */
			if(!e.toString().contains(JS_SEQUENCE)) {
				/*
				 * Using the fact that all the elements have a specific start point for the url, 9, and
				 * they have a specific end point, length - 2, to extract the url from the element.
				 */
				tmp = "http://www.armedicalboard.org/public/verify/" + 
						e.toString().substring(9, e.toString().length() - 2);
				Doctor tempDoc = getInfo(tmp);
				CSVFP.printRecord(tempDoc.asList());
			}
		}
	}
	
	/**Gets the doctors information needed from the given url using USER_AGENT.
	 * 
	 * @param  url				The url that the doctors information will be extracted from.
	 * @return tmpDoc			Doctor object that contains all the information extracted from the page.
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws FailingHttpStatusCodeException 
	 */
	public static Doctor getInfo(String url) {
		try {
			Doctor tmpDoc = new Doctor();
			USER_AGENT.visit(url);
			
			String name = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView1_ctrl0_Label1\">")
					.innerHTML();
			String city = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView2_ctrl0_Label3\">")
					.innerHTML();
			String state = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView2_ctrl0_Label4\">")
					.innerHTML();
			String zip = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView2_ctrl0_Label5\">")
					.innerHTML();
			String licNum = "";
			String expDate = "";
			String status = "";	
			/*
			 * This checks if the current Doctor showed up previously and acts accordingly based on how
			 * many times the doctor has shown up.
			 */
			if(name.equals(TEMP_DOC.getName())) {
				switch(TEMP_DOC.getAmount()) {
				case 1:
					licNum = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl1_Label1\">")
						.innerHTML();
					expDate = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl1_Label3\">")
						.innerHTML();
					status = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl1_Label5\">")
						.innerHTML();
					tmpDoc.addAmount();
					break;
				case 2: //Stops at case 2 since the highest amount of repeats in this data set is 2.
					licNum = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl2_Label1\">")
						.innerHTML();
					expDate = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl2_Label3\">")
						.innerHTML();
					status = USER_AGENT.doc.findFirst
						("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl2_Label5\">")
						.innerHTML();
					break;
				//Can be modified to take account of more than 2 repeats if needed.
				default: 
					break;
				}
			} else {
				licNum = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl0_Label1\">")
					.innerHTML();
				expDate = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl0_Label3\">")
					.innerHTML();
				status = USER_AGENT.doc.findFirst
					("<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_ListView3_ctrl0_Label5\">")
					.innerHTML();
			}
			
			tmpDoc.setAll(name, city, state, zip, licNum, expDate, status);
			tmpDoc.addAmount();
			TEMP_DOC = tmpDoc.clone();
			return tmpDoc;
		} catch (JauntException e) {
			System.err.println(e);
		}
		return null;
	}
}
