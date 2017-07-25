package vn.newai.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mailjet.client.Base64;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Email;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import vn.newai.core.OCR;

@Path("/invoice")
public class InvoiceProcessingService {
	/** The path to the folder where we want to store the uploaded files */
	private static final String UPLOAD_FOLDER = InvoiceProcessingService.class.getResource("../../../../../").getPath() + "uploads/";
	/** Path to folder that contains tessdata */
	private static final String TESSDATA_PATH = InvoiceProcessingService.class.getResource("../../../../../").getPath();
	/** Path to folder that contains openCV native lib */
	private static final String OPENCV_NATIVE_LIB_PATH = InvoiceProcessingService.class.getResource("../../../../../").getPath() + "opencv-native-lib/";
	/** Path to folder that contains mailjet_key.txt file */
	private static final String MAILJET_PATH = InvoiceProcessingService.class.getResource("../../../../../").getPath() + "config/mailjet_key.txt";

	/** MailJet API Key */
	private String mailjetApiKey = "5522d972a29002a12ae518b64a6056da";
	/** MailJet Secret Key */
	private String mailjetSecretKey = "bc95e9f47aca50bcb3b50d9bf5da8183";

	private boolean readMailJetKey() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(MAILJET_PATH), "UTF8"));
			String line = br.readLine();
			if (null != line)
				mailjetApiKey = line;
			line = br.readLine();
			if (null != line)
				mailjetSecretKey = line;
			br.close();
			System.out.println("Read mailjet key sucessfully");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Read mailjet key fail");
		}
		return false;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("email") String email) {
		// Read mailjet key from file
		readMailJetKey();

		// check if all form parameters are provided
		if (uploadedInputStream == null || fileDetail == null)
			return Response.status(400).entity("Invalid form data").build();

		// create our uploads folder, if it is not exists
		try {
			createFolderIfNotExists(UPLOAD_FOLDER);
		} catch (SecurityException e) {
			System.out.println(e);
			return Response.status(500).entity("Can not create destination folder on server").build();
		}
		String uploadedFileLocation = UPLOAD_FOLDER + fileDetail.getFileName();

		File uploaded = new File(uploadedFileLocation);

		try {
			saveToFile(uploadedInputStream, uploadedFileLocation);
		} catch (IOException e) {
			System.out.println(e);
			return Response.status(500).entity("Can not save file").build();
		}
		if (uploaded.exists()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					OCR ocr = new OCR(uploaded, TESSDATA_PATH, OPENCV_NATIVE_LIB_PATH);
					ocr.doOCR();
					ocr.deleteDirectory();
					File[] arrTextFiles = new File(UPLOAD_FOLDER).listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.startsWith(uploaded.getName()) && name.toLowerCase().endsWith(".txt");
						}
					});
					File[] arrImageFiles = new File(UPLOAD_FOLDER).listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.startsWith(uploaded.getName()) && name.toLowerCase().endsWith(".jpg");
						}
					});
					List<File> listAttachmentFile = new ArrayList<File>();
					listAttachmentFile.addAll(Arrays.asList(arrTextFiles));
					listAttachmentFile.addAll(Arrays.asList(arrImageFiles));
					sendEmail(listAttachmentFile, email);
				}
			}).start();
		} else {
			return Response.status(500).entity("File uploaded does not exist").build();
		}
		return Response.status(200).entity("OCR result will send to " + email).build();
	}

	/**
	 * Creates a folder to desired location if it not already exists
	 * 
	 * @param dirName
	 *            - full path to the folder
	 * @throws SecurityException
	 *             in case you don't have permission to create the folder
	 */
	private void createFolderIfNotExists(String dirName) throws SecurityException {
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdir();
			System.out.println("created " + dirName);
		}
	}

	/**
	 * Utility method to save InputStream data to target location/file
	 * 
	 * @param inStream
	 *            - InputStream to be saved
	 * @param target
	 *            - full path to destination file
	 */
	private void saveToFile(InputStream inStream, String target) throws IOException {
		OutputStream out = null;
		int read = 0;
		byte[] bytes = new byte[1024];

		out = new FileOutputStream(new File(target));
		while ((read = inStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
		out.flush();
		out.close();
	}

	private void sendEmail(List<File> listTextFile, String recipient) {
		try {
			JSONArray jsonArrAttachment = new JSONArray();
			for (File file : listTextFile) {
				System.out.println("Encoding to send email file " + file.getName());
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("Content-type", "text/html; charset=UTF-8");
				jsonObject.put("Filename", file.getName());
				jsonObject.put("content", Base64.encode(FileUtils.readFileToByteArray(file)));
				jsonArrAttachment.put(jsonObject);
			}
			MailjetClient client = new MailjetClient(mailjetApiKey, mailjetSecretKey);
			MailjetRequest email = new MailjetRequest(Email.resource).property(Email.FROMEMAIL, "noreply@newai.vn").property(Email.FROMNAME, "NewAI").property(Email.SUBJECT, "Your OCR result").property(Email.HTMLPART, "Hi,<br>Thanks for using our service. Please check your result in attachment.").property(Email.RECIPIENTS, new JSONArray().put(new JSONObject().put("Email", recipient))).property(Email.ATTACHMENTS, jsonArrAttachment);

			// trigger the API call
			MailjetResponse response = client.post(email);
			// Read the response data and status
			System.out.println("Email was sent to " + recipient + ", response: " + response.getStatus());
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (MailjetException e) {
			System.out.println("Mailjet Exception: " + e);
		}
	}
}
