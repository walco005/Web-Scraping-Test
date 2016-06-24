import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
	/*
	 * The warnings in the console at the start and on each new page are all due to HtmlUnit.
	 * 
	 * RADIO is which radio button is selected, 0 is License Number and 1 is Last Name.
	 * QUERY is the query used in the search.
	 */
	private final static int RADIO = 1;
	private final static String QUERY = "za";
	private final static String OUTPUT_FILE_NAME = "results.csv";
	private static final CSVFormat CSV_FILE_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n");
	private static final Object [] HEADER = 
		{"name","city","state","zip","licenseNum","expirationDate","licenseStatus"};

	public static void main(String[] args){
			ArMedicalParser p = new ArMedicalParser();
			List<Doctor> docList = p.execute(RADIO, QUERY);
			if(docList != null) {
				try {
					printDoctorList(docList, OUTPUT_FILE_NAME);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
	}
	/**
	 * Prints a List<Doctor> into a CSV file.
	 * 
	 * @param doctors				List of doctors to be printed
	 * @param fileName			Name of output file
	 * @throws IOException
	 */
	private static void printDoctorList(List<Doctor> doctors, String fileName) throws IOException {
		FileWriter FW = new FileWriter(fileName);
		CSVPrinter CSVFP = new CSVPrinter(FW, CSV_FILE_FORMAT);
		CSVFP.printRecord(HEADER);
		for(Doctor d : doctors) {
			CSVFP.printRecord(d.asList());
		}
		FW.flush();
		FW.close();
		CSVFP.close();
	}

}
