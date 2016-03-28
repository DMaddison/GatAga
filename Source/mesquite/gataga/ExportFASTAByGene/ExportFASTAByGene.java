package mesquite.gataga.ExportFASTAByGene;

/*~~  */

import java.util.*;
import java.awt.*;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;
import mesquite.categ.lib.*;



public class ExportFASTAByGene extends FileInterpreterI {
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
		return "fas";
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
		return CategoricalData.class.isAssignableFrom(dataClass);
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
	boolean convertAmbiguities = false;
	boolean useData = true;
	String addendum = "";
	String fileName = "untitled.fas";
	String lineEnding;
	
	boolean removeExcluded = false;

	public boolean getExportOptions(boolean taxaSelected){
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExporterDialog exportDialog = new ExporterDialog(this,containerOfModule(), "Export FASTA files by gene", buttonPressed);
		exportDialog.setSuppressLineEndQuery(false);
		exportDialog.setDefaultButton(null);
		exportDialog.appendToHelpString("Fusing matrices into multiple FASTA files, and exporting one matrix, then the next, and so on.");
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
	public  String getUnassignedSymbol(){
		return "N";
	}

	protected boolean includeGaps = false;
	protected boolean convertMultStateToMissing = true;
	protected boolean includeOnlyTaxaWithData = true;// TO DO: also have the option of only writing taxa with data in them

	/*.................................................................................................................*/

	protected boolean taxonHasData(CharacterData data, int it){
		for (int ic = 0; ic<data.getNumChars(); ic++) {
			if (!writeOnlySelectedData || (data.getSelected(ic))){
				if (!data.isUnassigned(ic, it) && !data.isInapplicable(ic, it))
					return true;
			}
		}
		return false;
	}

	protected String getSupplementForTaxon(Taxa taxa, int it){
		return "";
	}
	protected String getTaxonName(Taxa taxa, int it, CharacterData data){
			return taxa.getTaxonName(it);
			//return ParseUtil.tokenize(taxa.getTaxonName(it)+uniqueSuffix);
	}

	/*.................................................................................................................*/

 	public  StringBuffer getDataAsFileText(MesquiteFile file, Taxa taxa, int matrixNumber) {

		StringBuffer outputBuffer = new StringBuffer();
		int numMatrices = getProject().getNumberCharMatrices(taxa);

		boolean firstCategorical = true;
		//int numTaxa = taxa.getNumTaxa();
		int numTaxaToAddForEachMatrix = 1;
		int numTaxa = taxa.getNumTaxa();
		int counter = 1;


			for (int it = 0; it<numTaxa; it++){  //go through the taxa
				if (!writeOnlySelectedTaxa || (taxa.getSelected(it))) {   // find the taxa to write
					CharacterData data = getProject().getCharacterMatrix(taxa, matrixNumber);    //
					if ((data instanceof CategoricalData)) {  // write data for this matrix
						if (!includeOnlyTaxaWithData || taxonHasData(data, it)){
							boolean isProtein = data instanceof ProteinData;
							ProteinData pData =null;
							if (isProtein)
								pData = (ProteinData)data;

							counter = 1;
							outputBuffer.append(">");
							outputBuffer.append(getTaxonName(taxa,it, data));
							String sup = getSupplementForTaxon(taxa, it);
							if (StringUtil.notEmpty(sup))
								outputBuffer.append(sup);
							outputBuffer.append(getLineEnding());
							int numChars = data.getNumChars();

							for (int ic = 0; ic<numChars; ic++) {
								if (!writeOnlySelectedData || (data.getSelected(ic))){
									int currentSize = outputBuffer.length();
									boolean wroteMoreThanOneSymbol = false;
									boolean wroteSymbol = false;
									if (data.isUnassigned(ic, it) || (convertMultStateToMissing && isProtein && pData.isMultistateOrUncertainty(ic, it))){
										outputBuffer.append(getUnassignedSymbol());
										counter ++;
										wroteSymbol = true;
									}
									else if (includeGaps || (!data.isInapplicable(ic,it))) {
										data.statesIntoStringBuffer(ic, it, outputBuffer, false);
										counter ++;
										wroteSymbol = true;
									}
									wroteMoreThanOneSymbol = outputBuffer.length()-currentSize>1;
									if ((counter % 50 == 1) && (counter > 1) && wroteSymbol) {    // modulo
										outputBuffer.append(getLineEnding());
									}

									if (wroteMoreThanOneSymbol) {
										alert("Sorry, this data matrix can't be exported to this format (some character states aren't represented by a single symbol [char. " + CharacterStates.toExternal(ic) + ", taxon " + Taxon.toExternal(it) + "])");
										return null;
									}
								}
							}
							outputBuffer.append(getLineEnding());

						}

					}
				}




			

		}

		return outputBuffer;
 	}
 	/*.................................................................................................................*/
	public Taxa findTaxaToExport(MesquiteFile file, String arguments) { 
		return getProject().chooseTaxa(containerOfModule(), "Select taxa to export");

	}

	/*.................................................................................................................*/
	public boolean exportFile(MesquiteFile file, String arguments) { //if file is null, consider whole project open to export
		Arguments args = new Arguments(new Parser(arguments), true);
		boolean usePrevious = args.parameterExists("usePrevious");

		
		Taxa taxa = findTaxaToExport(file, arguments);
		if (taxa ==null) {
			showLogWindow(true);
			logln("WARNING: No suitable taxa available for export to a file of format \"" + getName() + "\".  The file will not be written.\n");
			return false;
		}
		if (!MesquiteThread.isScripting() && !usePrevious)
			if (!getExportOptions(taxa.anySelected()))
				return false;

		int numMatrices = getProject().getNumberCharMatrices(taxa);
		if (numMatrices<=0)
			return false;

		boolean someExported = false;
		String path = "";
		if (StringUtil.blank(arguments)) {
			path = getPathForExport(arguments,getProject().getHomeFileName(),null,null);
		}
		for (int im=0; im<numMatrices; im++) {		
			StringBuffer outputBuffer = getDataAsFileText(file, taxa, im);

			if (outputBuffer!=null) {
				CharacterData data = getProject().getCharacterMatrix(taxa, im);    //
				
				saveExportedFileWithExtension(outputBuffer, arguments, data.getName(), "fas", path+"."+data.getName() + ".fas");
				someExported=true;
			}
		}
		return someExported;
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
		return "Export FASTA by Gene";
	}
	/*.................................................................................................................*/

	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Exports FASTA files with sequences from each gene in a separate file." ;
	}
	/*.................................................................................................................*/


}


