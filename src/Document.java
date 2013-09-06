import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;


class Document {
	static Properties props;
    static StanfordCoreNLP pipeline;
    public static enum ParseMode {
    	POS,
    	WORD_TOKENIZE,
    	SENT_TOKENIZE,
    	NONE,
    }
    
	String text;
	int index;
	List<Sentence> sentences;
	Map<String, Integer> termCounts = null;
	Set<String> seen = new HashSet<>();

	public static void initialize(ParseMode mode) {
    	System.out.println("started");
		props = new Properties();
		if (mode == ParseMode.POS) {
			props.put("annotators", "tokenize, ssplit, pos");
		} else if (mode == ParseMode.WORD_TOKENIZE) {
			props.put("annotators", "tokenize, ssplit");
		} else if (mode == ParseMode.NONE) {
			props.put("annotators", "");
		}
		pipeline = new StanfordCoreNLP(props);
	}
	
	public Document(String text, int index) {
		this.text = text.replaceAll("\\s", " ");
		this.index = index;
		sentences = new ArrayList<Sentence>();
		List<CoreMap> annotations = getAnnotations();
		if (annotations == null) return;
		
		for(CoreMap sentence: annotations) {
			sentences.add(new Sentence(sentence, sentences.size(), this.index));
		}
	}
	
	public Document(String[] text, int index) {
		//If results are especially poor then use the string splitting done by DUC
		this.text = StringUtils.join(text);
		this.index = index;
		sentences = new ArrayList<Sentence>();
		for(CoreMap sentence: getAnnotations()) {
			sentences.add(new Sentence(sentence, sentences.size(), this.index ));
		}
	}
	
	public Set<String> getUniques() {
		Set<String> terms = new HashSet<String>();
		for (Sentence sentence : sentences) {
			terms.addAll(sentence.uniqueTerms);
		}
		return terms;
	}
	
	public List<String> getTerms() {
		List<String> terms = new ArrayList<String>();
		for (Sentence sentence : sentences) {
			terms.addAll(sentence.uniqueTerms);
		}
		return terms;
	}
	
	public void markAsSeen(String s){
		seen.add(s);
	}
	
	public Map<String, Integer> getTermCounts() {
		if (termCounts == null) {
			Counter count = new Counter();
			for (Sentence sentence : this.sentences) {
				for (String term : sentence.terms) 
				{
					count.add(term);
				}
			}
			termCounts = count.toHashMap();
		}
		return termCounts;
	}
	
	public void reduceTermCounts(Set<String> vocab) {

		Iterator<String> it = termCounts.keySet().iterator();
		while (it.hasNext())
		{
			String term = it.next();
			if (vocab.contains(term) == false)
			{
				it.remove();
			}
		}
	}
	
	private List<CoreMap> getAnnotations() {
		Annotation document = new Annotation(this.text);
	    // run all Annotators on this text
		pipeline.annotate(document);		
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    return sentences;
	}
	
	private ArrayList<Pair<Integer, Integer>> chooseTwo(int n) {
		ArrayList<Pair<Integer, Integer>> combinations = new ArrayList<Pair<Integer, Integer>>();
		for (int i = 0; i < n; i++)
		{
			for (int j = i; j < n; j++){
				combinations.add(new Pair<Integer, Integer>(i, j));
			}
		}
		return combinations;
	}
	
	public Pair<List<Sentence>, Double> getBestSnippet(ScoreFunction func, int minLength, int maxLength, int delta, int windowSize) {
		double highscore = Double.MIN_VALUE;
		List<Sentence> bestSentences = null;
		
		for (Pair<Integer, Integer> boundaries : chooseTwo(sentences.size())) {
			int start = boundaries.first();
			int end = boundaries.second();			
			if ((end - start) > maxLength || (end - start) < minLength ) continue;
			List<Sentence> currentSentences = sentences.subList(start, end);			
			int currentLength = 0;
			boolean seen = false;
			for (Sentence sentence : currentSentences){
				currentLength += sentence.terms.size();
				seen |= sentence.isSeen();
			}
			if (seen) continue;
			if (currentLength > windowSize) continue;
			//Set<String> uniqueTerms = new HashSet<String>();
			List<String> uniqueTerms = new ArrayList<String>();
			for (Sentence sentence : currentSentences){
				uniqueTerms.addAll(sentence.terms);	
			}
			double score = func.getScore(uniqueTerms, this.index) / (currentLength + delta);
			if (score > highscore) {
				highscore = score;			
				
				bestSentences = currentSentences;
			}
		}
		return new Pair<List<Sentence>, Double>(bestSentences, highscore);
	}
	
	public Pair<List<Sentence>, Double> getBestSentiment (ScoreFunction func, int sentiment, int minLength, int maxLength, int delta, int windowSize) {
		double highscore = Double.MIN_VALUE;
		List<Sentence> bestSentences = null;
		
		for (Pair<Integer, Integer> boundaries : chooseTwo(sentences.size())) {
			int start = boundaries.first();
			int end = boundaries.second();			
			if ((end - start) > maxLength || (end - start) < minLength ) continue;
			List<Sentence> currentSentences = sentences.subList(start, end);			
			int currentLength = 0;
			boolean seen = false;
			for (Sentence sentence : currentSentences){
				currentLength += sentence.terms.size();
				seen |= sentence.isSeen();
			}
			if (seen) continue;
			if (currentLength > windowSize) continue;
			List<String> allTerms = new ArrayList<String>();
			List<String> poss = new ArrayList<String>();
			for (Sentence sentence : currentSentences){
				allTerms.addAll(sentence.terms);	
				poss.addAll(sentence.posTags);
			}
			
			double score = func.getScore(allTerms, poss, sentiment) / (currentLength + delta);
			if (score > highscore) {
				highscore = score;				
				bestSentences = currentSentences;
			}
		}
		return new Pair<List<Sentence>, Double>(bestSentences, highscore);
	}
}