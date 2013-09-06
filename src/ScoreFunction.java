import java.util.List;
import java.util.Collection;


interface ScoreFunction {
	public double getScore(Collection<String> terms, int index);	
	public void setForegroundCorpus(Documents docs);
	public void update(List<String> terms);
	public double getScore(List<String> terms, List<String> pos, int index);
}
