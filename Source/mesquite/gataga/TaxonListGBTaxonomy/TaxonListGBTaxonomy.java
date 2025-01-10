

package mesquite.gataga.TaxonListGBTaxonomy;
/*~~  */

import java.awt.Checkbox;

import mesquite.cont.lib.*;
import mesquite.lists.lib.*;
import mesquite.lib.characters.*;
import mesquite.lib.duties.MatrixSourceCoord;
import mesquite.lib.*;
import mesquite.lib.table.*;
import mesquite.lib.taxa.Taxa;
import mesquite.lib.ui.ExtensibleDialog;
import mesquite.lib.ui.MesquiteMenuItemSpec;
import mesquite.lib.ui.SingleLineTextField;
import mesquite.molec.lib.*;

/* ======================================================================== */
public class TaxonListGBTaxonomy extends TaxonListAssistant {
	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed
		EmployeeNeed e = registerEmployeeNeed(MatrixSourceCoord.class, getName() + "  needs a source of sequences.",
		"The source of characters is arranged initially");
	}
	MatrixSourceCoord matrixSourceTask;
	Taxa currentTaxa = null;
	MCharactersDistribution observedStates =null;
	CharacterData data = null;
	MesquiteMenuItemSpec selectMenuItem;
	boolean wholeWord = false;
	boolean caseSensitive = false;
	MesquiteTable table = null;

	
	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		matrixSourceTask = (MatrixSourceCoord)hireCompatibleEmployee(MatrixSourceCoord.class, ContinuousState.class, "Source of character matrix (for " + getName() + ")"); 
		if (matrixSourceTask==null)
			return sorry(getName() + " couldn't start because no source of character matrices was obtained.");
		return true;
	}

	public String getExplanationForRow(int ic){
		return null;
	}
	
	/*.................................................................................................................*/
	/** Generated by an employee who quit.  The MesquiteModule should act accordingly. */
	public void employeeQuit(MesquiteModule employee) {
		if (employee == matrixSourceTask)  // character source quit and none rehired automatically
			iQuit();
	}
	/*.................................................................................................................*/
	public void employeeParametersChanged(MesquiteModule employee, MesquiteModule source, Notification notification) {
		observedStates = null;
		super.employeeParametersChanged(employee, source, notification);
	}
	/*.................................................................................................................*/
	/** Called to provoke any necessary initialization.  This helps prevent the module's initialization queries to the user from
   	happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/
	public void initialize(Taxa taxa){
		currentTaxa = taxa;
		matrixSourceTask.initialize(currentTaxa);
		observedStates = matrixSourceTask.getCurrentMatrix(taxa);
		data = observedStates.getParentData();
	}

	/*.................................................................................................................*/
	public void setTableAndTaxa(MesquiteTable table, Taxa taxa){
		deleteMenuItem(selectMenuItem);
		selectMenuItem = addMenuItem("Select by taxonomy search...", makeCommand("select",  this));
		if (this.currentTaxa != null)
			this.currentTaxa.removeListener(this);
		this.currentTaxa = taxa;
		if (this.currentTaxa != null)
			this.currentTaxa.addListener(this);
		this.table = table;
}
	/*.................................................................................................................*/
	public Object doCommand(String commandName, String arguments, CommandChecker checker) {
		if (checker.compare(this.getClass(), "Selects taxonomy information that contains the search word", "[]", commandName, "select")) {
			selectBySearch();
		}
		else
			return  super.doCommand(commandName, arguments, checker);
		return null;
	}
	
	/*.................................................................................................................*/
	public void selectBasedOnSearchWord(String searchWord) {
		if (MesquiteThread.isScripting())
			return;
		if (table==null || data==null || StringUtil.blank(searchWord))
			return;
		Associable associable = data.getTaxaInfo(true);
		for (int it=0; it<data.getNumTaxa(); it++) {
			String s = (String)associable.getAssociatedObject(NCBIUtil.TAXONOMY, it);
			if (StringUtil.notEmpty(s)){
				if (StringUtil.indexOf(s,searchWord, caseSensitive, wholeWord)>=0) {
					table.selectRow(it);
					table.redrawFullRow(it);
				}
			}
		}
	}
	/*.................................................................................................................*/
	public boolean selectBySearch() {
		MesquiteInteger buttonPressed = new MesquiteInteger(1);
		ExtensibleDialog dialog = new ExtensibleDialog(containerOfModule(), "Select by taxonomy search",buttonPressed);  //MesquiteTrunk.mesquiteTrunk.containerOfModule()
		dialog.addLabel("Select by taxonomy search");

		SingleLineTextField searchField = dialog.addTextField("", 30);
		Checkbox wholeWordBox = dialog.addCheckBox("whole word", wholeWord);
		Checkbox respectCaseBox = dialog.addCheckBox("case sensitive", caseSensitive);

		dialog.completeAndShowDialog(true);
		if (buttonPressed.getValue()==0)  {
			String searchWord = searchField.getText();
			wholeWord = wholeWordBox.getState();
			caseSensitive = respectCaseBox.getState();
			if (table!=null) {
				selectBasedOnSearchWord(searchWord);
			}
			storePreferences();
		}
		dialog.dispose();
		return (buttonPressed.getValue()==0);
	}
	/*.................................................................................................................*/
	/*.................................................................................................................*/
	public void dispose() {
		super.dispose();
		if (currentTaxa!=null)
			currentTaxa.removeListener(this);
	}
	public void changed(Object caller, Object obj, Notification notification){
		outputInvalid();
		parametersChanged(notification);
	}
	public String getStringForTaxon(int ic){
		
		if (currentTaxa!=null) {
			if (observedStates == null ) {
				observedStates = matrixSourceTask.getCurrentMatrix(currentTaxa);
			}
			if (observedStates==null)
				return null;
			data = observedStates.getParentData();
			
			Associable associable = data.getTaxaInfo(true);
			String s = (String)associable.getAssociatedObject(NCBIUtil.TAXONOMY, ic);
			if (StringUtil.notEmpty(s))
				return s;
		}
		return "-";
	}
	/*...............................................................................................................*/
	/** returns whether or not a cell of table is editable.*/
	public boolean isCellEditable(int row){
		return true;
	}
	/*...............................................................................................................*/
	public boolean useString(int ic){
		return true;
	}
	/*...............................................................................................................*/
	public String getWidestString(){
		return "88888888888888888  ";
	}
	/*...............................................................................................................*/
	public String getTitle() {
		return "Top BLAST Hit (Taxonomy)";
	}
	/*.................................................................................................................*/
	public String getName() {
		return "GATAGA - Top BLAST Hit Taxonomy";
	}
	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return false;  
	}
	/*.................................................................................................................*/
	/** returns the version number at which this module was first released.  If 0, then no version number is claimed.  If a POSITIVE integer
	 * then the number refers to the Mesquite version.  This should be used only by modules part of the core release of Mesquite.
	 * If a NEGATIVE integer, then the number refers to the local version of the package, e.g. a third party package*/
	public int getVersionOfFirstRelease(){
		return -1;  
	}
	/*.................................................................................................................*/
	/** returns whether this module is requesting to appear as a primary choice */
	public boolean requestPrimaryChoice(){
		return true;  
	}
	
	/*.................................................................................................................*/
	/** returns an explanation of what the module does.*/
	public String getExplanation() {
		return "Lists GenBank taxonomy for top BLAST match." ;
	}
}
