import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jaunt.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.IOException;

/**
 * Object that parses through search results on www.armedicalboard.org
 * @author walco005
 */
public class ArMedicalParser implements WebsiteParser<Doctor> {
	private final String SPAN_ID
		= "<span id=\"ctl00_ctl00_MainContentPlaceHolder_innercontent_";
	private final WebClient WEB_CLIENT = new WebClient();
	private final String PREFIX_URL = "http://www.armedicalboard.org/public/verify/";

	/**
	 * Executes the parser and goes through the Arkansas Medical Board website parsing the given
	 * search results, returning them as an ArrayList of Doctor objects.
	 * 
	 * @param radio				Which radio button is selected (0 = License Number, 1 = Last Name).
	 * @param query				What the user wants to search for.
	 * @return doctorList The list of Doctor objects found from the given search.
	 * @throws IOException 
	 * @throws JauntException
	 */
	public ArrayList<Doctor> execute(int radio, String query) throws IOException, JauntException{
		UserAgent userAgent = new UserAgent();
		ArrayList<Doctor> doctorList = new ArrayList<>();
		switch(radio) {
		case 0: //License Number Search
			userAgent.visit("http://www.armedicalboard.org/public/verify/lookup.aspx?LicNum="+query);
			beginLicenseSearch(userAgent.getLocation(), query);
		case 1: //Last Name Search
			userAgent.visit("http://www.armedicalboard.org/public/verify/lookup.aspx?LName="+query);
			HtmlPage page = WEB_CLIENT.getPage(userAgent.getLocation());
			doctorList = beginLastNameSearch(page, query);
		}
		userAgent.close();
		WEB_CLIENT.close();
		return doctorList;
	}
	
	/**
	 * Begins searching for a doctor with the specified license number and prints out the doctor found.
	 * @param url			The url from the search.
	 * @param query		The specified license number.
	 * @throws IOException 
	 * @throws JauntException
	 */
	private void beginLicenseSearch(String url, String query) throws IOException, JauntException {
		System.out.println("Searching for a doctor with license number " + query);
		if(url.contains("results.aspx")) {
			ArrayList<Doctor> docList = getFromUrl(url, query);
			for(Doctor d: docList) {
				System.out.println(d.toString());
			}
		}
	}
	
	/**
	 * Begins searching for doctors whose last names start with the specified substring.
	 * @param page				The first HtmlPage of the search result.
	 * @param query				The specified substring that the last names begin with.
	 * @return doctorList	The list of Doctor objects found from the given search.
	 * @throws IOException
	 * @throws JauntException
	 */
	private ArrayList<Doctor> beginLastNameSearch(HtmlPage page, String query) throws IOException, JauntException {
		System.out.println("Searching for all doctors whose last name begins with \"" 
				+ query + "\"");
		ArrayList<Doctor> doctorList = new ArrayList<>();
		LinkedHashSet<String> urlList = parseResultsPage(page, 1, new LinkedHashSet<>());
		int i = 1;
		int doctorAmount = urlList.size();
		System.out.println("Parsing " + doctorAmount + " doctors.");
		for(String s : urlList) {
			if(i % 10 == 0) {
				System.out.println(i + " of " + doctorAmount + " doctors parsed...");
			}
			doctorList.addAll(getFromUrl(s,""));
			i++;
		}
		System.out.println(doctorAmount + " doctors with " + doctorList.size() + " unique licenses found.");
		return doctorList;
	}
	
	/**
	 * Parses the results page and is looped until completion of the parsing.
	 * @param page					The first table page to start parsing through.
	 * @param startPageNum	The starting page number.
	 * @return 
	 * @throws IOException
	 * @throws JauntException 
	 */
	private LinkedHashSet<String> parseResultsPage(HtmlPage page, int startPageNum, 
			LinkedHashSet<String> urlList) throws IOException, JauntException {
		
		UserAgent userAgent = new UserAgent();
		int lastPageToParse;
		userAgent.openContent(page.asXml());
		Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
		String lastLink = links.getElement(links.size() - 1).innerHTML();
		lastLink = lastLink.replaceAll("\\s+", ""); //Removes all whitespace.
		switch(lastLink) {
		case "...": //The 'not completed' case. This will need to go through all the pages up until
								//the ... then restart on the page that the ... links to.
			String lastNumberedLink = links.getElement(links.size() - 2).innerHTML();
			lastNumberedLink = lastNumberedLink.replaceAll("\\s+", "");
			lastPageToParse = Integer.parseInt(lastNumberedLink);
			urlList.addAll(getUrls(page.asXml(), startPageNum, lastPageToParse));
			int newStartingPageNum = lastPageToParse + 1;
			parseResultsPage(WEB_CLIENT.getPage(getNextPage(newStartingPageNum)), newStartingPageNum,
					urlList);
			break;
		case "select": //The 'completed' case. This means the query returned only one page.
			urlList.addAll(getUrls(page.asXml(), startPageNum, startPageNum));
			break;
		default: //The 'almost completed' case. This will complete the parsing.
			if (lastLink.matches("\\d+")) { //Checks if lastLink is a number.
				if(links.size() < 20) { //Checks if this is the last page (less than 20 results)
					for(Element e : links) {
						if(e.toString().contains("results.aspx?strPHIDNO")) {
							urlList.add(grabUrlFromTag(e));
						}
					}
				} else {
					lastPageToParse = Integer.parseInt(lastLink);
					urlList.addAll(getUrls(page.asXml(), startPageNum, lastPageToParse));
				}
			}
			break;
		}
		userAgent.close();
		return urlList;
	}
	
