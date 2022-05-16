package uk.ncl.CSC8016.harrysmith;

import uk.ncl.CSC8016.harrysmith.utils.AtomicBigInteger;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Bank extends BankFacade {

    HashMap<String, Double> customers;
    HashMap<String, Semaphore> customerSem;
    AtomicBigInteger abi;
    ReentrantLock monitor;
    Condition okToTransact;

    public Bank(HashMap<String, Double> userIdToTotalInitialAmount) {
        super(userIdToTotalInitialAmount);
        customers = userIdToTotalInitialAmount;
        customerSem = new HashMap<>();

        Set<String> keys = customers.keySet();
        for(int i =0; i<keys.size(); i++){
            customerSem.put(keys.toArray()[i].toString(), new Semaphore(1));
        }


        abi = new AtomicBigInteger(BigInteger.ZERO);
        monitor = new ReentrantLock();
        okToTransact = monitor.newCondition();
    }

    @Override
    public String StudentID() {
        return "ngb113";
    }

    @Override
    public Optional<TransactionCommands> openTransaction(String userId) {
        if (customers.containsKey(userId)) {
            try {
                customerSem.get(userId).acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return Optional.of(new TransactionCommands() {
                boolean isProcessDone, isProcessAborted, isProcessCommitted;
                double totalLocalOperations;
                BigInteger currentTransactionId;

                {
                    totalLocalOperations = 0;
                    isProcessDone = isProcessAborted = isProcessCommitted = false;
                    currentTransactionId = abi.incrementAndGet();
                }

                @Override
                public BigInteger getTransactionId() {
                    return currentTransactionId;
                }

                @Override
                public double getTentativeTotalAmount() {
                    return customers.get(userId);
                }

                @Override
                public boolean withdrawMoney(double amount) {
                    if ((amount < 0) || (isProcessDone)) {
                        customerSem.get(userId).release();
                        return false;
                    } else {
                        double val = customers.get(userId);
                        if (val >= amount) {
                            totalLocalOperations -= amount;
                            customers.put(userId, val - amount);
                            return true;
                        } else
                            return false;
                    }
                }

                @Override
                public boolean payMoneyToAccount(double amount) {
                    if ((amount < 0) || (isProcessDone)) {
                        return false;
                    } else {
                        double val = customers.get(userId);
                        totalLocalOperations += amount;
                        customers.put(userId, val + amount);
                        return true;
                    }
                }

                @Override
                public void abort() {
                    if (!isProcessDone) {
                        customers.computeIfPresent(userId, (key, oldValue) -> oldValue - totalLocalOperations);
                        isProcessDone = isProcessAborted = true;
                        isProcessCommitted = false;
                        customerSem.get(userId).release();
                    }
                }

                @Override
                public CommitResult commit() {
                    if (!isProcessDone) {
                        isProcessAborted = false;
                        isProcessDone = isProcessCommitted = true;
                    }
                    return null;
                }

                @Override
                public void close() {
                    commit();
                    customerSem.get(userId).release();
                }
            });
        } else
            return Optional.empty();
    }
}
