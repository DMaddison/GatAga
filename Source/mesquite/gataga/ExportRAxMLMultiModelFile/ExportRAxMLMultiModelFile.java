package mesquite.gataga.ExportRAxMLMultiModelFile;

/*~~  */

import java.util.*;
import java.awt.*;

import mesquite.io.lib.IOUtil;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;



public class ExportRAxMLMultiModelFile extends FileInterpreterI {
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;  //make this depend on taxa reader being found?)
	}

	public boolean isPrerelease(){
		return true;
	}
	public boolean isSubstantive(){
		return true;
	}
	/*.................................................................................................................*/
	public String preferredDataFileExtension() {
		return "txt";
	}
	/*.................................................................................................................*/
	public boolean canExportEver() {  
		return true;  //
	}
	/*.................................................................................................................*/
	public boolean canExportProject(MesquiteProject project) {  
		return (project.getNumberCharMatricesVisible(MolecularState.class) > 0) ;
	}

	/*.................................................................................................................*/
	public boolean canExportData(Class dataClass) {  
		return MolecularData.class.isAssignableFrom(dataClass);
	}
	/*.................................................................................................................*/
	public boolean canImport() {  
		return false;
	}

	/*.................................................................................................................*/
	public void readFile(MesquiteProject mf, MesquiteFile file, String arguments) {
	}


	/* ============================  exporting ============================*/
	/*.................................................................................................................*/
	
/*
	public boolean getExportOptions(boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExporterDialog exportDialog = new ExporterDialog(this,containerOfModule(), "Export Fused Matrices as FASTA", buttonPressed);
		exportDialog.setSuppressLineEndQuery(false);
		exportDialog.setDefaultButton(null);
		exportDialog.appendToHelpString("Fusing matrices into a single FASTA file, and exporting one sequence from each matrix for one taxon, then the next taxon, and so on.");
		exportDialog.addHorizontalLine(1);
		Checkbox removeExcludedBox = exportDialog.addCheckBox("Remove excluded characters", removeExcluded);
		//		Checkbox convertToMissing = exportDialog.addCheckBox("convert partial ambiguities to missing", convertAmbiguities);

		exportDialog.completeAndShowDialog(false, taxaSelected);

		boolean ok = (exportDialog.query(false, taxaSelected)==0);

		//		convertAmbiguities = convertToMissing.getState();
		removeExcluded = removeExcludedBox.getState();

		exportDialog.dispose();
		lineEnding = getLineEnding();
		return ok;
	}	

 	/*.................................................................................................................*/
	public Taxa findTaxaToExport(MesquiteFile file, String arguments) { 
		return getProject().chooseTaxa(containerOfModule(), "Select taxa to export");

	}

	/*.................................................................................................................*/
	public boolean exportFile(MesquiteFile file, String arguments) { //if file is null, consider whole project open to export
		Arguments args = new Arguments(new Parser(arguments), true);
		boolean usePrevious = args.parameterExists("usePrevious");

		
		CharacterData firstData = getProject().getCharacterMatrixVisible(0, MolecularState.class); 

		StringBuffer outputBuffer = new StringBuffer();
		outputBuffer.append(IOUtil.getMultipleModelRAxMLString(this, firstData, true));

		if (outputBuffer!=null) {
			saveExportedFileWithExtension(outputBuffer,  arguments, "MultiModel", "txt", null);

			return true;
		}
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
	public String getName() {
		return "Export RAxML Multiple Model File";
	}
	/*.................................................................................................................*/

	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports multiple model file for the first molecular data matrix in the file." ;
	}
	/*.................................................................................................................*/


}


