import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Counter {
	HashMap<String, SingleCounter> counts;
	public Counter() { counts = new HashMap<String, SingleCounter>(); }
	
	public void add(String term) {
		add(term, 1);
	}	
	
	public void add(String term, int n) {
		if (counts.get(term) == null) {
			counts.put(term, new SingleCounter(term));
		}
		counts.get(term).value += n;
	}
	
	public HashMap<String, Integer> toHashMap() {
		HashMap<String, Integer> temp = new HashMap<String, Integer>();
		for (Map.Entry<String, SingleCounter> e : counts.entrySet()) {
			temp.put(e.getKey(), new Integer(e.getValue().value));
		}
		return temp;
	}
	
	public List<SingleCounter> topN(int n) {
		List<SingleCounter> topCounter = new ArrayList<SingleCounter>(counts.values());
		Collections.sort(topCounter);
		return topCounter.subList(0, Math.min(topCounter.size(), n));
	}
}

/* We wrap ints instead of Integers so we arent constantly creating new objects */
class SingleCounter implements Comparable<SingleCounter> {
	public int value;
	public String term;
	public SingleCounter(String term) {
		this.term = term;
		this.value = 0; 
	}
	
	@Override
	public int compareTo(SingleCounter that) {
		final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;
	    
		// TODO Auto-generated method stub
		if (this == that) return EQUAL;
		if (this.value < that.value) return AFTER;
	    if (this.value > that.value) return BEFORE;
		return EQUAL;
	}
}