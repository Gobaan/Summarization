import static java.nio.file.FileVisitResult.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.StringUtils;

public class LexRankVisitor extends SimpleFileVisitor<Path> {
	JSONParser parser = new JSONParser();
	String[] related;
	Documents docs;
	long dirStart;
	Extractor extractor = new Extractor();
	
	//static int id = 0;
	final static String ROOTDIR = "/home/graveendran/Downloads/DUC";
	//final static String ROOTDIR = "/home/graveendran/Results";

	static Properties props = new Properties();
	static StanfordCoreNLP pipeline;
	static Printer p;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Document.initialize(Document.ParseMode.NONE);
		
		int suffix = 0;
		
		String fname = "LexRank";
		String docset = ".Crawler";
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			docset = ".DUC";
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
		
		Path startingDir = FileSystems.getDefault().getPath(ROOTDIR);
		try {
			long start = System.currentTimeMillis();
			Files.walkFileTree(startingDir, new LexRankVisitor());
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
		
		String name = "" + dir;
		String[] comps = name.split("/");
		String tmp = comps[comps.length - 1].toLowerCase();
		related = tmp.split(Pattern.quote("+"));
		if (ROOTDIR.equals("/home/graveendran/Downloads/DUC")) {
			related = new String[0];
		}    

		p.startRow();
		
		dirStart = System.currentTimeMillis();
		return CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
			throws IOException {
		if (dir.toString().equalsIgnoreCase(ROOTDIR))
			return CONTINUE;
		String name = "" + dir;
		String[] comps = name.split("/");
		String tmp = comps[comps.length - 1].toLowerCase();
		
		if (related.length == 0) {
			p.addColumn(tmp, Printer.Type.BOTH);
		} else {
			p.addColumn(StringUtils.join(related, " "), Printer.Type.BOTH);
		}
		
		if (this.docs.documents.size() > 5000 ) {
			System.out.println("Too many files, skipping");
			p.addColumn(docs.documents.size(), Printer.Type.BOTH);
			p.addColumn(docs.numSentences(), Printer.Type.BOTH);								
			p.addColumn("0", Printer.Type.BOTH);
			p.addColumn("0", Printer.Type.TIMING);
			p.addColumn("Too Large", Printer.Type.TEXT);
			p.endRow();
			return CONTINUE;
		}
		
		
		File file = new File("LexRankFiles/Input");        
        String[] myFiles = file.list();  
        for (int i=0; i<myFiles.length; i++) {  
            File myFile = new File(file, myFiles[i]);   
            myFile.delete();  
        }   
        System.out.println(file.list().length);
        file = new File("Results/LexRank.INPU");
        if (file.exists()) {
        	file.delete();
        } else { 
        	System.out.println("Missing file");
        }
		int i = 0;
		
		for (Document d: this.docs.documents) {
			// Write Vocab
	    	file = new File("LexRankFiles/Input", ""+i);
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write("<DOC>\n");
			bw.write("<DOCNO>\n");
			bw.write(i + "\n");
			bw.write("</DOCNO>\n");
			bw.write("<DOCTYPE>NEWS</DOCTYPE>\n");
			bw.write("<TXTTYPE>NEWSWIRE</TXTTYPE>\n");
			bw.write("<TEXT>" + d.text + "</TEXT>\n");
			bw.write("</DOC>");			
			bw.close();
			i++;
		}		
		String[] cmd = { "java", "dragon.config.SummarizationEvaAppConfig", "/home/graveendran/workspace/StanfordNLPTest/sumcfg2005.xml",  "1" };
        Process proc;				
		proc = new ProcessBuilder(cmd).redirectError(Redirect.INHERIT).redirectOutput(Redirect.INHERIT).start();
        //proc = new ProcessBuilder(cmd).start();
        try {
			proc.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done Execution");
		
		File f = new File("Results/LexRank.INPU");
		List<String> summary = new ArrayList<String>();
		if (f.exists()) {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line;
			 
			while ((line = br.readLine()) != null) {
				summary.add(line);
			}
			br.close();
		} else {
			summary.add("Missing summary");
			System.out.println("Failed");
		}
		
		
		p.addColumn(docs.documents.size(), Printer.Type.BOTH);
		p.addColumn(docs.numSentences(), Printer.Type.BOTH);
		
		long totalWords = 0;		
		long executionTime = System.currentTimeMillis() - dirStart;
		
		p.addColumn(totalWords, Printer.Type.BOTH);
		p.addColumn(executionTime, Printer.Type.TIMING);
		p.addColumn(StringUtils.join(summary, " ").trim(), Printer.Type.TEXT);
		p.endRow();
		return CONTINUE;
	}
	
	public void writeFile(Path file, String text) {
		System.out.println(file);
		
	}
	
	// Read each file, recreate it in 2004 format
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		if (attr.isRegularFile()) {
			String name = "" + file;
			if (name.endsWith(".txt")) {
				for (JSONArray comments : extractor.extractFromCrawler(file, related)) {
					this.docs.addJSONComments(comments);
				}
			} else if (Character.isDigit(name.charAt(name.length() - 1))) {
				this.docs.addComments(extractor.extractFromDUC(file));
			}
		}
		return CONTINUE;
	}
}