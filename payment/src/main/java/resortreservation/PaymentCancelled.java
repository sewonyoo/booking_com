
package resortreservation;

public class PaymentCancelled extends AbstractEvent {

    private Long id;
    private Long reservId;
    private Long resortId;
    private Float resortPrice;
    private String reservStatus;
    private String paymentStatus;

    public Long getId() {
        return id;
    }

    public Long getResortId() {
        return resortId;
    }

    public void setResortId(Long resortId) {
        this.resortId = resortId;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getReservId() {
        return reservId;
    }

    public void setReservId(Long reservId) {
        this.reservId = reservId;
    }
    public Float getResortPrice() {
        return resortPrice;
    }

    public void setResortPrice(Float resortPrice) {
        this.resortPrice = resortPrice;
    }
    public String getReservStatus() {
        return reservStatus;
    }

    public void setReservStatus(String reservStatus) {
        this.reservStatus = reservStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}

