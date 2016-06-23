import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.util.List;
import java.io.FileWriter;
import java.io.IOException;

public class Test {
	
	private static FileWriter FW = null;
	private static CSVPrinter CSVFP = null;
	private static final CSVFormat CSV_FILE_FORMAT = CSVFormat.DEFAULT.withRecordSeparator("\n");
	private static final Object [] HEADER = 
		{"name","city","state","zip","licenseNum","expirationDate","licenseStatus"};

	public static void main(String[] args){
			ArMedicalParser p = new ArMedicalParser();
			List<Doctor> docList = p.execute(1, "za");
			try {
				printDoctorList(docList, "results.csv");
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * Prints a list of Doctor objects into a CSV file.
	 * 
	 * @param doctors				List of doctors to be printed
	 * @param fileName			Name of output file
	 * @throws IOException
	 */
	private static void printDoctorList(List<Doctor> doctors, String fileName) throws IOException {
		FW = new FileWriter(fileName);
		CSVFP = new CSVPrinter(FW, CSV_FILE_FORMAT);
		CSVFP = new CSVPrinter(FW, CSV_FILE_FORMAT);
		CSVFP.printRecord(HEADER);
		for(Doctor d : doctors) {
			CSVFP.printRecord(d.asList());
		}
		FW.flush();
		FW.close();
		CSVFP.close();
	}

}
