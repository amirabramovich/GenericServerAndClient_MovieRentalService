package bgu.spl181.net.srv.bidi;

import java.util.Comparator;
/**
 * This class represents the comparator we use in our heapqueue managing the movie ids
 * Each time movie added we give ti maxid+1
 * @author chen
 *
 */
public class MaxHeapComp implements Comparator<Integer> {

	@Override
	/**
	 * This method comapres two movie ids 
	 * @return difference
	 */
    public int compare(Integer a, Integer b) {
        return b - a; 
    }

}
