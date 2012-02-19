
package mesquite.gataga.IntrepretFASTAtoNucFreq;
/*~~  */

import java.util.*;
import java.awt.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.cont.lib.*;
import mesquite.categ.lib.*;
import mesquite.molec.lib.*;

public class IntrepretFASTAtoNucFreq extends FileInterpreterI {
	long A = CategoricalState.makeSet(0);
	long C = CategoricalState.makeSet(1);
	long G = CategoricalState.makeSet(2);
	long T = CategoricalState.makeSet(3);
	long AT = CategoricalState.setUncertainty(A | T, true);
	long CG =  CategoricalState.setUncertainty(C | G, true);
	long ACGT = CategoricalState.setUncertainty(A | C | G | T,true);

	static int charLength = 0;
	static int charA = 1;
	static int charC = 2;
	static int charG = 3;
	static int charT = 4;
	static int charAT = 5;
	static int charCG = 6;
	static int numChars = 7;

	Class[] acceptedClasses;

	static final int DONTBLAST = 0;
	static final int BLAST = 1;
	static final int BLASTX = 2;
	int blastOption = DONTBLAST;
	int lowerBlastSequenceLength = 1000;
	boolean saveTopHits = false;

	int numHits = 5;


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		acceptedClasses = new Class[] {ContinuousState.class};
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease() {  
		return true;  
	}
	/*.................................................................................................................*/
	public boolean canExportEver() {  
		return false;  
	}
	/*.................................................................................................................*/
	public boolean canExportProject(MesquiteProject project) {  
		return false;
	}
	/*.................................................................................................................*/
	public boolean canExportData(Class dataClass) {  
		return false; 
	}


	/*.................................................................................................................*/
	public boolean canImport(){
		return true;
	}

	/** Returns whether the module can read (import) files considering the passed argument string (e.g., fuse) */
	public boolean canImport(String arguments){
		return true;
	}

