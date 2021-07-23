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
    @Autowired ReservationRepository reservationRepository;

    
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_ReservationStatusChangePolicy(@Payload PaymentCancelled paymentCancelled){

        if(!paymentCancelled.validate()) return;

        System.out.println("\n\n##### listener ReservationStatusChangePolicy : " + paymentCancelled.toJson() + "\n\n");

        reservationRepository.findById(paymentCancelled.getId())
        .ifPresent(
            reservation->{
                reservation.setResortStatus("Cancelled");
                reservationRepository.save(reservation);
            }
        );      
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
