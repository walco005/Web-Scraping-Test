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
	private final List<Doctor> DOCTOR_LIST = new ArrayList<Doctor>();
	private final CharSequence JS_SEQUENCE = "javascript";
	private final String SPAN_ID
		= "<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_";

	/**
	 * Executes the parser and goes through the Arkansas Medical Board website parsing the given
	 * search results
	 * 
	 * @param radio			Which radio button is selected (0 = License Number, 1 = Last Name).
	 * @param request		What the user wants to search for.
	 * @throws IOException 
	 */
	public List<Doctor> execute(int radio, String request){
		UserAgent userAgent = new UserAgent();
		WebClient webClient = new WebClient();
		HtmlPage page = null;
		try {
			userAgent.visit("http://www.armedicalboard.org/public/verify/default.aspx");
			userAgent.doc.apply(radio, request);
			userAgent.doc.submit("Search");
			
			int numPagesToParse = 1;
			Elements hrefs = userAgent.doc.findFirst("<table>").findEvery("<a href>");
			String last = hrefs.getElement(hrefs.size() - 1).innerHTML();
			if(last.contains("...")) {
				System.out.println("The current system can only parse searches with 10 or less pages of " +
					"results, so only the first 10 pages will be parsed.");
				numPagesToParse = 10;
			}
			if(!last.equals("select") && numPagesToParse != 10) {
				numPagesToParse = Integer.parseInt(last);
			}
			
			for(int i = 1; i <= numPagesToParse; i++) {
				if(i == 1) {
					page = webClient.getPage(userAgent.getLocation());
				} else {
					page = webClient.getPage(getNextPage(i));
				}
        userAgent.openContent(page.asXml());
        Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
        getDoctorsFromSearchResults(links);
			}
		} catch (JauntException | IOException e) {
			System.out.println(e);
		} finally {
			try {
				userAgent.close();
				webClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return DOCTOR_LIST;
	}

	/**
	 * Returns a string with the javascript needed to load the next page of the table.
	 * 
	 * @param pageNumber	The page number that the user wants to load.
	 */
	private String getNextPage(int pageNumber) {
    return String.format("javascript:__doPostBack"
        + "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$%s')", pageNumber);
	}

	/**
	 * Accesses the url in each href given and then adds the doctor to the DOCTOR_LIST object.
	 * 
	 * @param hrefs		A list of elements that contains <a href> tags that contain urls of
	 * 								doctors' web pages.
	 * @throws IOException
	 */
	private void getDoctorsFromSearchResults(Elements hrefs) throws IOException {
		String tmp = "";
		for(Element e : hrefs) {
			/*
			 * This checks if the string contains "javascript" since those strings are not meant
			 * to be put through this loop.
			 */
			if(!e.toString().contains(JS_SEQUENCE)) {
				/* 
				 * The string is cut off at the following points:
				 * <a href=" | results.aspx?strPHIDNO=ASMB208746 | ">
				 */
				tmp = "http://www.armedicalboard.org/public/verify/" + 
						e.toString().substring(9, e.toString().length() - 2);
				Doctor tempDoc = getFromUrl(tmp);
				DOCTOR_LIST.add(tempDoc);
			}
		}
	}

	/**
	 * Gets the doctors information needed from the given url using USER_AGENT.
	 * 
	 * @param  url				The url that the doctors information will be extracted from.
	 * @return tmpDoc			Doctor object that contains all the information extracted from the page.
	 * @throws IOException 
	 * 
	 */
	private Doctor getFromUrl(String url) throws IOException {
		UserAgent userAgent = new UserAgent();
		try {
			Doctor tmpDoc = new Doctor();
			userAgent.visit(url);
			String name = userAgent.doc.findFirst(SPAN_ID + "ListView1_ctrl0_Label1\">").innerHTML();
			String city = "";
			String state = "";
			String zip = "";
			/*
			 * Sets city/state/zip as long as there's NOT a div containing "No data was returned".
			 */
			if(userAgent.doc.findEvery("<div>No data was returned").size() == 0) {
				city = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label3\">").innerHTML();
				state = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label4\">").innerHTML();
				zip = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label5\">").innerHTML();
			}
			String licNum = "";
			String expDate = "";
			String status = "";	
			/*
			 * This checks if there's actually something in the DOCTOR_LIST and if the previous doctor
			 * shares the same name as the current one.
			 */
			if(DOCTOR_LIST.size() != 0) {
				if(DOCTOR_LIST.get(DOCTOR_LIST.size() - 1).getName().equals(name)) {
					Doctor prevDoc = DOCTOR_LIST.get(DOCTOR_LIST.size() - 1);
					tmpDoc.setAmount(prevDoc.getAmount());
				}
			}
			licNum = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label1\">").innerHTML();
			expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label3\">").innerHTML();
			status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label5\">").innerHTML();
			tmpDoc.setAll(name, city, state, zip, licNum, expDate, status);
			tmpDoc.addAmount(1);
			return tmpDoc;
		} catch (JauntException e) {
			System.err.println(e);
		} finally {
			userAgent.close();
		}
		return null;
	}
}
