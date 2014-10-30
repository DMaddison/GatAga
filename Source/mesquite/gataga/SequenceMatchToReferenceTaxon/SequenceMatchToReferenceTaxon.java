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
	static final int numNumbers = 5;

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	} 

	/** Called to provoke any necessary initialization.  This helps prevent the module's initialization queries to the user from happening at inopportune times (e.p., while a long chart calculation is in mid-progress*/
	public void initialize(MCharactersDistribution data) {
	} 

	/*.................................................................................................................*/
	public int numSequencesSubsetOfNonReference(CategoricalData parentData){
		int count=0;
		int numTaxa = parentData.getNumTaxa();
		for (int it=0; it<numTaxa; it++){   
			for (int it2=it+1; it2<numTaxa; it2++){   
				if (it!=refSequence && it2 != refSequence && it!= it2){  
					if (parentData.firstApplicable(it)<parentData.firstApplicable(it2) && parentData.lastApplicable(it)>parentData.lastApplicable(it2)) // it2 is within it
						count++;
					else if (parentData.firstApplicable(it2)<parentData.firstApplicable(it) && parentData.lastApplicable(it2)>parentData.lastApplicable(it)) // it2 is within it
						count++;
				}
			}
		}
		return count;
	}

	/*.................................................................................................................*/
	public void calculateNumbers(MCharactersDistribution data, NumberArray result, MesquiteString resultString) {
		if (result == null || data == null)
			return;
		clearResultAndLastResult(result);

		long match = 0;
		long differentNucleotide = 0;
		long numConflicts = 0;
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
				boolean conflict = false;
				searchForConflict: 
					for (int it=0; it<numTaxa; it++){   //now let's see what is in the other sequences
						for (int it2=it+1; it2<numTaxa; it2++){   //now let's see what is in the other sequences
							if (it!=refSequence && it2 != refSequence && it!= it2){  //not reference sequence, lets see how many match ref sequence
								if (!parentData.isInapplicable(ic,it) && !parentData.isUnassigned(ic,it)) {  //something in sequence
									if (!parentData.sameStateIgnoreCase(ic, refSequence, ic, it, true, true, true)) {
										conflict=true;
										break searchForConflict;
									}
								}
							}
						}
					}


				if (baseFound) {
					if (baseMatches)
						match++;
					else
						differentNucleotide++;
					if (conflict)
						numConflicts++;
				}
				else
					missing++;
			}
		} 

		//	4.	Consensus sequence into FASTA file

		int fullyContainedSequences = numSequencesSubsetOfNonReference(parentData);


		//NOTE: if the number of numbers output is changed, must change numNumbers above!!!!
		if (countInRefSequence>0) {
			result.setValues(new double[] {match*1.0/countInRefSequence, (match+differentNucleotide)*1.0/countInRefSequence, numConflicts*1.0/countInRefSequence, fullyContainedSequences, numTaxa-1}); 
		}  else{
			result.setValues(new double[] {0.0,0.0, 0.0, 0.0, 0.0}); 
		}

		if (resultString!=null) {
			String s= "Fraction of reference sequence exactly present: " + result.getDouble(0);
			s+= ", fraction of reference sequence with at least one other sequence with aligned base: " + result.getDouble(1);
			s+=", fraction of sites at which non-reference sequences conflict: " + result.getDouble(2);
			s+=", number of pairwise comparisons in which non-reference sequences are contained within another: " + result.getDouble(3);
			s+=", number of comparison taxa: " + result.getDouble(4);
			resultString.setValue(s);
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
		return new String[] {"Fraction exact match","Fraction nucleotide present","Percent conflict within non-references", "Number of sequences fully contained within another non-ref sequence","Number of comparison taxa"};
	} 

	public int getNumberOfNumbers() {
		return numNumbers;
	} 

	public String getExplanation(){
		return "Calculates several numbers about how much the taxa in a matrix match the reference taxon.";
	} 

} 
