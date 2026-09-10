package com.pressfran.cobranzas;

public class Client {
    public long id;
    public String name;
    public String dni;
    public String phone;
    public String business;
    public String address;
    public String guarantor;
    public String notes;
    public boolean active;
    public boolean smsEnabled;
    public String createdDate;

    public Client(long id, String name, String dni, String phone, String business,
                  String address, String guarantor, String notes, boolean active,
                  boolean smsEnabled, String createdDate) {
        this.id = id;
        this.name = name;
        this.dni = dni;
        this.phone = phone;
        this.business = business;
        this.address = address;
        this.guarantor = guarantor;
        this.notes = notes;
        this.active = active;
        this.smsEnabled = smsEnabled;
        this.createdDate = createdDate;
    }

    @Override public String toString() {
        if (business != null && !business.trim().isEmpty()) return name + " · " + business;
        return name;
    }
}
