package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array

interface IHasTurnEffect
{
	val onTurnEffects: Array<AbstractMonsterAbility<*>>
}