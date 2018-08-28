package multithreading;

import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

public class Processor
{
	private LinkedList<Integer> list = new LinkedList<>();
	private int maxCapacity = 10000000;
	
	public synchronized void publish() throws InterruptedException
	{
		while(true)
		{
			if(list.size() == maxCapacity)
			{
				System.out.println("Publisher waiting queue size "+list.size());
				System.out.println("Publisher waiting..."+new Timestamp(new Date().getTime()));
				wait();
			}
			int randomValue = new Random().nextInt(100);
			//System.out.println("published value "+randomValue);
			list.add(randomValue);
			//System.out.println("List Size after Publishing "+list.size());
			notify();
		}
	}
	
	public synchronized void subscribe() throws InterruptedException
	{
		while(true)
		{
			if(list.isEmpty())
			{
				System.out.println("Subscriber waiting queue size "+list.size());
				System.out.println("Subscriber waiting..."+new Timestamp(new Date().getTime()));
				wait();
			}
			list.removeFirst();
			//System.out.println("Value Consumed : "+list.removeFirst());
			//System.out.println("List Size after Consuming "+list.size());
			notify();
			//System.out.println("Subscriber finished ");
			//Thread.sleep(2000);
		}
	}
	
	
	public void producer() throws InterruptedException
	{
		int value = 0;
		while(true)
		{
			synchronized (this)
			{
				if(list.size() == maxCapacity)
				{
					wait();
				}
				System.out.println("Producer running "+value);
				list.add(value++);
				notify();
				Thread.sleep(2000);
			}
		}
	}
	
	public void consumer () throws InterruptedException
	{
		while(true)
		{
			synchronized (this)
			{
				if(list.size() == 0)
				{
					wait();
				}
				
				int value = list.removeFirst();
				System.out.println("Consumer consumed "+value);
				notify();
				Thread.sleep(2000);
			}
		}
	}
}
