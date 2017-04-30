/* Gataga source code.  Copyright 2017 and onward, D. Maddison and D. Maddison. 
 */
package mesquite.gataga.SaveMatrixMatchingCriterion; 


import java.awt.*;

import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.CategoricalState;
import mesquite.distance.PDistance.PDistance;
import mesquite.distance.lib.DNATaxaDistFromMatrix;
import mesquite.distance.lib.DNATaxaDistance;
import mesquite.lib.*;
import mesquite.lib.characters.CharInclusionSet;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.characters.MCharactersDistribution;
import mesquite.lib.duties.*;
import mesquite.lib.table.*;

/* ======================================================================== */
public class SaveMatrixMatchingCriterion extends FileProcessor {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e2 = registerEmployeeNeed(FileInterpreterI.class, getName() + " needs a file exporter.",
				null);
		e2.setPriority(2);
	}
	MesquiteTable table;
	CharacterData data;
	MesquiteSubmenuSpec mss= null;
	FileInterpreter exporterTask;
	
	int windowSize = 100;
	boolean getNthLongestDistance = true;
	double distanceThreshold = 0.0;
	int nthDistance = 1;

	
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		/*if (arguments !=null) {
			exporterTask = (FileInterpreter)hireNamedEmployee(FileInterpreter.class, arguments);
			if (exporterTask == null)
				return sorry(getName() + " couldn't start because the requested data alterer wasn't successfully hired.");
		}
		else {
			exporterTask = (FileInterpreter)hireEmployee(FileInterpreter.class, "Exporter");
			if (exporterTask == null)
				return sorry(getName() + " couldn't start because no exporter obtained.");
		}*/
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return false;  
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return NEXTRELEASE;  
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
	public Snapshot getSnapshot(MesquiteFile file) { 
		Snapshot temp = new Snapshot();
		temp.addLine("setFileInterpreter ", exporterTask);  
		return temp;
	}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Sets the module that alters data", "[name of module]", commandName, "setFileInterpreter")) {
			FileInterpreter temp =  (FileInterpreter)replaceEmployee(FileInterpreter.class, arguments, "Exporter", exporterTask);
			if (temp!=null) {
				exporterTask = temp;
				return exporterTask;
			}

		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}

	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}

	/*.................................................................................................................*/
	String exporterString = "NEXUS file";
	String directoryPath;
	
	boolean queryOptions(){
		directoryPath = getProject().getHomeDirectoryName();
		Taxa taxa = null;
		if (getProject().getNumberTaxas()==0) {
			discreetAlert("Data matrices cannot be exported until taxa exist in file.");
			return false;
		}
		else 
			taxa = getProject().chooseTaxa(containerOfModule(), "For which block of taxa do you want to save copies of character matrices?");
		if (taxa == null)
			return false;

		incrementMenuResetSuppression();
		getProject().incrementProjectWindowSuppression();

		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog =  new ExtensibleDialog(containerOfModule(), "Export Matrices Matching Criterion", buttonPressed);
		String message = "This will export matrices to separate files";
		dialog.addLargeTextLabel(message);
		dialog.addBlankLine();
		dialog.suppressNewPanel();

		IntegerField windowSizeField = dialog.addIntegerField("Window size", windowSize, 12, 1, MesquiteInteger.infinite);
		DoubleField distanceThresholdField = dialog.addDoubleField("Maximum distance", distanceThreshold, 12, 0.0, MesquiteDouble.infinite);
		Checkbox getNthLongestDistanceField = dialog.addCheckBox("use nth distance" , getNthLongestDistance);
		IntegerField nthDistanceField = dialog.addIntegerField("nth distance", nthDistance, 12, 1, MesquiteInteger.infinite);

		MesquiteModule[] fInterpreters = getFileCoordinator().getImmediateEmployeesWithDuty(FileInterpreterI.class);
		int count=1;
		for (int i=0; i<fInterpreters.length; i++) {
			if (((FileInterpreterI)fInterpreters[i]).canExportEver())
				count++;
		}
		String [] exporterNames = new String[count];
		exporterNames[0] = "NEXUS file";
		count = 1;
		for (int i=0; i<fInterpreters.length; i++)
			if (((FileInterpreterI)fInterpreters[i]).canExportEver()) {
				exporterNames[count] = fInterpreters[i].getName();
				count++;
			}

		Choice exporterChoice = dialog.addPopUpMenu ("File Format", exporterNames, 0);
		exporterChoice.select(exporterString);
		dialog.addBlankLine();
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			windowSize = windowSizeField.getValue();
			distanceThreshold = distanceThresholdField.getValue();
			nthDistance = nthDistanceField.getValue();
			getNthLongestDistance=getNthLongestDistanceField.getState();
			exporterString = exporterChoice.getSelectedItem();
		}

		

		dialog.dispose();
		dialog = null;
		
