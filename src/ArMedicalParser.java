import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jaunt.*;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Object that parses through search results in www.armedicalboard.org
 * @author walco005
 */
public class ArMedicalParser implements WebsiteParser<Doctor> {
	private final List<Doctor> DOCTOR_LIST = new ArrayList<Doctor>();
	private final String SPAN_ID
		= "<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_";
	private final WebClient WEB_CLIENT = new WebClient();

	/**
	 * Executes the parser and goes through the Arkansas Medical Board website parsing the given
	 * search results
	 * @param radio		Which radio button is selected (0 = License Number, 1 = Last Name).
	 * @param query		What the user wants to search for.
	 * @throws IOException 
	 * @throws JauntException
	 */
	public List<Doctor> execute(int radio, String query) throws IOException, JauntException{
		UserAgent userAgent = new UserAgent();
		userAgent.visit("http://www.armedicalboard.org/public/verify/default.aspx");
		userAgent.doc.apply(radio, query);
		userAgent.doc.submit("Search");
		switch(radio) {
		case 0:
			System.out.println("Searching for a doctor with license number " + query);
			if(userAgent.getLocation().contains("results.aspx")) {
				DOCTOR_LIST.add(getFromUrl(userAgent.getLocation(), query));
			}
			break;
		case 1:
			System.out.println("Searching for all doctors whose last name begins with \"" 
				+ query + "\"");
			HtmlPage page = WEB_CLIENT.getPage(userAgent.getLocation());
			parseResultsPage(page, 1);
			break;
		}
		userAgent.close();
		WEB_CLIENT.close();
		return DOCTOR_LIST;
	}
	
	/**
	 * Parses the results page and is looped until completion of the parsing.
	 * @throws IOException
	 * @throws JauntException 
	 */
	private void parseResultsPage(HtmlPage page, int startPage) throws IOException, JauntException {
		UserAgent userAgent = new UserAgent();
		int numPagesToParse;
		userAgent.openContent(page.asXml());
		Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
		String lastLink = links.getElement(links.size() - 1).innerHTML();
		lastLink = lastLink.replaceAll("\\s+", ""); //Removes all whitespace.
		switch(lastLink) {
		case "...":
			/* The 'not completed' case. This will need to go through all the pages up until the ... 
			 * then restart on the page that the ... links to.
			 */
			String lastNumberedLink = links.getElement(links.size() - 2).innerHTML();
			lastNumberedLink = lastNumberedLink.replaceAll("\\s+", "");
			numPagesToParse = Integer.parseInt(lastNumberedLink);
			loopResultsPages(page.asXml(), startPage, numPagesToParse);
			int newStartingPageNum = numPagesToParse + 1;
			parseResultsPage(WEB_CLIENT.getPage(getNextPage(newStartingPageNum)), newStartingPageNum);
			break;
		case "select": //The 'completed' case. This means the query returned only one page.
			getDoctorsFromSearchResults(links);
			break;
		default: //The 'almost completed' case. This will complete the parsing.
			if (lastLink.matches("\\d+")) { //Checks if lastLink is a number.
				numPagesToParse = Integer.parseInt(lastLink);
				loopResultsPages(page.asXml(), startPage, numPagesToParse);
			}
			break;
		}
		userAgent.close();
	}
	
	/**
	 * Loops through a specified part of the result pages.
	 * @param pageAsXml 	The beginning page of the list as xml.
	 * @param startPage		The number of the starting page.
	 * @param endPage			The number of the ending page.
	 * @throws IOException
	 * @throws JauntException
	 */
	private void loopResultsPages(String firstPageAsXml, int startPage, int endPage) 
			throws IOException, JauntException{
		UserAgent userAgent = new UserAgent();
		WebClient webClient = new WebClient();
		for(int i = startPage; i <= endPage; i++) {
			if(i == startPage) {
				userAgent.openContent(firstPageAsXml);
			} else {
				HtmlPage page = WEB_CLIENT.getPage(getNextPage(i));
				userAgent.openContent(page.asXml());
			}
			Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
			System.out.println("Retrieving page " + i + "...");
			getDoctorsFromSearchResults(links);
		}
		userAgent.close();
		webClient.close();
	}

