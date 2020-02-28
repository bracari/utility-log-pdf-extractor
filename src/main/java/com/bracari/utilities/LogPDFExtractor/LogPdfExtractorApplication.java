package com.bracari.utilities.LogPDFExtractor;

import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootApplication
public class LogPdfExtractorApplication implements CommandLineRunner {

	@Value("${output.location}")
	private String outputLocation;

	@Value("${log.resource:classpath:web-1.log}")
	private Resource logLocation;

	public static void main(String[] args) {
		SpringApplication.run(LogPdfExtractorApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			File logFile = logLocation.getFile();
			Scanner scanner = new Scanner(logFile);
			Pattern attachment = Pattern.compile("<nc:Attachment>.+<\\/nc:Attachment>");

			while(scanner.findWithinHorizon(attachment,0) != null) {
				MatchResult result = scanner.match();
				if(result.groupCount()  != 0) return;

				String attachmentSnippet = result.group(0);
				Pattern dbqName = Pattern.compile("(?:<nc:BinaryCategoryText>)(.*?)(?:<\\/nc:BinaryCategoryText>)");
				Pattern base64 = Pattern.compile("(?:<nc:BinaryBase64Object>)(.*?)(?:<\\/nc:BinaryBase64Object>)");

				Matcher dbqNameMatcher = dbqName.matcher(attachmentSnippet);
				Matcher base64Matcher = base64.matcher(attachmentSnippet);
				dbqNameMatcher.find();
				base64Matcher.find();
				String dbqNameSnippet = dbqNameMatcher.group(1);
				String base64Snippet = base64Matcher.group(1);

				byte[] xhtml_source = null;
				try {
					xhtml_source = Base64.getMimeDecoder().decode(base64Snippet);
				} catch (IllegalArgumentException exception) {
					System.out.println(dbqNameSnippet);
				}

				if(xhtml_source == null) continue;

				try(ByteArrayInputStream input = new ByteArrayInputStream(xhtml_source);
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					FileOutputStream pdfStream = new FileOutputStream(outputLocation + dbqNameSnippet + ".pdf")){

					HtmlConverter.convertToPdf(input, output);
					output.writeTo(pdfStream);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
