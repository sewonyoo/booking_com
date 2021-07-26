# 리조트 예약 시스템


# Table of contents
- [리조트_예약시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석설계)
    - [AS-IS 조직 (Horizontally-Aligned)](#AS-IS-조직-Horizontally-Aligned)
    - [TO-BE 조직 (Vertically-Aligned)](#TO-BE-조직-Vertically-Aligned)
    - [Event Storming 결과](#Event-Storming-결과)
  - [구현](#구현)
    - [시나리오 흐름 테스트](#시나리오-흐름-테스트)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](#Gateway-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [CQRS & Kafka](#CQRS--Kafka)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트](#비동기식-호출--시간적-디커플링--장애격리--최종-Eventual-일관성-테스트)
  - [운영](#운영)
    - [CI/CD 설정](#CICD-설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출--서킷-브레이킹--장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [Zero-Downtime deploy (Readiness Probe)](#Zero-Downtime-deploy-Readiness-Probe)
    - [Self-healing (Liveness Probe)](#Self-healing-Liveness-Probe)
    - [ConfigMap 사용](#ConfigMap-사용)

    
# 서비스 시나리오
- 기능적 요구사항(전체)
1. 리조트 관리자는 리조트를 등록한다. 				
2. 고객이 리조트를 선택하여 예약한다. 				
3. 예약이 확정되면 리조트는 예약불가 상태로 바뀐다.					
4. 고객이 예약한 리조트를 결제승인한다.	
5. 결제승인이 완료되면 바우처가 고객에게 발송된다
6. 고객이 확정된 예약을 취소할 수 있다.					
7. 예약이 취소되면, 결제 바우처 상태가 바뀐다					
8. 결제가 취소되면 예약 취소, 리조트 예약 가능 상태가 된다.					
9. 고객은 리조트 예약 정보를 확인 할 수 있다.	
10. 

- 비기능적 요구사항
1. 트랜잭션
    1. 리조트 상태가 예약 가능상태가 아니면 아예 예약이 성립되지 않아야 한다  Sync 호출 
1. 장애격리
    1. 결제/바우처/마이페이지 기능이 수행되지 않더라도 주문은 365일 24시간 받을 수 있어야 한다.  Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중되면 사용자를 잠시동안 받지 않고 잠시후에 하도록 유도한다.  Circuit breaker, fallback
1. 성능
    1. 고객이 자신의 예약 상태를 확인할 수 있도록 마이페이지가 제공 되어야 한다.  CQRS

# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)

  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)

  ![image](https://user-images.githubusercontent.com/85722729/125151796-b9da7800-e183-11eb-8aa1-75206e01d5d1.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과: http://www.msaez.io/#/storming/e871JhG0OxcZNF58i3d5r1UX6di2/41ea8f5101b55f3d253f4dc687d52451


### 이벤트 도출
<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/125240557-3c4e6d80-e325-11eb-9115-3aab0c542df2.png">

### 이벤트 도출-부적격삭제
<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/125240558-3c4e6d80-e325-11eb-9fb7-7e4ee3403089.png">

### 액터, 커맨드 부착하여 읽기 좋게
<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/125240560-3ce70400-e325-11eb-826d-8c16ca723bc4.png">

### 어그리게잇으로 묶기
<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/125240563-3ce70400-e325-11eb-88b7-ae0f673b22c0.png">

### 바운디드 컨텍스트로 묶기
<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/126892365-1f8fc39f-5a0b-4d76-90c0-28cef50c8dda.png">

### 폴리시 부착 

<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/126892658-6c731912-e57e-488a-bfed-a08d317fcb4e.png">

### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

<img width="994" alt="image" src="https://user-images.githubusercontent.com/85722729/126892803-7f253f3c-b391-4451-9cb6-03fcb433dd3b.png">

### 완성된 모형
<img width="1113" alt="image" src="https://user-images.githubusercontent.com/85722729/126897162-12e0a51b-eb9a-45bb-91d8-764c3bde4275.png">

- View Model 추가
- 도메인 서열
  - Core : reservation
  - Supporting : resort, mypage
  - General : payment, voucher

## 헥사고날 아키텍처 다이어그램 도출
    
![image](https://user-images.githubusercontent.com/85722729/126720418-a448ce16-aee1-43ab-bfa6-6b830e2efdbd.png)



    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)


## 시나리오 흐름 테스트 (Saga, CQRS, Correlation)

포트생성확인

![image](https://user-images.githubusercontent.com/85722729/126923773-42a6b34d-07ef-42fd-8de2-e2525f3bcf09.png)


1. 리조트 관리자는 리조트를 등록한다.

http localhost:8082/resorts resortName="Jeju" resortType="Hotel" resortPrice=100000 resortStatus="Available" resortPeriod="7/29~30"

![image](https://user-images.githubusercontent.com/85722729/126922452-a9855be1-e98b-4b21-9e06-d0f43224730a.png)

2. 고객이 리조트를 선택하여 예약한다.

http localhost:8088/reservations resortId=1 memberName="kim sia"

![image](https://user-images.githubusercontent.com/85722729/126922518-7b45abe1-67fd-4b8a-825d-95e29071bd55.png)

http localhost:8088/payments  // 결제List 확인

![image](https://user-images.githubusercontent.com/85722729/126922527-27d36bbb-a0bb-41a1-8829-694916bfb2fe.png)

3. 예약이 확정되면 리조트는 예약불가 상태로 바뀐다.

http localhost:8088/resorts 

![image](https://user-images.githubusercontent.com/85722729/126922561-9103e1b9-c485-40fa-bc53-60ec653ecd94.png)

4. 고객이 예약한 리조트를 결제승인한다.

http PATCH localhost:8088/payments/1 paymentStatus="Approved" 

![image](https://user-images.githubusercontent.com/85722729/126922698-acbd5797-2048-422f-8d8e-1f7a92e7a7f9.png)

5. 결제승인이 완료되면 바우처가 고객에게 발송된다

http localhost:8088/vouchers  // 바우처 확인(voucherStatus=Valid)

![image](https://user-images.githubusercontent.com/85722729/126922781-55874274-1366-46a6-a43e-2a4d7e703c5d.png)

6. 고객이 확정된 예약을 취소할 수 있다.

http PATCH localhost:8088/reservations/3 resortStatus="Cancelled" // 예약 취소

![image](https://user-images.githubusercontent.com/85722729/126922824-7f09d8d2-7582-4b6e-92fb-e4cfbc9c967e.png)

7. 예약이 취소되면, 결제 바우처 상태가 바뀐다

http localhost:8088/payments/3   //결제 취소 확인

![image](https://user-images.githubusercontent.com/85722729/126922855-0d3c6f3b-6bdb-4976-a18a-0c96db33761e.png)

http localhost:8088/vouchers/3   // 바우처 무효: voucherStatus=Invalid 확인

![image](https://user-images.githubusercontent.com/85722729/126922890-2936f141-63e8-46e0-aed4-d03843b8fa8e.png)

8.  결제가 취소되면 예약 취소, 리조트 예약 가능 상태가 된다. 

http PATCH localhost:8088/payments/1 paymentStatus="Cancelled"

![image](https://user-images.githubusercontent.com/85722729/126922916-d1317fc1-a9b5-41e8-a00d-cd845c504531.png)


http localhost:8081/reservations/1 // 예약 취소: resortStatus=Cancelled 확인 

![image](https://user-images.githubusercontent.com/85722729/126922935-52ada0e2-f349-4b41-9a4a-2637fcadecbd.png)

http localhost:8082/resorts/1 // 리조트 예약가능: resortStatus=Available 확인

![image](https://user-images.githubusercontent.com/85722729/126922943-52ee07b4-0704-4d96-879a-c2cba5faae90.png)

http localhost:8088/vouchers/1 //바우처 무효: voucherStatus=Invalid 확인 

![image](https://user-images.githubusercontent.com/85722729/126922964-f4809778-2a1d-4a5e-841c-d0820b490214.png)

9. 고객은 휴양소 예약 정보를 확인 할 수 있다. (CQRS)

http localhost:8083/myPages

![image](https://user-images.githubusercontent.com/85722729/126922980-fb4967a4-4027-4596-ae6f-ea1dc27ca3a2.png)

![image](https://user-images.githubusercontent.com/85722729/126923490-db0aae66-afd3-4fa6-b33b-944f3af92ffd.png)


## DDD 의 적용
- 위 이벤트 스토밍을 통해 식별된 Micro Service 5개를 구현하였으며 그 중 mypage는 CQRS를 위한 서비스이다.

|MSA|기능|port|URL|
| :--: | :--: | :--: | :--: |
|reservation| 예약정보 관리 |8081|http://localhost:8081/reservations|
|resort| 리조트 관리 |8082|http://localhost:8082/resorts|
|mypage| 예약내역 조회 |8083|http://localhost:8083/mypages|
|payment| 결제 관리 |8084|http://localhost:8084/payments|
|voucher| 바우처 관리 |8085|http://localhost:8085/vouchers|
|gateway| gateway |8088|http://localhost:8088|

## Gateway 적용
- API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

gateway 서비스의 application.yml

<img width="420" alt="image" src="https://user-images.githubusercontent.com/85722729/126897622-152497fb-cb62-420d-9ac0-2532575b8094.png">
<img width="420" alt="image" src="https://user-images.githubusercontent.com/85722729/126897628-50568e62-07de-4d82-ba94-7b12bf6a7aa7.png">


## 폴리글랏 퍼시스턴스
- payment 서비스만 DB를 구분하여 적용함. 인메모리 DB인 hsqldb 사용하였다.

![image](https://user-images.githubusercontent.com/85722729/126924302-5d361bf6-6813-4602-9df3-8e4b647f16cf.png)


## 동기식 호출과 Fallback 처리

- 예약(reservation)->리조트상태확인(resort) 간의 호출을 req/res로 연동하여 구현하였다
  호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출.

- 리조트서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현하였다.

#예약(reservation)->ResortService.java

![image](https://user-images.githubusercontent.com/85722729/126930717-9c40a4df-c46d-41d2-878b-83ea6c5b4b04.png)

#Reservation.java

예약을 처리 하기 직전(@PrePersist)에 ResortSevice를 호출하여 서비스 상태와 Resort 세부정보도 가져온다.

![image](https://user-images.githubusercontent.com/85722729/126930763-826820f3-b8b8-422a-889e-f3c564f69dfa.png)

동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 시스템이 장애로 예약을 못받는다는 것을 확인하였다.

![image](https://user-images.githubusercontent.com/85722729/126930869-f8813f76-bfc0-4ecf-9e3b-8b1699bd8dcc.png)


## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
- 결제 승인 후 바우처생성을 요청하는 것은 동기식이 아니라 비 동기식으로 처리하여 결제승인 완료가 블로킹 되지 않아도록 처리한다.
- 이를 위하여 결제 승인 후 결제 승인완료 내용을 도메인 이벤트를 카프카로 송출한다(Publish)
 
![image](https://user-images.githubusercontent.com/85722729/126932366-872e0f5b-0194-4fde-a8a5-94af6e12c3fb.png)

- 바우처 시스템에서는 결제 승인완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다.


![image](https://user-images.githubusercontent.com/85722729/126932427-48943130-d838-4fa8-894a-23b24b54781f.png)


- 바우처 시스템은 결제시스템과 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 바우처시스템이 유지보수로 인해 잠시 내려간 상태라도 바우처생성을 받는데 문제가 없다.

1.바우처시스템 kill

![image](https://user-images.githubusercontent.com/85722729/126932762-8daf27f1-87c1-4bc6-b803-57b38d19ed7e.png)

2.결제 승인

http PATCH localhost:8088/payments/3 paymentStatus="Approved"  // 결제 승인

3.바우처 서비스 기동

4.바우처 내용 확인 가능

http localhost:8088/vouchers 
#정상적으로바우처 생성이 확인됨

![image](https://user-images.githubusercontent.com/85722729/126932843-033e40e9-afa2-4fb0-859d-2ac0d9695e58.png)

![image](https://user-images.githubusercontent.com/85722729/126932865-f3a8570d-f23f-46b5-ba94-51b1c73df987.png)







# 운영

## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 각 서비스별로 Docker로 빌드를 하여, ECR 에 등록 후 deployment.yaml을 통해 EKS에 배포함.

- 각서비스별 package, build, push 실행


cd resort #서비스별 폴더로 이동

mvn package -B -Dmaven.test.skip=true -각서비스 들어가서 실행

docker build -t 879772956301.dkr.ecr.ap-southeast-1.amazonaws.com/user14-resort:latest .
docker push 879772956301.dkr.ecr.ap-southeast-1.amazonaws.com/user14-resort:latest

![image](https://user-images.githubusercontent.com/85722729/126939230-038a0b24-5c48-4954-97d1-acb4396cc680.png)

kubectl apply -f deployall.yml #AWS deploy 수행

- 최종 Deploy완료

![image](https://user-images.githubusercontent.com/85722729/126938786-6871bb2a-f9ac-4047-ba2b-ec6d5c1bd12d.png)

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이크 프레임워크 : Spring FeignClient + Hystrix 옵션을 사용

- 시나리오 : 예약(reservation) -> 리조트(resort) 예약 시 RESTful Request/Response 로 구현이 하였고, 예약 요청이 과도할 경우 circuit breaker 를 통하여 장애격리.
Hystrix 설정: 요청처리 쓰레드에서 처리시간이 610 밀리초가 넘어서기 시작하여 어느정도 유지되면 circuit breaker 수행됨

#reservation에 application.yml 수정

![image](https://user-images.githubusercontent.com/85722729/126929823-0452dd9e-fd20-4874-a37d-dda04ab63cef.png)



피호출 서비스(리조트:resort) 의 임의 부하 처리 - 400 밀리초 ~ 620밀리초의 지연시간 부여

#resortreservation>external>ResortService.java 수정  

![image](https://user-images.githubusercontent.com/85722729/126929969-227f6b8d-9559-40a7-9afe-8f3e4021f96e.png)

#resortServiceFallback.class 추가

![image](https://user-images.githubusercontent.com/85722729/126929980-ab10dbbb-cb6f-4f42-9395-ea68a9face26.png)

#reservation -> reservation.java 수정

![image](https://user-images.githubusercontent.com/85722729/126930021-ce453a1f-808f-4efd-b051-44ff83d7e648.png)

#ResortController.java 수정

![image](https://user-images.githubusercontent.com/85722729/126930045-ec5cd29b-4b20-493b-9a32-3af2ff1bd75e.png)

#resort, reservation 서비스 구동
mvn spring-boot:run (resort, reservation 서비스)

휴양소 추가 : http http://localhost:8082/resorts resortName="Jeju" resortType="Hotel" resortPrice=100000 resortStatus="Available" resortPeriod="7/1~2"

휴양소 예약 : siege -v -c100 -t10S -r10 --content-type "application/json" 'http://localhost:8081/reservations/ POST {"resortId":1, "memberName":"SW"}'
            //100명이 10초동안 부하
            
![image](https://user-images.githubusercontent.com/85722729/126930137-87dba440-5054-49b3-a3f8-30272d519b73.png)

![image](https://user-images.githubusercontent.com/85722729/126930142-2870700f-fe33-403e-a87c-21d8544cfbd4.png)

## 오토스케일 아웃

- payment서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20프로를 넘어서면 replica 를 10개까지 늘려준다:
```bash
kubectl autoscale deployment resort --cpu-percent=20 --min=1 --max=10
```
- CB 에서 했던 방식대로 워크로드를 100초 동안 걸어준다.

siege -c20 -t100S -v http://resort:8080/resorts 

![image](https://user-images.githubusercontent.com/85722729/126941824-c63d1d75-0d53-404e-b012-652af5741547.png)



<img width="533" alt="image" src="https://user-images.githubusercontent.com/85722851/125200066-20ef4e00-e2a4-11eb-893e-7407615daa18.png">

- 오토스케일이 어떻게 되고 있는지 모니터링을 해보면 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:
<img width="704" alt="image" src="https://user-images.githubusercontent.com/85722851/125234907-926ae300-e31c-11eb-8be4-377f595f9a24.png">


## Zero-Downtime deploy (Readiness Probe)
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거하고 테스트함
- seige로 배포중에 부하를 발생과 재배포 실행
```bash
root@siege:/# siege -c1 -t30S -v http://resort:8080/resorts 
kubectl apply -f  kubernetes/deployment.yml 
```
- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

<img width="552" alt="image" src="https://user-images.githubusercontent.com/85722851/125045082-922dd600-e0d7-11eb-9128-4c9eff39654c.png">
배포기간중 Availability 가 평소 100%에서 80% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 

- 이를 막기위해 Readiness Probe 를 설정함: deployment.yaml 의 readiness probe 추가
```yml
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
```

- 동일한 시나리오로 재배포 한 후 Availability 확인
<img width="503" alt="image" src="https://user-images.githubusercontent.com/85722851/125044747-3cf1c480-e0d7-11eb-9c35-1091547bb099.png">
배포기간 동안 Availability 가 100%를 유지하기 때문에 무정지 재배포가 성공한 것으로 확인됨.

## Self-healing (Liveness Probe)
- Pod는 정상적으로 작동하지만 내부의 어플리케이션이 반응이 없다면, 컨테이너는 의미가 없다.
- 위와 같은 경우는 어플리케이션의 Liveness probe는 Pod의 상태를 체크하다가, Pod의 상태가 비정상인 경우 kubelet을 통해서 재시작한다.
- 임의대로 Liveness probe에서 path를 잘못된 값으로 변경 후, retry 시도 확인
```yml
          livenessProbe:
            httpGet:
              path: '/actuator/fakehealth' <-- path를 잘못된 값으로 변경
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```
- resort Pod가 여러차례 재시작 한것을 확인할 수 있다.
<img width="757" alt="image" src="https://user-images.githubusercontent.com/85722851/125048777-3cf3c380-e0db-11eb-99cd-97c7ebead85f.png">

## ConfigMap 사용
- 시스템별로 또는 운영중에 동적으로 변경 가능성이 있는 설정들을 ConfigMap을 사용하여 관리합니다. Application에서 특정 도메일 URL을 ConfigMap 으로 설정하여 운영/개발등 목적에 맞게 변경가능합니다.
configMap 생성
```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: resort-cm
data:
    api.resort.url: resort:8080
EOF
```
configmap 생성 후 조회
<img width="881" alt="image" src="https://user-images.githubusercontent.com/85722851/125245232-470c0100-e32b-11eb-9db1-54f35d1b2e4c.png">
deployment.yml 변경
```yml
      containers:
          ...
          env:
            - name: feign.resort.url
              valueFrom:
                configMapKeyRef:
                  name: resort-cm
                  key: api.resort.url
```
ResortService.java내용
```java
@FeignClient(name="resort", url="${feign.resort.url}")
public interface ResortService {

    @RequestMapping(method= RequestMethod.GET, value="/resorts/{id}", consumes = "application/json")
    public Resort getResortStatus(@PathVariable("id") Long id);

}
```
생성된 Pod 상세 내용 확인
<img width="1036" alt="image" src="https://user-images.githubusercontent.com/85722851/125245075-162bcc00-e32b-11eb-80ab-81fa57e774d8.png">


