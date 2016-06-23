import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jaunt.*;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Object that parses through search results in www.armedicalboard.org
 * 
 * @author walco005
 */
public class ArMedicalParser implements WebsiteParser<Doctor> {
	//This UserAgent traverses into each doctor's web page.
	private static UserAgent USER_AGENT = new UserAgent();
	//This UserAgent traverses the upper-level search results.
	private static UserAgent MAIN_UA = new UserAgent();
	//This WebClient grabs each search result page and hands it to MAIN_UA.
	private static WebClient MAIN_WC = new WebClient();
	private static List<Doctor> DOCTOR_LIST = new ArrayList<Doctor>();
	private static Doctor TEMP_DOC = new Doctor();
	private static final CharSequence JS_SEQUENCE = "javascript";
	private static final String SPAN_ID
	= "<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_";

	/**
	 * Executes the parser and goes through the Arkansas Medical Board website parsing the given
	 * search results
	 * 
	 * @param radio		Which radio button is selected (0 = License Number, 1 = Last Name).
	 * @param request		What the user wants to search for.
	 * @throws IOException 
	 */
	public List<Doctor> execute(int radio, String request){
		try {
			MAIN_UA.visit("http://www.armedicalboard.org/public/verify/default.aspx");
			MAIN_UA.doc.apply(radio, request);
			MAIN_UA.doc.submit("Search");

			/*
			 * This is meant for only 3 pages right now, but can be easily modified for more if needed.
			 */
			HtmlPage page1 = MAIN_WC.getPage(MAIN_UA.getLocation());
			MAIN_UA.openContent(page1.asXml());
			Elements hrefs1 = MAIN_UA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs1);

			HtmlPage page2 = page1.getAnchorByHref("javascript:__doPostBack"
					+ "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$2')").click();
			MAIN_UA.openContent(page2.asXml());
			Elements hrefs2 = MAIN_UA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs2);

			HtmlPage page3 = page2.getAnchorByHref("javascript:__doPostBack"
					+ "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$3')").click();
			MAIN_UA.openContent(page3.asXml());
			Elements hrefs3 = MAIN_UA.doc.findFirst("<table>").findEvery("<a href>");
			accessURL(hrefs3);
			
		} catch (JauntException | IOException e) {
			System.out.println(e);
		} finally {
			try {
				MAIN_UA.close();
				USER_AGENT.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return DOCTOR_LIST;
	}

	/**
	 * Accesses the url in each href given and then adds the doctor to the DOCTOR_LIST object.
	 * 
	 * @param hrefs		A list of elements that contains <a href> tags that contain urls of
	 * 								doctors' web pages.
	 * @throws IOException
	 */
	public void accessURL(Elements hrefs) throws IOException {
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
				DOCTOR_LIST.add(tempDoc);
//				CSVFP.printRecord(tempDoc.asList());
			}
		}
	}

	/**
	 * Gets the doctors information needed from the given url using USER_AGENT.
	 * 
	 * @param  url				The url that the doctors information will be extracted from.
	 * @return tmpDoc			Doctor object that contains all the information extracted from the page.
	 * 
	 */
	public Doctor getInfo(String url) {
		try {
			Doctor tmpDoc = new Doctor();
			USER_AGENT.visit(url);

			String name = USER_AGENT.doc.findFirst(SPAN_ID + "ListView1_ctrl0_Label1\">").innerHTML();
			String city = USER_AGENT.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label3\">").innerHTML();
			String state = USER_AGENT.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label4\">").innerHTML();
			String zip = USER_AGENT.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label5\">").innerHTML();
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
					licNum = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl1_Label1\">").innerHTML();
					expDate = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl1_Label3\">").innerHTML();
					status = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl1_Label5\">").innerHTML();
					tmpDoc.addAmount();
					break;
				case 2: //Stops at case 2 since the highest amount of repeats in this data set is 2.
					licNum = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl2_Label1\">").innerHTML();
					expDate = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl2_Label3\">").innerHTML();
					status = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl2_Label5\">").innerHTML();
					break;
					//Can be modified to take account of more than 2 repeats if needed.
				default: 
					break;
				}
			} else {
				licNum = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl0_Label1\">").innerHTML();
				expDate = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl0_Label3\">").innerHTML();
				status = USER_AGENT.doc.findFirst(SPAN_ID + "ListView3_ctrl0_Label5\">").innerHTML();
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
