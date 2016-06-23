import java.util.List;

/**
 * An interface for a class that parses websites.
 * 
 * @author walco005
 * @param <T>
 */
public interface WebsiteParser<T> {
	/**
	 * Executes the parser with the 
	 * @param val
	 * @param request
	 * @param fileName
	 * @return
	 */
	List<T> execute (int val, String request);
}