	/**
	 * Given an Anchor element (Jaunt), returns the full url that is linked to.
	 * 
	 * @param tag		The anchor tag that the user wishes to extract a url from.
	 * @return link	The link in the anchor tag appended to the ArMedical website url.
	 */
	private String grabUrlFromTag(Element tag) {
		/* TODO: Add an if check in here to throw a custom exception if the tag does not 
		 * follow the required format.
		 */
		String linkTag = tag.toString();
			/* The tag is cut off at the following points:
			 * <a href=" | results.aspx?strPHIDNO=ASMB208746 | ">
			 */
		String link = PREFIX_URL + linkTag.substring(9, linkTag.length() - 2);
		return link;
	}
	
	/**
	 * Loops through a specified part of the result pages and returns the unique urls in them.
	 * @param firstPageAsXml 	The starting page stored as XML in a string.
	 * @param startPage				Which page you're starting on.
	 * @param endPage					Which page you end on.
	 * @return urlList				The unique list of urls that lead to doctors pages.
	 * @throws IOException
	 * @throws JauntException
	 */
	
	private Set<String> getUrls(String firstPageAsXml, int startPage, int endPage) 
			throws IOException, JauntException{
		
		UserAgent userAgent = new UserAgent();
		Set<String> urlList = new LinkedHashSet<>();
		for(int i = startPage; i <= endPage; i++) {
			if(i == startPage) {
				userAgent.openContent(firstPageAsXml);
			} else {
				HtmlPage page = WEB_CLIENT.getPage(getNextPage(i));
				userAgent.openContent(page.asXml());
			}
			for(Element e : userAgent.doc.findFirst("<table>").findEvery("<a href>")) {
				if(e.toString().contains("results.aspx?strPHIDNO")) {
					urlList.add(grabUrlFromTag(e));
				}
			}
		}
		return urlList;
	}

	/**
	 * Returns a string with the JavaScript needed to load the next page of the table.
	 * @param pageNumber	The page number that the user wants to load.
	 * @return 						The string containing the JavaScript code to load the given page.
	 */
	private String getNextPage(int pageNumber) {
    return String.format("javascript:__doPostBack"
        + "('ctl00$ctl00$MainContentPlaceHolder$innercontent$gvLookup','Page$%s')", pageNumber);
	}

	/**
	 * Gets the doctors information needed from the given url, returning it as an ArrayList<Doctor>.
	 * 
	 * If the search is done by last name (not license) and there are multiple licenses the
	 * 	ArrayList will contain all the licenses that the Doctor has had.
	 * 
	 * @param  url				The url that the doctors information will be extracted from.
	 * @param  query			If the search is done by license number, this is the license number used.
	 * @return docList		A list of relevant information on the doctor (see above)
	 * @throws IOException 
	 * @throws JauntException
	 */
	public ArrayList<Doctor> getFromUrl(String url, String query) 
			throws IOException, JauntException {
		UserAgent userAgent = new UserAgent();
		ArrayList<Doctor> docList = new ArrayList<>();
		String licNum = "";
		String expDate = "";
		String status = "";	
		String city = "";
		String state = "";
		String zip = "";
		userAgent.visit(url);
		
		if(userAgent.doc.findEvery("<title>Error on page").size() == 1) {
			System.out.println("Doctor could not be returned because of error on webpage: " + url);
			return docList;
		}
		String name = userAgent.doc.findFirst(SPAN_ID + "ListView1_ctrl0_Label1\">").innerHTML();

		if(userAgent.doc.findEvery("<div>No data was returned").size() == 0) {
			city = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label3\">").innerHTML();
			state = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label4\">").innerHTML();
			zip = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label5\">").innerHTML();
		}
		
		if(query.isEmpty()) {
			int counter = 0;
			while(userAgent.doc.findEvery(SPAN_ID+"ListView3_ctrl"+counter+"_Label1\">").size() != 0) {
				licNum = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + counter
				+ "_Label1\">").innerHTML();
				expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + counter
				+ "_Label3\">").innerHTML();
				status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl" + counter
				+	"_Label5\">").innerHTML();
				Doctor tmpDoc = new Doctor(name, city, state, zip, licNum, expDate, status);
				docList.add(tmpDoc);
				counter++;
			}
		} else {
			String span = userAgent.doc.findFirst("<span>" + query).toString();
			String amount = span.substring(72, 73);
			licNum = query;
			expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+amount+"_Label3\">")
					.innerHTML();
			status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+amount+"_Label5\">")
					.innerHTML();
			docList.add(new Doctor(name, city, state, zip, licNum, expDate, status));
		}
		return docList;
	}
	
}