	/**
	 * Returns a string with the javascript needed to load the next page of the table.
	 * @param pageNumber	The page number that the user wants to load.
	 */
	private String getNextPage(int pageNumber) {
    return String.format("javascript:__doPostBack"
        + "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$%s')", pageNumber);
	}

	/**
	 * Accesses the url in each href given and then adds the doctor to the DOCTOR_LIST object.
	 * @param links		A list of elements that contains urls of doctors web pages.
	 * @throws IOException
	 * @throws JauntException 
	 */
	private void getDoctorsFromSearchResults(Elements links) throws IOException, JauntException {
		String tmp = "";
		for(Element e : links) {
			if(!e.toString().contains("javascript")) {
				/* The string is cut off at the following points:
				 * <a href=" | results.aspx?strPHIDNO=ASMB208746 | ">
				 */
				tmp = "http://www.armedicalboard.org/public/verify/" + 
					e.toString().substring(9, e.toString().length() - 2);
				Doctor tempDoc = getFromUrl(tmp, "");
				if(!tempDoc.getName().equals("NOT A REAL NAME THIS IS AN ERROR")) {
					DOCTOR_LIST.add(tempDoc);
				}
			}
		}
	}

	/**
	 * Gets the doctors information needed from the given url using USER_AGENT.
	 * @param  url				The url that the doctors information will be extracted from.
	 * @param  query			If the search is done by license number, this is the license number used.								
	 * @return tmpDoc			Doctor object that contains all the information extracted from the page.
	 * @throws IOException 
	 * 
	 */
	private Doctor getFromUrl(String url, String query) throws IOException, JauntException {
		UserAgent userAgent = new UserAgent();
		Doctor tmpDoc = new Doctor();
		String licNum = "";
		String expDate = "";
		String status = "";	
		String city = "";
		String state = "";
		String zip = "";
		String mailingAddress = "";
		userAgent.visit(url);

		if(userAgent.doc.findEvery("<title>Error on page").size() == 1) {
			System.out.println("Doctor could not be returned because of error on webpage: " + url);
			tmpDoc.setName("NOT A REAL NAME THIS IS AN ERROR");
			return tmpDoc;
		}

		String name = userAgent.doc.findFirst(SPAN_ID + "ListView1_ctrl0_Label1\">").innerHTML();

		if(userAgent.doc.findEvery("<div>No data was returned").size() == 0) {
			city = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label3\">").innerHTML();
			state = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label4\">").innerHTML();
			zip = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label5\">").innerHTML();
			mailingAddress = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label1\">").innerHTML();
		}

		if(query.isEmpty()) { //Means this is a last name search so it looks at the previous Doctor.
			if(DOCTOR_LIST.size() != 0) {
				Doctor prevDoc = DOCTOR_LIST.get(DOCTOR_LIST.size() - 1);
				if(prevDoc.getName().equals(name) && prevDoc.getMailingAddress().equals(mailingAddress)) {
					tmpDoc.setAmount(prevDoc.getAmount());
				}
			}
			licNum = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + tmpDoc.getAmount()
			+ "_Label1\">").innerHTML();
			expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + tmpDoc.getAmount()
			+ "_Label3\">").innerHTML();
			status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + tmpDoc.getAmount()
			+	"_Label5\">").innerHTML();
			tmpDoc.setAmount(tmpDoc.getAmount() + 1);
		} else { //Means this is a license number search so it grabs the correct license number.
			String span = userAgent.doc.findFirst("<span>" + query).toString();
			String amount = span.substring(72, 73);
			licNum = query;
			expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+amount+"_Label3\">")
					.innerHTML();
			status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+amount+"_Label5\">")
					.innerHTML();
		}
		tmpDoc.setAll(name, city, state, zip, licNum, expDate, status, mailingAddress);
		userAgent.close();
		return tmpDoc;
	}
}
