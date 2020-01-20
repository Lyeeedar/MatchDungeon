package com.lyeeedar

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.lyeeedar.Util.IPerformanceTracer
import com.lyeeedar.Util.ITrace

class AndroidPerformanceTracer : IPerformanceTracer
{
	override fun getTrace(name: String): ITrace
	{
		val trace = FirebasePerformance.getInstance().newTrace(name)

		return AndroidTrace(trace)
	}
}

class AndroidTrace(val trace: Trace) : ITrace
{
	override fun start()
	{
		trace.start()
	}

	override fun stop()
	{
		trace.stop()
	}

}