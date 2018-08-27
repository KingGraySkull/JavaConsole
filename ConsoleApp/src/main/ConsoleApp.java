package main;

import multithreading.Processor;

public class ConsoleApp
{
	public static void main(String[] args)
	{
		final Processor processor = new Processor();
		Thread t1 = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					processor.publish();
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		t1.start();
		
		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run()
			{
				try
				{
					processor.subscribe();
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		t2.start();

	}
}
