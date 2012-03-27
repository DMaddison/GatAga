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
		
		MolecularDataUtil.reverseComplementSequencesIfNecessary(data, module, taxa, 0, taxa.getNumTaxa(), proteinCoding);
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

			importer.readFileCore(parser, fileToRead, data,  taxa, numTaxa, progIndicator, arguments, true);	

			boolean proteinCoding = true;  // query about this  
			processData((DNAData)data, taxa, proteinCoding);   // query about this

						
			writeFile(fileToWrite);
			MesquiteFile.appendFileContents(fileToWrite.getPath(), appendToFile(proteinCoding), true);
			Debugg.println("================= wrote matrix ");
			
			data.deleteMe(false);
			taxa.deleteMe(false);		

		}
		decrementMenuResetSuppression();
	}
	/*.................................................................................................................*/
	private void processDirectory(String directoryPath){
		if (StringUtil.blank(directoryPath))
			return;
		File directory = new File(directoryPath);
		//	parser = new Parser();

		boolean abort = false;
		String path = "";
		if (directory!=null) {
			if (directory.exists() && directory.isDirectory()) {
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



