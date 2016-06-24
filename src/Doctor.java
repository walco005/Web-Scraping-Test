import java.util.ArrayList;
import java.util.List;

//The Doctor class stores information taken from a doctor's webpage
 
public final class Doctor {
	private String name = "";
	private String city = "";
	private String state = "";
	private String zip = "";
	private String licenseNum = "";
	private String expDate = "";
	private String licenseStatus = "";
	private int amountOfOccurences = 0;
	
	public Doctor() {
	}
	public Doctor(String name, String city, String state, String zip, String licenseNum, 
			String expDate, String licenseStatus) {
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
	 * Sets all the values of the Doctor at once, in the same order as the constructor.
	 */
	public void setAll(String name, String city, String state, String zip, String licenseNum, 
			String expDate, String licenseStatus) {
		this.name = name;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.licenseNum = licenseNum;
		this.expDate = expDate;
		this.licenseStatus = licenseStatus;
	}
	/**
	 * Increments the amount by one, meaning this doctor already showed up in the search.
	 */
	public void addAmount(int i) {
		this.amountOfOccurences += i;
	}
	
	@Override
	public String toString() {
		return "Doctor[name = " + name + ", city = " + city + ", state = " + state + ", zip = " + zip
						+ ", licenseNum = " + licenseNum + ", expDate = " + expDate
						+ ", licenseStatus = " + licenseStatus + "]";
	}
	
	/**
	 * Returns the object Doctor as a list of strings.
	 * 
	 * @return ret		A list containing all the values of the Doctor object.
	 */
	public List<String> asList() {
		List<String> ret = new ArrayList<String>();
		ret.add(name);
		ret.add(city);
		ret.add(state);
		ret.add(zip);
		ret.add(licenseNum);
		ret.add(expDate);
		ret.add(licenseStatus);
		return ret;
	}
}
