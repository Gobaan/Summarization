import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LDAScoreFunction implements ScoreFunction {	
	Map<Integer, Map<String, Double>> pwt;
	Map<Integer, List<Double>> ptd;
	int topic = 0;
	double prior;
	double mixture;
	int numTopics;
	
	public LDAScoreFunction (double prior, double mixture) {
		this.prior = prior;
		this.mixture = 0.99;
		this.pwt = new HashMap<>();
		this.ptd = new HashMap<>();
		this.numTopics = 0;
	}
	
	public double getScore(Collection<String> terms, int index) {
		double score = 0;
		
		for (String term: terms)
		{
			Map<String, Double> pt = pwt.get(topic);
			if (pt.containsKey(term)) {
			  score += (pwt.get(topic).get(term) * ptd.get(topic).get(index));
			}
		}

		return score != 0 ? score : Double.MIN_VALUE;
	}
	
	public void update(List<String> terms)
	{
		topic = (topic + 1) % numTopics;
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(Documents docs) {
	   
	}
	
	/**
	 * @param args
	 */
	public void setForegroundCorpus(File theta, File phi, String[] vocab, int numTopics) {
		try {
			System.out.println("Loading foreground");
			this.numTopics = numTopics;
			FileReader fr = new FileReader(phi);
			BufferedReader br = new BufferedReader(fr);
			for (int i = 0; i < numTopics; i++) {
				String line = br.readLine();
				pwt.put(i, new HashMap<String, Double>());
				int idx = 0;
				for (String score:line.split(" ")) {					
					double mixedScore = mixture * Double.parseDouble(score) + (1 - mixture) * prior;
					pwt.get(i).put(vocab[idx], mixedScore);
					idx += 1;
				}				
			}
			br.close();
			
			for (int i = 0; i < numTopics; i++)
			{
				ptd.put(i, new ArrayList<Double>());
			}
			fr = new FileReader(theta);
			br = new BufferedReader(fr);
			String line;
			while ((line = br.readLine()) != null) {
				int i = 0;
				for (String score:line.split(" ")) {						
					double mixedScore = Double.parseDouble(score) + (1 - mixture) * prior;
					ptd.get(i++).add(mixedScore);
				}				
			}
			br.close();
		} catch (IOException  e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public void setBackgroundCorpus() {
		// There is no background model for sumbasic
	}
	
	public double getScore(List<String> terms, List<String> pos, int index) { return Double.MIN_VALUE; };
}
