package fr.sremi.services.pdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import fr.sremi.dao.OrderRepository;
import fr.sremi.model.Address;
import fr.sremi.model.Client;
import fr.sremi.model.Order;
import fr.sremi.services.ConfigurationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fr.sremi.data.OrderDetailData;
import fr.sremi.data.invoice.InvoiceData;
import fr.sremi.data.invoice.ReceiptData;
import fr.sremi.exception.PdfException;
import fr.sremi.util.InvoiceUtils;

import javax.annotation.Resource;

@Component
public class PdfInvoiceCreator {

    @Resource
    private ConfigurationService configurationService;

    @Resource
    private OrderRepository orderRepository;

    public String createPdf(String invoiceNumber, InvoiceData invoiceData) throws PdfException {
        Document document = new Document(PageSize.A4);

        String filename = "FACTURE-" + invoiceNumber + ".pdf";
        try {
            File archiveFile = new File(configurationService.getInvoiceArchivePath() + filename);

            FileOutputStream fileout = new FileOutputStream(archiveFile);
            PdfWriter writer = PdfWriter.getInstance(document, fileout);
            writer.setPageEvent(new PdfInvoiceFooterEvent());
            writer.setPageEvent(new PdfHeaderEvent());

            document.open();

            Order order = orderRepository.findByReference(invoiceData.getReference());

            // Page 1: Exemplaire client
            document.add(createInformations(order.getClient()));
            document.add(createInfoCommand(invoiceData, invoiceNumber));
            document.add(createCommandTable(invoiceData.getAllOrderDetails()));
            PdfPTable table = (PdfPTable) createFooterTable(invoiceData);
            int bottomSpacing = 100;
            if (invoiceData.getWithVat()) {
                bottomSpacing = 140;
            }
            table.setSpacingBefore(writer.getVerticalPosition(true) - bottomSpacing);
            document.add(table);
            document.newPage();

            // Page 2: Exemplaire SREMI
            document.add(createInformations(order.getClient()));
            document.add(createInfoCommand(invoiceData, invoiceNumber));
            document.add(createCommandTable(invoiceData.getAllOrderDetails()));
            table = (PdfPTable) createFooterTable(invoiceData);
            table.setSpacingBefore(writer.getVerticalPosition(true) - bottomSpacing);
            document.add(table);
            document.newPage();

        } catch (FileNotFoundException e) {
            throw new PdfException("Impossible de trouver le fichier", e);
        } catch (DocumentException e) {
            throw new PdfException("Erreur de creation du fichier Pdf", e);
        } finally {
            document.close();
        }
        return filename;
    }

