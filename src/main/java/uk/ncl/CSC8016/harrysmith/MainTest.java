package uk.ncl.CSC8016.harrysmith;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class MainTest {

    public static void main(String[] args) throws InterruptedException {

        HashMap<String, Double> customers = new HashMap<>();
        customers.put("Tim", 1000.00);
        customers.put("Bob", 1000.00);

        HashMap<String, Semaphore> customerThreadControl = new HashMap<>();
        customerThreadControl.put("Tim", new Semaphore(1));
        customerThreadControl.put("Bob", new Semaphore(1));

        Bank bank = new Bank(customers);

        Thread thread1 = new Thread(() -> {
            var obj = bank.openTransaction("Tim");
            var tc = obj.get();
            tc.withdrawMoney(100);
            tc.withdrawMoney(200);
            tc.withdrawMoney(150);
            tc.abort();
            //System.out.println(customers.get("Tim"));
        });

        Thread thread2 = new Thread(() -> {
            var obj = bank.openTransaction("Tim");
            var tc = obj.get();
            tc.withdrawMoney(400);
            tc.withdrawMoney(500);
            tc.withdrawMoney(600);
            tc.close();
            //System.out.println(customers.get("Tim"));
        });

        Thread thread3 = new Thread(() -> {
            var obj = bank.openTransaction("Tim");
            var tc = obj.get();
            tc.payMoneyToAccount(600);
            tc.payMoneyToAccount(200);
            tc.payMoneyToAccount(217);
            tc.close();
            //System.out.println(customers.get("Tim"));
        });

        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread2.join();
        thread3.join();

        System.out.println(customers.get("Tim"));

    }

}
