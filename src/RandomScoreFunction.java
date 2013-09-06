import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomScoreFunction implements ScoreFunction {	
	Random r = new Random(9999);
	public RandomScoreFunction () {
		
	}
	
	public double getScore(Collection<String> terms, int index) {		
		return r.nextDouble();
	}
	
	public void update(List<String> terms)
	{
		
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(Documents docs) {
	   
	}
	

	/**
	 * @param args
	 */
	public void setBackgroundCorpus() {
		// There is no background model for sumbasic
	}
	
	public double getScore(List<String> terms, List<String> pos, int index) { return Double.MIN_VALUE; };
}
