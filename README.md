## Payment, Voucher 기능 추가

### 시나리오 설명 

1. 예약 후 결제 요청 -> 결제 List에 추가
2. 결제 List에서 결제 승인/취소 가능<br>
  2-1. 결제 승인 -> 바우처 생성 (**voucherStatus=Valid**)<br>
  2-2. 결제 취소 -> **예약 취소 및 리조트 예약가능으로 각 Status 변경**
3. 예약 취소 시<br>
  3-1. **결제 취소 및 바우처 무효(**voucherStatus=Invalid**)로 각 Status 변경**

<br>

### 실행 명령어

- 리조트 추가
```
http localhost:8082/resorts resortName="Jeju" resortType="Hotel" resortPrice=100000 resortStatus="Available" resortPeriod="7/23~25"
http localhost:8082/resorts resortName="Seoul" resortType="Hotel" resortPrice=200000 resortStatus="Available" resortPeriod="8/23~25"
http localhost:8082/resorts resortName="Busan" resortType="Pention" resortPrice=80000 resortStatus="Available" resortPeriod="9/23~25"
```

- 예약 및 결제요청 확인
```
http localhost:8088/reservations resortId=1 memberName="sim sang joon"
http localhost:8088/reservations resortId=2 memberName="sung jae kim"
http localhost:8088/reservations resortId=3 memberName="kill dong hong"

http localhost:8088/payments  // 결제List 확인
```

- 결제 승인 시, 바우처 생성 확인
```
http PATCH localhost:8088/payments/1 paymentStatus="Approved"  // 결제 승인
http PATCH localhost:8088/payments/2 paymentStatus="Approved" 

http localhost:8088/vouchers  // 바우처 확인(voucherStatus=Valid)
```

- 결제 취소 시, 예약/리조트 상태값 변경 확인 
```
http PATCH localhost:8088/payments/3 paymentStatus="Cancelled" // 결제 취소

// 예약 취소: resortStatus=Cancelled 확인 
http localhost:8082/reservations/3 

// 리조트 예약가능: resortStatus=Available 확인
http localhost:8081/resorts/3
```

- 예약 취소 시, 결제/바우처 상태값 변경 확인
```
http PATCH localhost:8088/reservations/2 resortStatus="Cancelled" // 예약 취소

// 결제 취소: paymentStatus=Cancelled 확인
http localhost:8088/payments/2

// 바우처 무효: voucherStatus=Invalid 확인
http localhost:8088/vouchers/2
```
