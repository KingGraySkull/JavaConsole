package main;

import multithreading.Processor;

public class ConsoleApp
{
	private static final Processor processor = new Processor();
	
	public static void main(String[] args)
	{
		Thread t1 = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					processor.publish();
				} 
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
		});
		
		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					processor.subscribe();
				} 
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
		});
		
		t1.start();
		t2.start();
	}
}
