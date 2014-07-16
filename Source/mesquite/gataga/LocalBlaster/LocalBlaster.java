package mesquite.gataga.LocalBlaster;

import java.awt.Checkbox;

import mesquite.lib.*;
import mesquite.molec.lib.*;

/*  Initiator: DRM
 * */

public class LocalBlaster extends Blaster implements ShellScriptWatcher {
	boolean preferencesSet = false;
	String programOptions = "" ;
	String databases = "nt" ;
	int numThreads = 1;
	MesquiteTimer timer = new MesquiteTimer();
	boolean localBlastDBHasTaxonomyIDs = true;
	boolean useIDInDefinition = true;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		programOptions = "";
		timer.start();
		loadPreferences();
		return true;
	}
	
	/*
	 -evalue 0.01
	 */
	
	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		 if ("programOptions".equalsIgnoreCase(tag))
				programOptions = StringUtil.cleanXMLEscapeCharacters(content);
		 else if ("databases".equalsIgnoreCase(tag))
			 databases = StringUtil.cleanXMLEscapeCharacters(content);
		else if ("numThreads".equalsIgnoreCase(tag))
			numThreads = MesquiteInteger.fromString(content);
		else if ("localBlastDBHasTaxonomyIDs".equalsIgnoreCase(tag))
			localBlastDBHasTaxonomyIDs = MesquiteBoolean.fromTrueFalseString(content);
		else if ("useIDInDefinition".equalsIgnoreCase(tag))
			useIDInDefinition = MesquiteBoolean.fromTrueFalseString(content);

		preferencesSet = true;
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "programOptions", programOptions);  
		StringUtil.appendXMLTag(buffer, 2, "databases", databases);  
		StringUtil.appendXMLTag(buffer, 2, "numThreads", numThreads);  
		StringUtil.appendXMLTag(buffer, 2, "localBlastDBHasTaxonomyIDs", localBlastDBHasTaxonomyIDs);  
		StringUtil.appendXMLTag(buffer, 2, "useIDInDefinition", useIDInDefinition);  

		preferencesSet = true;
		return buffer.toString();
	}

	public boolean initialize() {
		if (!MesquiteThread.isScripting())
			return queryOptions();
		return true;
	}
	
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Local BLAST Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Local BLAST Options");
		StringBuffer sb = new StringBuffer();
		sb.append("To use this local BLAST tool, you need to have installed the BLAST program on this computer, and need to have also set up local blast databases on your computer.\n");
		sb.append("If you are going to do a blastX to a local protein database that you downloaded from GenBank, you will need to check Use ID in Definition");
		dialog.appendToHelpString(sb.toString());

		SingleLineTextField databasesField = dialog.addTextField("Databases to search:", databases, 26, true);
		SingleLineTextField programOptionsField = dialog.addTextField("Additional Blast options:", programOptions, 26, true);
		Checkbox useIDInDefinitionBox = dialog.addCheckBox("Use ID in Definition (NCBI-provided databases)", useIDInDefinition);
		Checkbox localBlastDBHasTaxonomyIDsBox = dialog.addCheckBox("Local Blast database has NCBI taxonomy IDs", localBlastDBHasTaxonomyIDs);
		IntegerField numThreadsField = dialog.addIntegerField("Number of processor threads to use:", numThreads,20, 1, Integer.MAX_VALUE);
		
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			databases = databasesField.getText();
			programOptions = programOptionsField.getText();
			numThreads = numThreadsField.getValue();
			localBlastDBHasTaxonomyIDs = localBlastDBHasTaxonomyIDsBox.getState();
			useIDInDefinition = useIDInDefinitionBox.getState();
			
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void blastForMatches(String blastType, String sequenceName, String sequence, boolean isNucleotides, int numHits, int maxTime,  double eValueCutoff, StringBuffer blastResponse, boolean writeCommand) {
		
		getProject().incrementProjectWindowSuppression();
		
		String unique = MesquiteTrunk.getUniqueIDBase();
		String rootDir = createSupportDirectory() + MesquiteFile.fileSeparator;  
		String fileName = "sequenceToSearch" + MesquiteFile.massageStringToFilePathSafe(unique) + ".fa";   
		String filePath = rootDir +  fileName;

		StringBuffer fileBuffer = new StringBuffer();
		fileBuffer.append(NCBIUtil.createFastaString(sequenceName, sequence, isNucleotides));
		MesquiteFile.putFileContents(filePath, fileBuffer.toString(), true);


		String runningFilePath = rootDir + "running" + MesquiteFile.massageStringToFilePathSafe(unique);
		String outFileName = "blastResults" + MesquiteFile.massageStringToFilePathSafe(unique);
		String outFilePath = rootDir + outFileName;

		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir));
		String blastCommand = blastType + "  -query " + fileName;
		blastCommand+= " -db "+databases;
		if (numThreads>1)
			blastCommand+="  -num_threads " + numThreads;
		blastCommand+=" -out " + outFileName + " -outfmt 5";		
		blastCommand+=" -max_target_seqs " + numHits; // + " -num_alignments " + numHits;// + " -num_descriptions " + numHits;		
		blastCommand+=" " + programOptions + StringUtil.lineEnding();
		shellScript.append(blastCommand);
		if (writeCommand)
			logln("\n...................\nBLAST command: \n" + blastCommand);

		String scriptPath = rootDir + "batchScript" + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), true);

		timer.timeSinceLast();
		
		boolean success = ShellScriptUtil.executeAndWaitForShell(scriptPath, runningFilePath, null, true, getName(),null,null, this, true);

		if (success){
			String results = MesquiteFile.getFileContentsAsString(outFilePath, -1, 1000, false);
			if (blastResponse!=null && StringUtil.notEmpty(results)){
				blastResponse.setLength(0);
				blastResponse.append(results);
			}
		}
		deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();
		logln("   BLAST completed in " +timer.timeSinceLastInSeconds()+" seconds");
	}	
	
	/*.................................................................................................................*/
	public String getFastaFromIDs(String[] idList, boolean isNucleotides, StringBuffer blastResponse) {
		int count = 0;
		for (int i=0; i<idList.length; i++) 
			if (StringUtil.notEmpty(idList[i]))
				count++;
		if (count==0)
			return null;
		if (blastx)
			return NCBIUtil.fetchGenBankSequencesFromIDs(idList,  isNucleotides, null, false,  blastResponse,  null);

		count = 0;
		StringBuffer queryString = new StringBuffer();
		for (int i=0; i<idList.length; i++) 
			if (StringUtil.notEmpty(idList[i])){
				if (count>0)
					queryString.append(",");
				queryString.append("'"+idList[i]+"'");
				count++;
			}

		getProject().incrementProjectWindowSuppression();
		
		String unique = MesquiteTrunk.getUniqueIDBase();
		String rootDir = createSupportDirectory() + MesquiteFile.fileSeparator;  

		String runningFilePath = rootDir + "running" + MesquiteFile.massageStringToFilePathSafe(unique);
		
		String outFileName = "blastResults" + MesquiteFile.massageStringToFilePathSafe(unique);
		String outFilePath = rootDir + outFileName;

		StringBuffer shellScript = new StringBuffer(1000);
		shellScript.append(ShellScriptUtil.getChangeDirectoryCommand(rootDir));
		
		String blastCommand = "blastdbcmd  -entry "+queryString + " -outfmt %f";
		blastCommand+= " -db "+databases;
		blastCommand+=" -out " + outFileName;		

		shellScript.append(blastCommand);

		String scriptPath = rootDir + "batchScript" + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), true);

		boolean success = ShellScriptUtil.executeAndWaitForShell(scriptPath, runningFilePath, null, true, getName(),null,null, this, true);
	
		if (success){
			String results = MesquiteFile.getFileContentsAsString(outFilePath, -1, 1000, false);
			if (blastResponse!=null && StringUtil.notEmpty(results)){
				blastResponse.setLength(0);
				blastResponse.append(results);
			}
			deleteSupportDirectory();
			getProject().decrementProjectWindowSuppression();
			return results;
		}
		deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();
		return null;
	}	
	
	/*.................................................................................................................*/
	public  String getTaxonomyFromID(String id, boolean isNucleotides, boolean writeLog, StringBuffer report){
		if (blastx)
			id = NCBIUtil.cleanUpID(id);
		if (localBlastDBHasTaxonomyIDs)
			return NCBIUtil.fetchTaxonomyFromSequenceID(id, isNucleotides, writeLog, report);
		return null;
	}



	/*.................................................................................................................*/
	public  void postProcessingCleanup(BLASTResults blastResult){
		if (useIDInDefinition){
//			blastResult.setIDFromDefinition("|", 2);
			blastResult.setIDFromDefinition();
			blastResult.setAccessionFromDefinition("|", 4);
		}
			
	}

	public  String[] getNucleotideIDsfromProteinIDs(String[] ID){
		ID = NCBIUtil.cleanUpID(ID);
		return NCBIUtil.getNucIDsFromProtIDs(ID);
	}

	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}

	public String getName() {
		return "BLAST Local Server";
	}

	public String getExplanation() {
		return "BLASTs a database on the same computer as Mesquite.";
	}

	public boolean continueShellProcess(Process proc) {
		return true;
	}



}
