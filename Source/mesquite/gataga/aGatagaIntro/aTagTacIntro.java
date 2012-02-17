package mesquite.gataga.aGatagaIntro;

import mesquite.lib.Debugg;
import mesquite.lib.duties.PackageIntro;

public class aTagTacIntro extends PackageIntro {

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	public Class getDutyClass(){
		return aTagTacIntro.class;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "TagTac is a package of Mesquite modules providing tools for genomic analyses.";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;  
	}

	/*.................................................................................................................*/
	public String getName() {
		return "TagTac Package";
	}
	/*.................................................................................................................*/
	/** Returns the name of the package of modules (e.g., "Basic Mesquite Package", "Rhetenor")*/
	public String getPackageName(){
		return "TagTac Package";
	}
	/*.................................................................................................................*/
	/** Returns citation for a package of modules*/
	public String getPackageCitation(){
		return "Maddison, D.R..  2012.  TagTac.  A package of modules for Mesquite. Version 0.11.";
	}
	/*.................................................................................................................*/
	/** Returns whether there is a splash banner*/
	public boolean hasSplash(){
		return true; 
	}
}
