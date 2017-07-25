package vn.newai.core;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
//import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class InvoiceOCR {
	private String tessDataPath;
	private String openCVNativeLibPath;
	private Mat src;

	public InvoiceOCR(String tessDataPath, String openCVNativeLibPath) {
		this.tessDataPath = tessDataPath;
		this.openCVNativeLibPath = openCVNativeLibPath;
	}

	public String extractInfo(String folderPath, String imgName, String originalPath) {
		System.load(this.openCVNativeLibPath + "libopencv_java320.so");

		src = Imgcodecs.imread(folderPath + "/" + imgName);

		Mat src_res = src.clone();

		List<Rect> rectWord = getText(src_res, TessPageIteratorLevel.RIL_WORD);
		// List<Rect> rectTable = analysDocument(src_res);

		// drawRect(src_res, rectTable);
		drawRect(src_res, rectWord);

		Imgproc.resize(src_res, src_res, new Size(src_res.width()/2, src_res.height()/2));
		Imgcodecs.imwrite(originalPath + "/" + imgName + "-res.jpg", src_res);

		List<Rect> rectsLine = getText(src, TessPageIteratorLevel.RIL_TEXTLINE);

		Mat gray = new Mat();
		Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
		Mat binaryGray = new Mat();
		// Imgproc.threshold(gray, binaryGray, 175, 255, Imgproc.THRESH_BINARY);
		// Imgproc.adaptiveThreshold(gray, binaryGray, 255,
		// Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 99, 10);
		// Imgproc.adaptiveThreshold(gray, binaryGray, 255,
		// Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 161, 15);
		// Imgproc.adaptiveThreshold(gray, binaryGray, 255,
		// Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 191, 30);
		// Imgproc.adaptiveThreshold(gray, binaryGray, 255,
		// Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 121, 30);
		Imgproc.adaptiveThreshold(gray, binaryGray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 115, 30);

		//Imgcodecs.imwrite(folderPath + "/" + imgName + "-bin.png", binaryGray);

		String s = "";
		for (Rect rect : rectsLine) {
			Mat crop = binaryGray.submat(rect);

			try {
				ITesseract instance2 = new Tesseract();
				instance2.setDatapath(this.tessDataPath);
				instance2.setLanguage("vie+eng");
				instance2.setPageSegMode(TessPageSegMode.PSM_SINGLE_LINE);
				instance2.setTessVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÉÈẺẼẸÊẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÚÙỦŨỤƯỨỪỬỮỰÝỲỶỸỴĐáàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ");
				String res = instance2.doOCR(matToBufferedImage(crop));

				if (res.equals("")) {
					instance2.setPageSegMode(TessPageSegMode.PSM_SINGLE_WORD);
					res = instance2.doOCR(matToBufferedImage(crop));

					if (res.equals("")) {
						instance2.setPageSegMode(TessPageSegMode.PSM_SINGLE_CHAR);
						res = instance2.doOCR(matToBufferedImage(crop));
					}

				}
				System.out.println(res);
				s += res + " ";
			} catch (TesseractException e) {
				return "";
			}
		}
		System.out.println(s);

		return s;
	}

	public void drawRect(Mat src, List<Rect> rects) {
		for (Rect rect : rects) {
			Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0), 2, 8, 0);
		}
	}

	public List<Rect> getText(Mat src, int level) {
		Mat gray = new Mat();
		Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
		Mat binaryGray = new Mat();
		Imgproc.threshold(gray, binaryGray, 175, 255, Imgproc.THRESH_BINARY_INV);

		String datapath = this.tessDataPath;
		ITesseract instance = new Tesseract();
		instance.setDatapath(new File(datapath).getPath());
		instance.setLanguage("vie+eng");
		instance.setTessVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzÁÀẢÃẠĂẮẰẲẴẶÂẤẦẨẪẬÉÈẺẼẸÊẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÚÙỦŨỤƯỨỪỬỮỰÝỲỶỸỴĐáàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ");
		instance.setPageSegMode(TessPageSegMode.PSM_AUTO);

		List<Rect> resultRect = new ArrayList<>();
		List<Rectangle> wordResult;
		try {
			wordResult = instance.getSegmentedRegions(matToBufferedImage(binaryGray), level);
			for (Rectangle wordRect : wordResult) {
				Rect rect = new Rect(wordRect.x, wordRect.y, wordRect.width, wordRect.height);
				resultRect.add(rect);
			}
		} catch (TesseractException e) {
			e.printStackTrace();
		}
		return resultRect;
	}

	public static BufferedImage matToBufferedImage(Mat matrix) {
		BufferedImage bimg = null;
		if (matrix != null) {
			int cols = matrix.cols();
			int rows = matrix.rows();
			int elemSize = (int) matrix.elemSize();
			byte[] data = new byte[cols * rows * elemSize];
			int type;
			matrix.get(0, 0, data);
			switch (matrix.channels()) {
			case 1:
				type = BufferedImage.TYPE_BYTE_GRAY;
				break;
			case 3:
				type = BufferedImage.TYPE_3BYTE_BGR;
				byte b;
				for (int i = 0; i < data.length; i = i + 3) {
					b = data[i];
					data[i] = data[i + 2];
					data[i + 2] = b;
				}
				break;
			default:
				return null;
			}
			bimg = new BufferedImage(cols, rows, type);
			bimg.getRaster().setDataElements(0, 0, cols, rows, data);
		}
		return bimg;
	}

	boolean check(Rect a, List<Rect> recTables) {
		for (int i = 0; i < recTables.size(); i++) {
			Rect b = recTables.get(i);
			if (((a.x >= b.x && a.x <= b.x + b.width) && (a.y >= b.y && a.y <= b.y + b.height)) || ((b.x >= a.x && b.x <= a.x + a.width) && (b.y >= a.y && b.y <= a.y + a.height))) {
				return true;
			}
		}
		return false;
	}

	public List<Rect> analysDocument(Mat image) {
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
		Mat binaryGray = new Mat();
		Imgproc.threshold(gray, binaryGray, 170, 255, Imgproc.THRESH_BINARY_INV);
		Mat matVertical = binaryGray.clone();
		int numberErode = 12;
		int numberDilate = 12;
		for (int i = 0; i < numberErode; i++) {
			new Mat();
			Imgproc.morphologyEx(matVertical, matVertical, Imgproc.MORPH_ERODE, Mat.ones(1, 7, CvType.CV_8UC1));
		}
		for (int i = 0; i < numberDilate; i++) {
			Imgproc.morphologyEx(matVertical, matVertical, Imgproc.MORPH_DILATE, Mat.ones(1, 7, CvType.CV_8UC1));
		}
		Mat matHorizontal = binaryGray.clone();
		for (int i = 0; i < numberErode; i++) {
			Imgproc.morphologyEx(matHorizontal, matHorizontal, Imgproc.MORPH_ERODE, Mat.ones(7, 1, CvType.CV_8UC1));
		}
		for (int i = 0; i < numberDilate; i++) {
			Imgproc.morphologyEx(matHorizontal, matHorizontal, Imgproc.MORPH_DILATE, Mat.ones(7, 1, CvType.CV_8UC1));
		}
		List<Rect> rectTables = getTable(image, matHorizontal.clone(), matVertical.clone());

		return rectTables;
	}

	/*-private boolean checkIsTable(Mat src) {
	
		Mat src_gray = new Mat();
	
		if (src.channels() == 3) {
			Imgproc.cvtColor(src, src_gray, Imgproc.COLOR_BGR2GRAY);
		} else {
			src_gray = src.clone();
		}
	
		Mat not_gray = new Mat();
		Core.bitwise_not(src_gray, not_gray);
	
		Mat bw = new Mat();
		Imgproc.adaptiveThreshold(not_gray, bw, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, -2);
	
		Mat horizontal = bw.clone();
		Mat vertical = bw.clone();
	
		int scale = 15;
	
		int horizontalsize = horizontal.cols() / scale;
	
		Mat horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horizontalsize, 1));
	
		Imgproc.erode(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(horizontal, horizontal, horizontalStructure, new Point(-1, -1), 1);
	
		int verticalsize = vertical.rows() / scale;
	
		Mat verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, verticalsize));
	
		Imgproc.erode(vertical, vertical, verticalStructure, new Point(-1, -1), 1);
		Imgproc.dilate(vertical, vertical, verticalStructure, new Point(-1, -1), 1);
	
		Mat joints = new Mat();
		Core.bitwise_and(horizontal, vertical, joints);
	
		List<MatOfPoint> joints_contours = new ArrayList<>();
		Mat hierarchy2 = new Mat();
		Imgproc.findContours(joints, joints_contours, hierarchy2, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
	
		if (joints_contours.size() > 10)
			return true;
		return false;
	}*/

	public List<Rect> getTable(Mat src, Mat matHorizontal, Mat matVertical) {

		List<Rect> resultRect = new ArrayList<>();

		Mat mask = new Mat();
		Core.add(matHorizontal, matVertical, mask);

		Mat joints = new Mat();
		Core.bitwise_and(matHorizontal, matVertical, joints);

		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

		for (int i = 0; i < contours.size(); i++) {
			double area = Imgproc.contourArea(contours.get(i));

			if (area < 100)
				continue;

			MatOfPoint2f approxContour2f = new MatOfPoint2f();
			Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), approxContour2f, 3.0, true);
			Rect rect = Imgproc.boundingRect(new MatOfPoint(approxContour2f.toArray()));

			Mat roi = joints.submat(rect);

			List<MatOfPoint> joints_contours = new ArrayList<>();
			Mat hierarchy2 = new Mat();
			Imgproc.findContours(roi, joints_contours, hierarchy2, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

			if (joints_contours.size() <= 4)
				continue;

			int arrr = 0;
			for (int j = 0; j < joints_contours.size(); j++) {
				arrr += Imgproc.contourArea(joints_contours.get(j));
			}
			if (arrr > area / 12.0)
				continue;

			resultRect.add(rect);

		}

		List<Rect> resultRect2 = new ArrayList<>();

		List<Rect> maxRect = new ArrayList<>();
		maxRect.add(getMaxRect(resultRect));

		for (Rect rect : resultRect) {
			if (!check(rect, maxRect)) {
				System.out.println(true);
				if ((rect.width < rect.height && rect.width > rect.height / 10) || (rect.height < rect.width && rect.height > rect.width / 10)) {
					resultRect2.add(rect);
				}
			}
		}

		if (resultRect2.size() == 0) {
			resultRect.remove(getMaxRect(resultRect));
		}

		return resultRect;
	}

	private Rect getMaxRect(List<Rect> listRect) {
		Rect maxRect = listRect.get(0);
		for (Rect rect : listRect) {
			if (calArea(rect) > calArea(maxRect))
				maxRect = rect;
		}
		return maxRect;
	}

	private int calArea(Rect rect) {
		return rect.height * rect.width;
	}
}
