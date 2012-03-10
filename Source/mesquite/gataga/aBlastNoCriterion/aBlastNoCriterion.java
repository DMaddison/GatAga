package mesquite.gataga.aBlastNoCriterion;

import mesquite.gataga.lib.*;
import mesquite.molec.lib.BLASTResults;

public class aBlastNoCriterion extends BlastSeparateCriterion {

	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	
	public String[] getSubdirectoryNames() {
		return null;
	}
	
	public boolean queryOptions() {
		return true;
	}
	


	public int getCriterionMatch(BLASTResults blastResults) {
		return 0;
	}

	public boolean isActive(){
		return false;
	}

	public String getName() {
		return "None";
	}


}
