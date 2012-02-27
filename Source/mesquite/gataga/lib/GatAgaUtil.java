package mesquite.gataga.lib;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import mesquite.lib.*;


public class GatAgaUtil {


	/*.................................................................................................................*
	public static FastaFileRecordArray getTableFromLocalFastaFile(String localFastaFilePath, boolean notifyIfNotFound){ 
		if (localFastaFilePath==null)
			return null;
		DataInputStream stream;
		FastaFileRecordArray fastaArray = new FastaFileRecordArray(100);
		StringBuffer sBb= new StringBuffer(100);
		MesquiteLong filePos =new MesquiteLong(0);
		MesquiteInteger remnant = new MesquiteInteger(-1);
		Parser parser = new Parser();
		File testing = new File(localFastaFilePath);
		long fileLength = testing.length();
		testing = null;

		long startPos = 0;
		long endPos = 0;
		long previousFilePos = 0;
		int count=0;
//		ProgressIndicator progIndicator = new ProgressIndicator(mf,"Reading FASTA file corresponding to local blast database", file.existingLength());
//		progIndicator.start();

		if (!MesquiteTrunk.isApplet()) {
			StringBuffer sequenceDataBuffer = new StringBuffer(0);
			String sequenceName = "";
			try {
				FileInputStream fin = new FileInputStream(localFastaFilePath);
				stream = new DataInputStream(fin);
				String line = " ";
				while (line != null) {
					endPos = filePos.getValue();
					line =MesquiteFile.readLine(stream, sBb, remnant, filePos);
					if (StringUtil.notEmpty(line)){
						parser.setString(line);
						if (parser.nextDarkChar()=='>') {  // it is a sequence name
							if (sequenceDataBuffer.length()>0) {  // then there is data in it
								fastaArray.addAndFillNextUnassigned(new FastaFileRecord(sequenceName, startPos, endPos, localFastaFilePath));
							//	Debugg.println(sequenceDataBuffer.toString());
								sequenceDataBuffer.setLength(0);
								startPos=filePos.getValue();
							}
							sequenceName = parser.getRemaining();
						} else 
							sequenceDataBuffer.append(line+StringUtil.lineEnding());
					}
				}
			}
			catch( FileNotFoundException e ) {
				if (notifyIfNotFound){
					MesquiteMessage.warnProgrammer("File Busy or Not Found (5) : " + localFastaFilePath);
					MesquiteFile.throwableToLog(null, e);
				}
				return null;
			} 
			catch( IOException e ) {
				if (notifyIfNotFound){
					MesquiteMessage.warnProgrammer("IO Exception found (5): " + localFastaFilePath + "   " + e.getMessage());
					MesquiteFile.throwableToLog(null, e);
				}
				return null;
			}
			if (sequenceDataBuffer.length()>0) {  // then there is data in it
				fastaArray.addAndFillNextUnassigned(new FastaFileRecord(sequenceName, startPos, endPos, localFastaFilePath));
				sequenceDataBuffer.setLength(0);
				startPos=filePos.getValue();
			}
			return fastaArray;
		}
		else {
			if (url!=null) {
				try {
					stream = new DataInputStream(url.openStream());
					return stream.readEverything();
					}
				catch( IOException e ) {MesquiteModule.mesquiteTrunk.discreetAlert(MesquiteThread.isScripting(), "IO exception" );}
			}
		 
		}

		return null;

	}
	/*.................................................................................................................*
	public static String fetchSequencesFromFastaFileRecordArray(String[] idList,  FastaFileRecordArray fastaArray, StringBuffer results){ 
		if (results==null || idList==null || fastaArray == null)
			return null;
		StringBuffer sb = new StringBuffer(1000);
		FastaFileRecord fastaRecord = null;
		for (int i=0; i<idList.length; i++) {
			int index = fastaArray.indexOfIgnoreCase(idList[i]);
			if (index>=0) {
				fastaRecord = fastaArray.getValue(index);
				if (fastaRecord!=null) {
						sb.append(">"+fastaRecord.getSequenceName());
						sb.append(fastaRecord.getSequence());
				}
					
			}
		}
		return null;
	}

	/*.................................................................................................................*/

}
