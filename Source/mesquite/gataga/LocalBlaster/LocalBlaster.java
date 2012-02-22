package mesquite.gataga.LocalBlaster;

import java.awt.Checkbox;

import mesquite.lib.*;
import mesquite.molec.lib.*;

public class LocalBlaster extends Blaster implements ShellScriptWatcher {
	boolean preferencesSet = false;
	String programOptions = "" ;
	String databases = "nt" ;
	int numThreads = 1;
	MesquiteTimer timer = new MesquiteTimer();
	boolean useIDInDefinition = false;

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
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Local Blast Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Local Blast Options");

		SingleLineTextField databasesField = dialog.addTextField("Databases to search:", databases, 26, true);
		SingleLineTextField programOptionsField = dialog.addTextField("Additional Blast options:", programOptions, 26, true);
		Checkbox useIDInDefinitionBox = dialog.addCheckBox("Obtain GenBank ID from Definition", useIDInDefinition);
		IntegerField numThreadsField = dialog.addIntegerField("Number of processor threads to use:", numThreads,20, 1, Integer.MAX_VALUE);
		
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			databases = databasesField.getText();
			programOptions = programOptionsField.getText();
			numThreads = numThreadsField.getValue();
			useIDInDefinition = useIDInDefinitionBox.getState();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}

	/*.................................................................................................................*/
	public void blastForMatches(String blastType, String sequenceName, String sequence, boolean isNucleotides, int numHits, int maxTime,  double eValueCutoff, StringBuffer blastResponse, boolean writeCommand) {
		timer.timeSinceLast();
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
//		if (eValueCutoff>=0.0)
//			blastCommand+="  -evalue " + eValueCutoff;
		blastCommand+=" -out " + outFileName + " -outfmt 5";		
		blastCommand+=" -max_target_seqs " + numHits + " -num_alignments " + numHits + " -num_descriptions " + numHits;		
		blastCommand+=" " + programOptions + StringUtil.lineEnding();
		shellScript.append(blastCommand);
		if (writeCommand)
			logln("blast command: \n" + blastCommand);

		String scriptPath = rootDir + "batchScript" + MesquiteFile.massageStringToFilePathSafe(unique) + ".bat";
		MesquiteFile.putFileContents(scriptPath, shellScript.toString(), true);

		MesquiteTimer timer = new MesquiteTimer();
		timer.start();
		
		
		boolean success = ShellScriptUtil.executeAndWaitForShell(scriptPath, runningFilePath, null, true, getName(),null,null, this);

		if (success){
			String results = MesquiteFile.getFileContentsAsString(outFilePath);
			if (blastResponse!=null){
				blastResponse.setLength(0);
				blastResponse.append(results);
			}
		}
		deleteSupportDirectory();
		getProject().decrementProjectWindowSuppression();
		logln("Blast completed in " +timer.timeSinceLastInSeconds()+" seconds");
	}	
	
	/*.................................................................................................................*/
	public  void postProcessingCleanup(BLASTResults blastResult){
		if (useIDInDefinition){
			blastResult.setIDFromDefinition("|", 2);
			blastResult.setAccessionFromDefinition("|", 4);
		}
	}


	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true;
	}

	public String getName() {
		return "Blast Local Server";
	}

	public String getExplanation() {
		return "Blasts a blast database on the same computer as Mesquite.";
	}

	public boolean continueShellProcess(Process proc) {
		return true;
	}


}
