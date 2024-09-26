package mesquite.gataga.FastaToGCListing;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.UtilitiesAssistant;
import mesquite.categ.lib.*;
import mesquite.molec.lib.*;

public class FastaToGCListing extends UtilitiesAssistant {
	long A = CategoricalState.makeSet(0);
	long C = CategoricalState.makeSet(1);
	long G = CategoricalState.makeSet(2);
	long T = CategoricalState.makeSet(3);
	long AT = A | T;
	long CG =  C | G;
	long ACGT = A | C | G | T;

	public boolean startJob(String arguments, Object condition,  boolean hiredByName) {
		addMenuItem(null, "Convert FASTA to GC Listing...", makeCommand("convert", this));
		return true;
	}

	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Convert FASTA to GC Listing", null, commandName, "convert")) {
			startConvert();
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	public void initialize() {
	}

	/*.................................................................................................................*/
	public boolean startConvert(){
		initialize();
		MesquiteString originalDirPath = new MesquiteString();
		MesquiteString originalFileName = new MesquiteString();
		String originalFilePath = MesquiteFile.openFileDialog("Choose file to be converted:", originalDirPath, originalFileName);
		String originalFileContents = "";
		if (!StringUtil.blank(originalFilePath)) {
			originalFileContents = MesquiteFile.getFileContentsAsString(originalFilePath);
			if (!StringUtil.blank(originalFileContents)) {
				return convertToGCListing(originalFileContents, originalFileName.getValue());
			}
		}
		else {
			return false;
		}
		return false;
	}

	/*.................................................................................................................*/
	public boolean convertToGCListing(String fileContents, String originalFileName){
		String convertedFilePath = MesquiteFile.saveFileAsDialog("Save GC Listing file as:");
		if (StringUtil.blank(fileContents) || StringUtil.blank(convertedFilePath))
			return false;

		StringBuffer outputBuffer = new StringBuffer(10000);

		Parser parser = new Parser(fileContents);
		Parser subParser = new Parser();
		long pos = 0;

		StringBuffer sb = new StringBuffer(1000);
		sb.append(parser.getRawNextLine());
		String line = sb.toString();

		boolean abort = false;
		subParser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
		subParser.setPunctuationString(">");
		parser.setPunctuationString(">");
		String token = subParser.getFirstToken(line); //should be >

		while (!StringUtil.blank(line) && !abort) {

			//parser.setPunctuationString(null);

			token = subParser.getRemaining();  //taxon Name


			line = parser.getRemainingUntilChar('>', true);
			if (line==null) break;
			subParser.setString(line); 
			long s = 0;
			int ic = 0;

			int tot = 0;
			int count = 0;
			double value = 0.0;

			while (subParser.getPosition()<line.length()) {
				char c=subParser.nextDarkChar();
				if (c!= '\0') {
					s = DNAState.fromCharStatic(c);
					if (!CategoricalState.isUnassigned(s) && !CategoricalState.isInapplicable(s)) {
						if (s == A || s == T || s == AT) //monomorphic A or T or A&T or uncertain A or T
							tot++;
						else if (s == C || s == G || s == CG) { //monomorphic C or G or C&G or uncertain C or G
							tot++;
							count++;
						}
					
				}
				}
				ic += 1;
			}
			if (tot == 0)
				value = 0;
			else
				value = ((double)count)/tot;
			outputBuffer.append(token+"\t"+value+"\n");

			line = parser.getRawNextDarkLine();
			pos = parser.getPosition();

			subParser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
		}




		if (!StringUtil.blank(outputBuffer.toString())) 
			MesquiteFile.putFileContents(convertedFilePath, outputBuffer.toString(), true);
		return true;
	}


	public String getName() {
		return "Convert FASTA to GC Listing";
	}


}