	/*.................................................................................................................*/
	public CharacterData createData(CharactersManager charTask, Taxa taxa) {  
		return charTask.newCharacterData(taxa, numChars, ContinuousData.DATATYPENAME);  //
	}
	/*.................................................................................................................*/
	public void readFileCore(Parser parser, MesquiteFile file, ContinuousData data, Taxa taxa, int lastTaxonNumber, ProgressIndicator progIndicator, String arguments) {
		boolean wassave = data.saveChangeHistory;
		data.saveChangeHistory = false;
		Parser subParser = new Parser();
		long pos = 0;
		StringArray accessionNumbers = new StringArray(numHits);

		StringBuffer sb = new StringBuffer(1000);
		if (file!=null)
			file.readLine(sb);
		else
			sb.append(parser.getRawNextLine());
		String line = sb.toString();
		int taxonNumber = -1;

		boolean abort = false;
		subParser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
		subParser.setPunctuationString(">");
		parser.setPunctuationString(">");
		String token = subParser.getFirstToken(line); //should be >
		int charAdded = 0;
		int numFilledChars = data.getNumChars();
		boolean added = false;
		boolean replaceExisting = false;
		StringBuffer response = new StringBuffer();
		StringBuffer blastReport = new StringBuffer();
		StringBuffer fastaBLASTResults = new StringBuffer();
		boolean storeBlastSequences = saveTopHits;
		String pathForBLASTfiles = null;
		if (blastOption==BLAST){
			loglnEchoToStringBuffer("\n============\nBLAST Search", blastReport);
		} else if (blastOption==BLASTX){
			loglnEchoToStringBuffer("\n============\nBLASTX Search", blastReport);
		}
		if (blastOption==BLAST|| blastOption==BLASTX){
			loglnEchoToStringBuffer("   Blasting sequences at least "+ lowerBlastSequenceLength + " bases long", blastReport);
			if (saveTopHits&& StringUtil.blank(pathForBLASTfiles)) {
				pathForBLASTfiles = MesquiteFile.chooseDirectory("Choose directory to store top BLAST hits: ");
				if (StringUtil.blank(pathForBLASTfiles))
					storeBlastSequences = false;
				else if (!pathForBLASTfiles.endsWith(MesquiteFile.fileSeparator))
					pathForBLASTfiles+=MesquiteFile.fileSeparator;
			}
		}


		data.setCharacterName(charLength, "length");
		data.setCharacterName(charA, "A");
		data.setCharacterName(charC, "C");
		data.setCharacterName(charG, "G");
		data.setCharacterName(charT, "T");
		data.setCharacterName(charAT, "AT");
		data.setCharacterName(charCG, "CG");

		while (!StringUtil.blank(line) && !abort) {

			//parser.setPunctuationString(null);

			token = subParser.getRemaining();  //taxon Name
			String sequenceName = token;
			taxonNumber = taxa.whichTaxonNumber(token);
			replaceExisting = false;


			if (data.getNumTaxa()<=lastTaxonNumber) {
				int numTaxaAdded = 1;
				if (lastTaxonNumber>10000)
					numTaxaAdded=500;
				else if (lastTaxonNumber>5000)
					numTaxaAdded=200;
				else if (lastTaxonNumber>2500) 
					numTaxaAdded=100;
				else if (lastTaxonNumber>1000)
					numTaxaAdded=50;
				else if (lastTaxonNumber>500)
					numTaxaAdded=10;
				taxa.addTaxa(lastTaxonNumber-1, numTaxaAdded, false);
				added=true;
				data.addTaxa(lastTaxonNumber-1, numTaxaAdded);
			}
			taxonNumber = lastTaxonNumber;


			Taxon t = taxa.getTaxon(taxonNumber);

			if (t!=null) {
				t.setName(sequenceName);
				if (progIndicator!=null) {
					progIndicator.setText("Reading taxon " + taxonNumber+": "+sequenceName);
					CommandRecord.tick("Reading taxon " + taxonNumber+": "+sequenceName);
					progIndicator.setCurrentValue(pos);
				}
				if (file!=null)
					line = file.readLine(">");  // pull in sequence up until next >
				else
					line = parser.getRemainingUntilChar('>', true);
				if (line==null) break;
				subParser.setString(line); 
				long s = 0;
				int tot = 0;
				int countA = 0;
				int countC = 0;
				int countG = 0;
				int countT = 0;
				int countAT = 0;
				int countCG=0;
				int length = 0;
				StringBuffer sequence = new StringBuffer();

				while (subParser.getPosition()<line.length()) {
					char c=subParser.nextDarkChar();
					if (c!= '\0') {
						sequence.append(c);
						length++;
						s = DNAState.fromCharStatic(c);
						if (!CategoricalState.isUnassigned(s) && !CategoricalState.isInapplicable(s)) {
							if (s == A || s == T || s == AT){ //monomorphic A or T or A&T or uncertain A or T
								tot++;
								countAT++;
							}
							else if (s == C || s == G || s == CG) { //monomorphic C or G or C&G or uncertain C or G
								tot++;
								countCG++;
							} else {
								Debugg.println("c: " + c);
							}
							if (s==A)
								countA++;
							else if (s==C)
								countC++;
							else if (s==G)
								countG++;
							else if (s==T)
								countT++;

						}

					}
				}
				data.setState(charLength, taxonNumber, 0, ((double)length));
				if (tot != 0) {
					data.setState(charA, taxonNumber, 0, ((double)countA)/tot);
					data.setState(charC, taxonNumber, 0, ((double)countC)/tot);
					data.setState(charG, taxonNumber, 0, ((double)countG)/tot);
					data.setState(charT, taxonNumber, 0, ((double)countT)/tot);
					data.setState(charAT, taxonNumber, 0, ((double)countAT)/tot);
					data.setState(charCG, taxonNumber, 0, ((double)countCG)/tot);
				}
				if (length>=lowerBlastSequenceLength && (blastOption==BLAST|| blastOption==BLASTX)){
					loglnEchoToStringBuffer("\nBLASTing  " + sequenceName, blastReport);
					loglnEchoToStringBuffer("   Sequence length: "+ length, blastReport);
					BLASTResults blastResult = new BLASTResults(numHits);
					if (blastOption==BLAST)
						NCBIUtil.blastForMatches("blastn", token, sequence.toString(), true, numHits, 300, response);
					else if (blastOption==BLASTX)
						NCBIUtil.blastForMatches("blastx", token, sequence.toString(), true, numHits, 300, response);
					blastResult.processResultsFromBLAST(response.toString(), false);
					if (blastOption==BLASTX)
						loglnEchoToStringBuffer("   BLASTX search completed", blastReport);
					else 
						loglnEchoToStringBuffer("   BLAST search completed", blastReport);
					if (blastResult.geteValue(1)<0.0) {
						loglnEchoToStringBuffer("   No hits.", blastReport);
					} else {
					/*	loglnEchoToStringBuffer("   Top hit: " + blastResult.getDefinition(1), blastReport);
						loglnEchoToStringBuffer("   Accession: " + blastResult.getAccession(1), blastReport);
						loglnEchoToStringBuffer("   e-Value: " +blastResult.geteValue(1), blastReport);
					*/	Associable associable = data.getTaxaInfo(true);
						associable.setAssociatedDouble(NCBIUtil.EVALUE, taxonNumber, blastResult.geteValue(1));
						associable.setAssociatedDouble(NCBIUtil.BITSCORE, taxonNumber, blastResult.getBitScore(1));
						associable.setAssociatedObject(NCBIUtil.DEFINITION, taxonNumber, blastResult.getDefinition(1));
						associable.setAssociatedObject(NCBIUtil.ACCESSION, taxonNumber, blastResult.getAccession(1));

						fastaBLASTResults.setLength(0);
						blastResult.processResultsFromBLAST(response.toString(), false);

						String[] accessionNumberArray = blastResult.getAccessions();
						if (accessionNumberArray!=null) {
							loglnEchoToStringBuffer("   Top hits; Accession [eValue] Definition): ", blastReport);
							for (int i=0; i<numHits && i<accessionNumberArray.length; i++)
								if (StringUtil.notEmpty(accessionNumberArray[i]))
									loglnEchoToStringBuffer("        "+ accessionNumberArray[i] + "\t[" + blastResult.geteValue(i)+ "]\t" + blastResult.getDefinition(i), blastReport);
						}

						if (blastOption==BLAST) {
							if (storeBlastSequences) {
								String fasta = NCBIUtil.fetchGenBankSequencesFromAccessions(accessionNumberArray,  true, this, false,  fastaBLASTResults,  null);
								if (StringUtil.notEmpty(fasta)) {
									fastaBLASTResults.insert(0, ">"+sequenceName+"\n" + sequence + "\n");
									MesquiteFile.putFileContents(pathForBLASTfiles+sequenceName, fastaBLASTResults.toString(), true);
								}
							}
						}
					}
					accessionNumbers.deassignArray();


				}

			}
			if (added) 
				lastTaxonNumber++;
			//			file.readLine(sb);
			if (file!=null) {
				line = file.readNextDarkLine();		// added 1.01
				pos = file.getFilePosition();
			}
			else {
				line = parser.getRawNextDarkLine();
				pos = parser.getPosition();
			}
			subParser.setString(line); //sets the string to be used by the parser to "line" and sets the pos to 0
			if (file !=null && file.getFileAborted()) {
				abort = true;
			}
		}
		if (lastTaxonNumber<taxa.getNumTaxa())
			if (data.hasDataForTaxa(lastTaxonNumber+1, taxa.getNumTaxa()-1))
				MesquiteMessage.discreetNotifyUser("Warning: InterpretFASTA attempted to delete extra taxa, but these contained data, and so were not deleted");
			else
				taxa.deleteTaxa(lastTaxonNumber, taxa.getNumTaxa()-lastTaxonNumber, true);   // add a character if needed
		if (numFilledChars<data.getNumChars())
			if (data.hasDataForCharacters(numFilledChars+1, data.getNumChars()-1))
				MesquiteMessage.discreetNotifyUser("Warning: InterpretFASTA attempted to delete extra characters, but these contained data, and so were not deleted");
			else
				data.deleteCharacters(numFilledChars+1, data.getNumChars()-numFilledChars, true);   // add a character if needed

		if (StringUtil.notEmpty(blastReport.toString())) {
			if (blastOption==BLAST|| blastOption==BLASTX){
				loglnEchoToStringBuffer("\n============\n", blastReport);
			}
			StringBuffer fileNameBuffer = new StringBuffer("BLAST Report");
			String path = MesquiteFile.saveFileAsDialog("Save BLAST report", fileNameBuffer);
			if (path!=null)
				MesquiteFile.putFileContents(path, blastReport.toString(), true);
		}

		data.saveChangeHistory = wassave;
		data.resetChangedSinceSave();

		finishImport(progIndicator, file, abort);

	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");

		RadioButtons blastRadioButtons= dialog.addRadioButtons(new String[] {"don't BLAST", "BLAST", "BLASTX"}, blastOption);
		Checkbox saveTopHitsBox = dialog.addCheckBox("Save top hits to FASTA files", saveTopHits);
		IntegerField blastLowerLengthField = dialog.addIntegerField("Lower length limit of sequences to be BLASTed:", lowerBlastSequenceLength, 20, 0, Integer.MAX_VALUE);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			blastOption = blastRadioButtons.getValue();
			lowerBlastSequenceLength = blastLowerLengthField.getValue();
			saveTopHits = saveTopHitsBox.getState();
			storePreferences();

		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void readFile(MesquiteProject mf, MesquiteFile file, String arguments) {
		if (!queryOptions()) 
			return;
		incrementMenuResetSuppression();
		ProgressIndicator progIndicator = new ProgressIndicator(mf,"Importing File "+ file.getName(), file.existingLength());
		progIndicator.start();
		file.linkProgressIndicator(progIndicator);
		if (file.openReading()) {
			TaxaManager taxaTask = (TaxaManager)findElementManager(Taxa.class);
			CharactersManager charTask = (CharactersManager)findElementManager(CharacterData.class);
			Taxa taxa = null;

			if (taxa == null){
				taxa = taxaTask.makeNewTaxa(getProject().getTaxas().getUniqueName("Taxa"), 0, false);
				taxa.addToFile(file, getProject(), taxaTask);
			}
			ContinuousData data = null;
			if (data == null){
				data =(ContinuousData)createData(charTask,taxa);
				data.addToFile(file, getProject(), null);
			}
			int numTaxa = 0;

			readFileCore(parser, file, data,  taxa, numTaxa, progIndicator, arguments);	
		}
		decrementMenuResetSuppression();
	}

	/*.................................................................................................................*/
	public String getName() {
		return "FASTA to Summary Stats, with BLAST (DNA)";
	}
	/*.................................................................................................................*/

	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Imports FASTA DNA files into a continuous matrix showing nucleotide frequencies, with the option to BLAST." ;
	}

	public boolean exportFile(MesquiteFile file, String arguments) {
		return false;
	}


}

