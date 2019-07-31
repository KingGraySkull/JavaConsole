package controllers;

import java.awt.print.PrinterException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.SAXException;
import com.prowidesoftware.swift.io.parser.SwiftParser;
import com.prowidesoftware.swift.model.SwiftBlock1;
import com.prowidesoftware.swift.model.SwiftBlock2;
import com.prowidesoftware.swift.model.SwiftBlock4;
import com.prowidesoftware.swift.model.SwiftBlock5;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.Tag;
import service.Util;

@RestController
@RequestMapping(value = "report", method = RequestMethod.POST)
public class ReportGeneration {
	private static final Logger LOG = LogManager.getLogger(ReportGeneration.class);

	@Value("${printerServiceName}")
	private String printerServiceName;

	@Value("${pdfDestionationPath}")
	private String pdfDestionationPath;

	@PostMapping("swift")
	public ResponseEntity<String> generatePdf(@RequestBody String swiftMessage) {

		LOG.error("SWIFT MESSAGE BODY : " + swiftMessage);
		final SwiftParser parser = new SwiftParser(swiftMessage);
		SwiftMessage m = null;
		String pdfPath = null;
		String pdfFileName = "";
		try {
			m = parser.message();
			final String html = buildHtml(m);
			LOG.info("HTML GENERATED : " + html);
			final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final Document doc = builder.parse(new ByteArrayInputStream(html.getBytes("UTF-8")));
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
			LocalDateTime localDateTime = LocalDateTime.now();
			final String timestamp = localDateTime.format(formatter);
			pdfFileName = timestamp + ".pdf";
			pdfPath = pdfDestionationPath + pdfFileName;
			ITextRenderer render = new ITextRenderer();
			render.setDocument(doc, null);
			render.layout();
			OutputStream os = new FileOutputStream(pdfPath);
			render.createPDF(os);
			os.close();
		} catch (IOException e) {
			LOG.error(" ERROR GENERATING PDF \n " + e);
			return new ResponseEntity<String>("ERROR : PDF COULD NOT BE GENERATED", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ParserConfigurationException e) {
			LOG.error("INVALID HTML GENERATED \n" + e);
			return new ResponseEntity<String>("ERROR : PARSING PDF METADETA ", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SAXException e) {
			LOG.error("ERROR PARSING HTML \n" + e);
			return new ResponseEntity<String>("ERROR : PARSING PDF METADETA ", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			printPdf(pdfPath);
			LOG.info("BEGINNING PDF PRINTING...");
		} catch (InvalidPasswordException e) {
			LOG.error(e);
			return new ResponseEntity<String>("ERROR : PRINTER NOT RESPONDING", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			LOG.error(e);
			return new ResponseEntity<String>("ERROR : UNABLE TO READ PDF ", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (PrinterException e) {
			LOG.error(e);
			return new ResponseEntity<String>("ERROR : INTERNAL PRINTER ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<String>("PDF READY!", HttpStatus.ACCEPTED);
	}

	private void printPdf(String pdfPath) throws InvalidPasswordException, IOException, PrinterException {
		DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
		PrintService[] services = PrintServiceLookup.lookupPrintServices(flavor, null);
		FileInputStream psStream = null;
		LOG.info("PDF PATH " + pdfPath);
		psStream = new FileInputStream(pdfPath);
		if (services.length > 0) {
			PrintService printService = null;
			for (PrintService service : services) {
				LOG.info(" AVAILABLE PRINTER SERVICE : " + service.getName());
				
				if (service.getName().contains(printerServiceName)) {
					LOG.error("Printer Service present " + service.getName());
					printService = service;
					break;
				}
			}
			DocPrintJob printJob = printService.createPrintJob();
			Doc document = new SimpleDoc(psStream, flavor, null);
			try {
				printJob.print(document, null);
			} catch (PrintException e) {
				LOG.error(e);
				LOG.error("UNABLE TO PRINT");
			}
		} else {
			LOG.warn("NO PDF PRINTER AVAILABE");
		}
		psStream.close();
	}

	private String buildHtml(final SwiftMessage m) {
		final StringBuilder htmlBuilder = new StringBuilder();
		final String styleTag = wrapTag("style",
				" h1 { font-weight: 400; text-align: center; border-bottom: 2px solid black; padding-bottom: 2px; }  h2 {font-weight: 400; text-align: left; border-bottom: 2px solid #9999ff; padding-bottom: 2px; color: #9999ff;} td { padding-left: 16px; padding-right: 16px; padding-bottom: 10px; }");
		final StringBuilder htmlContent = new StringBuilder();
		final String headTag = wrapTag("head", styleTag);
		htmlContent.append(wrapTag("h1", "SWIFT REPORT PRINTOUT"))
					.append(addTransmission("Transmission", m.getBlock1()))
					.append(addMessageHeader("Message Header", m.getBlock2()))
					.append(addMessageText("Message Text", m.getBlock4()))
					.append(addMessageTrailer("Message Trailer", m.getBlock5()));
		final String bodyTag = wrapTag("body", htmlContent.toString());
		final String htmlTag = wrapTag("html", headTag + bodyTag);
		htmlBuilder.append(htmlTag);
		return htmlBuilder.toString();
	}

	private String addTransmission(String title, SwiftBlock1 block1) {
		final String applicationId = block1.getApplicationId();
		final String logicalTerminalId = block1.getLogicalTerminal();
		final String sessionNumber = block1.getSessionNumber();
		final String sequenceNumber = block1.getSequenceNumber();

		String elements = wrapTag("h2", title);
		elements += wrapTag("p", "Application Id : " + applicationId);
		elements += wrapTag("p", "Logical Terminal : " + logicalTerminalId);
		elements += wrapTag("p", "Session Number : " + sessionNumber);
		elements += wrapTag("p", "Sequence Number : " + sequenceNumber);
		final String content = wrapTag("div", elements);
		return content;
	}

	private String addMessageHeader(final String title, final SwiftBlock2 block2) {
		final String messagePriority = block2.getMessagePriority();
		final String messageType = block2.getMessageType();

		String elements = wrapTag("h2", title);
		elements += wrapTag("p", "Message Priority : " + messagePriority);
		elements += wrapTag("p", "Message Type : " + messageType);
		final String content = wrapTag("div", elements);
		return content;
	}

	private String addMessageText(final String title, final SwiftBlock4 block4) {
		List<Tag> tags = block4.getTags();
		String elements = wrapTag("h2", title);
		String table = "";
		for (Tag tag : tags) {
			String tableData = "";
			String name = tag.getName();
			tableData += wrapTag("td", name);
			String value = tag.getValue();
			tableData += wrapTag("td", value);
			String tagDescription = Util.tagMap.get(name);
			tableData += wrapTag("td", tagDescription);
			table += wrapTag("tr", tableData);

		}
		elements += wrapTag("table", table);
		final String content = wrapTag("div", elements);
		return content;
	}

	private String addMessageTrailer(final String title, final SwiftBlock5 block5) {
		String elements = wrapTag("h2", title);
		List<Tag> tags = block5.getTags();
		for (Tag tag : tags) {
			String name = tag.getName();
			String value = tag.getValue();
			elements += wrapTag("p", name + " : " + value);
		}
		final String content = wrapTag("div", elements);
		return content;
	}

	private String wrapTag(String tag, String content) {
		String element = "";
		element += "<" + tag + ">";
		element += content;
		element += "</" + tag + ">";
		return element;
	}
}
