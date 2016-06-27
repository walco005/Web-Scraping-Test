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
	private final WebClient WEB_CLIENT = new WebClient();

	/**
	 * Executes the parser and goes through the Arkansas Medical Board website parsing the given
	 * search results
	 * 
	 * @param radio			Which radio button is selected (0 = License Number, 1 = Last Name).
	 * @param request		What the user wants to search for.
	 * @throws IOException 
	 */
	
	public List<Doctor> execute(int radio, String query) {
		UserAgent userAgent = new UserAgent();
		try {
			userAgent.visit("http://www.armedicalboard.org/public/verify/default.aspx");
			userAgent.doc.apply(radio, query);
			userAgent.doc.submit("Search");
			HtmlPage page = WEB_CLIENT.getPage(userAgent.getLocation());
			if(radio == 0) {
				if(userAgent.doc.findEvery("<h1 style=\"color:Red\"> + "
					+ "THIS IS NOT AN OFFICIAL LICENSE VERIFICATION").size() == 1) {
					//TODO: create a function that grabs a single result based on the license number given.
				} else {
					return null;
				}
			}
			parseResultsPage(page, 1);
		} catch (JauntException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				userAgent.close();
				WEB_CLIENT.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return DOCTOR_LIST;
	}
	
	/**
	 * Parses the results page and is looped until completion of the parsing.
	 * @throws NotFound 
	 * @throws ResponseException 
	 * @throws IOException 
	 */
	private void parseResultsPage(HtmlPage page, int startPage){
		UserAgent userAgent = new UserAgent();
		try {
			int numPagesToParse;
			userAgent.openContent(page.asXml());
			Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
			String last = links.getElement(links.size() - 1).innerHTML();
			last = last.replaceAll("\\s+", "");
			switch(last) {
			case "...":
				//The 'not completed' case. This will need to go through all the pages, then restart on the '+1'th page.
				String lastNum = links.getElement(links.size() - 2).innerHTML();
				lastNum = lastNum.replaceAll("\\s+", "");
				numPagesToParse = Integer.parseInt(lastNum);
				loopResultsPages(page.asXml(), startPage, numPagesToParse);
				int newStartingPageNum = numPagesToParse + 1;
				parseResultsPage(WEB_CLIENT.getPage(getNextPage(newStartingPageNum)), newStartingPageNum);
				break;
			case "select":
				//The 'completed' case. This is the last page.
				getDoctorsFromSearchResults(links);
				break;
			default:
				//The 'almost completed' case. This will complete the parsing.
				if (last.matches("\\d+")) {
					numPagesToParse = Integer.parseInt(last);
					loopResultsPages(page.asXml(), startPage, numPagesToParse);
				}
				break;
			}
		}catch(JauntException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				userAgent.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Loops through a specified part of the result pages.
	 * 
	 * @param pageAsXml 	The beginning page of the list as Xml.
	 * @param startPage		The number of the starting page.
	 * @param endPage			The number of the ending page.
	 */
	private void loopResultsPages(String pageAsXml, int startPage, int endPage) {
		HtmlPage page = null;
		UserAgent userAgent = new UserAgent();
		WebClient webClient = new WebClient();
		try {
			for(int i = startPage; i <= endPage; i++) {
				if(i == startPage) {
					userAgent.openContent(pageAsXml);
				} else {
					page = WEB_CLIENT.getPage(getNextPage(i));
		      userAgent.openContent(page.asXml());
				}
	      Elements links = userAgent.doc.findFirst("<table>").findEvery("<a href>");
	      System.out.println("Retrieving page " + i + "...");
	      getDoctorsFromSearchResults(links);
			}
		} catch(JauntException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				userAgent.close();
				webClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
				if(!tempDoc.getName().equals("NOT A REAL NAME THIS IS AN ERROR")) {
					DOCTOR_LIST.add(tempDoc);
				}
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
			if(userAgent.doc.findEvery("<title>Error on page").size() == 1) {
				System.out.println("Doctor could not be returned because of error on webpage: " + url);
				tmpDoc.setName("NOT A REAL NAME THIS IS AN ERROR");
				return tmpDoc;
			}
			String name = userAgent.doc.findFirst(SPAN_ID + "ListView1_ctrl0_Label1\">").innerHTML();
			String city = "";
			String state = "";
			String zip = "";
			String mailingAddress = "";
			/*
			 * Sets city/state/zip as long as there's NOT a div containing "No data was returned".
			 */
			if(userAgent.doc.findEvery("<div>No data was returned").size() == 0) {
				city = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label3\">").innerHTML();
				state = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label4\">").innerHTML();
				zip = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label5\">").innerHTML();
				mailingAddress = userAgent.doc.findFirst(SPAN_ID + "ListView2_ctrl0_Label1\">").innerHTML();
			}
			String licNum = "";
			String expDate = "";
			String status = "";	
			/*
			 * This checks if there's actually something in the DOCTOR_LIST and if the previous doctor
			 * shares the same name as the current one.
			 */
			if(DOCTOR_LIST.size() != 0) {
				Doctor prevDoc = DOCTOR_LIST.get(DOCTOR_LIST.size() - 1);
				if(prevDoc.getName().equals(name) && prevDoc.getMailingAddress().equals(mailingAddress)) {
					tmpDoc.setAmount(prevDoc.getAmount());
				}
			}
			licNum = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label1\">").innerHTML();
			expDate = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label3\">").innerHTML();
			status = userAgent.doc.findFirst(SPAN_ID + "ListView3_ctrl"+tmpDoc.getAmount()+"_Label5\">").innerHTML();
			tmpDoc.setAll(name, city, state, zip, licNum, expDate, status, mailingAddress);
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