    private Element createInformations(Client client) {
        float[] colsWidth = { 58f, 42f };
        PdfPTable table = new PdfPTable(colsWidth);
        table.setWidthPercentage(100);

        Paragraph paragraph = new Paragraph();
        paragraph.add(new Phrase("SREMI", FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 28)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("SARL au capital de 15500 Euros", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("Touche Fougère", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("72320 Saint Maixent", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("N° Siret: 4397548100015", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("APE: 3320C", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("N° Intracommunautaire: FR 26 439 754 581", FontFactory.getFont(
                FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("Téléphone: 02.43.71.70.76", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("Télécopie:  02.43.71.70.94", FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);

        PdfPCell cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);

        paragraph = new Paragraph();
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase(client.getName(), FontFactory.getFont(FontFactory.TIMES_BOLD, 12)));
        paragraph.add(Chunk.NEWLINE);
        Address clientAddress = client.getAddress();
        paragraph.add(new Phrase(clientAddress.getStreet1(), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase(clientAddress.getPostalCode() + " " + clientAddress.getCity(), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        paragraph.add(Chunk.NEWLINE);
        paragraph.add(new Phrase("N° Intracommunautaire: " + client.getNumeroIntracommunautaire(), FontFactory.getFont(
                FontFactory.TIMES_ROMAN, 12)));

        cell = new PdfPCell(paragraph);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        return table;
    }

    private Element createInfoCommand(InvoiceData invoiceData, String invoiceNumber) {

        Paragraph paragraph = new Paragraph();
        paragraph.setAlignment(Element.ALIGN_LEFT);
        paragraph.setSpacingBefore(20);
        paragraph.add(new Phrase("FACTURE", FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 14)));

        // Main table
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(100.0f);
        // Left table
        PdfPCell firstTableCell = new PdfPCell();
        firstTableCell.setBorder(PdfPCell.NO_BORDER);

        firstTableCell.addElement(createLeftTable(invoiceNumber, invoiceData.getReference()));
        mainTable.addCell(firstTableCell);

        // Right table
        PdfPCell secondTableCell = new PdfPCell();
        secondTableCell.setBorder(PdfPCell.NO_BORDER);

        secondTableCell.addElement(createBLTable(invoiceData.getReceipts()));
        mainTable.addCell(secondTableCell);

        paragraph.add(mainTable);

        return paragraph;
    }

    private Element createLeftTable(String invoiceNumber, String reference) {
        float[] colsWidth = { 25f, 30f, 45f };
        PdfPTable leftTable = new PdfPTable(colsWidth);
        leftTable.setWidthPercentage(100);
        leftTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell headerCell = new PdfPCell(new Phrase("Numéro"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Date"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Commande Client"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(headerCell);

        PdfPCell cell = new PdfPCell(new Phrase(invoiceNumber, FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        cell.setMinimumHeight(20);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(cell);

        cell = new PdfPCell(new Phrase(new SimpleDateFormat("dd/MM/yyyy").format(InvoiceUtils.currentInvoiceDate()
                .toDate()), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        cell.setMinimumHeight(20);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(cell);

        cell = new PdfPCell(new Phrase(reference, FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
        cell.setMinimumHeight(20);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        leftTable.addCell(cell);

        return leftTable;
    }

    private Element createBLTable(List<ReceiptData> receiptDatas) {
        float[] colsWidth = { 40f, 70f, 40f, 70f };
        PdfPTable rightTable = new PdfPTable(colsWidth);
        rightTable.setWidthPercentage(70);
        rightTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell headerCell = new PdfPCell(new Phrase("BL N°", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(15);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Date", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(15);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("BL N°", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(15);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Date", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(15);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(headerCell);

        for (ReceiptData receiptData : receiptDatas) {
            PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(receiptData.getNumber()), FontFactory.getFont(
                    FontFactory.TIMES_ROMAN, 10)));
            cell.setMinimumHeight(15);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            rightTable.addCell(cell);

            cell = new PdfPCell(new Phrase(new SimpleDateFormat("dd/MM/yyyy").format(receiptData.getCreationDate()),
                    FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
            cell.setMinimumHeight(15);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            rightTable.addCell(cell);
        }
        PdfPCell cell = new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        cell.setMinimumHeight(15);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(cell);
        cell = new PdfPCell(new Phrase("", FontFactory.getFont(FontFactory.TIMES_ROMAN, 10)));
        cell.setMinimumHeight(15);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        rightTable.addCell(cell);

        return rightTable;
    }

    private Element createCommandTable(List<OrderDetailData> commands) {
        float[] colsWidth = { 16f, 36f, 10f, 14f, 14f };
        PdfPTable table = new PdfPTable(colsWidth);
        table.setWidthPercentage(100);
        table.setSpacingBefore(40);

        PdfPCell headerCell = new PdfPCell(new Phrase("Article"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Description"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Quantité"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Pu. HT"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(headerCell);

        headerCell = new PdfPCell(new Phrase("Montant HT"));
        headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        headerCell.setMinimumHeight(20);
        headerCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(headerCell);

        PdfPCell cell;
        for (OrderDetailData command : commands) {
            cell = new PdfPCell(new Phrase(command.getReference(), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            cell.setMinimumHeight(20);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(command.getDescription(), FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
            cell.setMinimumHeight(20);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(Integer.toString(command.getQuantity()), FontFactory.getFont(
                    FontFactory.TIMES_ROMAN, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);

            DecimalFormat myFormatter = new DecimalFormat("###.00 €");
            String unitPrice = "?";
            String total = "?";
            if (command.getUnitPriceHT() != null) {
                unitPrice = myFormatter.format(command.getUnitPriceHT());
                total = myFormatter.format(command.getUnitPriceHT() * command.getQuantity());
            }
            cell = new PdfPCell(new Phrase(unitPrice, FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(total, FontFactory.getFont(FontFactory.TIMES_ROMAN, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_RIGHT);
            table.addCell(cell);
        }
        return table;
    }

    private Element createFooterTable(InvoiceData invoiceData) {
        List<OrderDetailData> commands = invoiceData.getAllOrderDetails();
        float[] colsWidth = { 62f, 14f, 14f };
        PdfPTable table = new PdfPTable(colsWidth);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);

        Double totalHt = Double.valueOf(0);
        for (OrderDetailData command : commands) {
            if (command.getUnitPriceHT() != null) {
                totalHt += command.getUnitPriceHT() * command.getQuantity();
            }

        }
        PdfPCell cell = new PdfPCell();
        if (StringUtils.isNotEmpty(invoiceData.getCertificateNumber())) {
            cell.setPhrase(new Phrase("ATTESTATION N° " + invoiceData.getCertificateNumber(), FontFactory.getFont(
                    FontFactory.TIMES_ROMAN, 12)));
        }
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setMinimumHeight(20);
        table.addCell(cell);

        cell = new PdfPCell(new Phrase("TOTAL HT", FontFactory.getFont(FontFactory.TIMES_BOLD, 12)));
        cell.setMinimumHeight(20);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);

        DecimalFormat myFormatter = new DecimalFormat("###.00 €");
        cell = new PdfPCell(new Phrase(myFormatter.format(totalHt), FontFactory.getFont(FontFactory.TIMES_BOLD, 12)));
        cell.setMinimumHeight(20);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        table.addCell(cell);

        if (invoiceData.getWithVat()) {
            cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
            table.addCell(cell);
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            Double montantTva = totalHt * invoiceData.getVatRate() / 100;
            cell = new PdfPCell(new Phrase("TVA à " + invoiceData.getVatRate() + "%", FontFactory.getFont(
                    FontFactory.TIMES_BOLD, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase(myFormatter.format(montantTva), FontFactory.getFont(FontFactory.TIMES_BOLD,
                    12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
            table.addCell(cell);
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);

            cell = new PdfPCell(new Phrase("TOTAL TTC", FontFactory.getFont(FontFactory.TIMES_BOLD, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);

            Double totalTTC = totalHt + montantTva;
            cell = new PdfPCell(new Phrase(myFormatter.format(totalTTC),
                    FontFactory.getFont(FontFactory.TIMES_BOLD, 12)));
            cell.setMinimumHeight(20);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        return table;
    }
}
