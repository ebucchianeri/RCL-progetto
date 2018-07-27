package impiccato;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LockAttesaAck { 
	final ReentrantLock lock = new ReentrantLock();
	final Condition inizio  = lock.newCondition(); 
	boolean ricevutoack;
	boolean timer;
	boolean chiusurapartita;

	public LockAttesaAck(){
		ricevutoack = false;
		timer = false;
		chiusurapartita = false;
	}
	
	public void  w () throws InterruptedException {
	    while (ricevutoack == false && timer == false && chiusurapartita == false) {
	    	lock.lock();
	        boolean i = inizio.await(3, TimeUnit.SECONDS);
	        //    false if the waiting time detectably elapsed before return from the method, else true
	        if( i == false) timer = true;
	        lock.unlock();
	    }
	}
	
	
	
	public void  ricevutoAck () throws InterruptedException {
		lock.lock();
		ricevutoack = true;
		System.out.println("RICEVUTO ACK!!!!");
	    inizio.signalAll(); 
	    lock.unlock();
	}
	
	public void  finePartita () throws InterruptedException {
		lock.lock();
		ricevutoack = false;
		timer = false;
		chiusurapartita = true;
	    inizio.signalAll(); 
	    lock.unlock();
	}
	
	public void  resetLockAttesaAck () {
		ricevutoack = false;
		timer = false;
	}
}