
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

	Blaster blasterTask;

	Class[] acceptedClasses;

	static final int DONTBLAST = 0;
	static final int BLAST = 1;
	static final int BLASTX = 2;
	int blastOption = DONTBLAST;
	int lowerBlastSequenceLength = 1000;
	boolean saveTopHits = false;
	boolean resaveFastaFile = false;
	double eValueCutoff = 10.0;
	double bestBLASTSearchesCutoff = 0.0;
	int numHits = 5;

	boolean fetchTaxonomy = false;

	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(Blaster.class, getName() + "  needs a Blast module.","");
	}

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		acceptedClasses = new Class[] {ContinuousState.class};
		loadPreferences();
		return true;  
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("fetchTaxonomy".equalsIgnoreCase(tag))
			fetchTaxonomy = MesquiteBoolean.fromTrueFalseString(content);
		else if ("saveTopHits".equalsIgnoreCase(tag))
			saveTopHits = MesquiteBoolean.fromTrueFalseString(content);
		else if ("resaveFastaFile".equalsIgnoreCase(tag))
			resaveFastaFile = MesquiteBoolean.fromTrueFalseString(content);
		else if ("blastOption".equalsIgnoreCase(tag))
			blastOption = MesquiteInteger.fromString(content);
		else if ("lowerBlastSequenceLength".equalsIgnoreCase(tag))
			lowerBlastSequenceLength = MesquiteInteger.fromString(content);
		else if ("numHits".equalsIgnoreCase(tag))
			numHits = MesquiteInteger.fromString(content);		
		else if ("eValueCutoff".equalsIgnoreCase(tag))
			eValueCutoff = MesquiteDouble.fromString(content);		
		else if ("bestBLASTSearchesCutoff".equalsIgnoreCase(tag))
			bestBLASTSearchesCutoff = MesquiteDouble.fromString(content);		
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "fetchTaxonomy", fetchTaxonomy);  
		StringUtil.appendXMLTag(buffer, 2, "saveTopHits", saveTopHits);  
		StringUtil.appendXMLTag(buffer, 2, "resaveFastaFile", resaveFastaFile);  
		StringUtil.appendXMLTag(buffer, 2, "blastOption", blastOption);  
		StringUtil.appendXMLTag(buffer, 2, "lowerBlastSequenceLength", lowerBlastSequenceLength);  
		StringUtil.appendXMLTag(buffer, 2, "numHits", numHits);  
		StringUtil.appendXMLTag(buffer, 2, "eValueCutoff", eValueCutoff);  
		StringUtil.appendXMLTag(buffer, 2, "bestBLASTSearchesCutoff", bestBLASTSearchesCutoff);  
		return buffer.toString();
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
		if (blasterTask==null)
			return;
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
		String pathForBestBLASTfiles = null;
		String pathForBlastReport = null;
		String pathForResavedFile = null;
		if (blastOption==BLAST){
			loglnEchoToStringBuffer("\n============\nBLAST Search", blastReport);
		} else if (blastOption==BLASTX){
			loglnEchoToStringBuffer("\n============\nBLASTX Search", blastReport);
		}
		if (blastOption==BLAST|| blastOption==BLASTX){
			loglnEchoToStringBuffer("   Blasting sequences at least "+ lowerBlastSequenceLength + " bases long", blastReport);
			if (eValueCutoff>=0.0) 
				loglnEchoToStringBuffer("   Only keeping hits with e-values less than or equal to "+ eValueCutoff, blastReport);
			if (saveTopHits&& StringUtil.blank(pathForBLASTfiles)) {
				pathForBLASTfiles = MesquiteFile.chooseDirectory("Choose directory to store top BLAST hits: ");
				if (StringUtil.blank(pathForBLASTfiles))
					storeBlastSequences = false;
				else if (!pathForBLASTfiles.endsWith(MesquiteFile.fileSeparator))
					pathForBLASTfiles+=MesquiteFile.fileSeparator;
			}
			if (storeBlastSequences) {
				if (bestBLASTSearchesCutoff==0.0)
					pathForBestBLASTfiles =pathForBLASTfiles+"Best eValue=0"+MesquiteFile.fileSeparator;
				else
					pathForBestBLASTfiles =pathForBLASTfiles+"Best eValue<="+bestBLASTSearchesCutoff+MesquiteFile.fileSeparator;
				MesquiteFile.createDirectory(pathForBestBLASTfiles);
			}
		}
		boolean someBlastsDone = false;

		if (blastOption==BLAST|| blastOption==BLASTX){
			loglnEchoToStringBuffer("\n============\n", blastReport);
			StringBuffer fileNameBuffer = new StringBuffer("BLAST Report");
			pathForBlastReport = MesquiteFile.saveFileAsDialog("Save BLAST report", fileNameBuffer);
			if (pathForBlastReport!=null)
				MesquiteFile.putFileContents(pathForBlastReport, blastReport.toString(), true);
			blastReport.setLength(0);
			fileNameBuffer.setLength(0);
			fileNameBuffer.append("MOD"+file.getName());
			if (resaveFastaFile){
				pathForResavedFile = MesquiteFile.saveFileAsDialog("Save Modified FASTA file", fileNameBuffer);
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
				String sequenceNameSuffix = "";
				if (length>=lowerBlastSequenceLength && (blastOption==BLAST|| blastOption==BLASTX)){
					loglnEchoToStringBuffer("\nBLASTing  " + sequenceName, blastReport);
					loglnEchoToStringBuffer("   Sequence length: "+ length, blastReport);
					BLASTResults blastResult = new BLASTResults(numHits);
					if (blastOption==BLASTX)
						blasterTask.blastForMatches("blastx", sequenceName, sequence.toString(), true, numHits, 300, eValueCutoff, response, taxonNumber==1);
					else if (blastOption==BLAST)
						blasterTask.blastForMatches("blastn", sequenceName, sequence.toString(), true, numHits, 300, eValueCutoff, response,  taxonNumber==1);
					someBlastsDone = true;
					blastResult.processResultsFromBLAST(response.toString(), false, eValueCutoff);
					blasterTask.postProcessingCleanup(blastResult);

					if (blastOption==BLASTX)
						loglnEchoToStringBuffer("   BLASTX search completed", blastReport);
					else 
						loglnEchoToStringBuffer("   BLAST search completed", blastReport);
					if (blastResult.geteValue(1)<0.0) {
						loglnEchoToStringBuffer("   No hits.", blastReport);
					} else if (blastResult.geteValue(1)>eValueCutoff) {
						loglnEchoToStringBuffer("   No acceptable hits.", blastReport);
					} else {
						Associable associable = data.getTaxaInfo(true);
						associable.setAssociatedDouble(NCBIUtil.EVALUE, taxonNumber, blastResult.geteValue(0));
						associable.setAssociatedDouble(NCBIUtil.BITSCORE, taxonNumber, blastResult.getBitScore(0));
						associable.setAssociatedObject(NCBIUtil.DEFINITION, taxonNumber, blastResult.getDefinition(0));
						associable.setAssociatedObject(NCBIUtil.ACCESSION, taxonNumber, blastResult.getAccession(0));
						sequenceNameSuffix = " [top hit: " + StringUtil.getItem(blastResult.getDefinition(0), "|",5) + ", eValue=" + blastResult.geteValue(0) +  "]";

						fastaBLASTResults.setLength(0);

						String[] IDs = blastResult.getIDs();
						if (IDs!=null) {
							loglnEchoToStringBuffer(blastResult.toString(numHits), blastReport);
						}

						if (storeBlastSequences) {
							if (blastOption==BLASTX)
								IDs = NCBIUtil.getNucIDsFromProtIDs(IDs);


							String fasta = NCBIUtil.fetchGenBankSequencesFromIDs(IDs,  true, this, false,  fastaBLASTResults,  null);
							if (StringUtil.notEmpty(fasta)) {
								fastaBLASTResults.insert(0, ">"+sequenceName+"\n" + StringUtil.wrap(sequence.toString(), 60) + "\n");
								String fileName = pathForBLASTfiles+sequenceName;
								if (blastResult.geteValue(0)<=bestBLASTSearchesCutoff)
									fileName = pathForBestBLASTfiles+sequenceName;
								MesquiteFile.putFileContents(fileName, fastaBLASTResults.toString(), true);
							}

						}
						if (fetchTaxonomy) {
							String tax = NCBIUtil.fetchTaxonomyFromSequenceID(blastResult.getID(1), true, true, null);
							associable.setAssociatedObject(NCBIUtil.TAXONOMY, taxonNumber, tax);
						}
					}
					accessionNumbers.deassignArray();


				}
				if (pathForResavedFile!=null) {
					String newSequenceName = sequenceName+sequenceNameSuffix;
					MesquiteFile.appendFileContents(pathForResavedFile, ">"+newSequenceName+"\n" + StringUtil.wrap(sequence.toString(), 60) + "\n", true);
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
			if (pathForBlastReport!=null)
				MesquiteFile.appendFileContents(pathForBlastReport, blastReport.toString(), true);
			blastReport.setLength(0);

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



		data.saveChangeHistory = wassave;
		data.resetChangedSinceSave();

		if (abort && AlertDialog.query(containerOfModule(), "Save partial file?" , "Save the portion that has been read in and processed?","Save", "Cancel"))
			abort=false;  // for this importer, save whatever you have read in
		finishImport(progIndicator, file, abort);

	}

	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");

		RadioButtons blastRadioButtons= dialog.addRadioButtons(new String[] {"don't BLAST", "BLAST", "BLASTX"}, blastOption);
		IntegerField blastLowerLengthField = dialog.addIntegerField("Lower length limit of sequences to be BLASTed:", lowerBlastSequenceLength, 10, 10, Integer.MAX_VALUE);
		IntegerField numHitsField = dialog.addIntegerField("Number of top hits:", numHits, 8, 1, Integer.MAX_VALUE);
		DoubleField eValueCutoffField = dialog.addDoubleField("Reject hits with eValues greater than: ", eValueCutoff, 20, 0.0, Double.MAX_VALUE);
		Checkbox resaveFastaFileBox = dialog.addCheckBox("Save copy of original FASTA file but with names appended with top hit", resaveFastaFile);
		Checkbox saveTopHitsBox = dialog.addCheckBox("Save top hits to new FASTA files", saveTopHits);
		DoubleField bestBLASTSearchesCutoffField = dialog.addDoubleField("Move FASTA file to a separate folder if eValue is less than: ", bestBLASTSearchesCutoff, 20, 0.0, Double.MAX_VALUE);
		Checkbox fetchTaxonomyBox = dialog.addCheckBox("Fetch taxonomic information", fetchTaxonomy);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			blastOption = blastRadioButtons.getValue();
			lowerBlastSequenceLength = blastLowerLengthField.getValue();
			eValueCutoff = eValueCutoffField.getValue();
			numHits = numHitsField.getValue();
			saveTopHits = saveTopHitsBox.getState();
			resaveFastaFile = resaveFastaFileBox.getState();
			fetchTaxonomy = fetchTaxonomyBox.getState();
			bestBLASTSearchesCutoff = bestBLASTSearchesCutoffField.getValue();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void readFile(MesquiteProject mf, MesquiteFile file, String arguments) {
		if (blasterTask==null)
			blasterTask = (Blaster)hireEmployee(Blaster.class, "Blaster (for " + getName() + ")"); 
		if (blasterTask!=null)
			if (!blasterTask.initialize())
				return;

		if (!MesquiteThread.isScripting())
			if (!queryOptions()) 
				return;
		incrementMenuResetSuppression();
		ProgressIndicator progIndicator = new ProgressIndicator(mf,"Importing File "+ file.getName(), file.existingLength());
		progIndicator.start();
		file.linkProgressIndicator(progIndicator);
		if (file.openReading()) {
			logln("\nReading and processing file " + file.getName());
			if (blasterTask!=null && blastOption!=DONTBLAST)
				logln("  Blast uses \"" + blasterTask.getName() + "\"");
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

