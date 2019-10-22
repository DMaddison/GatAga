package mesquite.gataga.NumTaxaPartWithDataInMatrix;



import java.util.Vector;

import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;

public class NumTaxaPartWithDataInMatrix extends NumberForMatrix {

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
		int count = 0;
		Taxa taxa = data.getTaxa();
		TaxaPartition partition = (TaxaPartition) taxa.getCurrentSpecsSet(TaxaPartition.class);
		if (partition == null){
			boolean found = false;
			for (int it = 0; it<data.getNumTaxa() && !found; it++){
				if (hasData(data, it)){
					found = true;
				}
			}
			if (found)
				count = 1;
		}
		else {
			Vector foundPartitions = new Vector();

			for (int it = 0; it<data.getNumTaxa(); it++){
				if (hasData(data, it)){
					TaxaGroup group = partition.getTaxaGroup(it);
					if (foundPartitions.indexOf(group)<0){
						foundPartitions.addElement(group);
					}
				}
			}
			count = foundPartitions.size();
		}

		if (count>0) {
			result.setValue(count); 
		}  else
			result.setValue(0.0); 

		if (resultString!=null) {
			resultString.setValue("Number of taxon partitions with data: " + result.toString());
		}
		saveLastResult(result);
		saveLastResultString(resultString);
	} 
	boolean hasData(MCharactersDistribution data, int it){
		CharacterState cs = null;
		try {
			for (int ic=0; ic<data.getNumChars(); ic++) {
				cs = data.getCharacterState(cs, ic, it);
				if (cs == null)
					return false;
				if (!cs.isInapplicable() && !cs.isUnassigned()) 
					return true;

			}
		}
		catch (NullPointerException e){
		}
		return false;
	}

	public boolean isPrerelease (){
		return false;
	}

	public String getName() {
		return "Number of Taxa Partitions with Data in Matrix";
	} 

	public String getExplanation(){
		return "Counts the number of taxa partitions containing taxa that have data (not ? and not gaps) the matrix.";
	} 

} 
