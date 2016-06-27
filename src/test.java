import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.jaunt.JauntException;

import java.util.List;
import java.util.logging.Level;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
	private final static int RADIO = 1; //Which radio button is selected on the search page.
																			//0 = License Number, 1 = Last Name
	private final static String QUERY = "za";
	private final static String OUTPUT_FILE_NAME = "results.csv";
	private static final CSVFormat CSV_FILE_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n");
	private static final Object [] HEADER = 
		{"name","city","state","zip","licenseNum","expirationDate","licenseStatus"};

	public static void main(String[] args) throws JauntException, IOException{
		//This hides all the warnings produced by HtmlUnit
		java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF); 
		ArMedicalParser p = new ArMedicalParser();
		List<Doctor> docList = p.execute(RADIO, QUERY);
		printDoctorList(docList, OUTPUT_FILE_NAME);
	}
	
	/**
	 * Prints a List<Doctor> into a CSV file.
	 * 
	 * @param doctors				List of doctors to be printed
	 * @param fileName			Name of output file
	 * @throws IOException
	 */
	private static void printDoctorList(List<Doctor> doctors, String fileName) throws IOException {
		if(doctors.size() != 0) {
			System.out.println("Printing results...");
			FileWriter FW = new FileWriter(fileName);
			CSVPrinter CSVFP = new CSVPrinter(FW, CSV_FILE_FORMAT);
			CSVFP.printRecord(HEADER);
			for(Doctor d : doctors) {
				CSVFP.printRecord(d.asList());
			}
			System.out.println("Results printed to " + OUTPUT_FILE_NAME);
			FW.flush();
			FW.close();
			CSVFP.close();
		} else {
			switch(RADIO) {
			case 0:
				System.out.println("No doctors found with the license number " + QUERY);
				break;
			case 1:
				System.out.println("No doctors found whose last name begins with \"" + QUERY + "\"");
				break;
			}	
		}
	}
}
