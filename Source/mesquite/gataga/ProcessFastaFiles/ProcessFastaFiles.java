/* Mesquite GatAga source code.  Copyright 2012 David Maddison
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


	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;
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


	/*.................................................................................................................*/
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
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Add File Alterer?",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		if (count == 0){
			dialog.addLabel("For each file processed, do you want to alter it?");
			dialog.completeAndShowDialog("Yes", "No", null, "No");
		}
		else {
			dialog.addLabel("For each file processed, do you want to add another step in altering it?");
			dialog.addLabel("The altering steps already requested are:");
			String[] steps = new String[fileAlterers.size()];
			for (int i = 0; i<steps.length; i++)
				steps[i] = "(" + (i+1) + ") " + ((FileAlterer)fileAlterers.elementAt(i)).getName();
			dialog.addList (steps, null, null, 8);
			dialog.completeAndShowDialog("Add", "Done", null, "Done");
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	Vector fileAlterers = null;
	private void hireAlterersIfNeeded(){
		int count = 0;
		if (fileAlterers == null)
			while (showAlterDialog(count)){
				if (fileAlterers == null)
					fileAlterers = new Vector();
				count++;
				FileAlterer alterer = (FileAlterer)project.getCoordinatorModule().hireEmployee(FileAlterer.class, "File processor (" + count+ ")");
				fileAlterers.addElement(alterer);
			}
	}
	/*.................................................................................................................*/
	public void processFile(MesquiteFile fileToRead, String arguments) {
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

			hireAlterersIfNeeded();  //needs to be done here after file read in case alterers need to know if there are matrices etc in file

			boolean proteinCoding = true;  // query about this  
			if (fileAlterers != null){
				boolean success = true;
				for (int i= 0; i< fileAlterers.size() && success; i++){
					FileAlterer alterer = (FileAlterer)fileAlterers.elementAt(i);
					success = alterer.alterFile(fileToWrite);

					if (!success)
						Debugg.println("Sorry,  " + alterer.getName() + " did not succeed in altering the file " + fileToRead.getFileName()+".nex");
					else
						Debugg.println("" + alterer.getName() + " successfully altered the file " + fileToRead.getFileName()+".nex");
				}
			}
			//processData((DNAData)data, taxa, proteinCoding);   // query about this


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
		if (directory!=null) {
			if (directory.exists() && directory.isDirectory()) {
				//Hire file alterers

				String[] files = directory.list();
				progIndicator = new ProgressIndicator(null,"Processing Folder of Fasta Files", files.length);
				progIndicator.start();
				for (int i=0; i<files.length; i++) {
					progIndicator.setCurrentValue(i);
					if (progIndicator.isAborted())
						abort = true;
					if (abort)
						break;
					if (files[i]!=null) {
						path = directoryPath + MesquiteFile.fileSeparator + files[i];
						File cFile = new File(path);
						MesquiteFile file = new MesquiteFile();
						file.setPath(path);
						if (cFile.exists() && !cFile.isDirectory() && (!files[i].startsWith("."))) {
							processFile( file,null);
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
		fileToWrite.setPath(directoryPath+MesquiteFile.fileSeparator+"temp.nex");
		processDirectory(directoryPath);

		fireEmployee(importer);
		if (success){
			project.autosave = true;
			return project;
		}
		project.developing = false;
		return null;
	}

	/*.................................................................................................................*/
	public String getName() {
		return "Process Fasta Files";
	}
	/*.................................................................................................................*/
	public String getNameForMenuItem() {
		return "Process Fasta Files...";
	}
	/*.................................................................................................................*/
	public boolean showCitation() {
		return false;
	}

	/*.................................................................................................................*/
	public String getExplanation() {
		return "Processes a folder of fasta files.";
	}
}




