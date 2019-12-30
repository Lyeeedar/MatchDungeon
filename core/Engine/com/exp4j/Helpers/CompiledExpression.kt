package com.exp4j.Helpers

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectFloatMap
import com.exp4j.Functions.*
import com.exp4j.Operators.BooleanOperators
import com.exp4j.Operators.PercentageOperator
import com.lyeeedar.Util.Random
import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder

class CompiledExpression(private val eqnStr: String, variableMap: ObjectFloatMap<String> = ObjectFloatMap())
{
	private val cachedValue: Float?
	private val expression: Expression?
	private val seededFunctions = Array<SeededFunction>(3)

	init
	{
		cachedValue = try
		{
			eqnStr.toFloat()
		}
		catch (ex: Exception)
		{
			null
		}

		if (cachedValue == null)
		{
			val expB = ExpressionBuilder(eqnStr.unescapeCharacters())
			expB.exceptionOnMissingVariables = false

			BooleanOperators.applyOperators(expB)
			expB.operator(PercentageOperator.operator)

			val randomFun = RandomFunction.obtain()
			val chanceFun = ChanceFunction.obtain()
			val probFun = ProbabilityFunction.obtain()
			val signFun = SignFunction.obtain()
			seededFunctions.add(randomFun)
			seededFunctions.add(chanceFun)
			seededFunctions.add(probFun)
			seededFunctions.add(signFun)

			for (func in seededFunctions) expB.function(func)
			MathUtilFunctions.applyFunctions(expB)

			for (key in variableMap.keys())
			{
				expB.variable(key)
			}
			expB.variable("null")

			expression = expB.build()
		}
		else
		{
			expression = null
		}
	}

	fun evaluate(variableMap: ObjectFloatMap<String> = ObjectFloatMap(), seed: Long? = null): Float
	{
		if (cachedValue != null)
		{
			return cachedValue
		}
		else if (expression != null)
		{
			val seed = seed ?: Random.random.nextLong()
			for (func in seededFunctions)
			{
				func.set(seed)
			}

			val valuesToBeSet = expression.variableNames

			for (pair in variableMap)
			{
				expression.setVariable(pair.key, pair.value.toDouble())
				valuesToBeSet.remove(pair.key)
			}

			for (variable in valuesToBeSet)
			{
				expression.setVariable(variable, 0.0)
			}
			expression.setVariable("null", 0.0)

			return expression.evaluate().toFloat()
		}
		else
		{
			throw Exception("Invalid expression '$eqnStr'")
		}
	}

	fun free()
	{
		for (func in seededFunctions)
		{
			func.free()
		}
	}
}