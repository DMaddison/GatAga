package mesquite.gataga.SequenceMatchToReferenceTaxon;


/* TODO: 
 * restrict to MolecularData
 * */
import mesquite.categ.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.*;

public class SequenceMatchToReferenceTaxon extends NumberArrayForMatrix {
	int refSequence = 0;
	boolean exactMatch = true;
	static final int numNumbers = 3;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	} 

	/** Called to provoke any necessary initialization.  This helps prevent the module's initialization queries to the user from happening at inopportune times (e.p., while a long chart calculation is in mid-progress*/
	public void initialize(MCharactersDistribution data) {
	} 

	public void calculateNumbers(MCharactersDistribution data, NumberArray result, MesquiteString resultString) {
		if (result == null || data == null)
			return;
		clearResultAndLastResult(result);

		long match = 0;
		long differentNucleotide = 0;
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

		for (int ic=0; ic<numChars; ic++){
			if (!parentData.isInapplicable(ic,refSequence) && !parentData.isUnassigned(ic,refSequence)) {  // not gap and not missing in reference
				boolean baseFound = false;
				boolean baseMatches = true;
				for (int it=0; it<numTaxa; it++){   //now let's see what is in the other sequences
					if (it!=refSequence){  //not reference sequence, lets see how many match ref sequence
						if (!parentData.isInapplicable(ic,it) && !parentData.isUnassigned(ic,it)) {  //something in sequence
							baseFound = true;
							if (!parentData.sameStateIgnoreCase(ic, refSequence, ic, it))
								baseMatches=false;
						}
					}
				}
				if (baseFound)
					if (baseMatches)
						match++;
					else
						differentNucleotide++;
				else
					missing++;
			}
		} 

		//NOTE: if the number of numbers output is changed, must change numNumbers above!!!!
		if (countInRefSequence>0) {
			result.setValues(new double[] {match*1.0/countInRefSequence, (match+differentNucleotide)*1.0/countInRefSequence, numTaxa-1}); 
		}  else{
			result.setValues(new double[] {0.0,0.0, 0.0}); 
		}

		if (resultString!=null) {
			resultString.setValue("Fraction of reference sequence exactly present: " + result.getDouble(0) + ", fraction of reference sequence with at least one other sequence with aligned base: " + result.getDouble(2)+ ", Number of comparison taxa: " + result.getDouble(2));
		}
		saveLastResult(result);
		saveLastResultString(resultString);
	} 

	public boolean isPrerelease (){
		return true;
	}

	public String getName() {
		return "Matching to Reference Sequence";
	} 

	public String[] getNumbersNames() {
		return new String[] {"Fraction exact match","Fraction nucleotide present","Number of comparison taxa"};
	} 

	public int getNumberOfNumbers() {
		return numNumbers;
	} 

	public String getExplanation(){
		return "Calculates several numbers about how much the taxa in a matrix match the reference taxon.";
	} 

} 
