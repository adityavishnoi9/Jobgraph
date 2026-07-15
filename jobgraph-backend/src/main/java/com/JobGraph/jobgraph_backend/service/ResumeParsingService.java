package com.JobGraph.jobgraph_backend.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeParsingService {

    private final Tika tika = new Tika();

    /**
     * Builds the final resume text used downstream (LLM structuring stage).
     *
     * Pipeline:
     *  1. Extract plain text normally (Tika) - same as before.
     *  2. Separately walk the PDF's link annotations (PDFBox) and, for each
     *     one, extract the text sitting inside that annotation's bounding
     *     box on the page - this is the actual anchor/label text.
     *  3. Append a "label: url" line for each link found, instead of
     *     leaving bare URLs disconnected from their labels.
     *
     * Only handles PDF for now (resumes are almost always PDF). Falls back
     * to plain Tika extraction for other file types.
     */
    public String extractText(MultipartFile file) throws IOException, org.apache.tika.exception.TikaException {
        String filename = file.getOriginalFilename();
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");

        if (!isPdf) {
            // Non-PDF (e.g. .docx) - fall back to plain extraction for now.
            try (var stream = file.getInputStream()) {
                return tika.parseToString(stream);
            }
        }

        try (var stream = file.getInputStream();
             PDDocument document = PDDocument.load(stream)) {

            // Step 1: plain text (unchanged from before)
            String plainText;
            try (var textStream = file.getInputStream()) {
                plainText = tika.parseToString(textStream);
            }
            System.out.println(plainText);
            // Step 2: find each hyperlink's label by reading the text
            // inside its bounding box, page by page.
//            Map<String, String> labelToUrl = new LinkedHashMap<>();
//
//            for (PDPage page : document.getPages()) {
//                List<PDAnnotation> annotations = page.getAnnotations();
//                for (PDAnnotation annotation : annotations) {
//                    if (!(annotation instanceof PDAnnotationLink link)) continue;
//                    if (!(link.getAction() instanceof PDActionURI uriAction)) continue;
//
//                    String url = uriAction.getURI();
//                    if (url == null) continue;
//
//                    Rectangle2D rect = link.getRectangle().toGeneralPath().getBounds2D();
//
//                    PDFTextStripperByArea stripper = new PDFTextStripperByArea();
//                    stripper.addRegion("linkRegion", rect.getBounds());
//                    stripper.extractRegions(page);
//                    String label = stripper.getTextForRegion("linkRegion");
//
//                    if (label != null) label = label.trim();
//                    if (label == null || label.isEmpty()) label = url;
//
//                    labelToUrl.put(label, url);
//                }
//            }
//
//            // Step 3: build final text - original content + a clean
//            // "label: url" block instead of bare disconnected URLs.
            StringBuilder result = new StringBuilder();
//            result.append(plainText.trim());
//            result.append("\n\n---LINKS---\n");
//            for (Map.Entry<String, String> entry : labelToUrl.entrySet()) {
//                result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
//            }
//            System.out.println(result);
            return plainText;
        }
    }
}