package bgu.spl181.net.srv.bidi;
/**
 * This class represents Pair of id and name of movie
 * It is created to conviniently store data representing movie user holds
 * as spesified in userJson supplied movies is array of pairs of id and namez
 * @author chen
 *
 */
public class Pair {

	private String id;
	private String name;
	/**
	 * Constructor
	 * @param id 
	 * @param name
	 */
	public Pair(int id, String name) {
		this.id=String.valueOf(id);
		this.name=name;
	}
	/**
	 * This method retrieves name
	 * @return name
	 */
	public String getName() {
		return name;
	}
}
