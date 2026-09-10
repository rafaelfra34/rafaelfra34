package com.pressfran.cobranzas;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "pressfran.db";
    private static final int DB_VERSION = 2;

    public DbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "dni TEXT DEFAULT ''," +
                "phone TEXT NOT NULL," +
                "business TEXT DEFAULT ''," +
                "address TEXT DEFAULT ''," +
                "original_amount REAL NOT NULL DEFAULT 0," +
                "total_financed REAL NOT NULL DEFAULT 0," +
                "balance REAL NOT NULL," +
                "installment REAL NOT NULL," +
                "installment_count INTEGER NOT NULL DEFAULT 1," +
                "paid_installments INTEGER NOT NULL DEFAULT 0," +
                "frequency TEXT NOT NULL DEFAULT 'Semanal'," +
                "period_interest REAL NOT NULL DEFAULT 10," +
                "due_date TEXT NOT NULL," +
                "daily_interest REAL NOT NULL DEFAULT 3," +
                "commitment_date TEXT," +
                "guarantor TEXT DEFAULT ''," +
                "notes TEXT DEFAULT ''," +
                "status TEXT NOT NULL DEFAULT 'ACTIVO'," +
                "created_date TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "client_id INTEGER NOT NULL," +
                "amount REAL NOT NULL," +
                "payment_date TEXT NOT NULL," +
                "type TEXT NOT NULL DEFAULT 'PAGO'," +
                "note TEXT DEFAULT '')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            addColumn(db, "clients", "dni", "TEXT DEFAULT ''");
            addColumn(db, "clients", "business", "TEXT DEFAULT ''");
            addColumn(db, "clients", "address", "TEXT DEFAULT ''");
            addColumn(db, "clients", "original_amount", "REAL NOT NULL DEFAULT 0");
            addColumn(db, "clients", "total_financed", "REAL NOT NULL DEFAULT 0");
            addColumn(db, "clients", "installment_count", "INTEGER NOT NULL DEFAULT 1");
            addColumn(db, "clients", "paid_installments", "INTEGER NOT NULL DEFAULT 0");
            addColumn(db, "clients", "frequency", "TEXT NOT NULL DEFAULT 'Semanal'");
            addColumn(db, "clients", "period_interest", "REAL NOT NULL DEFAULT 10");
            addColumn(db, "clients", "guarantor", "TEXT DEFAULT ''");
            addColumn(db, "clients", "notes", "TEXT DEFAULT ''");
            addColumn(db, "clients", "created_date", "TEXT DEFAULT ''");
            db.execSQL("CREATE TABLE IF NOT EXISTS payments (id INTEGER PRIMARY KEY AUTOINCREMENT, client_id INTEGER NOT NULL, amount REAL NOT NULL, payment_date TEXT NOT NULL, type TEXT NOT NULL DEFAULT 'PAGO', note TEXT DEFAULT '')");
            db.execSQL("UPDATE clients SET original_amount=balance WHERE original_amount=0");
            db.execSQL("UPDATE clients SET total_financed=balance WHERE total_financed=0");
        }
    }

    private void addColumn(SQLiteDatabase db, String table, String column, String def) {
        try { db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + def); } catch (Exception ignored) {}
    }

    public long addLoan(String name, String dni, String phone, String business, String address,
                        double originalAmount, double totalFinanced, double installment,
                        int installmentCount, String frequency, double periodInterest,
                        String dueDate, double dailyInterest, String guarantor, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("dni", dni); v.put("phone", phone); v.put("business", business);
        v.put("address", address); v.put("original_amount", originalAmount); v.put("total_financed", totalFinanced);
        v.put("balance", totalFinanced); v.put("installment", installment); v.put("installment_count", installmentCount);
        v.put("paid_installments", 0); v.put("frequency", frequency); v.put("period_interest", periodInterest);
        v.put("due_date", dueDate); v.put("daily_interest", dailyInterest); v.put("guarantor", guarantor);
        v.put("notes", notes); v.put("status", "ACTIVO"); v.put("created_date", LocalDate.now().toString());
        return getWritableDatabase().insert("clients", null, v);
    }

    public List<Client> getClients() {
        List<Client> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,name,dni,phone,business,address,original_amount,total_financed,balance,installment,installment_count,paid_installments,frequency,period_interest,due_date,daily_interest,commitment_date,guarantor,notes,status,created_date FROM clients ORDER BY CASE WHEN status='ACTIVO' THEN 0 ELSE 1 END, due_date ASC", null);
        while (c.moveToNext()) out.add(fromCursor(c));
        c.close();
        return out;
    }

    public Client getClient(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,name,dni,phone,business,address,original_amount,total_financed,balance,installment,installment_count,paid_installments,frequency,period_interest,due_date,daily_interest,commitment_date,guarantor,notes,status,created_date FROM clients WHERE id=?",
                new String[]{String.valueOf(id)});
        Client client = c.moveToFirst() ? fromCursor(c) : null;
        c.close(); return client;
    }

    private Client fromCursor(Cursor c) {
        return new Client(c.getLong(0), safe(c,1), safe(c,2), safe(c,3), safe(c,4), safe(c,5),
                c.getDouble(6), c.getDouble(7), c.getDouble(8), c.getDouble(9), c.getInt(10), c.getInt(11),
                safe(c,12), c.getDouble(13), safe(c,14), c.getDouble(15), safe(c,16), safe(c,17), safe(c,18), safe(c,19), safe(c,20));
    }

    private String safe(Cursor c, int i) { return c.isNull(i) ? "" : c.getString(i); }

    public void setCommitment(long id, String date) {
        ContentValues v = new ContentValues(); v.put("commitment_date", date);
        getWritableDatabase().update("clients", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void registerPayment(Client client, double amount, String note) {
        if (amount <= 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues p = new ContentValues();
            p.put("client_id", client.id); p.put("amount", amount); p.put("payment_date", LocalDate.now().toString());
            p.put("type", amount + 0.01 >= client.installment ? "CUOTA" : "PAGO PARCIAL"); p.put("note", note);
            db.insert("payments", null, p);

            double newBalance = Math.max(0, client.balance - amount);
            ContentValues v = new ContentValues(); v.put("balance", newBalance); v.putNull("commitment_date");
            if (amount + 0.01 >= client.installment) {
                v.put("paid_installments", Math.min(client.installmentCount, client.paidInstallments + 1));
                v.put("due_date", nextDate(LocalDate.parse(client.dueDate), client.frequency).toString());
            }
            if (newBalance <= 0.01) v.put("status", "PAGADO");
            db.update("clients", v, "id=?", new String[]{String.valueOf(client.id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private LocalDate nextDate(LocalDate d, String frequency) {
        if ("Quincenal".equalsIgnoreCase(frequency)) return d.plusDays(15);
        if ("Mensual".equalsIgnoreCase(frequency)) return d.plusMonths(1);
        return d.plusWeeks(1);
    }

    public List<String> getPaymentHistory(long clientId) {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT payment_date,amount,type,note FROM payments WHERE client_id=? ORDER BY id DESC", new String[]{String.valueOf(clientId)});
        while (c.moveToNext()) {
            String row = c.getString(0) + " · " + c.getString(2) + " · " + MessageBuilder.money(c.getDouble(1));
            String note = c.isNull(3) ? "" : c.getString(3);
            if (!note.trim().isEmpty()) row += " · " + note;
            out.add(row);
        }
        c.close(); return out;
    }
}
