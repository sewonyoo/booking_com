package resortreservation;

import resortreservation.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired VoucherRepository voucherRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_VoucherRequestPolicy(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        System.out.println("\n\n##### listener VoucherRequestPolicy : " + paymentApproved.toJson() + "\n\n");

        // Sample Logic //
        Voucher voucher = new Voucher();
        voucher.setReservId(paymentApproved.getReservId());
        voucher.setVoucherCode(
            new StringBuilder("V")
            .append(paymentApproved.getId())
            .append(paymentApproved.getReservId())
            .append(paymentApproved.getResortPrice().intValue())
            .toString());
        voucher.setVoucherStatus("Valid");
        voucher.setSendStatus("Before Send");
        voucherRepository.save(voucher);

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_VoucherCancelPolicy(@Payload PaymentCancelled paymentCancelled){

        if(!paymentCancelled.validate()) return;

        System.out.println("\n\n##### listener VoucherCancelPolicy : " + paymentCancelled.toJson() + "\n\n");

        // Sample Logic //
        Voucher voucher = voucherRepository.findByReservId(paymentCancelled.getReservId());
        voucher.setVoucherStatus("Invalid");
        voucherRepository.save(voucher);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverReservationCanceled_VoucherCancelPolicy(@Payload ReservationCanceled reservationCanceled){

        if(!reservationCanceled.validate()) return;

        System.out.println("\n\n##### listener VoucherCancelPolicy : " + reservationCanceled.toJson() + "\n\n");

        Voucher voucher = voucherRepository.findByReservId(reservationCanceled.getId());
        voucher.setVoucherStatus("Invalid");
        voucherRepository.save(voucher);
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}

}