/*
 * 		if (buttonPressed.getValue()==0)  {
			directoryPath = MesquiteFile.chooseDirectory("Where to save files?"); //MesquiteFile.saveFileAsDialog("Base name for files (files will be named <name>1.nex, <name>2.nex, etc.)", baseName);
			if (StringUtil.blank(directoryPath))
				return false;
		}
*/
		
		return (buttonPressed.getValue()==0);

	}
	/*.................................................................................................................*/
	boolean slideWindow(CategoricalData data, MesquiteInteger startWindow, MesquiteInteger endWindow, int windowSize) {
		if (startWindow!=null && endWindow!=null){
			startWindow.add(1);
			endWindow.add(1);
			if (endWindow.getValue()>=data.getNumChars() && startWindow.getValue()>0)
				return false;
			else
				return true;
		}
		return false;
	}
	
	
	MCharactersDistribution observedStates;
	
	/*.................................................................................................................*/
	void includeOnlyWindow(CategoricalData data, int icStart, int icEnd) {
		CharInclusionSet inclusionSet = (CharInclusionSet) data.getCurrentSpecsSet(CharInclusionSet.class);
		if (inclusionSet == null) {
			inclusionSet= new CharInclusionSet("Inclusion Set", data.getNumChars(), data);
			inclusionSet.selectAll();
			inclusionSet.addToFile(data.getFile(), getProject(), module.findElementManager(CharInclusionSet.class)); //THIS
			data.setCurrentSpecsSet(inclusionSet, CharInclusionSet.class);
		}
		if (inclusionSet != null) {
			for (int i=0; i<data.getNumChars(); i++) {
				if (i>=icStart && i<=icEnd) 
					inclusionSet.setSelected(i, true);
				else 
					inclusionSet.setSelected(i, false);
			}
		}
	}

	/*.................................................................................................................*/
	boolean windowMeetsCriterion(CategoricalData data, int icStart, int icEnd) {
		
		includeOnlyWindow(data,icStart,icEnd);
		observedStates = data.getMCharactersDistribution();
		PTaxaDistance pDistance = new PTaxaDistance(this, data.getTaxa(), observedStates, true);
		double distance = 0.0;
		if (getNthLongestDistance) {
			distance = pDistance.getNthLongestDistance(nthDistance);
		} else {
			distance = pDistance.getAverageDistance();
		}
		return distance<=distanceThreshold;
	}

	/*.................................................................................................................*/
	boolean meetsCriterion(CategoricalData data) {
		if (data==null) 
			return false;
		MesquiteInteger startWindow = new MesquiteInteger(-1);
		MesquiteInteger endWindow = new MesquiteInteger(windowSize-2);
		while (slideWindow(data,startWindow,endWindow,windowSize)) {
			if (windowMeetsCriterion(data,startWindow.getValue(), endWindow.getValue())) {
				return true;
			}
		}
		return false;
	}
	/*.................................................................................................................*/
	/** Called to alter file. */
	public boolean processFile(MesquiteFile file){
		boolean usePrevious = false;
		if (okToInteractWithUser(CAN_PROCEED_ANYWAY, "Querying about options")){ //need to check if can proceed
			if (!queryOptions())
				return false;

		}
		else
			usePrevious = true;
		MesquiteProject proj = file.getProject();
		FileCoordinator coord = getFileCoordinator();
		exporterTask = (FileInterpreter)coord.findEmployeeWithName(exporterString);
		if (exporterTask == null)
			return false;
		Taxa taxa;
		if (proj == null)
			return false;
		getProject().incrementProjectWindowSuppression();
		incrementMenuResetSuppression();
		CompatibilityTest test = exporterTask.getCompatibilityTest();
		int numMatrices = proj.getNumberCharMatrices(file);
		boolean multiple = numMatrices>1;
		for (int im = 0; im < numMatrices; im++){
			if (proj.getCharacterMatrix(file, im) instanceof CategoricalData){
				CategoricalData data = (CategoricalData)proj.getCharacterMatrix(file, im);
				Debugg.println("file: " + file.getName());
				if ((test == null || test.isCompatible(data.getStateClass(), getProject(), this)) && meetsCriterion(data)) {
					taxa = data.getTaxa();

					String path = file.getPath();
					directoryPath = file.getDirectoryPathFromFilePath(path);
					String fileName = file.getFileName();
					if (path.endsWith(".nex") || path.endsWith(".fas")){
						path = path.substring(0, path.length()-4);
						fileName = fileName.substring(0, fileName.length()-4);

					}
					if (multiple){
						path = path + (im + 1);
						fileName = fileName + (im + 1);
					}

					path = path + "." + exporterTask.preferredDataFileExtension(); 
					if (!StringUtil.blank(exporterTask.preferredDataFileExtension()) && !fileName.endsWith(exporterTask.preferredDataFileExtension()))
						fileName = fileName + "." + exporterTask.preferredDataFileExtension();
					MesquiteFile tempDataFile = (MesquiteFile)coord.doCommand("newLinkedFile", StringUtil.tokenize(path), CommandChecker.defaultChecker); //TODO: never scripting???
					TaxaManager taxaManager = (TaxaManager)findElementManager(Taxa.class);
					Taxa newTaxa =taxa.cloneTaxa();
					newTaxa.addToFile(tempDataFile, null, taxaManager);

					tempDataFile.exporting =1;
					if (data.getNumChars()  == 0){
						MesquiteMessage.warnUser("Matrix to be written has no characters; it will not be written.  Name: " + data.getName() + " (type: " + data.getDataTypeName() + ")");
						return false;
					}
					CharacterData			newMatrix = data.cloneData();
					if (newMatrix == null){
						MesquiteMessage.warnUser("Matrix NOT successfully cloned for file saving: " + data.getName() + " (type: " + data.getDataTypeName() + "; " + data.getNumChars() + " characters)");
						return false;
					}
					newMatrix.setName(data.getName());

					logln("Saving file " + path);	
					newMatrix.addToFile(tempDataFile, getProject(), null);
					data.copyCurrentSpecsSetsTo(newMatrix);
					tempDataFile.setPath(path);


					//should allow choice here
					saveFile(exporterString, tempDataFile, fileName, directoryPath, coord, usePrevious); 
					tempDataFile.exporting = 2;  //to say it's the second or later export in sequence

					newMatrix.deleteMe(false);
					newMatrix = null;
					tempDataFile.close();
					System.gc();
				}
			}
		}



		resetAllMenuBars();
		getProject().decrementProjectWindowSuppression();
		decrementMenuResetSuppression();

		return true;
	}
	public void saveFile(String exporterName, MesquiteFile file, String fileName, String directoryPath, FileCoordinator coord, boolean usePrevious){
		if (exporterName.equals("NEXUS file"))
			coord.writeFile(file);
		else if (exporterTask instanceof FileInterpreterI) {
			String s = "file = " + StringUtil.tokenize(fileName) + " directory = " + StringUtil.tokenize(directoryPath) + " noTrees";
			if (usePrevious)
				s += " usePrevious";
			coord.export((FileInterpreterI)exporterTask, file, s);
		}
	}


	/*.................................................................................................................*/
	public String getName() {
		return "Export Matrices Matching Criterion";
	}
	/*.................................................................................................................*/
	public String getNameAndParameters() {
		if (exporterTask==null)
			return "Export Matrices Matching Criterion";
		else
			return "Export Matrices Matching Criterion (" + exporterTask.getName() + ")";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Manages file exporting modules to export all matrices in a file that match a criterion." ;
	}

}


class PTaxaDistance extends DNATaxaDistance {
	MesquiteModule ownerModule;
	
	public PTaxaDistance(MesquiteModule ownerModule, Taxa taxa, MCharactersDistribution observedStates, boolean estimateAmbiguityDifferences){
		super(ownerModule, taxa, observedStates,estimateAmbiguityDifferences, false);
		this.ownerModule = ownerModule;
		MesquiteDouble N = new MesquiteDouble();
		MesquiteDouble D = new MesquiteDouble();
		setEstimateAmbiguityDifferences(true);


		for (int taxon1=0; taxon1<getNumTaxa(); taxon1++) {
			for (int taxon2=taxon1; taxon2<getNumTaxa(); taxon2++) {
				double[][] fxy = calcPairwiseDistance(taxon1, taxon2, N, D);
				distances[taxon1][taxon2]= D.getValue();
			}
		}
		copyDistanceTriangle();

	}
	
	public String getName() {
		return ownerModule.getName();
	}

}



