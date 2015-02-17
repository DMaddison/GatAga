
package mesquite.gataga.CopyNumForMatrixToSimpleFile; 

import java.awt.FileDialog;

import mesquite.lib.*;
import mesquite.lib.characters.CharacterData;
import mesquite.lib.duties.*;
import mesquite.molec.lib.Blaster;

/* ======================================================================== */
public class CopyNumForMatrixToSimpleFile extends FileProcessor {
	NumberForMatrix numTask;
	String saveFile = null;
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(NumberForMatrix.class, getName() + "  needs a NumberForMatrix module.","");
	}

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName){
		numTask = (NumberForMatrix)hireEmployee(NumberForMatrix.class, "NumberForMatrix (for " + getName() + ")"); 
		if (numTask==null)
			return sorry(getName() + " couldn't start because no NumberForMatrix module could be obtained.");
		return true;
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return false;  
	}
 	public String getNameForProcessorList() {
 		if (numTask != null)
 			return getName() + "(" + numTask.getName() + ")";
 		return getName();
   	}
	/*.................................................................................................................*/
	 public Snapshot getSnapshot(MesquiteFile file) { 
	 	Snapshot temp = new Snapshot();
	 	temp.addLine("setNumberTask ", numTask); 
	 	return temp;
	 }
	/*.................................................................................................................*/
 	 public Object doCommand(String commandName, String arguments, CommandChecker checker) {
 	 	if (checker.compare(this.getClass(), "Sets the number task", "[name of module]", commandName, "setNumberTask")) {
 	 		NumberForMatrix temp = (NumberForMatrix)replaceEmployee(NumberForMatrix.class, arguments, "Aligner", numTask);
			if (temp !=null){
				numTask = temp;
 	 			return numTask;
 	 		}
 	 	}
 	 	
		else return  super.doCommand(commandName, arguments, checker);
		return null;
	 }
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;
	}
	/*.................................................................................................................*/
	public boolean isSubstantive(){
		return true; //not really, but to force checking of prerelease
	}

	/** if returns true, then requests to remain on even after alterFile is called.  Default is false*/
	public boolean pleaseLeaveMeOn(){
		return false;
	}
	/*.................................................................................................................*/
	/** Called to alter file. */
	public boolean processFile(MesquiteFile file){
		if (numTask==null)
			return false;
		if (saveFile == null || okToInteractWithUser(CAN_PROCEED_ANYWAY, "Asking for file to save")){ //need to check if can proceed
			
			MesquiteFileDialog fdlg= new MesquiteFileDialog(containerOfModule(), "Output File for Numbers for Matrices", FileDialog.SAVE);
			fdlg.setBackground(ColorTheme.getInterfaceBackground());
			fdlg.setVisible(true);
			String fileName=fdlg.getFile();
			String directory=fdlg.getDirectory();
			// fdlg.dispose();
			if (StringUtil.blank(fileName) || StringUtil.blank(directory))
				return false;
			saveFile = MesquiteFile.composePath(directory, fileName);
			MesquiteFile.appendFileContents(saveFile, "file name" + numTask.getName() + StringUtil.lineEnding(), true);
		}
		if (saveFile == null)
			return false;
		
		MesquiteNumber result = new MesquiteNumber();
		MesquiteString resultString = new MesquiteString("");
		
		StringBuffer sb = new StringBuffer();
		sb.append(file.getName());
   		for (int im = 0; im < proj.getNumberCharMatrices(file); im++){
 			CharacterData data = proj.getCharacterMatrix(file, im);
  			sb.append("\t"+ data.getName());
  			numTask.calculateNumber(data.getMCharactersDistribution(), result, resultString);
    		sb.append(result.toString());
   		}
		MesquiteFile.appendFileContents(saveFile, sb.toString() + StringUtil.lineEnding(), true);
		return true;

	}
	/*.................................................................................................................*/
	public String getName() {
		return "Put Number for Matrix into Simple File";
	}
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Puts a calculated number about a matrix into a simple text file." ;
	}

}


