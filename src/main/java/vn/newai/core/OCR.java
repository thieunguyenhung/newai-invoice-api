package vn.newai.core;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;

import vn.newai.fileout.FileWriter;

public class OCR {
	private String tessDataPath;
	private String openCVNativeLibPath;
	private String fileName;
	private String filePath;
	private String folderPath;

	public OCR(File file, String tessDataPath, String openCVNativeLibPath) {
		this.tessDataPath = tessDataPath;
		this.openCVNativeLibPath = openCVNativeLibPath;
		this.fileName = file.getName();
		this.filePath = FilenameUtils.getFullPathNoEndSeparator(file.getAbsolutePath());
		createDirectory();
	}

	private void createDirectory() {
		File file = new File(filePath + "/" + FilenameUtils.removeExtension(fileName));
		if (file.mkdirs()) {
			folderPath = file.getAbsolutePath();
			System.out.println("Created dir: " + file.getAbsolutePath());
		}
	}

	public void doOCR() {
		doConvertToPNG();
		renamePNGFile();
		InvoiceOCR invoiceOCR = new InvoiceOCR(tessDataPath, openCVNativeLibPath);
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		});
		for (File file : arrFiles) {
			System.out.println("OCR-ing file " + file.getName());
			FileWriter.writeTextFile(invoiceOCR.extractInfo(folderPath, file.getName(), filePath), filePath + "/" + file.getName() + ".txt");
		}
	}

	/**
	 * Delete created directory for processed file
	 * 
	 * @throws IOException
	 */
	public void deleteDirectory() {
		try {
			FileUtils.deleteDirectory(new File(folderPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void doConvertToPNG() {
		// create command
		ConvertCmd cmd = new ConvertCmd();
		// create the operation, add images and operators/options
		IMOperation op = new IMOperation();

		op.colorspace("sRGB");
		op.density(300);
		// op.resize(1240, 1754);
		op.addRawArgs("-auto-orient");
		// op.profile(TESSDATA + "/tessdata/adobe.icc");
		op.units("PixelsPerInch");
		op.alpha("off"); // No transparent background
		op.addImage(filePath + "/" + fileName);
		op.depth(8);
		op.addImage(folderPath + "/" + fileName + ".png");

		// execute the operation
		try {
			cmd.run(op);
		} catch (InterruptedException | IM4JavaException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Converted " + filePath + "/" + fileName);
		}
	}

	private void renamePNGFile() {
		File[] arrFiles = new File(this.folderPath).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".png");
			}
		});
		if (null != arrFiles && arrFiles.length == 1) {
			File oldFile = arrFiles[0];
			File newFile = new File(this.folderPath + "/" + this.fileName + "-0.png");
			oldFile.renameTo(newFile);
		}
		System.out.println("Renamed file in " + this.folderPath);
	}
}
