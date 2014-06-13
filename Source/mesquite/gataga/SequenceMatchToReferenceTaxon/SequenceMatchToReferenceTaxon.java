package mesquite.gataga.SequenceMatchToReferenceTaxon;


/* TODO: 
 * restrict to MolecularData
 * */
import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;

public class SequenceMatchToReferenceTaxon extends NumberForMatrix {
	int refSequence = 0;
	boolean exactMatch = true;

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

		long match = 0;
		long numSequencesProcessed = 0;
		CategoricalData parentData = (CategoricalData)data.getParentData();
		int numTaxa = parentData.getNumTaxa();
		int numChars = parentData.getNumChars();

		long missing=0;
		long countInRefSequence =0;
		CategoricalState refCS = new CategoricalState();
		CategoricalState cs = new CategoricalState();

		for (int ic=0; ic<numChars; ic++){
			if (!parentData.isInapplicable(ic,refSequence) && !parentData.isUnassigned(ic,refSequence)) {  // not gap and not missing in reference
				countInRefSequence++;
			}
		}
		

		for (int it=0; it<numTaxa; it++)
			if (it!=refSequence){  //not reference sequence, lets see how many match ref sequence
				numSequencesProcessed++;
				for (int ic=0; ic<numChars; ic++){
					if (!parentData.isInapplicable(ic,refSequence) && !parentData.isUnassigned(ic,refSequence)) {  // not gap and not missing in reference
						if (!parentData.isInapplicable(ic,it) && !parentData.isUnassigned(ic,it)) {  //something in sequence
							if (parentData.sameStateIgnoreCase(ic, refSequence, ic, it))
								match++;
							else
								missing++;
						} else {  //didn't get this one
							missing++;
						}
					}
				}
			} 

//		Debugg.println("match: " + match);
//		Debugg.println("countInRefSequence " +countInRefSequence);
		if (countInRefSequence*numSequencesProcessed>0) {
			result.setValue(match*1.0/(countInRefSequence*numSequencesProcessed)); 
		}  else
			result.setValue(0.0); 

		if (resultString!=null) {
			resultString.setValue("Fraction of reference sequence present: " + result.toString());
		}
		saveLastResult(result);
		saveLastResultString(resultString);
	} 

	public boolean isPrerelease (){
		return true;
	}

	public String getName() {
		return "Fraction of reference sequence present";
	} 

	public String getExplanation(){
		return "Calculates the fraction of the matrix that is xxx.";
	} 

} 
