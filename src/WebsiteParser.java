import java.io.IOException;
import java.util.List;

import com.jaunt.JauntException;

/**
 * An interface for a class that parses websites.
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
	 * @throws JauntException 
	 * @throws IOException 
	 */
	List<T> execute (int val, String request) throws IOException, JauntException;
}
