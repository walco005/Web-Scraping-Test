import java.util.ArrayList;
import java.util.List;

/**
 * The Doctor class stores information about a doctor, meant to be used in a parser.
 * @author walco005
 */
 
public final class Doctor {
	private String name = "";
	private String city = "";
	private String state = "";
	private String zip = "";
	private String mailingAddress = "";
	private String licenseNum = "";
	private String expDate = "";
	private String licenseStatus = "";
	private int amountOfOccurences = 0;
	
	public Doctor() {
	}
	public Doctor(String name, String city, String state, String zip, String mailingAddress,
			String licenseNum, String expDate, String licenseStatus) {
		this.name = name;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.mailingAddress = mailingAddress;
		this.licenseNum = licenseNum;
		this.expDate = expDate;
		this.licenseStatus = licenseStatus;
	}
	public Doctor(String name, String city, String state, String zip,
			String licenseNum, String expDate, String licenseStatus) {
		this.name = name;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.licenseNum = licenseNum;
		this.expDate = expDate;
		this.licenseStatus = licenseStatus;
	}
	public String getName() {
		return name;
	}
	public String getCity() {
		return city;
	}
	public String getState() {
		return state;
	}
	public String getZip() {
		return zip;
	}
	public String getMailingAddress() {
		return mailingAddress;
	}
	public String getLicenseNum() {
		return licenseNum;
	}
	public String getExpDate() {
		return expDate;
	}
	public String getLicenseStatus() {
		return licenseStatus;
	}
	public int getAmount() {
		return amountOfOccurences;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public void setState(String state) {
		this.state = state;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
	public void setMailingAddress(String mailingAddress) {
		this.mailingAddress = mailingAddress;
	}
	public void setLicenseNum(String licenseNum) {
		this.licenseNum = licenseNum;
	}
	public void setExpirationDate(String expDate) {
		this.expDate = expDate;
	}
	public void setLicenseStatus(String licenseStatus) {
		this.licenseStatus = licenseStatus;
	}
	public void setAmount(int amount) {
		this.amountOfOccurences = amount;
	}

	/**
	 * Sets all the values of the Doctor object.
	 * @param name
	 * @param city
	 * @param state
	 * @param zip
	 * @param licenseNum
	 * @param expDate
	 * @param licenseStatus
	 * @param mailingAddress
	 */
	public void setAll(String name, String city, String state, String zip, String licenseNum, 
			String expDate, String licenseStatus, String mailingAddress) {
		this.name = name;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.licenseNum = licenseNum;
		this.expDate = expDate;
		this.licenseStatus = licenseStatus;
		this.mailingAddress = mailingAddress;
	}

	@Override
	public String toString() {
		return "Doctor[name = " + name + ", city = " + city + ", state = " + state + ", zip = " + zip
						+ ", licenseNum = " + licenseNum + ", expDate = " + expDate
						+ ", licenseStatus = " + licenseStatus + "]";
	}
	
	/**
	 * Returns the object Doctor as a list of strings.
	 * @return returnList		A list containing all the relevant values of the Doctor object.
	 */
	public List<String> asList() {
		List<String> returnList = new ArrayList<String>();
		returnList.add(name);
		returnList.add(city);
		returnList.add(state);
		returnList.add(zip);
		returnList.add(licenseNum);
		returnList.add(expDate);
		returnList.add(licenseStatus);
		return returnList;
	}
}
