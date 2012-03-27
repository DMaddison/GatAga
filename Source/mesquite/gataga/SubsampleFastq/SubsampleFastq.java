package mesquite.gataga.SubsampleFastq;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.io.File;
import java.util.Random;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.DNAData;
import mesquite.charMatrices.ManageCodonsBlock.ManageCodonsBlock;
import mesquite.gataga.lib.BlastSeparateCriterion;
import mesquite.io.lib.InterpretFasta;
import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.CharacterState;
import mesquite.lib.duties.CharactersManager;
import mesquite.lib.duties.TaxaManager;
import mesquite.lib.duties.UtilitiesAssistant;

public class SubsampleFastq extends UtilitiesAssistant {

	RandomBetween rng = new RandomBetween();
	double sampleFreq = 0.5;
	boolean pairedFiles = false;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		loadPreferences();
		addMenuItem(null, "Subsample Fastq Files...", makeCommand("subsample", this));
		rng.setSeed(System.currentTimeMillis());
		return true;
	}

	/*.................................................................................................................*/
	public void processSingleXMLPreference (String tag, String content) {
		if ("pairedFiles".equalsIgnoreCase(tag))
			pairedFiles = MesquiteBoolean.fromTrueFalseString(content);
		else if ("sampleFreq".equalsIgnoreCase(tag))
			sampleFreq = MesquiteDouble.fromString(content);		
	}
	/*.................................................................................................................*/
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer();	
		StringUtil.appendXMLTag(buffer, 2, "pairedFiles", pairedFiles);  
		StringUtil.appendXMLTag(buffer, 2, "sampleFreq", sampleFreq);  
		return buffer.toString();
	}
	/*.................................................................................................................*/
	public boolean queryOptions() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), getName() + " Options",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel(getName() + " Options");

		DoubleField sampleFreqField = dialog.addDoubleField("Probability of sampling a read: ", sampleFreq, 8, 0.0, 1.0);
		Checkbox pairedFilesBox = dialog.addCheckBox("Paired Files", pairedFiles);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			pairedFiles = pairedFilesBox.getState();
			sampleFreq = sampleFreqField.getValue();
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Subsample Fastq Files", null, commandName, "subsample")) {
			if (!MesquiteThread.isScripting())
				if (!queryOptions()) 
					return null;
			subsample();
		}
		else
			return super.doCommand(commandName, arguments, checker);
		return null;
	}
	/*.................................................................................................................*/
	public void processFile(MesquiteFile[] fileToRead, MesquiteFile[] fileToWrite) {
		if (fileToRead==null || fileToRead[0]==null)
			return;
		incrementMenuResetSuppression();
		ProgressIndicator progIndicator = new ProgressIndicator(null,"Subsampling File "+ fileToRead[0].getName(), fileToRead[0].existingLength());
		progIndicator.start();
		fileToRead[0].linkProgressIndicator(progIndicator);
		int numFiles = fileToRead.length;
		MesquiteTimer timer = new MesquiteTimer();
		timer.start();


		boolean filesOpened = true;
		for (int i=0; i<numFiles; i++)
			if (!fileToRead[i].openReading()){
				filesOpened = false;
				break;
			}

		if (filesOpened) {
			for (int i=0; i<numFiles; i++) {
				MesquiteFile.putFileContents(fileToWrite[i].getPath(), "", true);
			}

			StringBuffer sb = new StringBuffer(1000);

			String[] line = new String[numFiles];
			for (int i=0; i<numFiles; i++) {
				sb.setLength(0);
				fileToRead[i].readLine(sb);
				line[i] = sb.toString();
				if (StringUtil.blank(line[i] ))
					line[i]  = fileToRead[i].readNextDarkLine();		
			}
			boolean abort = false;
			long pos = 0;
			long total = 0;
			long count = 0;

			while (!StringUtil.blank(line[0]) && !abort && total<1000000) {
				if (progIndicator!=null) {
					if (total % 10000 == 0 && total>0) {
						double ratio = 1.0*count/total;
						progIndicator.setText("Sampled " +count+" of " + total + " ("+ MesquiteDouble.toStringDigitsSpecified(ratio, 4)+")");
					}
					if (total % 1000 == 0)
						progIndicator.setCurrentValue(pos);
				}
				total++;
				if (line[0].charAt(0)=='@') {
				///	Debugg.println("read " + total);
				}
				boolean copy=rng.getDouble(0.0, 1.0)<=sampleFreq;

				if (copy) {
					count++;
					///Debugg.println("   Sample" + total);
				//	if (total % 10000 == 0 && total>0) {
				//		logln("Sampled " +count+" of " + total + " ("+ 1.0*count/total+")");
				//	}
					for (int i=0; i<numFiles; i++) {
						MesquiteFile.appendFileContents(fileToWrite[i].getPath(), line[i]+StringUtil.lineEnding(), true);
					}
					//Debugg.println("   " + line);

				}

				for (int i=0; i<numFiles; i++) {
					for (int j=1; j<=3; j++) {
						line[i] = fileToRead[i].readNextDarkLine();		
						if (copy){
							MesquiteFile.appendFileContents(fileToWrite[i].getPath(), line[i]+StringUtil.lineEnding(), true);
						}
					}
				}

				for (int i=0; i<numFiles; i++) 
					line[i] = fileToRead[i].readNextDarkLine();		
				pos = fileToRead[0].getFilePosition();

				if (fileToRead[0].getFileAborted()) {
					abort = true;
				}
			}
			
			logln("Number of reads sampled:  " + count);
			logln("Total number of reads:  " + total);
			if (total>0)
				logln("Ratio sampled:  " + 1.0*count/total);
			logln("Time taken:  " + timer.timeSinceVeryStartInHoursMinutesSeconds());

		}
		if (progIndicator!=null)
			progIndicator.goAway();
		decrementMenuResetSuppression();
	}
	/*.................................................................................................................*/
	public void subsample() {
		int numFiles = 1;
		if (pairedFiles) 
			numFiles=2;
		MesquiteFile[] fileToWrite = new MesquiteFile[numFiles];
		MesquiteFile[] fileToRead = new MesquiteFile[numFiles];
		for (int i=0; i<numFiles; i++) {
			MesquiteString directory = new MesquiteString();
			MesquiteString fileName = new MesquiteString();
			String fullPath = MesquiteFile.openFileDialog("Choose FASTQ file " + (i+1) + " to sample", directory, fileName);
			fileToWrite[i] = new MesquiteFile();
			fileToWrite[i].setPath(directory+"Sampled"+fileName);
			fileToRead[i] = new MesquiteFile();
			fileToRead[i].setPath(fullPath);
		}
		processFile(fileToRead, fileToWrite);

	}

	/*.................................................................................................................*
	public void processSingleXMLPreference (String tag, String content) {
		if ("translationFilePath".equalsIgnoreCase(tag))
			translationFilePath = StringUtil.cleanXMLEscapeCharacters(content);
		preferencesSet = true;
	}
	/*.................................................................................................................*
	public String preparePreferencesForXML () {
		StringBuffer buffer = new StringBuffer(200);
		StringUtil.appendXMLTag(buffer, 2, "translationFilePath", translationFilePath);  
		preferencesSet = true;
		return buffer.toString();
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}

	public String getName() {
		return "Subsample Fastq files";
	}

}
