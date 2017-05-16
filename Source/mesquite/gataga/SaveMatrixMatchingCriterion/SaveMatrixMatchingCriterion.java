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
	
	static final int MINDISTANCE=0;
	static final int MAXDISTANCE=0;
	static final int NTHDISTANCE=1;
	static final int AVGDISTANCE=2;
	int maxDistanceCriterion = MAXDISTANCE;
	int minDistanceCriterion = MINDISTANCE;
	
//	boolean useMaximumDistance = true;
	double maxDistanceThreshold = 1.0;
	double minDistanceThreshold = 0.0;
	double fractionApplicable = 0.9;
	int minimumNumberOfSequences = 4;
	int nthDistance = 1;
	boolean verboseReport = false;
	boolean appendAvgDivergence = true;
	boolean orderedDistances = false;

	boolean writeOnlyWindow = false;
	boolean sequesterMatchedFiles = false;
	int windowEdgeBuffer = 0;
	

	
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
		ExtensibleDialog dialog =  new ExtensibleDialog(containerOfModule(), "Export Matrices Matching Criteria", buttonPressed);
		String message = "This will examine each file and export those that match the specified distance criteria.";
		dialog.addLargeTextLabel(message);
		dialog.addBlankLine();
		dialog.suppressNewPanel();
		
		dialog.appendToHelpString("The window edge buffer is the minimum distance that window needs to be from the edge of the matrix.  ");
		dialog.appendToHelpString("If no acceptable window is far enough away from the edge, then the one furthest from the edge will be used instead.  ");
		dialog.appendToHelpString("If verbose report is requested, then appended to the line for a file that meets the criteria will be the start of the window, and the end of the window, the average pairwise distance, followed by a list of pairwise distances. ");
		dialog.appendToHelpString("If distances are not reordered for the verbose report, then the order of distance is 1 vs 2, 1 vs 3, ... 1 vs n, 2 vs 3, 2 vs 4...");

		IntegerField windowSizeField = dialog.addIntegerField("Window size", windowSize, 12, 1, MesquiteInteger.infinite);
		IntegerField windowEdgeBufferField = dialog.addIntegerField("Window edge buffer", windowEdgeBuffer, 12, 0, MesquiteInteger.infinite);
		IntegerField minimumNumberSeqField = dialog.addIntegerField("Minimum number of sequences represented in window", minimumNumberOfSequences, 12, 1, MesquiteInteger.infinite);
		DoubleField fractionApplicableField = dialog.addDoubleField("Minimum fraction of data in the window for sequence", fractionApplicable, 12, 0.0, 1.0);
		
		dialog.addHorizontalLine(1);
		
		DoubleField maxDistanceThresholdField = dialog.addDoubleField("Maximum distance threshold", maxDistanceThreshold, 12, 0.0, MesquiteDouble.infinite);
		dialog.addLabel("Distance to examine:", Label.CENTER);
		RadioButtons maxDistanceCriterionRB = dialog.addRadioButtons(new String[] {"largest distance", "nth largest distance", "average distance"}, maxDistanceCriterion);
		DoubleField minDistanceThresholdField = dialog.addDoubleField("Minimum distance threshold", minDistanceThreshold, 12, 0.0, MesquiteDouble.infinite);
		dialog.addLabel("Distance to examine:", Label.CENTER);
		RadioButtons minDistanceCriterionRB = dialog.addRadioButtons(new String[] {"smallest distance", "nth smallest distance", "average distance"}, maxDistanceCriterion);
	//	Checkbox getMaximumDistanceCheckbox = dialog.addCheckBox("Use maximum (as opposed to average) distance" , useMaximumDistance);
		IntegerField nthDistanceField = dialog.addIntegerField("value of n for nth distance calculation", nthDistance, 12, 1, MesquiteInteger.infinite);

		dialog.addHorizontalLine(1);

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
		Checkbox writeOnlyWindowCheckbox = dialog.addCheckBox("export only the matching window of data" , writeOnlyWindow);
		Checkbox appendAvgDivergenceCheckbox = dialog.addCheckBox("add average divergence to exported file name" , appendAvgDivergence);
		Checkbox sequesterMatchedFilesCheckbox = dialog.addCheckBox("sequester original files that matched criteria" , sequesterMatchedFiles);
		Checkbox verboseReportCheckbox = dialog.addCheckBox("verbose report" , verboseReport);
		Checkbox orderedDistancesCheckbox = dialog.addCheckBox("reorder distances in ascending order in verbose report" , orderedDistances);

		dialog.addBlankLine();
		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			windowSize = windowSizeField.getValue();
			maxDistanceThreshold = maxDistanceThresholdField.getValue();
			maxDistanceCriterion = maxDistanceCriterionRB.getValue();
			minDistanceThreshold = minDistanceThresholdField.getValue();
			minDistanceCriterion = minDistanceCriterionRB.getValue();
			nthDistance = nthDistanceField.getValue();
	//		useMaximumDistance=getMaximumDistanceCheckbox.getState();
			minimumNumberOfSequences=minimumNumberSeqField.getValue();
			exporterString = exporterChoice.getSelectedItem();
			writeOnlyWindow = writeOnlyWindowCheckbox.getState();
			sequesterMatchedFiles = sequesterMatchedFilesCheckbox.getState();
			verboseReport = verboseReportCheckbox.getState();
			fractionApplicable = fractionApplicableField.getValue();
			appendAvgDivergence = appendAvgDivergenceCheckbox.getState();
			orderedDistances = orderedDistancesCheckbox.getState();
			windowEdgeBuffer = windowEdgeBufferField.getValue();
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
	boolean acceptableRepresentation (CategoricalData data, int icStart, int icEnd) {
		int width = icEnd-icStart+1;
		int count = 0;
		for (int it=0; it<data.getNumTaxa(); it++) {
			int num = data.getNumberApplicableInTaxon(it, icStart, icEnd, false);
			if (1.0*num/width >= fractionApplicable)
				count++;
		}
		return count>=minimumNumberOfSequences;
	}

	/*.................................................................................................................*/
	boolean windowMeetsCriterion(CategoricalData data, int icStart, int icEnd) {
		
		if (!acceptableRepresentation(data,icStart, icEnd))
			return false;
		includeOnlyWindow(data,icStart,icEnd);
		observedStates = data.getMCharactersDistribution();
		PTaxaDistance pDistance = new PTaxaDistance(this, data.getTaxa(), observedStates, true);
		double maxDistance = 0.0;
		switch (maxDistanceCriterion) {
		case MAXDISTANCE:
			maxDistance = pDistance.getMaximumDistance();
			break;
		case NTHDISTANCE:
			maxDistance = pDistance.getNthDistance(nthDistance, true);
			break;
		case AVGDISTANCE:
			maxDistance = pDistance.getAverageDistance();
			break;
		}
		double minDistance = 0.0;
		switch (minDistanceCriterion) {
		case MINDISTANCE:
			minDistance = pDistance.getMaximumDistance();
			break;
		case NTHDISTANCE:
			minDistance = pDistance.getNthDistance(nthDistance, false);
			break;
		case AVGDISTANCE:
			minDistance = pDistance.getAverageDistance();
			break;
		}
		//Debugg.println("DISTANCE: " + distance);
		return minDistance>=minDistanceThreshold && maxDistance<=maxDistanceThreshold;
	}
	/*.................................................................................................................*/
	int distanceFromEdge(CategoricalData data, MesquiteInteger startWindow, MesquiteInteger endWindow) {
		int startDistance = startWindow.getValue();
		int endDistance = data.getNumChars()-(endWindow.getValue()+1);
		if (endDistance<startDistance)
			return endDistance;
		return startDistance;
		
	}

	/*.................................................................................................................*/
	void setDivergences (CategoricalData data, MesquiteDouble avgDivergence, MesquiteString divergences) {
		if ((verboseReport || appendAvgDivergence) && divergences!=null && avgDivergence!=null) {
			observedStates = data.getMCharactersDistribution();
			PTaxaDistance pDistance = new PTaxaDistance(this, data.getTaxa(), observedStates, true);
			avgDivergence.setValue(pDistance.getAverageDistance());
			divergences.setValue(pDistance.getDistanceString(orderedDistances));
		}
	}
	/*.................................................................................................................*/
	boolean meetsCriterion(CategoricalData data, MesquiteInteger startWindow, MesquiteInteger endWindow, MesquiteDouble avgDivergence, MesquiteString divergences) {
		if (data==null || startWindow==null || endWindow==null) 
			return false;
		boolean goodWindowAvailable = false;
		int bestWindowStart = 0;
		int bestWindowEnd=0;
		int bestWindowBuffer = -1;
		while (slideWindow(data,startWindow,endWindow,windowSize)) {
			if (windowMeetsCriterion(data,startWindow.getValue(), endWindow.getValue())) {
				int distanceFromEdge = distanceFromEdge(data, startWindow, endWindow);
				if (distanceFromEdge>= windowEdgeBuffer){  // meets all criteria - no need to look further. 
					setDivergences(data, avgDivergence, divergences);
					return true;
				}
				goodWindowAvailable = true;
				if (distanceFromEdge>bestWindowBuffer && distanceFromEdge<=windowEdgeBuffer) {  // if it beyond the buffer, no need to change
					bestWindowBuffer = distanceFromEdge;
					bestWindowStart = startWindow.getValue();
					bestWindowEnd = endWindow.getValue();
				}
					
			}
		}
		if (goodWindowAvailable) {
			startWindow.setValue(bestWindowStart);
			endWindow.setValue(bestWindowEnd);
			includeOnlyWindow(data,bestWindowStart,bestWindowEnd);
			setDivergences(data, avgDivergence, divergences);
			return true;
		}
		return false;
	}
	/*.................................................................................................................*/
	/** Called to alter file. */
	public boolean processFile(MesquiteFile file, MesquiteString result){
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
				logln("Processing file: " + file.getName());
				MesquiteInteger startWindow = new MesquiteInteger(-1);
				MesquiteInteger endWindow = new MesquiteInteger(windowSize-2);
				MesquiteDouble avgDivergence = new MesquiteDouble(0.0);
				MesquiteString divergences = new MesquiteString();
				if ((test == null || test.isCompatible(data.getStateClass(), getProject(), this)) && meetsCriterion(data, startWindow, endWindow, avgDivergence, divergences)) {
					taxa = data.getTaxa();

					String path = file.getPath();
					directoryPath = file.getDirectoryPathFromFilePath(path);
					String fileName = file.getFileName();
					if (path.endsWith(".nex") || path.endsWith(".fas")){
						path = path.substring(0, path.length()-4);
						fileName = fileName.substring(0, fileName.length()-4);

					}
					String distanceFileName = fileName;
					if (appendAvgDivergence) {
						fileName+="&d"+avgDivergence.getValue();
					}
					if (multiple){
						path = path + (im + 1);
						fileName = fileName + (im + 1);
					}
					fileName = MesquiteFile.getUniqueModifiedFileName(directoryPath+fileName, exporterTask.getStandardFileExtensionForExport());
					fileName = MesquiteFile.getFileNameFromFilePath(fileName);
					distanceFileName = MesquiteFile.getUniqueModifiedFileName(directoryPath+distanceFileName,"ident");
					distanceFileName = MesquiteFile.getFileNameFromFilePath(distanceFileName);


					path = path + "." + exporterTask.preferredDataFileExtension(); 
					if (!StringUtil.blank(exporterTask.preferredDataFileExtension()) && !fileName.endsWith(exporterTask.preferredDataFileExtension())){
						fileName = fileName + "." + exporterTask.preferredDataFileExtension();
					}
					path = MesquiteFile.getDirectoryPathFromFilePath(path) + fileName;
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
					if (result!=null) {
						result.append("*");
						if (verboseReport) {
							result.append("\t"+(startWindow.getValue()+1) + "\t" + (endWindow.getValue()+1));
							result.append("\t"+avgDivergence.getValue()+"\t"+divergences.toString());
							saveDistanceFile(distanceFileName, directoryPath, data, startWindow, endWindow);
						}
					}
					if (sequesterMatchedFiles)
						setPleaseSequester(true);
				}
			}
		}



		resetAllMenuBars();
		getProject().decrementProjectWindowSuppression();
		decrementMenuResetSuppression();

		return true;
	}
	public void saveFile(String exporterName, MesquiteFile file, String fileName, String directoryPath, FileCoordinator coord, boolean usePrevious){
		file.writeExcludedCharacters=!writeOnlyWindow;
		if (exporterName.equals("NEXUS file")) {
			coord.writeFile(file);
		}
		else if (exporterTask instanceof FileInterpreterI) {
			String s = "file = " + StringUtil.tokenize(fileName) + " directory = " + StringUtil.tokenize(directoryPath) + " noTrees";
			if (usePrevious)
				s += " usePrevious";
			((FileInterpreterI)exporterTask).writeExcludedCharacters = !writeOnlyWindow;
			coord.export((FileInterpreterI)exporterTask, file, s);
		}
	}
	
	
	public void saveDistanceFile(String fileName, String directoryPath,CategoricalData data, MesquiteInteger startWindow, MesquiteInteger endWindow){
		includeOnlyWindow(data,startWindow.getValue(),endWindow.getValue());
		observedStates = data.getMCharactersDistribution();
		PTaxaDistance pDistance = new PTaxaDistance(this, data.getTaxa(), observedStates, true);
		StringBuffer sb = new StringBuffer();
		Taxa taxa = data.getTaxa();
		sb.append(data.getNumTaxaWithAnyApplicable(startWindow.getValue(), endWindow.getValue())+"\n");
		Parser parser = new Parser();
		parser.setWhitespaceString(" ");

		for (int taxon1 = 0; taxon1<data.getNumTaxa(); taxon1++) {
			if (data.anyApplicableInRange(startWindow.getValue(), endWindow.getValue(), taxon1)) {
				parser.setString(taxa.getTaxonName(taxon1));
				sb.append(StringUtil.blanksToUnderline(parser.getFirstToken()));
				for (int taxon2=0; taxon2<data.getNumTaxa(); taxon2++) {
					if (data.anyApplicableInRange(startWindow.getValue(), endWindow.getValue(), taxon2)) {
						double distance = pDistance.getDistance(taxon1,taxon2);
						if (MesquiteDouble.isCombinable(distance)) {
							sb.append("\t"+((1.0-distance)*100.0));	
						}
					}
				}
				sb.append("\n");
			}
		}
		MesquiteFile.putFileContents(directoryPath+fileName, sb.toString(), false);
	}


	/*.................................................................................................................*/
	public String getName() {
		return "Export Matrices Matching Criteria";
	}
	/*.................................................................................................................*/
	String distanceCriterionName(boolean max) {
		if (max) {
			switch (maxDistanceCriterion) {
			case MAXDISTANCE:
				return "largest distance";
			case NTHDISTANCE:
				return "nth largest distance";
			case AVGDISTANCE:
				return "average distance";
			}
		} else {
			switch (minDistanceCriterion) {
			case MINDISTANCE:
				return "smallest distance";
			case NTHDISTANCE:
				return "nth smallest distance";
			case AVGDISTANCE:
				return "average distance";
			}
		}
		return "";
	}
	/*.................................................................................................................*/
	public String getNameAndParameters() {
		StringBuffer sb = new StringBuffer();
		if (exporterTask==null)
			sb.append("Export Matrices Matching Criteria");
		else
			sb.append("Export Matrices Matching Criteria (" + exporterTask.getName() + ")");
		sb.append(StringUtil.lineEnding());
		sb.append("  Criteria:" + StringUtil.lineEnding());
		sb.append("    Window Size: " + windowSize+StringUtil.lineEnding());
		sb.append("    Maximum Distance Threshold: " + maxDistanceThreshold+StringUtil.lineEnding());
		sb.append("    Maximum Distance Criterion: " + distanceCriterionName(true)+StringUtil.lineEnding());
		sb.append("    Minimum Distance Threshold: " + minDistanceThreshold+StringUtil.lineEnding());
		sb.append("    Minimum Distance Criterion: " + distanceCriterionName(false)+StringUtil.lineEnding());
		if (maxDistanceCriterion==NTHDISTANCE)
			sb.append("    Value of n: " + nthDistance+StringUtil.lineEnding());
		sb.append("    Minimum Number of Sequences: " + minimumNumberOfSequences+StringUtil.lineEnding());
		sb.append("    Minimum fraction of data in a sequence in the window for it to count: " + fractionApplicable+StringUtil.lineEnding());
		if (writeOnlyWindow)
			sb.append("    For each sequence write only the region in the matching window "+StringUtil.lineEnding());
		else
			sb.append("    Write entire sequences "+StringUtil.lineEnding());
		return sb.toString();
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Manages file exporting modules to export all matrices in a file that match defined distance criteria." ;
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



