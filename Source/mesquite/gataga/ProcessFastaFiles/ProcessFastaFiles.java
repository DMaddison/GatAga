/* Mesquite GatAga source code.  Copyright 2012 David Maddison & Wayne Maddison
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
 */

package mesquite.gataga.ProcessFastaFiles; 

import java.io.*;
import java.util.Vector;

import org.apache.commons.lang.SystemUtils;

import mesquite.lib.*;
import mesquite.io.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CodonPositionsSet;
import mesquite.lib.duties.*;
import mesquite.lib.system.SystemUtil;
import mesquite.lists.lib.ListModule;
import mesquite.basic.ManageSetsBlock.ManageSetsBlock;
import mesquite.categ.lib.*;
import mesquite.charMatrices.ManageCodonsBlock.ManageCodonsBlock;
import mesquite.gataga.lib.*;


/* ======================================================================== */
public class ProcessFastaFiles extends GeneralFileMaker { 
	MesquiteProject project = null;
	FileCoordinator fileCoord = null;
	String directoryPath=null;
	ProgressIndicator progIndicator = null;
	InterpretFasta importer = null;
	MesquiteFile fileToWrite;

	String script = null;
	boolean incorporateScript = false;
	/*"addProcessor  #mesquite.charMatrices.AlterAllMatrices.AlterAllMatrices;\n"
			+" tell It;\n"
			+"  	setDataAlterer  #mesquite.gataga.MultipleAlignService.MultipleAlignService;\n"
			+"  	tell It;\n"
			+"  		setAligner  #mesquite.align.MAFFTAlign.MAFFTAlign;\n"
			+"  	endTell;\n"
			+"endTell;\n"
			+" addProcessor  #mesquite.charMatrices.AlterAllMatrices.AlterAllMatrices;\n"
			+" tell It;\n"
			+" 	setDataAlterer  #mesquite.gataga.SetCodonPositions.SetCodonPositions;\n"
			+"endTell;\n";
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		loadPreferences();
		return true;
	}
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("script".equalsIgnoreCase(tag))
			script = StringUtil.cleanXMLEscapeCharacters(content);

	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(60);	
		StringUtil.appendXMLTag(buffer, 2, "script", script);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	Vector fileProcessors = null;
	boolean cancelProcessing = false;

	private boolean hireProcessorsIfNeeded(){
		if (!firstTime)
			return true;

		if (fileProcessors == null){
			fileProcessors = new Vector();

			String currentScript = script;
			if (currentScript == null)
				currentScript = "";
			while (showAlterDialog(fileProcessors.size())){
				if (incorporateScript){
					if (script != null){    //HERE IT SHOULD QUERY and give a choice of options like availabel macros, saved with names, rather than just the single previous script
						Puppeteer p = new Puppeteer(this);
						CommandRecord mr = MesquiteThread.getCurrentCommandRecord();
						MesquiteThread.setCurrentCommandRecord(CommandRecord.macroRecord);
						p.execute(this, script, new MesquiteInteger(0), null, false);
						MesquiteThread.setCurrentCommandRecord(mr);
					}	
					incorporateScript = false;
				}
				else {
					FileProcessor processor = (FileProcessor)project.getCoordinatorModule().hireEmployee(FileProcessor.class, "File processor (" + (fileProcessors.size() + 1)+ ")");
					currentScript += "\naddProcessor " + " #" + processor.getClass().getName() + ";\n";
					String sn =Snapshot.getSnapshotCommands(processor, getProject().getHomeFile(), "  ");
					currentScript +="\ntell It;\n" + sn + "\nendTell;";
					fileProcessors.addElement(processor);
					if (cancelProcessing){
						if (fileProcessors != null){
							for (int i= 0; i< fileProcessors.size(); i++){
								FileProcessor alterer = (FileProcessor)fileProcessors.elementAt(i);
								fireEmployee(alterer);
							}
						}
						return false;
					}
				}

			}
			script = currentScript;
			storePreferences();
		}
		return true;
	}
	/*.................................................................................................................*
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		if (fileProcessors != null)
			for (int i=0; i<fileProcessors.size(); i++) {
				temp.addLine("setFileProcessor",(FileProcessor)fileProcessors.elementAt(i));
			}
		return temp;
	}
	boolean preferencesSet=false;
	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String content) {
		if ("fileProcessor".equalsIgnoreCase(tag)) {
			String fileProcessorName = StringUtil.cleanXMLEscapeCharacters(content);
			FileProcessor processor = (FileProcessor)project.getCoordinatorModule().hireNamedEmployee(FileProcessor.class, fileProcessorName);
			if (fileProcessors == null)
				fileProcessors = new Vector();
			fileProcessors.addElement(processor);
		}

		preferencesSet = true;
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		if (fileProcessors != null)
			for (int i=0; i<fileProcessors.size(); i++) {
				StringUtil.appendXMLTag(buffer, 2, "fileProcessor ",(FileProcessor)fileProcessors.elementAt(i));
			}

		preferencesSet = true;
		return buffer.toString();
	}

	MesquiteInteger pos = new MesquiteInteger();
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the module processing files", "[name of module]", commandName, "addProcessor")) {
			FileProcessor processor = (FileProcessor)project.getCoordinatorModule().hireNamedEmployee(FileProcessor.class, arguments);
			if (processor!=null) {
				if (fileProcessors == null)
					fileProcessors = new Vector();
				fileProcessors.addElement(processor);
			}
			return processor;
		}
		else 
			super.doCommand(commandName, arguments, checker);
		return null;
	}

	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return false;
	}
	/*.................................................................................................................*/
	public CharacterData createData(CharactersManager charTask, Taxa taxa) {  
		return charTask.newCharacterData(taxa, 0, DNAData.DATATYPENAME);  //
	}

