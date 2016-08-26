import java.io.IOException;
import java.util.List;

import com.jaunt.JauntException;

/**
 * An interface for a class that parses web sites.
 * @author walco005
 * @param <T>
 */
public interface WebsiteParser<T> {
	/**
	 * Executes the parser with the given value and request
	 * @param val						What you want to search for (last name/license number)
	 * @param request				What you are searching for
	 * @throws JauntException 
	 * @throws IOException 
	 */
	List<T> execute (int val, String request) throws IOException, JauntException;
}
