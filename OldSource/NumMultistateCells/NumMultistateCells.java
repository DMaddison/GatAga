package mesquite.gataga.NumMultistateCells;



import mesquite.categ.lib.CategoricalData;
import mesquite.categ.lib.MolecularData;
import mesquite.categ.lib.RequiresAnyCategoricalData;
import mesquite.categ.lib.RequiresAnyMolecularData;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;

public class NumMultistateCells extends NumberForMatrix {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	} 

	/** Called to provoke any necessary initialization.  This helps prevent the module's initialization queries to the user from happening at inopportune times (e.p., while a long chart calculation is in mid-progress*/
	public void initialize(MCharactersDistribution data) {
	} 

	public void calculateNumber(MCharactersDistribution data, MesquiteNumber result, MesquiteString resultString) {
		if (result == null || data == null)
			return;
		clearResultAndLastResult(result);

		CharacterData parentDData = data.getParentData();
		if (parentDData == null){
			if (resultString != null)
				resultString.setValue("Proportion of Multistate Cells can be calculated only for stored matrices");
			return;
		}
		if (!(parentDData instanceof CategoricalData)){
			if (resultString != null)
				resultString.setValue("Proportion of Multistate Cells can be calculated only for categorical or molecular matrices");
			return;
		}
		CategoricalData parentData = (CategoricalData)parentDData;
		int numTaxa = parentData.getNumTaxa();
		int numChars = parentData.getNumChars();
		
		int count = 0;
		int total = 0;
		for (int it=0; it<numTaxa; it++)
			for (int ic=0; ic<numChars; ic++){
				if (!parentData.isInapplicable(ic, it))
					total++;
				if (parentData.isMultistateOrUncertainty(ic,  it))
					count ++;
			}
		
		if (total>0) {
			result.setValue(count*1.0/total); 
		}  else
			result.setValue(0.0); 

		if (resultString!=null) {
			resultString.setValue("Proportion of Multistate Cells: " + result.toString());
		}
		saveLastResult(result);
		saveLastResultString(resultString);
	} 

	/*.................................................................................................................*/
	/** Returns CompatibilityTest so other modules know if this is compatible with some object. */
	public CompatibilityTest getCompatibilityTest(){
		return new RequiresAnyCategoricalData();
	}
	public boolean isPrerelease (){
		return false;
	}

	public String getName() {
		return "Proportion of Multistate Cells (uncertainties or polymorphisms)";
	} 

	public String getExplanation(){
		return "Calculates the proportion of multistate cells in the matrix.";
	} 

} 