	/*.................................................................................................................*/
	private void append(StringBuffer sb, int numTabs, String content) {
		sb.append("\n");
		for (int i=0; i<numTabs; i++)
			sb.append("\t");
		sb.append(content+";");
	}
	/*.................................................................................................................*/
	public String appendToFile(boolean proteinCoding) {  
		long charID = System.currentTimeMillis();
		long taxID = charID+1;
		StringBuffer sb = new StringBuffer(1000);
		sb.append("\n\n");
		sb.append("\nBegin MESQUITE;");
		append(sb,1,"MESQUITESCRIPTVERSION 2");
		append(sb,1,"tell ProjectCoordinator");

		append(sb,1,"getWindow");
		append(sb,1,"tell It");
		append(sb,2,"setSize 1000 600");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee #mesquite.minimal.ManageTaxa.ManageTaxa");
		append(sb,1,"tell It");
		append(sb,2,"setID 0 " +taxID + "");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee #mesquite.charMatrices.ManageCharacters.ManageCharacters");
		append(sb,1,"tell It");
		append(sb,2,"setID 0 " +charID + "");
		append(sb,1,"endTell");

		append(sb,1,"getEmployee  #mesquite.minimal.ManageTaxa.ManageTaxa");
		append(sb,1,"tell It");
		append(sb,2,"showTaxa #" + taxID + " #mesquite.lists.TaxonList.TaxonList");
		append(sb,2,"tell It");
		append(sb,3,"setTaxa #" + taxID + "");
		append(sb,3,"getWindow");
		append(sb,3,"tell It");

		append(sb,3,"newAssistant  #mesquite.lists.NumForTaxaList.NumForTaxaList");

		append(sb,3,"tell It");
		append(sb,4, "suppress");
		append(sb,4, "setValueTask #mesquite.molec.NumberStopsInTaxon.NumberStopsInTaxon");
		append(sb,4,"tell It");
		append(sb,5,"getEmployee #mesquite.charMatrices.CharMatrixCoordIndep.CharMatrixCoordIndep");
		append(sb,5,"tell It");
		append(sb,6,"setCharacterSource #mesquite.charMatrices.StoredMatrices.StoredMatrices");
		append(sb,6,"tell It");
		append(sb,6,"setDataSet #"+ charID);
		append(sb,6,"endTell");
		append(sb,5,"endTell");
		append(sb,4,"endTell");
		append(sb,4, "desuppress");

		append(sb,3,"endTell");
		append(sb,3,"endTell");
		append(sb,2,"endTell");
		append(sb,1,"endTell");


		append(sb,1,"getEmployee  #mesquite.charMatrices.BasicDataWindowCoord.BasicDataWindowCoord");
		append(sb,1,"tell It");
		append(sb,2,"showDataWindow #" + charID + " #mesquite.charMatrices.BasicDataWindowMaker.BasicDataWindowMaker");

		append(sb,2,"tell It");
		append(sb,3,"getWindow");

		append(sb,3,"tell It");
		append(sb,4,"getTable");
		append(sb,4,"tell It");
		append(sb,5,"rowNamesWidth 250");
		append(sb,4,"endTell");
		if (proteinCoding)
			append(sb,4,"colorCells  #mesquite.charMatrices.ColorByAA.ColorByAA");
		else
			append(sb,4,"colorCells  #mesquite.charMatrices.ColorByState.ColorByState");
		append(sb,3,"endTell");
		append(sb,3,"showWindow");
		append(sb,3,"getWindow");
		append(sb,3,"tell It");
		append(sb,4,"forceAutosize");
		append(sb,3,"endTell");
		append(sb,2,"endTell");
		append(sb,1,"endTell");
		append(sb,1,"endTell");
		sb.append("\nend;\n");

		return sb.toString();
	}


