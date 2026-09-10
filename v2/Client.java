package com.pressfran.cobranzas;

public class Client {
    public long id;
    public String name;
    public String dni;
    public String phone;
    public String business;
    public String address;
    public double originalAmount;
    public double totalFinanced;
    public double balance;
    public double installment;
    public int installmentCount;
    public int paidInstallments;
    public String frequency;
    public double periodInterest;
    public String dueDate;
    public double dailyInterest;
    public String commitmentDate;
    public String guarantor;
    public String notes;
    public String status;
    public String createdDate;

    public Client(long id, String name, String dni, String phone, String business, String address,
                  double originalAmount, double totalFinanced, double balance, double installment,
                  int installmentCount, int paidInstallments, String frequency, double periodInterest,
                  String dueDate, double dailyInterest, String commitmentDate, String guarantor,
                  String notes, String status, String createdDate) {
        this.id = id;
        this.name = name;
        this.dni = dni;
        this.phone = phone;
        this.business = business;
        this.address = address;
        this.originalAmount = originalAmount;
        this.totalFinanced = totalFinanced;
        this.balance = balance;
        this.installment = installment;
        this.installmentCount = installmentCount;
        this.paidInstallments = paidInstallments;
        this.frequency = frequency;
        this.periodInterest = periodInterest;
        this.dueDate = dueDate;
        this.dailyInterest = dailyInterest;
        this.commitmentDate = commitmentDate;
        this.guarantor = guarantor;
        this.notes = notes;
        this.status = status;
        this.createdDate = createdDate;
    }
}
