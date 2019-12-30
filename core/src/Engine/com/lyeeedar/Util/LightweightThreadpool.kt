package com.lyeeedar.Util

import java.util.concurrent.atomic.AtomicInteger

const val allowParkThreads = false
const val disableThreading = true

// ----------------------------------------------------------------------
class ThreadPoolThread(val index: Int)
{
	val lock = java.lang.Object()

	private val jobQueue = kotlin.Array<(()->Unit)?>(10000) { null }
	private var executeIndex = AtomicInteger(0)
	private val queueIndex = AtomicInteger(0)
	private lateinit var thread: Thread

	init
	{
		if (!disableThreading)
		{
			thread = object : Thread()
			{
				override fun run()
				{
					while (true)
					{
						if (allowParkThreads && queueIndex.get() == 0)
						{
							synchronized(lock)
							{
								lock.wait()
							}
						}
						else if (executeIndex.get() >= queueIndex.get())
						{
							yield()
						}
						else
						{
							val execute = executeIndex.getAndIncrement()

							if (execute < queueIndex.get())
							{
								val job = jobQueue[execute]!!
								job.invoke()
							}

							if (allowParkThreads && executeIndex.get() >= queueIndex.get())
							{
								synchronized(lock)
								{
									lock.notifyAll()
								}
							}
						}
					}
				}
			}
			thread.name = "LightweightThreadPool $index"
			thread.start()
		}
	}

	fun addJob(job: ()->Unit)
	{
		jobQueue[queueIndex.get()] = job
		queueIndex.getAndIncrement()

		if (allowParkThreads)
		{
			synchronized(lock)
			{
				lock.notifyAll()
			}
		}
	}

	fun awaitAllJobs()
	{
		// Help with executing the current jobs instead of just sleeping
		while (executeIndex.get() < queueIndex.get())
		{
			val execute = executeIndex.getAndIncrement()

			if (execute < queueIndex.get())
			{
				val job = jobQueue[execute]!!
				job.invoke()
			}
		}

		queueIndex.set(0)
		executeIndex.set(0)
	}

	fun isFull() = queueIndex.get() == jobQueue.size
}

// ----------------------------------------------------------------------
/**
 * Assumes all tasks are roughly equal cost to compute
 * Assumes all tasks will be enqueued from a single thread
 */
// ----------------------------------------------------------------------
class LightweightThreadpool(val numThreads: Int)
{
	private var queueIndex = 0
	private val threads: kotlin.Array<ThreadPoolThread> = kotlin.Array<ThreadPoolThread>(numThreads) { i -> ThreadPoolThread(i) }

	fun addJob(job: ()->Unit)
	{
		if (disableThreading)
		{
			job.invoke()
			return
		}

		val start = queueIndex
		while (true)
		{
			val thread = threads[queueIndex]
			queueIndex = (queueIndex + 1).rem(numThreads)

			if (thread.isFull())
			{
				if (queueIndex == start)
				{
					thread.awaitAllJobs()
					thread.addJob(job)
					break
				}
			}
			else
			{
				thread.addJob(job)
				break
			}
		}
	}

	fun awaitAllJobs()
	{
		if (disableThreading)
		{
			return
		}

		for (i in 0 until numThreads)
		{
			val thread = threads[i]
			thread.awaitAllJobs()
		}
	}
}