	/*.................................................................................................................*/
	public void writeFile(MesquiteFile nMF){
		NexusFileInterpreter nfi =(NexusFileInterpreter) fileCoord.findImmediateEmployeeWithDuty(NexusFileInterpreter.class);
		if (nfi!=null) {
			nfi.writeFile(project, nMF);
		}
	}


	/*.................................................................................................................*
	private void processData(DNAData data, Taxa taxa, boolean proteinCoding) {


		MolecularDataUtil.reverseComplementSequencesIfNecessary(data, module, taxa, 0, taxa.getNumTaxa(), proteinCoding, false);
		MolecularDataUtil.pairwiseAlignMatrix(this, data, 0, false);
		if (proteinCoding){
			MolecularDataUtil.setCodonPositionsToMinimizeStops(data, module, taxa, 0, taxa.getNumTaxa());
			//MolecularDataUtil.shiftToMinimizeStops(data, module, taxa, 0, taxa.getNumTaxa());
		}
		// then alter taxon names
		//open character matrix
		// color by amino acid if protein coding

	}
	/*.................................................................................................................*/
	public boolean showAlterDialog(int count) {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Add File Processor?",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		boolean initialSetup = false;
		if (count == 0){
			dialog.addLabel("For each file examined, do you want to process it?");
			String b2 = "Use Previous";
			if (script == null)
				b2 = null;
			dialog.completeAndShowDialog("Yes", "No", b2, "No");
			initialSetup = true;
		}
		else {
			dialog.addLabel("For each file examined, do you want to add another step in processing it?");
			dialog.addLabel("The processing steps already requested are:");
			String[] steps = new String[fileProcessors.size()];
			for (int i = 0; i<steps.length; i++){
				if (fileProcessors.elementAt(i)!=null)
					steps[i] = "(" + (i+1) + ") " + ((FileProcessor)fileProcessors.elementAt(i)).getNameAndParameters();
			}
			dialog.addList (steps, null, null, 8);
			dialog.completeAndShowDialog("Add", "Done", "Cancel", "Done");
		}
		dialog.dispose();
		boolean addProcess =  (buttonPressed.getValue()==0);
		if (buttonPressed.getValue()==2) {
			if (initialSetup) {
				addProcess = true;
				incorporateScript = true;				
			}
			else
				cancelProcessing = true;
		}
		return addProcess;
	}

	boolean firstResultsOverall = true;
	boolean firstResultsOverallFound = false;
	StringBuffer resultsHeading = new StringBuffer();
	/*.................................................................................................................*/
	void processFile(MesquiteFile fileToRead, String arguments, StringBuffer results) {
		Debugg.println("Processing file " + fileToRead.getName() + " in " + fileToRead.getDirectoryName() + "...");
		incrementMenuResetSuppression();
		ProgressIndicator progIndicator = new ProgressIndicator(null,"Importing File "+ fileToRead.getName(), fileToRead.existingLength());
		progIndicator.start();
		fileToRead.linkProgressIndicator(progIndicator);

		if (fileToRead.openReading()) {
			TaxaManager taxaTask = (TaxaManager)findElementManager(Taxa.class);
			CharactersManager charTask = (CharactersManager)findElementManager(CharacterData.class);
			Taxa taxa = null;
			if (taxa == null){
				taxa = taxaTask.makeNewTaxa("Taxa", 0, false);
				taxa.addToFile(fileToWrite, project, taxaTask);
			}
			CategoricalData data = null;
			if (data == null){
				data =(CategoricalData)createData(charTask,taxa);
				data.addToFile(fileToWrite, project, null);
			}

			int numTaxa = 0;
			ManageCodonsBlock manager = (ManageCodonsBlock)fileCoord.findEmployeeWithDuty(ManageCodonsBlock.class);
			if (manager!=null)
				manager.fileReadIn(data.getFile());

			fileToWrite.setFileName(fileToRead.getFileName()+".nex");

			//	importer.readFile(getProject(), fileToRead, arguments);	
			importer.readFileCore(parser, fileToRead, data,  taxa, numTaxa, progIndicator, arguments, true);	
			Debugg.println(" PFF data " + data);
			if (data != null)
				Debugg.println(" PFF taxa of data " + data.getTaxa());

			if (!hireProcessorsIfNeeded()){  //needs to be done here after file read in case alterers need to know if there are matrices etc in file
				return;
			}

			Debugg.println(" PFF data visible: " + data.isUserVisible());
			boolean firstResult = true;
			boolean proteinCoding = true;  // query about this  
			if (fileProcessors != null){
				boolean success = true;
				MesquiteString result = new MesquiteString();
				results.append(fileToRead.getFileName());
				for (int i= 0; i< fileProcessors.size() && success; i++){
					FileProcessor alterer = (FileProcessor)fileProcessors.elementAt(i);
					if (alterer!=null) {
						result.setValue((String)null);
						success = alterer.processFile(fileToWrite, result);

						if (!success)
							logln("Sorry,  " + alterer.getNameAndParameters() + " did not succeed in processing the file " + fileToRead.getFileName()+".nex");
						else {
							logln("" + alterer.getNameAndParameters() + " successfully processed the file " + fileToRead.getFileName()+".nex");
							if (result.getValue() != null) {
								firstResultsOverallFound = true;
								results.append("\t");
								results.append(result.getValue());
								results.append("");
								result.setValue((String)null);
								if (firstResultsOverall){
									if (firstResult)
										resultsHeading.append("File");
									resultsHeading.append("\t");
									resultsHeading.append(alterer.getNameAndParameters());
								}
							}
						}
					} else
						logln("There was a problem processing files; one of the processors was null.");

				}
				firstResult = false;
			}
			//processData((DNAData)data, taxa, proteinCoding);   // query about this
			results.append("\n");
			if (firstResultsOverall){
				resultsHeading.append("\n");
			}

			writeFile(fileToWrite);
			MesquiteFile.appendFileContents(fileToWrite.getPath(), appendToFile(proteinCoding), true);
			Debugg.println("================= wrote matrix ");

			firstTime = false;
			project.removeAllFileElements(CharacterData.class, false);	
			project.removeAllFileElements(TreeVector.class, false);	
			project.removeAllFileElements(Taxa.class, false);	

		}
		decrementMenuResetSuppression();
	}
	/*.................................................................................................................*/

