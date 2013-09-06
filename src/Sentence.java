import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.util.CoreMap;


class Sentence {
	String sentence;
	int index, docnum;
	List<String> terms;
	List<String> posTags;
	Set<String> uniqueTerms;
	static Set<String> seen;
	
	enum PhraseType {
		NOUN,
		PUNCTUATION,
		OTHER
	}
	
	public Phrases phrases;
	
	public static void resetSeen() {
		seen = new HashSet<String>();
	}
	
	public boolean isSeen() {
		return seen.contains(sentence);
	}

	@SuppressWarnings("static-access")
	public void setSeen() {
		this.seen.add(sentence);
	}

	public Sentence (CoreMap annotatedSentence, int index, int docnum)
	{
		this.sentence = annotatedSentence.get(TextAnnotation.class);
		this.terms = new ArrayList<String>();
		this.posTags = new ArrayList<String>();
		this.index = index;
		this.docnum = docnum;
		for (CoreLabel token: annotatedSentence.get(TokensAnnotation.class)) {
			String term = token.get(TextAnnotation.class);
			String pos = token.get(PartOfSpeechAnnotation.class);
			this.terms.add(term.toLowerCase());
			this.posTags.add(pos);
		} 
		this.uniqueTerms = new HashSet<String>(terms);
		this.phrases = new Phrases();
		if (this.posTags.size() > 0 && this.posTags.get(0) != null) {
			setPhrases();
		}
	}
	
	public String toString() {
		return this.sentence;
	}
	
	public PhraseType getType(int termNum) {
		String pos = posTags.get(termNum);
		
		if (pos.startsWith("NN") || pos.startsWith("JJ")) return PhraseType.NOUN;
		else if (Pattern.matches("\\p{Punct}", this.terms.get(termNum))) return PhraseType.PUNCTUATION;
		return PhraseType.OTHER;
	}
	
	public void setPhrases() {
		List<TermPos> temp = new ArrayList<TermPos>();
		int i = 0;
		int size = this.terms.size();
		while (i < size) {
			PhraseType p;
			while (i < size && ((p = getType(i)) != PhraseType.OTHER)) {
				if (p == PhraseType.NOUN) {
					temp.add(new TermPos(terms.get(i), posTags.get(i), "BNP"));
				}
				i++;
			}
			phrases.addPhrase(temp);
			
			temp = new ArrayList<TermPos>();
			while (i < size && ((p = getType(i)) != PhraseType.NOUN)) {
				if (p == PhraseType.OTHER) {
					temp.add(new TermPos(terms.get(i), posTags.get(i), "OTHER"));
				}
				i++;
			}		
			phrases.addPhrase(temp);
		}		
	}
}