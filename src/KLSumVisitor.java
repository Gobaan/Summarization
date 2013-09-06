import static java.nio.file.FileVisitResult.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;

public class KLSumVisitor extends SimpleFileVisitor<Path> {
	JSONParser parser = new JSONParser();
	String[] related;
	Documents docs;
	long dirStart;
	Extractor extractor = new Extractor();
	//final static String ROOTDIR = "/home/graveendran/Downloads/DUC";
	final static String ROOTDIR = "/home/graveendran/Results";

	static Properties props = new Properties();
	static StanfordCoreNLP pipeline;
	static ScoreFunction func;
	static Printer p;
	static ScoreFunctionFactory.Mode mode = ScoreFunctionFactory.Mode.SUMBASIC;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		

		func = ScoreFunctionFactory.getScoreFunction(mode);
		int suffix = 0;
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			suffix = 1;
		}
		if (mode == ScoreFunctionFactory.Mode.KLSUM) {
			((KLSumPlusScoreFunction) func)
					.setBackgroundCorpus(ROOTDIR, suffix);
		}
		String fname = null;
		String docset = ".Crawler";
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			docset = ".DUC";
		}
		if (mode == ScoreFunctionFactory.Mode.RANDOM) {
			fname = "Random";
			Document.initialize(Document.ParseMode.WORD_TOKENIZE);
		} else if (mode == ScoreFunctionFactory.Mode.KLSUM) {
			fname = "KLSum";
			Document.initialize(Document.ParseMode.WORD_TOKENIZE);
		} else if (mode == ScoreFunctionFactory.Mode.SUMBASIC) {
			fname = "SumBasic";
			Document.initialize(Document.ParseMode.WORD_TOKENIZE);
		}
		fname += docset;
		p = new Printer(Printer.Type.BOTH, fname);
		p.startRow();
		p.addColumn("Topic", Printer.Type.BOTH);
		p.addColumn("Num Comments", Printer.Type.BOTH);
		p.addColumn("Num Sentences", Printer.Type.BOTH);
		p.addColumn("Summary Word Count", Printer.Type.BOTH);
		p.addColumn("Execution Time", Printer.Type.TIMING);
		p.addColumn("Summary", Printer.Type.TEXT);
		p.endRow();

		props.put("annotators", "tokenize, ssplit");
		pipeline = new StanfordCoreNLP(props);

		Path startingDir = FileSystems.getDefault().getPath(ROOTDIR);
		try {
			long start = System.currentTimeMillis();
			Files.walkFileTree(startingDir, new KLSumVisitor());
			long end = System.currentTimeMillis();
			System.out.println(end - start);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		docs = new Documents();
		System.gc();
		dirStart = System.currentTimeMillis();
		String name = "" + dir;
		String[] comps = name.split("/");
		String tmp = comps[comps.length - 1].toLowerCase();
		related = tmp.split(Pattern.quote("+"));
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			related = new String[0];
		}

		p.startRow();
		return CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {

		String name = "" + dir;
                if (!name.toLowerCase().contains("sopa")) {
                	return CONTINUE;
                }
		if (name.equalsIgnoreCase(ROOTDIR))
			return CONTINUE;
		String[] comps = name.split("/");
		String tmp = comps[comps.length - 1].toLowerCase();
		func.setForegroundCorpus(docs);
		Sentence.resetSeen();
		if (related.length == 0) {
			p.addColumn(tmp, Printer.Type.BOTH);
		} else {
			p.addColumn(StringUtils.join(related, " "), Printer.Type.BOTH);
		}
		p.addColumn(docs.documents.size(), Printer.Type.BOTH);
		p.addColumn(docs.numSentences(), Printer.Type.BOTH);

		long totalWords = 0;
		List<String> summary = new ArrayList<String>();
		while (totalWords < 100) {
			List<Sentence> sentences;
			if (mode == ScoreFunctionFactory.Mode.KLSUM) {
				((KLSumPlusScoreFunction) func).printTopTerms(10);
				sentences = docs.getBestSnippet(func, 2, 5, 35, 90);
			} else {
				((SumBasicScoreFunction) func).printTopTerms(5);
				sentences = docs.getBestSnippet(func, 1, 1, 0, 99999999);
			}
			
			if (sentences == null)	break;
			for (Sentence sentence : sentences) {
				summary.add(sentence.sentence);
				totalWords += sentence.terms.size();
				sentence.setSeen();
				func.update(sentence.terms);
				System.out.println(sentence);
			}
			System.out.println("-----------------------------------------------------");
		}
		long executionTime = System.currentTimeMillis() - dirStart;

		p.addColumn(totalWords, Printer.Type.BOTH);
		p.addColumn(executionTime, Printer.Type.TIMING);
		p.addColumn(StringUtils.join(summary, " "), Printer.Type.TEXT);
		p.endRow();
		return CONTINUE;
	}

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		String name = "" + file;
		
		if (!name.toLowerCase().contains("sopa")) {
			return CONTINUE;
		}
		if (attr.isRegularFile()) {
			
			if (name.endsWith(".txt")) {
				for (JSONArray comments : extractor.extractFromCrawler(file,
						related)) {
					this.docs.addJSONComments(comments);
				}
			} else if (Character.isDigit(name.charAt(name.length() - 1))) {
				this.docs.addComments(extractor.extractFromDUC(file));
			}
		}

		return CONTINUE;
	}
}
