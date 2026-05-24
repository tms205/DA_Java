# Cafe Management Web - Huong dan chay va deploy

Ung dung da duoc chuyen tu JavaFX desktop sang Spring Boot Web.

## Chay local

```bash
mvn spring-boot:run
```

Mo trinh duyet:

```text
http://localhost:8080
```

## Build file deploy

```bash
mvn clean package
java -jar target/cafe-management-web-1.0.0.jar
```

## Cau hinh database

Mac dinh ung dung dung SQL Server:

```text
jdbc:sqlserver://TMS;instanceName=MSSQLSERVER01;databaseName=CAFEMANAGEMENT;encrypt=true;trustServerCertificate=true
user: sa
password: 123
```

Khi deploy, nen truyen bien moi truong:

```bash
DB_URL=jdbc:sqlserver://host:1433;databaseName=CAFEMANAGEMENT;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=your_password
PORT=8080
```

## Cau hinh gui OTP bang Gmail

Bat xac minh 2 buoc tren tai khoan Gmail dung de gui mail, tao **App Password**
va cau hinh bien moi truong truoc khi chay ung dung:

```bash
MAIL_USERNAME=gg.tms205@gmail.com
MAIL_PASSWORD=your_16_character_app_password
MAIL_FROM=gg.tms205@gmail.com
```

Dia chi gui mac dinh trong project la `gg.tms205@gmail.com`, nen co the bo qua
`MAIL_USERNAME` va `MAIL_FROM` khi chay local. Ung dung dung SMTP Gmail mac dinh
(`smtp.gmail.com:587`, STARTTLS). Khong dung mat khau dang nhap Gmail thong
thuong va khong ghi App Password vao source code.

Neu chay ung dung tu IDE, can khai bao `MAIL_PASSWORD` trong cau hinh Run cua IDE
va khoi dong lai ung dung. Bien moi truong dat sau khi app da chay se khong duoc
tien trinh Java hien tai nhan vao.

Trong workspace VS Code, chon cau hinh Run and Debug `Cafe Management (OTP Gmail)`.
VS Code se hoi Gmail App Password khi khoi dong ung dung va chi dua gia tri nay
vao bien `MAIL_PASSWORD` cua tien trinh dang chay, khong luu mat khau vao source.

Neu database da ton tai truoc cac chuc nang OTP, anh san pham va checkout customer,
mo file migration, thay cac dong `UPDATE dbo.[User]` bang Gmail that cua admin/
nhan vien, sau do chay thu cong:

```text
src/main/resources/db/otp-email-migration.sql
```

File tong the de tao lai database la:

```text
src/main/resources/db/cafe-management-reset.sql
```

Luu y: file reset xoa va tao lai cac bang du lieu. Chi chay khi ban chap nhan
mat du lieu hien co. Sau khi cap nhat database, admin cung co the nhap email
khoi phuc cho tung tai khoan tai trang `Admin > Nhan vien`. OTP dang ky customer
se chi tao tai khoan sau khi ma gui den email duoc nhap dung.

## Cac man hinh web

- `/login`: dang nhap nhan vien, khach hang, dang ky khach hang.
- `/admin`: dashboard admin, thong ke, quan ly san pham, quan ly nhan vien.
- `/staff/pos`: man hinh ban hang cho nhan vien.
- `/customer/menu`: menu dat mon cho khach hang.

## Yeu cau

- Java 21+
- Maven 3.9+
- SQL Server va database `CAFEMANAGEMENT`
- Cac stored procedure hien co: `sp_LoginStaff`, `sp_RegisterStaff`, `sp_LoginCustomer`, `sp_RegisterCustomer`, `sp_CreateOrder`, `sp_AddProductToOrder`, `sp_PayOrder`
