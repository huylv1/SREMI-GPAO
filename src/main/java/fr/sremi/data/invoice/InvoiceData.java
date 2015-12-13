package fr.sremi.data.invoice;

import fr.sremi.data.OrderDetailData;

import java.util.List;

/**
 * Created by fgallois on 12/8/15.
 */
public class InvoiceData {
    private String reference;
    private List<OrderDetailData> orderDetails;
    private List<ReceiptData> receipts;

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public List<OrderDetailData> getOrderDetails() {
        return orderDetails;
    }

    public void setOrderDetails(List<OrderDetailData> orderDetails) {
        this.orderDetails = orderDetails;
    }

    public List<ReceiptData> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<ReceiptData> receipts) {
        this.receipts = receipts;
    }
}