	boolean firstTime = true;
	private void processDirectory(String directoryPath){
		if (StringUtil.blank(directoryPath))
			return;
		File directory = new File(directoryPath);
		//	parser = new Parser();
		firstTime = true;
		project.getCoordinatorModule().setWhomToAskIfOKToInteractWithUser(this);
		boolean abort = false;
		String path = "";
		StringBuffer results = new StringBuffer();
		if (directory!=null) {
			if (directory.exists() && directory.isDirectory()) {
				//Hire file alterers

				String[] files = directory.list();
				progIndicator = new ProgressIndicator(null,"Processing Folder of Fasta Files", files.length);
				progIndicator.start();
				for (int i=0; i<files.length; i++) {
					progIndicator.setCurrentValue(i);
					if (progIndicator.isAborted()|| cancelProcessing) 
						abort = true;
					if (abort)
						break;
					if (files[i]!=null) {
						path = directoryPath + MesquiteFile.fileSeparator + files[i];
						File cFile = new File(path);
						MesquiteFile file = new MesquiteFile();
						file.setPath(path);
						if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {
							results.setLength(0);
							processFile( file,null, results);
							if (firstResultsOverallFound && firstResultsOverall && resultsHeading.length()>0){
								MesquiteFile.appendFileContents(fileToWrite.getDirectoryName() + "ProcessingResults.txt", resultsHeading.toString(), true);
								MesquiteFile.appendFileContents(fileToWrite.getDirectoryName() + "ProcessingResults.txt", StringUtil.lineEnding(), true);
								firstResultsOverall = false;
							}
							if (results.length()>0){
								MesquiteFile.appendFileContents(fileToWrite.getDirectoryName() + "ProcessingResults.txt", results.toString(), true);
								MesquiteFile.appendFileContents(fileToWrite.getDirectoryName() + "ProcessingResults.txt", StringUtil.lineEnding(), true);
							}
							logln(" ");
						}
					}
				}
				progIndicator.goAway();

			}
		}
	}
	public boolean okToInteractWithUser(int howImportant, String messageToUser){
		return firstTime;
	}
	/*.................................................................................................................*/
	public MesquiteProject establishProject(String arguments) {
		boolean success= false;
		fileCoord = getFileCoordinator();
		fileToWrite = new MesquiteFile();
		project = fileCoord.initiateProject(fileToWrite.getFileName(), fileToWrite);
		importer = (InterpretFasta)fileCoord.findEmployeeWithName("#InterpretFastaDNA");

		directoryPath = MesquiteFile.chooseDirectory("Choose directory containing fasta files:", null); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
		if (StringUtil.blank(directoryPath))
			return null;

		// what file reader?
		// filter by extension?
		// save script
		//

		fileToWrite.setPath(directoryPath+MesquiteFile.fileSeparator+"temp.nex");
		processDirectory(directoryPath);

		fireEmployee(importer);  //why would you fire this??? Debugg.println
		if (success){
			project.autosave = true;
			return project;
		}
		project.developing = false;
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Process FASTA Files";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "Process FASTA Files...";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Processes a folder of FASTA files.";
	}
}




