package mesquite.gataga.aGatagaIntro;

import mesquite.lib.Debugg;
import mesquite.lib.duties.PackageIntro;

public class aGatagaIntro extends PackageIntro {

	/*.................................................................................................................*/
	public boolean startJob(String arguments, Object condition, boolean hiredByName) {
		return true;
	}
	public Class getDutyClass(){
		return aGatagaIntro.class;
	}
	/*.................................................................................................................*/
	public String getExplanation() {
		return "GatAga is a package of Mesquite modules providing tools for genomic analyses.";
	}

	/*.................................................................................................................*/
	public boolean isPrerelease(){
		return true;  
	}

	/*.................................................................................................................*/
	public String getName() {
		return "GatAga Package";
	}
	/*.................................................................................................................*/
	/** Returns the name of the package of modules (e.g., "Basic Mesquite Package", "Rhetenor")*/
	public String getPackageName(){
		return "GatAga Package";
	}
	/*.................................................................................................................*/
	/** Returns citation for a package of modules*/
	public String getPackageCitation(){
		return "Maddison, D.R..  2012.  GatAga.  A package of modules for Mesquite for processing high-throughput sequencing data. Version 0.12.";
	}
	/*.................................................................................................................*/
	/** Returns whether there is a splash banner*/
	public boolean hasSplash(){
		return true; 
	}
}
