package multithreading;

import java.util.LinkedList;
import java.util.Random;

public class Processor
{
	private LinkedList<Integer> list = new LinkedList<>();
	private int maxCapacity = 10;
	
	public synchronized void publish() throws InterruptedException
	{
		while(true)
		{
			if(list.size() == 1)
			{
				wait();
				System.out.println("Publisher waiting...");
			}
			int randomValue = new Random().nextInt(100);
			System.out.println("published value "+randomValue);
			list.add(randomValue);
			System.out.println("List Size after Publishing "+list.size());
			notify();
		}
	}
	
	public synchronized void subscribe() throws InterruptedException
	{
		while(true)
		{
			if(list.isEmpty())
			{
				wait();
				System.out.println("Subscriber waiting...");
			}
			System.out.println("Value Consumed : "+list.removeFirst());
			System.out.println("List Size after Consuming "+list.size());
			notify();
			Thread.sleep(8000);
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
